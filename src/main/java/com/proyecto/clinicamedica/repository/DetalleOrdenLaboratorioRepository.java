package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * =========================================================
 * REPOSITORY: DETALLE ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 *
 * Permite:
 *
 * - Guardar los exámenes seleccionados.
 * - Listar detalles de una orden.
 * - Verificar duplicados dentro de la misma orden.
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
}