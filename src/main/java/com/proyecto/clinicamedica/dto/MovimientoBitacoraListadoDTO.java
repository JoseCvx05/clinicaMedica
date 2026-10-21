
package com.proyecto.clinicamedica.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * =========================================================
 * CU-15 - DTO: LISTADO DE MOVIMIENTOS DE INVENTARIO
 * =========================================================
 *
 * Flujo Normal Básico - Paso 2.
 *
 * Contiene las columnas exigidas:
 *
 * - Tipo.
 * - Medicamento.
 * - Sucursal.
 * - Cantidad.
 * - Stock anterior.
 * - Stock nuevo.
 * - Costo.
 * - Referencia.
 * - Usuario.
 * - Fecha.
 *
 * También proporciona:
 *
 * - ID del movimiento.
 * - Estado activo/inactivo.
 *
 * SEGURIDAD:
 *
 * - Solo representa información de consulta.
 * - No permite modificar inventario.
 * - No recibe datos enviados por el navegador.
 * - El Service construirá este DTO utilizando
 *   la información autorizada de PostgreSQL.
 *
 * COMPATIBILIDAD:
 *
 * - CU-11: despachos históricos.
 * - CU-14: movimientos anteriores.
 * - CU-15: movimientos con tipo específico.
 *
 * =========================================================
 */
public record MovimientoBitacoraListadoDTO(

        Long idMovimiento,

        String tipo,

        String medicamento,

        String sucursal,

        Integer cantidad,

        Integer stockAnterior,

        Integer stockNuevo,

        BigDecimal costoUnitario,

        String referencia,

        String usuarioResponsable,

        OffsetDateTime fechaHora,

        Boolean activo

) {
}
