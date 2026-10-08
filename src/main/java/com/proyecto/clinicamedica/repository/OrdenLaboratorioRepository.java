package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio.EstadoOrdenLaboratorio;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * Permite:
 *
 * - Guardar órdenes.
 * - Buscar por ID.
 * - Buscar por número de orden.
 * - Consultar órdenes de una cita.
 * - Consultar órdenes de una consulta médica.
 * - Validar acceso del Médico propietario.
 * - Listar órdenes de una sucursal.
 * - Buscar una orden dentro de una sucursal.
 * - Bloquear una orden durante operaciones críticas.
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


    // =====================================================
    // CU-09
    // LISTAR ÓRDENES DE UNA SUCURSAL
    // =====================================================
    //
    // El Laboratorista únicamente debe consultar órdenes
    // correspondientes a su misma sucursal.
    //
    // Importante:
    //
    // También se incluyen las órdenes externas.
    // CU-09 necesita mostrarlas identificadas como Externa.
    //
    // La restricción para NO procesarlas internamente
    // será aplicada posteriormente en el Service.
    //
    // =====================================================

    List<OrdenLaboratorio>
    findByCita_Sucursal_IdOrderByFechaHoraCreacionDesc(
            Integer idSucursal
    );


    // =====================================================
    // CU-09
    // BUSCAR ORDEN SEGURA POR SUCURSAL
    // =====================================================
    //
    // Evita que un Laboratorista cambie manualmente
    // el ID de la URL para consultar una orden
    // perteneciente a otra sucursal.
    //
    // =====================================================

    Optional<OrdenLaboratorio>
    findByIdAndCita_Sucursal_Id(
            Integer idOrden,
            Integer idSucursal
    );


    // =====================================================
    // CU-09
    // BUSCAR ORDEN CON BLOQUEO
    // =====================================================
    //
    // Se utilizará en operaciones que modifican datos:
    //
    // - iniciar procesamiento;
    // - guardar resultados;
    // - validar;
    // - publicar;
    // - completar la orden.
    //
    // PESSIMISTIC_WRITE evita que dos operaciones
    // modifiquen simultáneamente la misma orden.
    //
    // =====================================================

    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT o
            FROM OrdenLaboratorio o
            WHERE o.id = :idOrden
              AND o.cita.sucursal.id = :idSucursal
            """)
    Optional<OrdenLaboratorio>
    buscarPorIdYSucursalConBloqueo(

            @Param("idOrden")
            Integer idOrden,

            @Param("idSucursal")
            Integer idSucursal
    );
    // =====================================================
// CU-09
// FILTRAR ÓRDENES DE LABORATORIO
// =====================================================
//
// Filtros requeridos:
//
// - Estado.
// - Paciente.
// - Médico.
//
// Siempre limita la búsqueda a la sucursal
// del Laboratorista autenticado.
//
// Las órdenes externas también se muestran.
// =====================================================

    @Query("""
        SELECT DISTINCT o
        FROM OrdenLaboratorio o

        JOIN FETCH o.cita c
        JOIN FETCH c.paciente p
        JOIN FETCH o.medico m
        JOIN FETCH c.sucursal s

        WHERE s.id = :idSucursal

          AND (
                :estado IS NULL
                OR o.estado = :estado
              )

          AND (
                :paciente = ''
                OR LOWER(p.nombreCompleto)
                   LIKE LOWER(CONCAT('%', :paciente, '%'))
              )

          AND (
                :medico = ''
                OR LOWER(m.nombreCompleto)
                   LIKE LOWER(CONCAT('%', :medico, '%'))
              )

        ORDER BY o.fechaHoraCreacion DESC
        """)
    List<OrdenLaboratorio>
    filtrarOrdenesLaboratorio(

            @Param("idSucursal")
            Integer idSucursal,

            @Param("estado")
            EstadoOrdenLaboratorio estado,

            @Param("paciente")
            String paciente,

            @Param("medico")
            String medico
    );
}