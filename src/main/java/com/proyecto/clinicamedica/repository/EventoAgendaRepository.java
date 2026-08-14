package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.EventoAgenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * REPOSITORY: EVENTO DE AGENDA
 * =========================================================
 *
 * Detecta eventos del médico que se cruzan con
 * un intervalo candidato para una cita.
 * =========================================================
 */
@Repository
public interface EventoAgendaRepository
        extends JpaRepository<EventoAgenda, Integer> {


    @Query("""
            SELECT COUNT(e) > 0
            FROM EventoAgenda e
            WHERE e.medico.id = :idMedico
              AND e.fechaInicio < :fin
              AND e.fechaFin > :inicio
            """)
    boolean existeEventoSolapado(
            @Param("idMedico")
            Integer idMedico,

            @Param("inicio")
            OffsetDateTime inicio,

            @Param("fin")
            OffsetDateTime fin
    );
}