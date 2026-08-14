package com.proyecto.clinicamedica.service.pago;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * =========================================================
 * PASARELA DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Abstracción de la pasarela externa.
 *
 * Permite utilizar una implementación simulada durante
 * desarrollo y posteriormente sustituirla por una
 * pasarela real sin modificar PagoService.
 *
 * IMPORTANTE:
 *
 * Los datos sensibles recibidos en Solicitud:
 *
 * - número de tarjeta;
 * - CVV;
 * - vencimiento;
 * - titular;
 *
 * son únicamente transitorios.
 *
 * Nunca deben persistirse ni escribirse en logs.
 * =========================================================
 */
public interface PasarelaPago {


    // =====================================================
    // PROCESAR
    // =====================================================

    Resultado procesar(
            Solicitud solicitud
    );


    // =====================================================
    // SOLICITUD
    // =====================================================

    record Solicitud(

            String numeroTarjeta,

            String nombreTitular,

            String vencimiento,

            String cvv,

            BigDecimal monto,

            UUID idempotencyKey
    ) {
    }


    // =====================================================
    // RESULTADO
    // =====================================================

    record Resultado(

            CodigoResultado codigo,

            String numeroTransaccion,

            TipoTarjeta tipoTarjeta
    ) {

        public boolean aprobado() {

            return codigo ==
                    CodigoResultado.APROBADO;
        }
    }


    // =====================================================
    // TIPO DE TARJETA
    // =====================================================

    enum TipoTarjeta {

        CREDITO,

        DEBITO
    }


    // =====================================================
    // RESULTADOS NORMALIZADOS
    // =====================================================

    enum CodigoResultado {

        APROBADO,

        FONDOS_INSUFICIENTES,

        TARJETA_INVALIDA,

        TARJETA_VENCIDA,

        RECHAZO_BANCARIO,

        ERROR_PROCESAMIENTO,

        ERROR_COMUNICACION
    }
}