package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.SucursalEspecialidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: SUCURSAL - ESPECIALIDAD
 * =========================================================
 *
 * CU-03:
 *
 * Consulta las especialidades activas configuradas
 * para una sucursal.
 *
 * CU-04:
 *
 * Permite obtener la configuración exacta de una
 * sucursal y especialidad para determinar de forma
 * segura el precio de la consulta.
 *
 * =========================================================
 */
@Repository
public interface SucursalEspecialidadRepository
        extends JpaRepository<SucursalEspecialidad, Integer> {


    // =====================================================
    // CU-03 - ESPECIALIDADES DE UNA SUCURSAL
    // =====================================================

    List<SucursalEspecialidad>
    findBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrueOrderByEspecialidad_NombreAsc(
            Integer idSucursal
    );


    // =====================================================
    // CU-03 - ¿POSEE ESPECIALIDADES?
    // =====================================================

    boolean
    existsBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrue(
            Integer idSucursal
    );


    // =====================================================
    // CU-04 - CONFIGURACIÓN SUCURSAL / ESPECIALIDAD
    // =====================================================

    Optional<SucursalEspecialidad>
    findBySucursal_IdAndEspecialidad_IdAndActivoTrue(
            Integer idSucursal,
            Integer idEspecialidad
    );
}