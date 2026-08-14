package com.proyecto.clinicamedica.event;


/**
 * =========================================================
 * EVENTO: PAGO APROBADO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Se publica cuando:
 *
 * - El pago quedó APROBADO.
 * - La cita quedó PAGADA.
 *
 * El evento contiene únicamente el identificador del pago.
 *
 * No contiene datos sensibles de tarjeta.
 * =========================================================
 */
public record PagoAprobadoEvent(

        Integer idPago

) {
}