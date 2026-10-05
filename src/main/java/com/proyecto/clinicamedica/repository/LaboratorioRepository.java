package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Laboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * =========================================================
 */
@Repository
public interface LaboratorioRepository
        extends JpaRepository<Laboratorio, Integer> {


    // =====================================================
    // LISTAR LABORATORIOS ACTIVOS
    // =====================================================

    List<Laboratorio>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // BUSCAR LABORATORIO ACTIVO POR ID
    // =====================================================

    Optional<Laboratorio>
    findByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR NOMBRE DUPLICADO ACTIVO
    // =====================================================

    boolean
    existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );
}