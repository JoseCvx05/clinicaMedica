package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.proyecto.clinicamedica.entity.Pago;
import java.time.OffsetDateTime;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: CITA
 * =========================================================
 *
 * CU-03:
 *
 * - Disponibilidad de horarios.
 * - Cancelación de citas pendientes de pago expiradas.
 *
 * CU-04:
 *
 * - Obtener una cita perteneciente al paciente autenticado.
 * - Obtener datos para la pantalla de pago.
 * - Bloquear la cita durante el procesamiento del pago.
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
    // CU-03 - CANCELAR PAGOS EXPIRADOS
    // =====================================================

    @Modifying
    @Query("""
    UPDATE Cita c

    SET c.estadoCita = :estadoCancelada,
        c.fechaModificacion = :ahora

    WHERE LOWER(c.estadoCita.nombre) = 'pendiente de pago'

      AND c.fechaExpiracionPago IS NOT NULL

      AND c.fechaExpiracionPago <= :ahora

      AND NOT EXISTS (
            SELECT p.id
            FROM Pago p
            WHERE p.cita = c
              AND p.estado = 'PROCESANDO'
      )
    """)
    int cancelarPendientesDePagoExpiradas(
            @Param("estadoCancelada")
            EstadoCita estadoCancelada,

            @Param("ahora")
            OffsetDateTime ahora
    );


    // =====================================================
    // CU-04 - CARGAR CITA PARA PANTALLA DE PAGO
    // =====================================================

    /**
     * Obtiene la cita únicamente si pertenece al paciente
     * autenticado.
     *
     * Los JOIN FETCH permiten construir el resumen de pago
     * sin depender de Open Session in View.
     *
     * Protege además contra IDOR:
     *
     * un paciente no puede consultar la cita de otro
     * simplemente cambiando el ID en la URL.
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
          AND c.paciente.id = :idPaciente
        """)
    Optional<Cita> buscarParaPago(
            @Param("idCita")
            Integer idCita,

            @Param("idPaciente")
            Integer idPaciente
    );


    // =====================================================
    // CU-04 - BLOQUEAR CITA DURANTE EL PAGO
    // =====================================================

    /**
     * Obtiene la cita aplicando PESSIMISTIC_WRITE.
     *
     * Se utilizará exclusivamente durante la sección
     * crítica del procesamiento del pago.
     *
     * Ejemplo:
     *
     * Request A
     *      ↓
     * bloquea cita
     *
     * Request B
     *      ↓
     * debe esperar
     *
     * Cuando B pueda continuar, el servicio volverá
     * a comprobar el estado de la cita y el pago.
     */
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
}