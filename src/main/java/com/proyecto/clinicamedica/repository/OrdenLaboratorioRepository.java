package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.OrdenLaboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 *
 * Permite:
 *
 * - Guardar órdenes.
 * - Buscar por ID.
 * - Buscar por número de orden.
 * - Consultar órdenes de una cita.
 * - Consultar órdenes de una consulta médica.
 * - Validar acceso del Médico propietario.
 *
 * =========================================================
 */
@Repository
public interface OrdenLaboratorioRepository
        extends JpaRepository<OrdenLaboratorio, Integer> {


    // =====================================================
    // BUSCAR POR NÚMERO DE ORDEN
    // =====================================================

    Optional<OrdenLaboratorio>
    findByNumeroOrden(
            String numeroOrden
    );


    // =====================================================
    // VALIDAR NÚMERO DUPLICADO
    // =====================================================

    boolean
    existsByNumeroOrden(
            String numeroOrden
    );


    // =====================================================
    // ÓRDENES DE UNA CITA
    // =====================================================

    List<OrdenLaboratorio>
    findByCita_IdOrderByFechaHoraCreacionDesc(
            Integer idCita
    );


    // =====================================================
    // ÓRDENES DE UNA CONSULTA
    // =====================================================

    List<OrdenLaboratorio>
    findByConsultaMedica_IdOrderByFechaHoraCreacionDesc(
            Integer idConsultaMedica
    );


    // =====================================================
    // ORDEN PERTENECIENTE AL MÉDICO
    // =====================================================

    Optional<OrdenLaboratorio>
    findByIdAndMedico_Id(
            Integer idOrden,
            Integer idMedico
    );


    // =====================================================
    // LISTAR ÓRDENES DEL MÉDICO
    // =====================================================

    List<OrdenLaboratorio>
    findByMedico_IdOrderByFechaHoraCreacionDesc(
            Integer idMedico
    );
}