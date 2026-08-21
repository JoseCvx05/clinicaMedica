package com.proyecto.clinicamedica.service.caja;

import com.proyecto.clinicamedica.model.caja.ResultadoPos;
import com.proyecto.clinicamedica.model.caja.ResultadoProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.SolicitudProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import org.springframework.stereotype.Component;


/**
 * =========================================================
 * PROCESADOR: COBRO CON TARJETA
 * =========================================================
 *
 * CU-06 - FA01 / FA04.
 *
 * Procesa:
 *
 * - Visa.
 * - Mastercard.
 * - Débito.
 *
 * Responsabilidades:
 *
 * - Validar los últimos 4 dígitos.
 * - Invocar TerminalPos.
 * - Interpretar aprobación/rechazo.
 *
 * No:
 *
 * - Guarda pagos.
 * - Modifica citas.
 * - Genera comprobantes.
 *
 * =========================================================
 */
@Component
public class ProcesadorCobroTarjeta
        implements ProcesadorCobroCaja {


    // =====================================================
    // MENSAJE EXACTO FA04
    // =====================================================

    private static final String MENSAJE_RECHAZO =
            "La transacción con tarjeta fue rechazada por el banco. "
                    + "Solicite al paciente otro método de pago.";


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final TerminalPos terminalPos;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ProcesadorCobroTarjeta(
            TerminalPos terminalPos
    ) {

        this.terminalPos =
                terminalPos;
    }


    // =====================================================
    // SOPORTA
    // =====================================================

    @Override
    public boolean soporta(
            TipoCobroCaja tipoCobro
    ) {

        return tipoCobro != null
                && tipoCobro.isTarjeta();
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
                || !soporta(
                solicitud.tipoCobro()
        )) {

            throw new IllegalArgumentException(
                    "La solicitud no corresponde a un cobro con tarjeta."
            );
        }


        if (solicitud.montoTotal() == null
                || solicitud.montoTotal()
                .signum() <= 0) {

            throw new IllegalArgumentException(
                    "El monto total de la consulta no es válido."
            );
        }


        // =================================================
        // ÚLTIMOS 4 DÍGITOS
        // =================================================

        String ultimos4 =
                normalizar(
                        solicitud.ultimos4Tarjeta()
                );


        if (ultimos4.isBlank()) {

            return ResultadoProcesamientoCobro.rechazado(

                    "ULTIMOS4_REQUERIDOS",

                    "Debe ingresar los últimos 4 dígitos de la tarjeta.",

                    null,

                    null,

                    solicitud.tipoCobro()
                            .getTipoTarjeta(),

                    null
            );
        }


        if (!ultimos4.matches("\\d{4}")) {

            return ResultadoProcesamientoCobro.rechazado(

                    "ULTIMOS4_INVALIDOS",

                    "Los últimos 4 dígitos de la tarjeta "
                            + "deben contener exactamente 4 números.",

                    null,

                    null,

                    solicitud.tipoCobro()
                            .getTipoTarjeta(),

                    ultimos4
            );
        }


        // =================================================
        // PROCESAR EN POS
        // =================================================

        ResultadoPos resultadoPos =
                terminalPos.procesar(

                        solicitud.montoTotal(),

                        solicitud.tipoCobro(),

                        ultimos4
                );


        // =================================================
        // FA04 - TARJETA RECHAZADA
        // =================================================

        if (!resultadoPos.aprobado()) {

            return ResultadoProcesamientoCobro.rechazado(

                    "TARJETA_RECHAZADA",

                    MENSAJE_RECHAZO,

                    null,

                    null,

                    solicitud.tipoCobro()
                            .getTipoTarjeta(),

                    ultimos4
            );
        }


        // =================================================
        // TARJETA APROBADA
        // =================================================

        return ResultadoProcesamientoCobro.aprobado(

                "APROBADO",

                "Transacción con tarjeta aprobada.",

                null,

                null,

                solicitud.tipoCobro()
                        .getTipoTarjeta(),

                ultimos4
        );
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }
}