
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Despacho;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * =========================================================
 * REPOSITORY: DESPACHO DE MEDICAMENTOS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite:
 *
 * - Consultar despachos registrados.
 * - Buscar operaciones por clave única.
 * - Consultar el historial de una receta.
 * - Buscar despachos de una sucursal.
 *
 * IMPORTANTE:
 *
 * Las validaciones de seguridad, receta,
 * farmacéutico, cantidades y sucursal
 * se realizarán en el Service del backend.
 *
 * =========================================================
 */
@Repository
public interface DespachoRepository
        extends JpaRepository<Despacho, Integer> {

    // =====================================================
    // BUSCAR POR CLAVE ÚNICA DE OPERACIÓN
    // PROTECCIÓN CONTRA DUPLICADOS
    // =====================================================

    @EntityGraph(attributePaths = {
            "recetaMedica",
            "farmaceutico",
            "sucursal"
    })
    Optional<Despacho> findByClaveOperacion(
            UUID claveOperacion
    );


    // =====================================================
    // BUSCAR DESPACHO POR ID Y SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "recetaMedica",
            "farmaceutico",
            "sucursal"
    })
    Optional<Despacho> findByIdAndSucursal_Id(
            Integer idDespacho,
            Integer idSucursal
    );


    // =====================================================
    // HISTORIAL DE DESPACHOS DE UNA RECETA
    // =====================================================

    @EntityGraph(attributePaths = {
            "recetaMedica",
            "farmaceutico",
            "sucursal"
    })
    List<Despacho>
    findByRecetaMedica_IdOrderByFechaHoraRegistroDesc(
            Integer idRecetaMedica
    );


    // =====================================================
    // HISTORIAL DE DESPACHOS POR SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "recetaMedica",
            "farmaceutico",
            "sucursal"
    })
    List<Despacho>
    findBySucursal_IdOrderByFechaHoraRegistroDesc(
            Integer idSucursal
    );
    // =====================================================
    // CU-11 - FA03
    // VERIFICAR SI LA RECETA YA TIENE NO_ADQUIRIDO
    // =====================================================
    //
    // Evita que se registre varias veces la misma
    // decision del paciente utilizando UUID diferentes.
    //
    // IMPORTANTE:
    //
    // - No modifica informacion.
    // - No bloquea despachos confirmados.
    // - No descuenta inventario.
    // - La validacion se aplicara desde el Service.
    //
    // =====================================================

    boolean existsByRecetaMedica_IdAndEstado(
            Integer idRecetaMedica,
            Despacho.EstadoDespacho estado
    );
}
