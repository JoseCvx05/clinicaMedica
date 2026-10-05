package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.RecetaMedica;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 *
 * Permite:
 *
 * - Guardar recetas.
 * - Buscar recetas por ID.
 * - Consultar recetas de una cita.
 * - Consultar recetas de una consulta.
 * - Validar propiedad del Médico.
 * - Consultar recetas activas.
 * - Bloquear una receta durante operaciones sensibles.
 *
 * =========================================================
 */
@Repository
public interface RecetaMedicaRepository
        extends JpaRepository<RecetaMedica, Integer> {


    // =====================================================
    // RECETAS DE UNA CITA
    // =====================================================

    List<RecetaMedica>
    findByCita_IdOrderByFechaEmisionDesc(
            Integer idCita
    );


    // =====================================================
    // RECETAS DE UNA CONSULTA
    // =====================================================

    List<RecetaMedica>
    findByConsultaMedica_IdOrderByFechaEmisionDesc(
            Integer idConsultaMedica
    );


    // =====================================================
    // RECETAS ACTIVAS DE UNA CONSULTA
    // =====================================================

    List<RecetaMedica>
    findByConsultaMedica_IdAndActivoTrueOrderByFechaEmisionDesc(
            Integer idConsultaMedica
    );


    // =====================================================
    // RECETAS DEL MÉDICO
    // =====================================================

    List<RecetaMedica>
    findByMedico_IdOrderByFechaEmisionDesc(
            Integer idMedico
    );


    // =====================================================
    // RECETA DEL MÉDICO
    // =====================================================

    Optional<RecetaMedica>
    findByIdAndMedico_Id(
            Integer idReceta,
            Integer idMedico
    );


    // =====================================================
    // RECETA ACTIVA POR ID
    // =====================================================

    Optional<RecetaMedica>
    findByIdAndActivoTrue(
            Integer idReceta
    );


    // =====================================================
    // RECETA ACTIVA + MÉDICO
    // =====================================================

    Optional<RecetaMedica>
    findByIdAndMedico_IdAndActivoTrue(
            Integer idReceta,
            Integer idMedico
    );


    // =====================================================
    // BLOQUEO PESIMISTA
    // =====================================================
    //
    // Será útil posteriormente para evitar que dos procesos
    // modifiquen/desactiven la misma receta al mismo tiempo.
    //
    // =====================================================

    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    Optional<RecetaMedica>
    findWithLockById(
            Integer idReceta
    );
}