package com.proyecto.clinicamedica.dto.recepcion;


/**
 * =========================================================
 * DTO: BÚSQUEDA DE RECEPCIÓN
 * =========================================================
 *
 * CU-05 Recepción y Verificación de Cita.
 *
 * La interfaz utilizará botones de alternancia:
 *
 * - Por No. Cita
 * - Por DPI
 *
 * Solo se conserva el criterio seleccionado y el valor
 * escrito por el recepcionista.
 * =========================================================
 */
public class RecepcionBusquedaDTO {


    public static final String TIPO_CITA =
            "CITA";

    public static final String TIPO_DPI =
            "DPI";


    private String tipoBusqueda =
            TIPO_CITA;


    private String valorBusqueda;


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