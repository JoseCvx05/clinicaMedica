package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ConsultaMedicaHistorial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * =========================================================
 * REPOSITORY: HISTORIAL DE CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * RNF-026:
 * El historial de consultas debe mantener versionamiento
 * para auditoría.
 *
 * Responsabilidades:
 *
 * - Persistir versiones anteriores.
 * - Consultar historial por consulta.
 * - Detectar versiones duplicadas.
 *
 * =========================================================
 */
@Repository
public interface ConsultaMedicaHistorialRepository
        extends JpaRepository<ConsultaMedicaHistorial, Long> {


    // =====================================================
    // HISTORIAL COMPLETO
    // =====================================================

    List<ConsultaMedicaHistorial>
    findByConsulta_IdOrderByVersionDesc(
            Integer idConsulta
    );


    // =====================================================
    // ¿YA EXISTE ESTA VERSIÓN?
    // =====================================================

    boolean existsByConsulta_IdAndVersion(
            Integer idConsulta,
            Integer version
    );
}