
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleDespacho;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * =========================================================
 * REPOSITORY: DETALLE DE DESPACHO
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite:
 *
 * - Consultar medicamentos entregados.
 * - Recuperar detalles de un despacho.
 * - Consultar cantidades ya despachadas.
 * - Preparar validaciones de entregas parciales.
 * - Evitar superar cantidades prescritas.
 *
 * IMPORTANTE:
 *
 * Las validaciones definitivas se realizarán
 * en el Service del backend.
 *
 * =========================================================
 */
@Repository
public interface DetalleDespachoRepository
        extends JpaRepository<DetalleDespacho, Integer> {

    // =====================================================
    // DETALLES DE UN DESPACHO POR SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "despacho",
            "detalleRecetaMedica",
            "medicamentoEntregado"
    })
    List<DetalleDespacho>
    findByDespacho_IdAndDespacho_Sucursal_IdOrderByIdAsc(
            Integer idDespacho,
            Integer idSucursal
    );


    // =====================================================
    // SUMAR CANTIDADES PREVIAMENTE ENTREGADAS
    // =====================================================
    //
    // La suma se realiza sobre el detalle original
    // de la receta médica.
    //
    // Esto permite controlar también sustituciones,
    // porque conservan el mismo detalle de receta.
    //
    // Solo se suman despachos con el estado indicado.
    //
    // Para comprobar medicamentos efectivamente
    // entregados, el Service utilizará CONFIRMADO.
    //
    // Cuando no existan registros, SUM devuelve null.
    // El Service deberá interpretarlo como cero.
    //
    // =====================================================

    @Query("""
            SELECT SUM(detalle.cantidadEntregada)
            FROM DetalleDespacho detalle
            WHERE detalle.detalleRecetaMedica.id = :idDetalleReceta
              AND detalle.despacho.estado = :estado
            """)
    Long sumarCantidadEntregadaPorDetalleYEstado(
            @Param("idDetalleReceta")
            Integer idDetalleReceta,

            @Param("estado")
            Despacho.EstadoDespacho estado
    );


    // =====================================================
    // CU-11 - HISTORIAL DE MEDICAMENTOS DEL PACIENTE
    // =====================================================
    //
    // Consulta los medicamentos realmente entregados
    // y asociados a las citas del paciente.
    //
    // SEGURIDAD:
    //
    // - El ID del paciente se obtiene en el Service
    //   desde el usuario autenticado.
    //
    // - Solo se consultan despachos CONFIRMADOS.
    //
    // - Incluye entregas normales y sustituciones.
    //
    // - Ordena desde el despacho más reciente.
    //
    // - No modifica inventario ni despachos.
    //
    // =====================================================

    @Query("""
            SELECT detalle
            FROM DetalleDespacho detalle

            JOIN FETCH detalle.despacho despacho

            JOIN FETCH despacho.recetaMedica receta

            JOIN receta.cita cita

            WHERE cita.paciente.id = :idPaciente

              AND despacho.estado = :estado

            ORDER BY
                despacho.fechaHoraRegistro DESC,
                detalle.id DESC
            """)
    List<DetalleDespacho>
    buscarHistorialMedicamentosPaciente(

            @Param("idPaciente")
            Integer idPaciente,

            @Param("estado")
            Despacho.EstadoDespacho estado

    );

}
