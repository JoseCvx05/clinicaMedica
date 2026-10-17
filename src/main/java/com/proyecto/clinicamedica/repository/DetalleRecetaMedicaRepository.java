package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * =========================================================
 * REPOSITORY: DETALLE RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 *
 * Permite:
 *
 * - Guardar medicamentos prescritos.
 * - Listar detalles de una receta.
 * - Contar medicamentos de una receta.
 * - Consultar medicamentos asociados.
 *
 * =========================================================
 */
@Repository
public interface DetalleRecetaMedicaRepository
        extends JpaRepository<DetalleRecetaMedica, Integer> {


    // =====================================================
    // DETALLES DE UNA RECETA
    // =====================================================

    List<DetalleRecetaMedica>
    findByRecetaMedica_IdOrderByIdAsc(
            Integer idRecetaMedica
    );


    // =====================================================
    // CONTAR MEDICAMENTOS
    // =====================================================

    long
    countByRecetaMedica_Id(
            Integer idRecetaMedica
    );


    // =====================================================
    // BUSCAR MEDICAMENTO DENTRO DE RECETA
    // =====================================================

    List<DetalleRecetaMedica>
    findByRecetaMedica_IdAndMedicamento_Id(
            Integer idRecetaMedica,
            Integer idMedicamento
    );


    // =====================================================
    // VALIDAR EXISTENCIA DEL MEDICAMENTO EN LA RECETA
    // =====================================================

    boolean
    existsByRecetaMedica_IdAndMedicamento_Id(
            Integer idRecetaMedica,
            Integer idMedicamento
    );


    // =====================================================
    // CU-14 - VERIFICAR MEDICAMENTO EN RECETAS MÉDICAS
    // =====================================================
    //
    // Comprueba si un medicamento del catálogo
    // está relacionado con alguna receta médica.
    //
    // Incluye recetas históricas.
    //
    // Se utilizará para verificar dependencias
    // antes de realizar operaciones administrativas.
    //
    // IMPORTANTE:
    //
    // - No modifica recetas existentes.
    // - No altera precios históricos.
    // - No elimina detalles de recetas.
    // - No modifica inventarios.
    //
    // =====================================================

    boolean existsByMedicamento_Id(
            Integer idMedicamento
    );

}