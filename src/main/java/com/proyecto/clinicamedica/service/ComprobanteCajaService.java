package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.repository.PagoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;


/**
 * =========================================================
 * SERVICIO: COMPROBANTE DE PAGO EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Responsabilidades:
 *
 * - Recuperar un pago aprobado.
 * - Construir los datos del comprobante.
 * - Permitir consultar nuevamente un comprobante
 *   previamente emitido.
 *
 * RNF-033:
 *
 * El comprobante puede reimprimirse sin límite.
 *
 * IMPORTANTE:
 *
 * El comprobante NO se almacena en otra tabla.
 *
 * Se reconstruye a partir de:
 *
 * Pago
 *   -> Cita
 *      -> Paciente
 *      -> Médico
 *      -> Especialidad
 *      -> Sucursal
 *
 * =========================================================
 */
@Service
public class ComprobanteCajaService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final PagoRepository pagoRepository;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ComprobanteCajaService(

            PagoRepository pagoRepository,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.pagoRepository =
                pagoRepository;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // OBTENER COMPROBANTE
    // =====================================================

    @Transactional(readOnly = true)
    public ComprobanteCaja obtener(
            String numeroTransaccion
    ) {

        String numero =
                normalizar(
                        numeroTransaccion
                );


        if (numero.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el número de transacción."
            );
        }


        Pago pago =
                pagoRepository
                        .buscarComprobanteCaja(
                                numero
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró un comprobante "
                                                        + "de pago aprobado para "
                                                        + "la transacción indicada."
                                        )
                        );


        return construirComprobante(
                pago
        );
    }


    // =====================================================
    // CONSTRUIR COMPROBANTE
    // =====================================================

    private ComprobanteCaja construirComprobante(
            Pago pago
    ) {

        // =================================================
        // FORMA DE PAGO VISIBLE
        // =================================================

        String formaPago =
                construirFormaPagoVisible(
                        pago
                );


        // =================================================
        // REFERENCIA DE TARJETA
        // =================================================

        String referenciaTarjeta =
                construirReferenciaTarjeta(
                        pago
                );


        // =================================================
        // DETALLE DEL SERVICIO
        // =================================================

        String detalleServicio =
                "Consulta médica - Cita No. "
                        + pago.getCita()
                        .getId()
                        + " - "
                        + pago.getCita()
                        .getEspecialidad()
                        .getNombre();


        // =================================================
        // DATOS DEL CAJERO
        // =================================================

        String nombreCajero =
                pago.getCajero() != null
                        ? pago.getCajero()
                        .getNombreCompleto()
                        : null;


        // =================================================
        // RESULTADO
        // =================================================

        return new ComprobanteCaja(

                pago.getNumeroTransaccion(),

                pago.getCita()
                        .getId(),

                pago.getCita()
                        .getPaciente()
                        .getNombreCompleto(),

                pago.getCita()
                        .getEspecialidad()
                        .getNombre(),

                pago.getCita()
                        .getMedico()
                        .getNombreCompleto(),

                pago.getCita()
                        .getSucursal()
                        .getNombre(),

                convertirAZonaHoraria(
                        pago.getCita()
                                .getFechaHoraCita()
                ),

                normalizarMonto(
                        pago.getMonto()
                ),

                formaPago,

                normalizarMonto(
                        pago.getMontoRecibido()
                ),

                normalizarMonto(
                        pago.getCambio()
                ),

                referenciaTarjeta,

                convertirAZonaHoraria(
                        pago.getFechaHoraPago()
                ),

                detalleServicio,

                nombreCajero
        );
    }


    // =====================================================
    // FORMA DE PAGO VISIBLE
    // =====================================================

    private String construirFormaPagoVisible(
            Pago pago
    ) {

        if (pago.getTipoTarjeta() != null
                && !pago.getTipoTarjeta()
                .isBlank()) {

            return switch (
                    pago.getTipoTarjeta()
                            .trim()
                            .toUpperCase()
                    ) {

                case "VISA" ->
                        "Visa";

                case "MASTERCARD" ->
                        "Mastercard";

                case "DEBITO" ->
                        "Débito";

                default ->
                        pago.getFormaPago() != null
                                ? pago.getFormaPago()
                                .getNombre()
                                : "Tarjeta";
            };
        }


        if (pago.getFormaPago() != null
                && pago.getFormaPago()
                .getNombre() != null) {

            return pago.getFormaPago()
                    .getNombre();
        }


        return "No especificada";
    }


    // =====================================================
    // REFERENCIA DE TARJETA
    // =====================================================

    private String construirReferenciaTarjeta(
            Pago pago
    ) {

        if (pago.getUltimos4Tarjeta() == null
                || pago.getUltimos4Tarjeta()
                .isBlank()) {

            return null;
        }


        /*
         * Únicamente mostramos los últimos cuatro
         * dígitos que ya fueron persistidos.
         *
         * Nunca existe acceso al número completo
         * de tarjeta.
         */
        return "**** "
                + pago.getUltimos4Tarjeta();
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
    // ZONA HORARIA
    // =====================================================

    private OffsetDateTime convertirAZonaHoraria(
            OffsetDateTime fechaHora
    ) {

        if (fechaHora == null) {

            return null;
        }


        return fechaHora
                .atZoneSameInstant(
                        zonaHoraria
                )
                .toOffsetDateTime();
    }


    // =====================================================
    // NORMALIZAR TEXTO
    // =====================================================

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // PAYMENT RECEIPT
    // =====================================================

    /**
     * Modelo de lectura utilizado por la vista
     * PaymentReceipt de CU-06.
     */
    public record ComprobanteCaja(

            String numeroTransaccion,

            Integer numeroCita,

            String nombrePaciente,

            String especialidad,

            String medico,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            BigDecimal montoPagado,

            String formaPago,

            BigDecimal montoRecibido,

            BigDecimal cambio,

            String referenciaTarjeta,

            OffsetDateTime fechaHoraTransaccion,

            String detalleServicio,

            String cajero

    ) {


        // =================================================
        // ¿PAGO EN EFECTIVO?
        // =================================================

        public boolean esEfectivo() {

            return montoRecibido != null;
        }


        // =================================================
        // ¿PAGO CON TARJETA?
        // =================================================

        public boolean esTarjeta() {

            return referenciaTarjeta != null;
        }
    }
}