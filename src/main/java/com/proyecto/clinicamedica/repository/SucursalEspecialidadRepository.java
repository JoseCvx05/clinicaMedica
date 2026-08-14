package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.SucursalEspecialidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * =========================================================
 * REPOSITORY: SUCURSAL - ESPECIALIDAD
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Consulta las especialidades activas configuradas
 * para una sucursal.
 * =========================================================
 */
@Repository
public interface SucursalEspecialidadRepository
        extends JpaRepository<SucursalEspecialidad, Integer> {


    List<SucursalEspecialidad>
    findBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrueOrderByEspecialidad_NombreAsc(
            Integer idSucursal
    );


    boolean
    existsBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrue(
            Integer idSucursal
    );
}