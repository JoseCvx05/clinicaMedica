package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: CITA
 * =========================================================
 *
 * CU-03:
 * - Disponibilidad de horarios.
 * - Cancelación de citas pendientes de pago expiradas.
 *
 * CU-04:
 * - Consulta de citas para pago.
 * - Bloqueo durante procesamiento de pago.
 *
 * CU-05:
 * - Búsqueda para recepción.
 * - Búsqueda de citas activas por paciente.
 * - Bloqueo durante registro de llegada.
 *
 * =========================================================
 */
@Repository
public interface CitaRepository
        extends JpaRepository<Cita, Integer> {


    // =====================================================
    // CU-03 - ¿EXISTE CITA QUE SE SOLAPA?
    // =====================================================

@Query("""
        SELECT COUNT(c) > 0
        FROM Cita c

        WHERE c.medico.id = :idMedico

          AND LOWER(c.estadoCita.nombre) <> 'cancelada'

          AND (
                LOWER(c.estadoCita.nombre) <> 'pendiente de pago'

                OR

                (
                    LOWER(c.estadoCita.nombre) = 'pendiente de pago'

                    AND (
                        c.fechaExpiracionPago IS NULL
                        OR c.fechaExpiracionPago > :ahora
                    )
                )
              )

          AND (
                (
                    c.fechaHoraFin IS NOT NULL
                    AND c.fechaHoraCita < :fin
                    AND c.fechaHoraFin > :inicio
                )

                OR

                (
                    c.fechaHoraFin IS NULL
                    AND c.fechaHoraCita >= :inicio
                    AND c.fechaHoraCita < :fin
                )
              )
        """)
    boolean existeCitaSolapada(

            @Param("idMedico")
            Integer idMedico,

            @Param("inicio")
            OffsetDateTime inicio,

            @Param("fin")
            OffsetDateTime fin,

            @Param("ahora")
            OffsetDateTime ahora
    );


    // =====================================================
// CU-03 / CU-06 - CANCELAR PAGOS EXPIRADOS
// =====================================================

    @Modifying
    @Query("""
    UPDATE Cita c

    SET c.estadoCita = :estadoCancelada,
        c.fechaModificacion = :ahora

    WHERE LOWER(c.estadoCita.nombre) = 'pendiente de pago'

      AND LOWER(TRIM(c.canalOrigen)) = 'portal web'

      AND c.fechaExpiracionPago IS NOT NULL

      AND c.fechaExpiracionPago <= :ahora

      AND NOT EXISTS (
            SELECT p.id
            FROM Pago p
            WHERE p.cita = c
              AND UPPER(p.estado) IN ('PROCESANDO', 'APROBADO')
      )
    """)
    int cancelarPendientesDePagoExpiradas(

            @Param("estadoCancelada")
            EstadoCita estadoCancelada,

            @Param("ahora")
            OffsetDateTime ahora
    );


    // =====================================================
    // CU-04 - CARGAR CITA PARA PAGO
    // =====================================================

    @Query("""
        SELECT c
        FROM Cita c

        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        WHERE c.id = :idCita
          AND c.paciente.id = :idPaciente
        """)
    Optional<Cita> buscarParaPago(

            @Param("idCita")
            Integer idCita,

            @Param("idPaciente")
            Integer idPaciente
    );


    // =====================================================
    // CU-04 - BLOQUEAR CITA DURANTE PAGO
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Cita c

        WHERE c.id = :idCita
          AND c.paciente.id = :idPaciente
        """)
    Optional<Cita> buscarParaPagoConBloqueo(

            @Param("idCita")
            Integer idCita,

            @Param("idPaciente")
            Integer idPaciente
    );


    // =====================================================
    // CITAS DEL PACIENTE AUTENTICADO
    // =====================================================

    @Query("""
        SELECT c
        FROM Cita c

        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        WHERE c.paciente.id = :idPaciente

        ORDER BY c.fechaHoraCita DESC
        """)
    List<Cita> buscarCitasDelPaciente(

            @Param("idPaciente")
            Integer idPaciente
    );


    // =====================================================
    // CU-05 - BUSCAR CITA POR NÚMERO
    // =====================================================

    /**
     * Incluye cualquier estado porque recepción debe
     * distinguir, entre otros:
     *
     * - Pendiente de pago.
     * - Pagada.
     * - Confirmada.
     * - Cancelada.
     * - Paciente Presente.
     */
    @Query("""
        SELECT c
        FROM Cita c

        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        WHERE c.id = :idCita
        """)
    Optional<Cita> buscarParaRecepcionPorNumero(

            @Param("idCita")
            Integer idCita
    );


    // =====================================================
    // CU-05 - CITAS ACTIVAS POR PACIENTE
    // =====================================================

    /**
     * Se utiliza después de localizar al paciente mediante
     * dpi_hash.
     *
     * Para recepción únicamente se consideran estados que
     * todavía requieren alguna acción dentro del flujo:
     *
     * - Pendiente de pago.
     * - Pagada.
     * - Confirmada.
     * - Paciente Presente.
     *
     * Orden:
     *
     * 1. Si existe una cita "Paciente Presente", tiene
     *    prioridad inmediata.
     *
     * 2. Después se selecciona la próxima cita futura.
     *
     * 3. Finalmente se consideran citas pasadas todavía
     *    activas, comenzando por la más reciente.
     *
     * Esto evita seleccionar arbitrariamente citas mediante
     * citas.get(0).
     */
    @Query("""
        SELECT c
        FROM Cita c

        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        WHERE c.paciente.id = :idPaciente

          AND LOWER(c.estadoCita.nombre) IN (
                'pendiente de pago',
                'pagada',
                'confirmada',
                'paciente presente'
          )

        ORDER BY

            CASE
                WHEN LOWER(c.estadoCita.nombre)
                     = 'paciente presente'
                THEN 0
                ELSE 1
            END ASC,

            CASE
                WHEN c.fechaHoraCita >= :ahora
                THEN 0
                ELSE 1
            END ASC,

            CASE
                WHEN c.fechaHoraCita >= :ahora
                THEN c.fechaHoraCita
                ELSE NULL
            END ASC,

            CASE
                WHEN c.fechaHoraCita < :ahora
                THEN c.fechaHoraCita
                ELSE NULL
            END DESC,

            c.id DESC
        """)
    List<Cita> buscarCitasActivasParaRecepcion(

            @Param("idPaciente")
            Integer idPaciente,

            @Param("ahora")
            OffsetDateTime ahora
    );


    // =====================================================
    // CU-05 - BLOQUEO PARA REGISTRAR LLEGADA
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Cita c

        JOIN FETCH c.paciente
        JOIN FETCH c.estadoCita

        WHERE c.id = :idCita
        """)
    Optional<Cita> buscarParaRegistrarLlegadaConBloqueo(

            @Param("idCita")
            Integer idCita
    );
    // =====================================================
// CU-06 - CITA PENDIENTE DE PAGO POR NÚMERO
// =====================================================

    /**
     * Busca una cita específica únicamente cuando todavía
     * se encuentra Pendiente de pago.
     *
     * Se cargan todas las relaciones necesarias para:
     *
     * - mostrar el resumen en Caja;
     * - obtener el precio;
     * - generar posteriormente el comprobante.
     */
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.medico
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.estadoCita

    WHERE c.id = :idCita

      AND LOWER(c.estadoCita.nombre)
          = 'pendiente de pago'
    """)
    Optional<Cita> buscarPendienteDePagoParaCajaPorNumero(
            @Param("idCita")
            Integer idCita
    );


// =====================================================
// CU-06 - CITAS PENDIENTES POR PACIENTE
// =====================================================

    /**
     * Busca únicamente citas Pendiente de pago del paciente.
     *
     * Prioridad:
     *
     * 1. Próxima cita futura.
     * 2. Si no hay futuras, cita pasada más reciente
     *    que todavía continúe Pendiente de pago.
     */
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.medico
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.estadoCita

    WHERE c.paciente.id = :idPaciente

      AND LOWER(c.estadoCita.nombre)
          = 'pendiente de pago'

    ORDER BY

        CASE
            WHEN c.fechaHoraCita >= :ahora
            THEN 0
            ELSE 1
        END ASC,

        CASE
            WHEN c.fechaHoraCita >= :ahora
            THEN c.fechaHoraCita
            ELSE NULL
        END ASC,

        CASE
            WHEN c.fechaHoraCita < :ahora
            THEN c.fechaHoraCita
            ELSE NULL
        END DESC,

        c.id DESC
    """)
    List<Cita> buscarPendientesDePagoParaCajaPorPaciente(

            @Param("idPaciente")
            Integer idPaciente,

            @Param("ahora")
            OffsetDateTime ahora
    );


// =====================================================
// CU-06 - BLOQUEAR CITA DURANTE EL COBRO
// =====================================================

    /**
     * Se utilizará posteriormente en la sección crítica
     * del registro del pago.
     *
     * Evita que dos Cajeros cobren simultáneamente
     * la misma cita.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.estadoCita

    WHERE c.id = :idCita
    """)
    Optional<Cita> buscarParaCobroCajaConBloqueo(
            @Param("idCita")
            Integer idCita
    );
    // =====================================================
// CU-07 - PANEL DE ENFERMERÍA POR ESTADO
// =====================================================

    /**
     * Obtiene las citas que deben mostrarse en una sección
     * específica del panel de Enfermería.
     *
     * CU-07 utiliza:
     *
     * - Paciente Presente
     * - Signos Vitales
     *
     * Las emergencias indicadas previamente por Recepción
     * aparecen primero.
     */
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.medico
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.estadoCita

    WHERE LOWER(c.estadoCita.nombre)
          = LOWER(:estado)

      AND NOT EXISTS (
            SELECT sv.id
            FROM SignosVitales sv
            WHERE sv.cita = c
      )

    ORDER BY

        CASE
            WHEN LOWER(COALESCE(c.prioridad, ''))
                 = 'emergencia'
            THEN 0
            ELSE 1
        END ASC,

        c.fechaHoraCita ASC,
        c.id ASC
    """)
    List<Cita> buscarParaPanelEnfermeriaPorEstado(
            @Param("estado")
            String estado
    );


// =====================================================
// CU-07 - BLOQUEO AL LLAMAR PACIENTE
// =====================================================

    /**
     * Bloquea la cita durante la transición:
     *
     * Paciente Presente -> Signos Vitales
     *
     * Evita que dos enfermeros llamen simultáneamente
     * al mismo paciente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.estadoCita
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad

    WHERE c.id = :idCita
    """)
    Optional<Cita> buscarParaLlamarEnfermeriaConBloqueo(
            @Param("idCita")
            Integer idCita
    );
    // =====================================================
// CU-07 - BLOQUEO AL REGISTRAR SIGNOS VITALES
// =====================================================

    /**
     * Bloquea la cita durante el registro definitivo
     * de signos vitales.
     *
     * La cita debe ser revalidada posteriormente por
     * SignosVitalesService.
     *
     * Evita que dos enfermeros registren simultáneamente
     * los signos de una misma cita.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT c
    FROM Cita c

    JOIN FETCH c.paciente
    JOIN FETCH c.estadoCita
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad

    WHERE c.id = :idCita
    """)
    Optional<Cita> buscarParaRegistrarSignosVitalesConBloqueo(
            @Param("idCita")
            Integer idCita
    );
}