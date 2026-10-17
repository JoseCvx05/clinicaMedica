
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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


    // =====================================================
    // CU-14 - PROTEGER SUCURSALES CON INVENTARIO
    // =====================================================
    //
    // Comprueba si una sucursal tiene medicamentos
    // con existencias mayores que cero.
    //
    // Se utilizará antes de desactivar una sucursal.
    //
    // IMPORTANTE:
    //
    // - No modifica inventarios.
    // - No descuenta medicamentos.
    // - No elimina registros históricos.
    // - Protege las existencias del CU-11.
    //
    // =====================================================

    boolean existsBySucursal_IdAndCantidadDisponibleGreaterThan(
            Integer idSucursal,
            Integer cantidad
    );



    // =====================================================
    // CU-14 - PROTEGER MEDICAMENTOS CON EXISTENCIAS
    // =====================================================
    //
    // Comprueba si un medicamento tiene inventario
    // disponible mayor que cero en alguna sucursal.
    //
    // IMPORTANTE:
    //
    // - Consulta todas las sucursales.
    // - No modifica las existencias.
    // - No elimina inventarios.
    // - No afecta los despachos del CU-11.
    //
    // Se utilizará para validar dependencias antes
    // de desactivar medicamentos del catálogo.
    //
    // =====================================================

    boolean existsByMedicamento_IdAndCantidadDisponibleGreaterThan(
            Integer idMedicamento,
            Integer cantidad
    );


    // =====================================================
    // CU-14 - CATÁLOGO 8: MANTENIMIENTO DE INVENTARIO
    // =====================================================
    //
    // Consultas administrativas para:
    //
    // - Buscar medicamentos por nombre.
    // - Consultar inventarios de todas las sucursales.
    // - Filtrar inventario por sucursal.
    // - Paginar resultados.
    //
    // IMPORTANTE:
    //
    // - No modifica cantidades disponibles.
    // - No realiza movimientos de inventario.
    // - No afecta despachos del CU-11.
    // - Conserva los métodos y bloqueos existentes.
    //
    // =====================================================


    // =====================================================
    // BÚSQUEDA GENERAL DE INVENTARIO
    // =====================================================

    @EntityGraph(attributePaths = {
            "medicamento",
            "sucursal"
    })
    Page<InventarioFarmacia>
    findByMedicamento_NombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );


    // =====================================================
    // BÚSQUEDA DE INVENTARIO POR SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "medicamento",
            "sucursal"
    })
    Page<InventarioFarmacia>
    findBySucursal_IdAndMedicamento_NombreContainingIgnoreCase(
            Integer idSucursal,
            String nombre,
            Pageable pageable
    );


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // BLOQUEAR INVENTARIO POR ID
    // =====================================================
    //
    // Protege las operaciones administrativas:
    //
    // - Actualización de inventario.
    // - Desactivación lógica.
    // - Validación de existencias.
    //
    // IMPORTANTE:
    //
    // - Debe utilizarse dentro de @Transactional.
    // - No modifica las cantidades.
    // - No afecta los despachos del CU-11.
    // - El bloqueo dura hasta finalizar la transacción.
    //
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventarioFarmacia i
            WHERE i.id = :id
            """)
    Optional<InventarioFarmacia> bloquearPorId(
            @Param("id") Integer id
    );


}
