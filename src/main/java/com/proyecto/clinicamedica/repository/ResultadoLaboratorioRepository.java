package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ResultadoLaboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: RESULTADO DE LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Permite:
 *
 * - Buscar el resultado de un examen específico.
 * - Evitar resultados duplicados.
 * - Listar resultados de una orden.
 * - Listar únicamente resultados publicados.
 * - Contar resultados registrados.
 * - Contar resultados publicados.
 *
 * =========================================================
 */
@Repository
public interface ResultadoLaboratorioRepository
        extends JpaRepository<ResultadoLaboratorio, Integer> {


    // =====================================================
    // RESULTADO POR DETALLE
    // =====================================================
    //
    // Cada DetalleOrdenLaboratorio puede tener solamente
    // un resultado.
    //
    // La BD también posee UNIQUE sobre:
    //
    // id_detalle_orden_laboratorio
    //
    // =====================================================

    Optional<ResultadoLaboratorio>
    findByDetalleOrdenLaboratorio_Id(
            Integer idDetalleOrdenLaboratorio
    );


    // =====================================================
    // EXISTENCIA
    // =====================================================

    boolean existsByDetalleOrdenLaboratorio_Id(
            Integer idDetalleOrdenLaboratorio
    );


    // =====================================================
    // RESULTADOS DE UNA ORDEN
    // =====================================================
    //
    // Relación:
    //
    // ResultadoLaboratorio
    //        ↓
    // DetalleOrdenLaboratorio
    //        ↓
    // OrdenLaboratorio
    //
    // =====================================================

    List<ResultadoLaboratorio>
    findByDetalleOrdenLaboratorio_OrdenLaboratorio_IdOrderByDetalleOrdenLaboratorio_NombreExamenAsc(
            Integer idOrdenLaboratorio
    );


    // =====================================================
    // RESULTADOS PUBLICADOS
    // =====================================================
    //
    // Será utilizado posteriormente para que el Médico
    // vea únicamente resultados que ya fueron publicados.
    //
    // Un resultado simplemente guardado o validado
    // todavía NO debe ser visible para el Médico.
    //
    // =====================================================

    List<ResultadoLaboratorio>
    findByDetalleOrdenLaboratorio_OrdenLaboratorio_IdAndPublicadoTrueOrderByDetalleOrdenLaboratorio_NombreExamenAsc(
            Integer idOrdenLaboratorio
    );

    // =====================================================
// CU-09
// RESULTADOS PUBLICADOS PARA EL MÉDICO POR CITA
// =====================================================
//
// Seguridad:
//
// - Solo devuelve resultados publicados.
// - La cita debe pertenecer al Médico indicado.
// - Permite recuperar resultados de una o varias
//   órdenes asociadas a la misma cita.
//
// No devuelve:
//
// - Resultados únicamente guardados.
// - Resultados pendientes de validación.
// - Resultados validados pero no publicados.
//
// =====================================================

    @Query("""
        SELECT r
        FROM ResultadoLaboratorio r

        JOIN FETCH r.detalleOrdenLaboratorio d
        JOIN FETCH d.ordenLaboratorio o

        WHERE o.cita.id = :idCita

          AND o.medico.id = :idMedico

          AND r.publicado = true

        ORDER BY
            o.fechaHoraCreacion DESC,
            d.nombreExamen ASC
        """)
    List<ResultadoLaboratorio>
    listarPublicadosParaMedicoPorCita(

            @Param("idCita")
            Integer idCita,

            @Param("idMedico")
            Integer idMedico
    );


    // =====================================================
    // CONTAR RESULTADOS REGISTRADOS
    // =====================================================

    long countByDetalleOrdenLaboratorio_OrdenLaboratorio_Id(
            Integer idOrdenLaboratorio
    );


    // =====================================================
    // CONTAR RESULTADOS PUBLICADOS
    // =====================================================
    //
    // Nos servirá para determinar posteriormente si TODOS
    // los exámenes de la orden ya fueron publicados.
    //
    // Cuando:
    //
    // cantidadDetalles == cantidadPublicados
    //
    // la orden podrá pasar a COMPLETADA.
    //
    // =====================================================

    long countByDetalleOrdenLaboratorio_OrdenLaboratorio_IdAndPublicadoTrue(
            Integer idOrdenLaboratorio
    );
}