package com.proyecto.clinicamedica.dto.caja;


/**
 * =========================================================
 * DTO: BÚSQUEDA DE CITA EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Permite buscar una cita pendiente de pago mediante:
 *
 * - Número de cita.
 * - DPI del paciente.
 *
 * La validación específica del criterio se realizará
 * en CobroCajaService.
 *
 * =========================================================
 */
public class CajaBusquedaDTO {


    // =====================================================
    // TIPOS DE BÚSQUEDA
    // =====================================================

    public static final String TIPO_CITA =
            "CITA";

    public static final String TIPO_DPI =
            "DPI";


    // =====================================================
    // CAMPOS
    // =====================================================

    /**
     * CITA o DPI.
     */
    private String tipoBusqueda =
            TIPO_CITA;


    /**
     * Número de cita o DPI introducido.
     */
    private String valorBusqueda;


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getTipoBusqueda() {

        return tipoBusqueda;
    }


    public void setTipoBusqueda(
            String tipoBusqueda
    ) {

        this.tipoBusqueda =
                tipoBusqueda;
    }


    public String getValorBusqueda() {

        return valorBusqueda;
    }


    public void setValorBusqueda(
            String valorBusqueda
    ) {

        this.valorBusqueda =
                valorBusqueda;
    }
}