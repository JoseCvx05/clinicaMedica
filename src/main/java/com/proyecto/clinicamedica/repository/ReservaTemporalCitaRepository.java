package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ReservaTemporalCita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: RESERVA TEMPORAL DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Permite consultar:
 *
 * - reservas por token;
 * - reservas activas de un médico;
 * - reservas expiradas;
 * - reservas activas del paciente.
 *
 * La creación, expiración y liberación pertenecen
 * al Service.
 * =========================================================
 */
@Repository
public interface ReservaTemporalCitaRepository
        extends JpaRepository<ReservaTemporalCita, Integer> {


    // =====================================================
    // TOKEN
    // =====================================================

    Optional<ReservaTemporalCita>
    findByTokenReservaAndActivaTrue(
            String tokenReserva
    );


    // =====================================================
    // RESERVAS ACTIVAS DEL MÉDICO
    // =====================================================

    List<ReservaTemporalCita>
    findByMedico_IdAndActivaTrueAndFechaHoraInicioBetween(
            Integer idMedico,
            OffsetDateTime desde,
            OffsetDateTime hasta
    );


    // =====================================================
    // ¿HORARIO TEMPORALMENTE OCUPADO?
    // =====================================================

    boolean
    existsByMedico_IdAndFechaHoraInicioAndActivaTrue(
            Integer idMedico,
            OffsetDateTime fechaHoraInicio
    );


    // =====================================================
    // RESERVA ACTIVA DEL PACIENTE
    // =====================================================

    Optional<ReservaTemporalCita>
    findFirstByPaciente_IdAndActivaTrueOrderByFechaCreacionDesc(
            Integer idPaciente
    );


    // =====================================================
    // RESERVAS EXPIRADAS
    // =====================================================

    List<ReservaTemporalCita>
    findTop100ByActivaTrueAndFechaExpiracionLessThanEqualOrderByFechaExpiracionAsc(
            OffsetDateTime ahora
    );

    List<ReservaTemporalCita>
    findByPaciente_IdAndActivaTrue(
            Integer idPaciente
    );

    Optional<ReservaTemporalCita>
    findByTokenReserva(
            String tokenReserva
    );

    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservaTemporalCita r
        WHERE r.medico.id = :idMedico
          AND r.activa = true
          AND r.fechaExpiracion > :ahora
          AND r.fechaHoraInicio < :fin
          AND r.fechaHoraFin > :inicio
        """)
    boolean existeReservaActivaSolapada(
            @Param("idMedico")
            Integer idMedico,

            @Param("inicio")
            OffsetDateTime inicio,

            @Param("fin")
            OffsetDateTime fin,

            @Param("ahora")
            OffsetDateTime ahora
    );
}