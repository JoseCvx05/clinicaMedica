package com.proyecto.clinicamedica.service.caja;

import com.proyecto.clinicamedica.model.caja.ResultadoProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.SolicitudProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * =========================================================
 * PROCESADOR: COBRO EN EFECTIVO
 * =========================================================
 *
 * CU-06 - Flujo Normal Básico.
 *
 * Responsabilidades:
 *
 * - Validar monto recibido.
 * - Compararlo contra el monto total.
 * - Rechazar montos insuficientes.
 * - Calcular el cambio.
 * - Generar el mensaje establecido por CU-06.
 *
 * No persiste pagos.
 * No modifica citas.
 *
 * Esas responsabilidades corresponden a CobroCajaService.
 *
 * =========================================================
 */
@Component
public class ProcesadorCobroEfectivo
        implements ProcesadorCobroCaja {


    // =====================================================
    // CÓDIGOS
    // =====================================================

    private static final String CODIGO_APROBADO =
            "APROBADO";


    private static final String CODIGO_EFECTIVO_INSUFICIENTE =
            "EFECTIVO_INSUFICIENTE";


    private static final String CODIGO_MONTO_REQUERIDO =
            "MONTO_RECIBIDO_REQUERIDO";


    // =====================================================
    // SOPORTA
    // =====================================================

    @Override
    public boolean soporta(
            TipoCobroCaja tipoCobro
    ) {

        return TipoCobroCaja.EFECTIVO
                == tipoCobro;
    }


    // =====================================================
    // PROCESAR
    // =====================================================

    @Override
    public ResultadoProcesamientoCobro procesar(
            SolicitudProcesamientoCobro solicitud
    ) {

        // =================================================
        // VALIDACIÓN DEFENSIVA
        // =================================================

        if (solicitud == null
                || solicitud.tipoCobro() == null
                || !soporta(solicitud.tipoCobro())) {

            throw new IllegalArgumentException(
                    "La solicitud no corresponde a un cobro en efectivo."
            );
        }


        // =================================================
        // MONTO TOTAL
        // =================================================

        BigDecimal montoTotal =
                normalizarMonto(
                        solicitud.montoTotal()
                );


        if (montoTotal == null
                || montoTotal.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "El monto total de la consulta no es válido."
            );
        }


        // =================================================
        // MONTO RECIBIDO
        // =================================================

        BigDecimal montoRecibido =
                normalizarMonto(
                        solicitud.montoRecibido()
                );


        if (montoRecibido == null) {

            return ResultadoProcesamientoCobro.rechazado(

                    CODIGO_MONTO_REQUERIDO,

                    "Debe ingresar el monto recibido.",

                    null,

                    null,

                    null,

                    null
            );
        }


        // =================================================
        // MONTO NO VÁLIDO
        // =================================================

        if (montoRecibido.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return ResultadoProcesamientoCobro.rechazado(

                    CODIGO_MONTO_REQUERIDO,

                    "Debe ingresar un monto recibido mayor a Q0.00.",

                    montoRecibido,

                    null,

                    null,

                    null
            );
        }


        // =================================================
        // MONTO INSUFICIENTE
        // =================================================
        //
        // Mensaje exacto establecido por CU-06.
        // =================================================

        if (montoRecibido.compareTo(
                montoTotal
        ) < 0) {

            return ResultadoProcesamientoCobro.rechazado(

                    CODIGO_EFECTIVO_INSUFICIENTE,

                    "El monto recibido (Q"
                            + formatearMonto(
                            montoRecibido
                    )
                            + ") es menor al monto a cobrar (Q"
                            + formatearMonto(
                            montoTotal
                    )
                            + ")",

                    montoRecibido,

                    null,

                    null,

                    null
            );
        }


        // =================================================
        // CALCULAR CAMBIO
        // =================================================

        BigDecimal cambio =
                montoRecibido
                        .subtract(
                                montoTotal
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        // =================================================
        // MENSAJE
        // =================================================

        String mensaje =
                "Monto recibido: Q"
                        + formatearMonto(
                        montoRecibido
                )
                        + ". Cambio a devolver: Q"
                        + formatearMonto(
                        cambio
                )
                        + ".";


        return ResultadoProcesamientoCobro.aprobado(

                CODIGO_APROBADO,

                mensaje,

                montoRecibido,

                cambio,

                null,

                null
        );
    }


    // =====================================================
    // NORMALIZAR MONTO
    // =====================================================

    private BigDecimal normalizarMonto(
            BigDecimal monto
    ) {

        if (monto == null) {

            return null;
        }


        return monto.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    // =====================================================
    // FORMATEAR MONTO
    // =====================================================

    private String formatearMonto(
            BigDecimal monto
    ) {

        return monto
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }
}