package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ExamenLaboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: EXAMEN DE LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * Responsabilidades:
 *
 * - Listar exámenes activos.
 * - Listar exámenes activos por laboratorio.
 * - Buscar un examen activo por ID.
 *
 * =========================================================
 */
@Repository
public interface ExamenLaboratorioRepository
        extends JpaRepository<ExamenLaboratorio, Integer> {


    // =====================================================
    // LISTAR TODOS LOS EXÁMENES ACTIVOS
    // =====================================================

    List<ExamenLaboratorio>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // LISTAR EXÁMENES ACTIVOS DE UN LABORATORIO
    // =====================================================

    List<ExamenLaboratorio>
    findByLaboratorio_IdAndActivoTrueOrderByNombreAsc(
            Integer idLaboratorio
    );


    // =====================================================
    // BUSCAR EXAMEN ACTIVO POR ID
    // =====================================================

    Optional<ExamenLaboratorio>
    findByIdAndActivoTrue(
            Integer id
    );
}