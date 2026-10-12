
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORY: INVENTARIO DE FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite consultar las existencias de medicamentos
 * correspondientes a cada sucursal.
 *
 * IMPORTANTE:
 *
 * - Las consultas deben respetar la sucursal.
 * - No se debe permitir despachar sin stock suficiente.
 * - No se deben registrar inventarios duplicados.
 * - El control de concurrencia utiliza @Version.
 *
 * La seguridad y las validaciones del despacho
 * se realizarán en los Services del backend.
 *
 * =========================================================
 */
@Repository
public interface InventarioFarmaciaRepository
        extends JpaRepository<InventarioFarmacia, Integer> {


    // =====================================================
    // BUSCAR INVENTARIO POR MEDICAMENTO Y SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "medicamento",
            "sucursal"
    })
    Optional<InventarioFarmacia>
    findByMedicamento_IdAndSucursal_Id(
            Integer idMedicamento,
            Integer idSucursal
    );


    // =====================================================
    // LISTAR INVENTARIO DE UNA SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "medicamento",
            "sucursal"
    })
    List<InventarioFarmacia>
    findBySucursal_IdOrderByMedicamento_NombreAsc(
            Integer idSucursal
    );


    // =====================================================
    // VERIFICAR SI YA EXISTE INVENTARIO REGISTRADO
    // =====================================================

    boolean existsByMedicamento_IdAndSucursal_Id(
            Integer idMedicamento,
            Integer idSucursal
    );


    // =====================================================
    // CU-11 - RNF-025
    // BLOQUEAR INVENTARIO DURANTE UN DESPACHO
    // =====================================================
    //
    // Esta consulta utiliza PESSIMISTIC_WRITE.
    //
    // Su objetivo es bloquear la fila del inventario
    // mientras se realiza una operación transaccional.
    //
    // Así evitamos que dos farmacéuticos consulten
    // y descuenten simultáneamente el mismo stock
    // sin esperar la finalización de la transacción.
    //
    // IMPORTANTE:
    //
    // - Debe ejecutarse dentro de @Transactional.
    // - El Service verificará nuevamente el stock.
    // - El Service validará la sucursal autenticada.
    // - No modifica el inventario por sí sola.
    // - Si el inventario no existe, retorna Optional.empty().
    //
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventarioFarmacia i
            WHERE i.medicamento.id = :idMedicamento
              AND i.sucursal.id = :idSucursal
            """)
    Optional<InventarioFarmacia>
    bloquearPorMedicamentoYSucursal(
            @Param("idMedicamento")
            Integer idMedicamento,

            @Param("idSucursal")
            Integer idSucursal
    );

}
