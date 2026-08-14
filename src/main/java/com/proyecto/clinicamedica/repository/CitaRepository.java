package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Cita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.proyecto.clinicamedica.entity.EstadoCita;

import org.springframework.data.jpa.repository.Modifying;
import java.time.OffsetDateTime;


/**
 * =========================================================
 * REPOSITORY: CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Acceso a las citas registradas.
 *
 * La lógica para decidir si un horario está ocupado
 * permanece fuera del Repository.
 * =========================================================
 */
@Repository
public interface CitaRepository
        extends JpaRepository<Cita, Integer> {


    // =====================================================
    // ¿EXISTE CITA QUE SE SOLAPA?
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
    @Modifying
    @Query("""
        UPDATE Cita c

        SET c.estadoCita = :estadoCancelada,
            c.fechaModificacion = :ahora

        WHERE LOWER(c.estadoCita.nombre) = 'pendiente de pago'

          AND c.fechaExpiracionPago IS NOT NULL

          AND c.fechaExpiracionPago <= :ahora
        """)
    int cancelarPendientesDePagoExpiradas(
            @Param("estadoCancelada")
            EstadoCita estadoCancelada,

            @Param("ahora")
            OffsetDateTime ahora
    );
}