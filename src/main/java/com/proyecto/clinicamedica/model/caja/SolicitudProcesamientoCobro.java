package com.proyecto.clinicamedica.model.caja;

import java.math.BigDecimal;


/**
 * =========================================================
 * SOLICITUD DE PROCESAMIENTO DE COBRO
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Contiene los datos mínimos necesarios para que una
 * estrategia de cobro pueda procesar la operación.
 *
 * No contiene entidades JPA ni información sensible
 * completa de tarjetas.
 *
 * =========================================================
 */
public record SolicitudProcesamientoCobro(

        TipoCobroCaja tipoCobro,

        BigDecimal montoTotal,

        BigDecimal montoRecibido,

        String ultimos4Tarjeta

) {
}