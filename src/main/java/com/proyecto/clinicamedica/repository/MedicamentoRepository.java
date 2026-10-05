package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Medicamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: MEDICAMENTO
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 *
 * Permite:
 *
 * - Consultar medicamentos activos.
 * - Buscar un medicamento activo por ID.
 * - Validar existencia del medicamento.
 *
 * La receta utilizará siempre el ID del catálogo
 * y volverá a consultar PostgreSQL.
 *
 * =========================================================
 */
@Repository
public interface MedicamentoRepository
        extends JpaRepository<Medicamento, Integer> {


    // =====================================================
    // LISTAR CATÁLOGO ACTIVO
    // =====================================================

    List<Medicamento>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // BUSCAR MEDICAMENTO ACTIVO
    // =====================================================

    Optional<Medicamento>
    findByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR EXISTENCIA ACTIVA
    // =====================================================

    boolean
    existsByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR NOMBRE DUPLICADO EN CATÁLOGO
    // =====================================================

    boolean
    existsByNombreIgnoreCase(
            String nombre
    );
}