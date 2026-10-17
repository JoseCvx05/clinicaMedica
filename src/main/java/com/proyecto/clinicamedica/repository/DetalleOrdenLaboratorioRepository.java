package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: DETALLE ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * Permite:
 *
 * - Guardar los exámenes seleccionados.
 * - Listar detalles de una orden.
 * - Verificar duplicados dentro de la misma orden.
 * - Contar exámenes de una orden.
 * - Buscar de forma segura un detalle dentro
 *   de una orden específica.
 *
 * =========================================================
 */
@Repository
public interface DetalleOrdenLaboratorioRepository
        extends JpaRepository<DetalleOrdenLaboratorio, Integer> {


    // =====================================================
    // DETALLES DE UNA ORDEN
    // =====================================================

    List<DetalleOrdenLaboratorio>
    findByOrdenLaboratorio_IdOrderByNombreExamenAsc(
            Integer idOrdenLaboratorio
    );


    // =====================================================
    // CU-09
    // BUSCAR DETALLE SEGURO DENTRO DE UNA ORDEN
    // =====================================================
    //
    // Evita confiar únicamente en idDetalle.
    //
    // Ejemplo:
    //
    // Orden 3
    //     detalle 7
    //
    // Si alguien intenta enviar:
    //
    // Orden 3
    //     detalle 15
    //
    // pero el detalle 15 pertenece a otra orden,
    // este método devolverá Optional.empty().
    //
    // =====================================================

    Optional<DetalleOrdenLaboratorio>
    findByIdAndOrdenLaboratorio_Id(
            Integer idDetalleOrdenLaboratorio,
            Integer idOrdenLaboratorio
    );


    // =====================================================
    // VALIDAR EXAMEN DUPLICADO
    // =====================================================

    boolean
    existsByOrdenLaboratorio_IdAndExamenLaboratorio_Id(
            Integer idOrdenLaboratorio,
            Integer idExamenLaboratorio
    );


    // =====================================================
    // CONTAR EXÁMENES DE UNA ORDEN
    // =====================================================

    long
    countByOrdenLaboratorio_Id(
            Integer idOrdenLaboratorio
    );


    // =====================================================
    // CU-14 - VERIFICAR EXAMEN UTILIZADO EN ÓRDENES
    // =====================================================
    //
    // Comprueba si un examen del catálogo aparece
    // en alguna orden de laboratorio registrada.
    //
    // Incluye órdenes históricas.
    //
    // Permite aplicar las reglas de protección antes
    // de desactivar un examen de laboratorio.
    //
    // IMPORTANTE:
    //
    // - No modifica órdenes existentes.
    // - No altera precios históricos.
    // - No elimina detalles.
    //
    // =====================================================

    boolean existsByExamenLaboratorio_Id(
            Integer idExamenLaboratorio
    );

}