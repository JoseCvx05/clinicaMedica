
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
/**
 * =========================================================
 * CU-14 - REPOSITORY: MOVIMIENTOS DE INVENTARIO
 * =========================================================
 *
 * Catálogo 8 - Inventario de Medicamentos.
 *
 * Responsabilidades:
 *
 * - Consultar movimientos de un inventario.
 * - Consultar movimientos por sucursal.
 * - Aplicar paginación.
 * - Consultar movimientos por período.
 * - Preparar el resumen mensual de inventario.
 *
 * IMPORTANTE:
 *
 * - No modifica existencias.
 * - No elimina registros históricos.
 * - No realiza despachos.
 * - La seguridad y auditoría se controlarán
 *   desde los Services correspondientes.
 *
 * =========================================================
 */
@Repository
public interface MovimientoInventarioFarmaciaRepository
        extends JpaRepository<MovimientoInventarioFarmacia, Long> {


    // =====================================================
    // CU-14 - HISTORIAL DE UN INVENTARIO
    // =====================================================
    //
    // Consulta todos los movimientos de un inventario.
    //
    // Orden:
    // - Fecha más reciente primero.
    // - ID más reciente primero.
    //
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Page<MovimientoInventarioFarmacia>
    findByInventario_IdOrderByFechaHoraDescIdDesc(
            Integer idInventario,
            Pageable pageable
    );


    // =====================================================
    // CU-14 - HISTORIAL POR SUCURSAL
    // =====================================================
    //
    // Respeta la sucursal seleccionada.
    // Permite consultar entradas, salidas y ajustes.
    //
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Page<MovimientoInventarioFarmacia>
    findByInventario_Sucursal_IdOrderByFechaHoraDescIdDesc(
            Integer idSucursal,
            Pageable pageable
    );


    // =====================================================
    // CU-14 - MOVIMIENTOS POR PERÍODO
    // =====================================================
    //
    // Permitirá calcular el resumen mensual.
    //
    // La fecha inicial es inclusiva.
    // La fecha final es exclusiva.
    //
    // Ejemplo:
    //
    // Desde: 01/10/2026
    // Hasta: 01/11/2026
    //
    // Consulta únicamente octubre.
    //
    // =====================================================

    @Query("""
            SELECT m
            FROM MovimientoInventarioFarmacia m
            WHERE m.inventario.id = :idInventario
              AND m.fechaHora >= :desde
              AND m.fechaHora < :hasta
            ORDER BY m.fechaHora ASC, m.id ASC
            """)
    List<MovimientoInventarioFarmacia>
    buscarPorInventarioYPeriodo(
            @Param("idInventario")
            Integer idInventario,

            @Param("desde")
            OffsetDateTime desde,

            @Param("hasta")
            OffsetDateTime hasta
    );

    // =====================================================
    // CU-14 - CATÁLOGO 8
    // RESUMEN MENSUAL DE INVENTARIO
    // =====================================================
    //
    // Obtiene el último movimiento registrado antes
    // de la fecha inicial del período consultado.
    //
    // Permite determinar las existencias iniciales
    // del mes, incluso cuando no hubo movimientos
    // durante ese período.
    //
    // Solo consulta información histórica.
    // No modifica existencias ni despachos.
    //
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Optional<MovimientoInventarioFarmacia>
    findFirstByInventario_IdAndFechaHoraLessThanOrderByFechaHoraDescIdDesc(
            Integer idInventario,
            OffsetDateTime fechaInicio
    );

}
