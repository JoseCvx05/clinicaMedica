package com.proyecto.clinicamedica.model.caja;

import java.math.BigDecimal;


/**
 * =========================================================
 * RESULTADO DE PROCESAMIENTO DE COBRO
 * =========================================================
 *
 * CU-06.
 *
 * Resultado normalizado producido por cualquiera de las
 * estrategias de cobro.
 *
 * Esto permite que CobroCajaService no necesite conocer
 * los detalles internos de:
 *
 * - Efectivo.
 * - Tarjeta.
 *
 * =========================================================
 */
public record ResultadoProcesamientoCobro(

        boolean aprobado,

        String codigoResultado,

        String mensaje,

        BigDecimal montoRecibido,

        BigDecimal cambio,

        String tipoTarjeta,

        String ultimos4Tarjeta

) {


    // =====================================================
    // APROBADO
    // =====================================================

    public static ResultadoProcesamientoCobro aprobado(

            String codigoResultado,

            String mensaje,

            BigDecimal montoRecibido,

            BigDecimal cambio,

            String tipoTarjeta,

            String ultimos4Tarjeta
    ) {

        return new ResultadoProcesamientoCobro(

                true,

                codigoResultado,

                mensaje,

                montoRecibido,

                cambio,

                tipoTarjeta,

                ultimos4Tarjeta
        );
    }


    // =====================================================
    // RECHAZADO
    // =====================================================

    public static ResultadoProcesamientoCobro rechazado(

            String codigoResultado,

            String mensaje,

            BigDecimal montoRecibido,

            BigDecimal cambio,

            String tipoTarjeta,

            String ultimos4Tarjeta
    ) {

        return new ResultadoProcesamientoCobro(

                false,

                codigoResultado,

                mensaje,

                montoRecibido,

                cambio,

                tipoTarjeta,

                ultimos4Tarjeta
        );
    }
}