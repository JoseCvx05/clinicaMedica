package com.proyecto.clinicamedica.service.caja;

import com.proyecto.clinicamedica.model.caja.ResultadoPos;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * =========================================================
 * TERMINAL POS SIMULADA
 * =========================================================
 *
 * CU-06.
 *
 * Simula el comportamiento de un dispositivo POS mientras
 * no existe integración con un proveedor real.
 *
 * REGLA DE PRUEBA:
 *
 * últimos4 = 0000
 *
 *      -> TARJETA RECHAZADA
 *
 * cualquier otro valor válido
 *
 *      -> APROBADA
 *
 * Esto permite probar formalmente FA04.
 *
 * =========================================================
 */
@Component
public class TerminalPosSimulada
        implements TerminalPos {


    private static final String TARJETA_PRUEBA_RECHAZADA =
            "0000";


    @Override
    public ResultadoPos procesar(

            BigDecimal monto,

            TipoCobroCaja tipoTarjeta,

            String ultimos4Tarjeta
    ) {

        // =================================================
        // VALIDACIÓN DEFENSIVA
        // =================================================

        if (monto == null
                || monto.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "El monto enviado al POS no es válido."
            );
        }


        if (tipoTarjeta == null
                || !tipoTarjeta.isTarjeta()) {

            throw new IllegalArgumentException(
                    "El tipo de tarjeta enviado al POS no es válido."
            );
        }


        if (ultimos4Tarjeta == null
                || !ultimos4Tarjeta.matches("\\d{4}")) {

            throw new IllegalArgumentException(
                    "La referencia de tarjeta no es válida."
            );
        }


        // =================================================
        // FA04 - SIMULAR RECHAZO
        // =================================================

        if (TARJETA_PRUEBA_RECHAZADA
                .equals(
                        ultimos4Tarjeta
                )) {

            return ResultadoPos.rechazado();
        }


        // =================================================
        // APROBACIÓN SIMULADA
        // =================================================

        String referencia =
                "POS-"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();


        return ResultadoPos.aprobado(
                referencia
        );
    }
}