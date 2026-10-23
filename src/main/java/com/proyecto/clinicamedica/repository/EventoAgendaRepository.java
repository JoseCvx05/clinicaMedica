package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.EventoAgenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

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


    // =====================================================
    // CU-16 - GESTIÓN DE AGENDA MÉDICA
    // CONSULTAR EVENTOS DEL MÉDICO PARA EL CALENDARIO
    // =====================================================
    //
    // FLUJO NORMAL:
    // - Mostrar eventos personales del médico.
    // - Calendario mensual, semanal y diario.
    // - Mostrar eventos junto con citas.
    //
    // SEGURIDAD:
    // - Solo devuelve eventos del médico indicado.
    // - El Service obtendrá el ID del médico
    //   directamente de su sesión autenticada.
    //
    // FECHAS:
    // - Inicio del rango inclusivo.
    // - Fin del rango exclusivo.
    // - Incluye eventos que comienzan antes
    //   del rango y terminan dentro de él.
    // - Incluye eventos de varios días.
    //
    // IMPORTANTE:
    // - No modifica eventos.
    // - No modifica citas.
    // - No altera disponibilidad del CU-03.
    // =====================================================

    @Query("""
            SELECT e
            FROM EventoAgenda e

            WHERE e.medico.id = :idMedico

              AND e.fechaInicio < :hasta

              AND e.fechaFin > :desde

            ORDER BY
                e.fechaInicio ASC,
                e.id ASC
            """)
    List<EventoAgenda> buscarEventosParaAgendaMedica(

            @Param("idMedico")
            Integer idMedico,

            @Param("desde")
            OffsetDateTime desde,

            @Param("hasta")
            OffsetDateTime hasta
    );

}