package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.AtencionEmergencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * =========================================================
 * REPOSITORIO: ATENCIÓN DE EMERGENCIA
 * =========================================================
 *
 * CU-05 - FA01.
 *
 * Se encarga únicamente de la persistencia de ingresos
 * de emergencia.
 *
 * =========================================================
 */
@Repository
public interface AtencionEmergenciaRepository
        extends JpaRepository<AtencionEmergencia, Integer> {

}