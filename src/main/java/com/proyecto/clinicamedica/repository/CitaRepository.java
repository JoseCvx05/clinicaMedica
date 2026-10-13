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
 * - Disponibilidad.
 * - Cancelación de reservas expiradas.
 *
 * CU-04:
 * - Pago.
 *
 * CU-05:
 * - Recepción.
 *
 * CU-06:
 * - Caja.
 *
 * CU-07:
 * - Enfermería.
 *
 * CU-08:
 * - Panel del Médico.
 * - Inicio de consulta.
 * - No asistió.
 * - Finalización de atención.
 *
 * =========================================================
 */
@Repository
public interface CitaRepository
        extends JpaRepository<Cita, Integer> {


    // =====================================================
    // CU-03 - SOLAPAMIENTO
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

            WHERE LOWER(c.estadoCita.nombre)
                  = 'pendiente de pago'

              AND LOWER(TRIM(c.canalOrigen))
                  = 'portal web'

              AND c.fechaExpiracionPago IS NOT NULL

              AND c.fechaExpiracionPago <= :ahora

              AND NOT EXISTS (
                    SELECT p.id
                    FROM Pago p
                    WHERE p.cita = c
                      AND UPPER(p.estado)
                          IN ('PROCESANDO', 'APROBADO')
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
    // CU-04 - BLOQUEO DURANTE PAGO
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
    // CITAS DEL PACIENTE
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
    // CU-05 - CITA POR NÚMERO
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
            """)
    Optional<Cita> buscarParaRecepcionPorNumero(

            @Param("idCita")
            Integer idCita
    );


    // =====================================================
    // CU-05 - CITAS ACTIVAS DEL PACIENTE
    // =====================================================

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
    // CU-06 - CITA PENDIENTE POR NÚMERO
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
    // CU-06 - BLOQUEO DURANTE COBRO
    // =====================================================

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
    // CU-07 - PANEL DE ENFERMERÍA
    // =====================================================

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
    // CU-07 - BLOQUEO AL REGISTRAR SIGNOS
    // =====================================================

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


    // =====================================================
    // CU-08 - PANEL MÉDICO / EN ESPERA
    // =====================================================

    /**
     * Devuelve únicamente las citas:
     *
     * - asignadas al Médico autenticado;
     * - en estado En Espera;
     * - con signos vitales registrados.
     *
     * Las emergencias aparecen primero.
     */
    @Query("""
            SELECT c
            FROM Cita c

            JOIN FETCH c.paciente
            JOIN FETCH c.medico
            JOIN FETCH c.sucursal
            JOIN FETCH c.especialidad
            JOIN FETCH c.estadoCita

            WHERE c.medico.id = :idMedico

              AND LOWER(c.estadoCita.nombre)
                  = LOWER(:estado)

              AND EXISTS (
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
    List<Cita> buscarParaPanelMedicoEnEspera(

            @Param("idMedico")
            Integer idMedico,

            @Param("estado")
            String estado
    );


    // =====================================================
    // CU-08 - BLOQUEO PARA OPERACIONES MÉDICAS
    // =====================================================

    /**
     * Recupera y bloquea una cita únicamente cuando
     * pertenece al Médico indicado.
     *
     * Se utilizará para:
     *
     * - Iniciar consulta.
     * - Marcar No Asistió.
     * - Finalizar atención.
     *
     * El Service revalidará el estado permitido
     * según la operación que se esté ejecutando.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM Cita c

            JOIN FETCH c.paciente
            JOIN FETCH c.medico
            JOIN FETCH c.sucursal
            JOIN FETCH c.especialidad
            JOIN FETCH c.estadoCita

            WHERE c.id = :idCita

              AND c.medico.id = :idMedico
            """)
    Optional<Cita> buscarParaOperacionMedicaConBloqueo(

            @Param("idCita")
            Integer idCita,

            @Param("idMedico")
            Integer idMedico
    );

    // =====================================================
// CU-08 FA02 / RN-CU11-05
// CITAS DE SEGUIMIENTO PARA RECORDATORIO
// =====================================================

    @Query("""
        SELECT DISTINCT c
        FROM Cita c

        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        WHERE c.esSeguimiento = true

          AND c.fechaHoraCita >= :inicio
          AND c.fechaHoraCita < :fin

          
          AND LOWER(TRIM(c.estadoCita.nombre))
              IN ('pagada', 'confirmada')
          

        ORDER BY c.fechaHoraCita ASC
        """)
    List<Cita> buscarSeguimientosParaRecordatorio(

            @Param("inicio")
            OffsetDateTime inicio,

            @Param("fin")
            OffsetDateTime fin
    );
}