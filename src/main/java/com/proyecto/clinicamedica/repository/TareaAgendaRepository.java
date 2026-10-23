
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.TareaAgenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * CU-16 - REPOSITORY: TAREA DE AGENDA MÉDICA
 * =========================================================
 *
 * FA06 - GESTIÓN DE TAREAS (TASKPANEL).
 *
 * Responsabilidades:
 *
 * - Consultar tareas del Médico por fecha.
 * - Filtrar tareas pendientes.
 * - Filtrar tareas completadas.
 * - Consultar todas las tareas del día.
 * - Contar las tareas del día seleccionado.
 * - Buscar una tarea por ID y Médico propietario.
 *
 * SEGURIDAD:
 *
 * Las consultas incluyen el ID del Médico.
 *
 * El Service debe obtener ese ID del usuario
 * autenticado, nunca confiar en un ID enviado
 * libremente desde el navegador.
 *
 * IMPORTANTE:
 *
 * - No modifica citas médicas.
 * - No modifica eventos de agenda.
 * - No afecta disponibilidad del CU-03.
 * - No permite acceder a tareas de otro Médico
 *   mediante las consultas específicas.
 *
 * =========================================================
 */
@Repository
public interface TareaAgendaRepository
        extends JpaRepository<TareaAgenda, Integer> {

    // =====================================================
    // 1. LISTAR TODAS LAS TAREAS DEL DÍA
    // =====================================================
    //
    // Se muestran:
    // - Pendientes primero.
    // - Completadas después.
    // - Prioridad alta primero.
    //
    // =====================================================

    @Query("""
            SELECT t
            FROM TareaAgenda t

            WHERE t.medico.id = :idMedico

              AND t.fechaLimite = :fecha

            ORDER BY
                t.isCompleted ASC,
                t.prioridad DESC,
                t.id ASC
            """)
    List<TareaAgenda> listarTareasDelDia(

            @Param("idMedico")
            Integer idMedico,

            @Param("fecha")
            LocalDate fecha
    );

    // =====================================================
    // 2. FILTRAR TAREAS POR ESTADO
    // =====================================================
    //
    // false = Pendientes.
    // true  = Completadas.
    //
    // =====================================================

    @Query("""
            SELECT t
            FROM TareaAgenda t

            WHERE t.medico.id = :idMedico

              AND t.fechaLimite = :fecha

              AND t.isCompleted = :completada

            ORDER BY
                t.prioridad DESC,
                t.id ASC
            """)
    List<TareaAgenda> listarTareasPorEstado(

            @Param("idMedico")
            Integer idMedico,

            @Param("fecha")
            LocalDate fecha,

            @Param("completada")
            Boolean completada
    );

    // =====================================================
    // 3. CONTAR TAREAS DEL DÍA
    // =====================================================
    //
    // Utilizado para el contador del TaskPanel.
    //
    // =====================================================

    @Query("""
            SELECT COUNT(t)
            FROM TareaAgenda t

            WHERE t.medico.id = :idMedico

              AND t.fechaLimite = :fecha
            """)
    long contarTareasDelDia(

            @Param("idMedico")
            Integer idMedico,

            @Param("fecha")
            LocalDate fecha
    );

    // =====================================================
    // 4. BUSCAR TAREA DEL MÉDICO AUTENTICADO
    // =====================================================
    //
    // Permite comprobar la propiedad de una tarea
    // antes de completar, descompletar o eliminar.
    //
    // No basta con conocer el ID de la tarea.
    //
    // =====================================================

    @Query("""
            SELECT t
            FROM TareaAgenda t

            WHERE t.id = :idTarea

              AND t.medico.id = :idMedico
            """)
    Optional<TareaAgenda> buscarTareaPropia(

            @Param("idTarea")
            Integer idTarea,

            @Param("idMedico")
            Integer idMedico
    );
}
