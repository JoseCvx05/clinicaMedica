package com.proyecto.clinicamedica.service.pago;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;


/**
 * =========================================================
 * PASARELA DE PAGO SIMULADA
 * =========================================================
 *
 * Implementación utilizada únicamente para desarrollo
 * y pruebas de CU-04.
 *
 * No realiza cobros reales.
 * =========================================================
 */
@Component
@Profile("!prod")
public class PasarelaPagoSimulada
        implements PasarelaPago {


    // =====================================================
    // PROCESAR
    // =====================================================

    @Override
    public Resultado procesar(
            Solicitud solicitud
    ) {

        if (solicitud == null) {

            return error(
                    CodigoResultado.ERROR_PROCESAMIENTO
            );
        }


        String numero =
                solicitud.numeroTarjeta();


        if (numero == null
                || numero.length() < 4) {

            return error(
                    CodigoResultado.TARJETA_INVALIDA
            );
        }


        // =================================================
        // ÚLTIMOS CUATRO
        // =================================================
        //
        // Se utilizan únicamente para controlar escenarios
        // de prueba.
        //
        // NO se almacenan.
        // =================================================

        String ultimosCuatro =
                numero.substring(
                        numero.length() - 4
                );


        // =================================================
        // ESCENARIOS DE ERROR
        // =================================================

        if ("0001".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.FONDOS_INSUFICIENTES
            );
        }


        if ("0002".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.TARJETA_INVALIDA
            );
        }


        if ("0003".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.TARJETA_VENCIDA
            );
        }


        if ("0004".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.RECHAZO_BANCARIO
            );
        }


        if ("0005".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.ERROR_PROCESAMIENTO
            );
        }


        if ("0006".equals(
                ultimosCuatro
        )) {

            return error(
                    CodigoResultado.ERROR_COMUNICACION
            );
        }


        // =================================================
        // TIPO DE TARJETA SIMULADO
        // =================================================
        //
        // IMPORTANTE:
        //
        // Esta regla existe únicamente para pruebas.
        //
        // Una integración bancaria real obtendrá esta
        // información de la propia pasarela.
        //
        // 4... -> crédito
        // 5... -> débito
        // =================================================

        TipoTarjeta tipoTarjeta =
                numero.startsWith("5")
                        ? TipoTarjeta.DEBITO
                        : TipoTarjeta.CREDITO;


        // =================================================
        // APROBADO
        // =================================================

        return new Resultado(
                CodigoResultado.APROBADO,
                generarNumeroTransaccion(
                        solicitud.idempotencyKey()
                ),
                tipoTarjeta
        );
    }


    // =====================================================
    // RESULTADO DE ERROR
    // =====================================================

    private Resultado error(
            CodigoResultado codigo
    ) {

        return new Resultado(
                codigo,
                null,
                null
        );
    }


    // =====================================================
    // GENERAR TRANSACCIÓN
    // =====================================================

    private String generarNumeroTransaccion(
            UUID idempotencyKey
    ) {

        if (idempotencyKey == null) {

            return null;
        }


        return "TX-"
                + idempotencyKey
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }
}