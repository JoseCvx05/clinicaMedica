package com.proyecto.clinicamedica.model.caja;


/**
 * =========================================================
 * TIPO DE COBRO EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Representa las opciones que el Cajero puede seleccionar
 * en la interfaz:
 *
 * - Efectivo
 * - Visa
 * - Mastercard
 * - Débito
 *
 * IMPORTANTE:
 *
 * Este enum NO sustituye al catálogo FormaPago.
 *
 * FormaPago representa:
 *
 * - Efectivo
 * - Tarjeta de crédito
 * - Tarjeta de débito
 *
 * Mientras que TipoCobroCaja representa las opciones
 * específicas que se presentan en CU-06.
 *
 * =========================================================
 */
public enum TipoCobroCaja {


    // =====================================================
    // OPCIONES
    // =====================================================

    EFECTIVO(
            "Efectivo",
            "Efectivo",
            null,
            false
    ),


    VISA(
            "Visa",
            "Tarjeta de crédito",
            "VISA",
            true
    ),


    MASTERCARD(
            "Mastercard",
            "Tarjeta de crédito",
            "MASTERCARD",
            true
    ),


    DEBITO(
            "Débito",
            "Tarjeta de débito",
            "DEBITO",
            true
    );


    // =====================================================
    // CAMPOS
    // =====================================================

    /**
     * Texto que verá el Cajero.
     */
    private final String etiqueta;


    /**
     * Nombre exacto utilizado para localizar
     * FormaPago en el catálogo de base de datos.
     */
    private final String nombreFormaPago;


    /**
     * Tipo de tarjeta almacenado en Pago.
     *
     * Para EFECTIVO es null.
     */
    private final String tipoTarjeta;


    /**
     * Indica si el cobro requiere procesamiento
     * mediante terminal POS.
     */
    private final boolean tarjeta;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    TipoCobroCaja(
            String etiqueta,
            String nombreFormaPago,
            String tipoTarjeta,
            boolean tarjeta
    ) {

        this.etiqueta =
                etiqueta;

        this.nombreFormaPago =
                nombreFormaPago;

        this.tipoTarjeta =
                tipoTarjeta;

        this.tarjeta =
                tarjeta;
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public String getEtiqueta() {

        return etiqueta;
    }


    public String getNombreFormaPago() {

        return nombreFormaPago;
    }


    public String getTipoTarjeta() {

        return tipoTarjeta;
    }


    public boolean isTarjeta() {

        return tarjeta;
    }


    public boolean isEfectivo() {

        return this == EFECTIVO;
    }
}