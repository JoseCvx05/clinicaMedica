package com.proyecto.clinicamedica.model.caja;


/**
 * =========================================================
 * RESULTADO DE TERMINAL POS
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Representa la respuesta normalizada de un dispositivo
 * o proveedor POS.
 *
 * No contiene información sensible de tarjeta.
 *
 * =========================================================
 */
public record ResultadoPos(

        boolean aprobado,

        String codigoResultado,

        String referencia

) {


    public static ResultadoPos aprobado(
            String referencia
    ) {

        return new ResultadoPos(
                true,
                "APROBADO",
                referencia
        );
    }


    public static ResultadoPos rechazado() {

        return new ResultadoPos(
                false,
                "TARJETA_RECHAZADA",
                null
        );
    }
}