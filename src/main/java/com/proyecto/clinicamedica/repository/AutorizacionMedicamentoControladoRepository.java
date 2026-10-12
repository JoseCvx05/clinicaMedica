
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.AutorizacionMedicamentoControlado;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORY: AUTORIZACION DE MEDICAMENTO CONTROLADO
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * RNF-017:
 * Los medicamentos controlados requieren
 * autorizacion adicional y auditoria.
 *
 * Permite:
 *
 * - Buscar autorizaciones por detalle de receta.
 * - Verificar si ya existe una autorizacion.
 * - Consultar las autorizaciones de una receta.
 *
 * IMPORTANTE:
 *
 * Este repositorio NO autoriza medicamentos.
 *
 * La autorizacion solo sera registrada
 * por el Service correspondiente, despues
 * de validar en el backend:
 *
 * - Usuario autenticado.
 * - Rol MEDICO.
 * - Medico responsable de la receta.
 * - Receta activa y vigente.
 * - Medicamento controlado.
 * - Cantidad autorizada.
 * - Registro de auditoria.
 *
 * =========================================================
 */
@Repository
public interface AutorizacionMedicamentoControladoRepository
        extends JpaRepository<
        AutorizacionMedicamentoControlado,
        Integer
        > {

    // =====================================================
    // BUSCAR AUTORIZACION POR DETALLE DE RECETA
    // =====================================================

    @EntityGraph(attributePaths = {
            "detalleRecetaMedica",
            "detalleRecetaMedica.recetaMedica",
            "detalleRecetaMedica.medicamento",
            "medicoAutorizador"
    })
    Optional<AutorizacionMedicamentoControlado>
    findByDetalleRecetaMedica_Id(
            Integer idDetalleReceta
    );


    // =====================================================
    // VERIFICAR AUTORIZACION EXISTENTE
    // =====================================================
    //
    // Evita intentar crear autorizaciones
    // duplicadas para un mismo detalle.
    //
    // La restriccion UNIQUE de PostgreSQL
    // sera la proteccion definitiva frente
    // a solicitudes simultaneas.
    //
    // =====================================================

    boolean existsByDetalleRecetaMedica_Id(
            Integer idDetalleReceta
    );


    // =====================================================
    // AUTORIZACIONES DE UNA RECETA
    // =====================================================
    //
    // Permite recuperar todas las autorizaciones
    // relacionadas con una receta medica.
    //
    // El Service validara que el usuario tenga
    // permiso para consultar esa receta.
    //
    // =====================================================

    @EntityGraph(attributePaths = {
            "detalleRecetaMedica",
            "detalleRecetaMedica.recetaMedica",
            "detalleRecetaMedica.medicamento",
            "medicoAutorizador"
    })
    List<AutorizacionMedicamentoControlado>
    findByDetalleRecetaMedica_RecetaMedica_IdOrderByIdAsc(
            Integer idReceta
    );
}
