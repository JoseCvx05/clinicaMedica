package com.proyecto.clinicamedica.dto.signos;

import java.math.BigDecimal;
import com.proyecto.clinicamedica.service.signos.DatosSignosVitalesEntrada;

/**
 * =========================================================
 * DTO: REGISTRO DE SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Recibe los datos capturados por Enfermería.
 *
 * IMPORTANTE:
 *
 * - idCita se enviará como campo oculto.
 * - idEnfermero puede precargarse como contexto oculto,
 *   pero NUNCA se utilizará como fuente confiable.
 * - El enfermero real se obtiene de la sesión autenticada.
 *
 * Las validaciones clínicas definitivas se realizarán
 * en SignosVitalesService.
 *
 * =========================================================
 */
public class RegistroSignosVitalesDTO
        implements DatosSignosVitalesEntrada {


    // =====================================================
    // CONTEXTO
    // =====================================================

    private Integer idCita;

    private Integer idEnfermero;


    // =====================================================
    // PRESIÓN ARTERIAL
    // =====================================================

    private Short presionSistolica;

    private Short presionDiastolica;


    // =====================================================
    // TEMPERATURA
    // =====================================================

    private BigDecimal temperatura;


    // =====================================================
    // PESO
    // =====================================================

    private BigDecimal peso;


    // =====================================================
    // TALLA
    // =====================================================

    private BigDecimal talla;


    // =====================================================
    // FRECUENCIA CARDÍACA
    // =====================================================

    private Short frecuenciaCardiaca;


    // =====================================================
    // EMERGENCIA
    // =====================================================

    private Boolean esEmergencia = false;


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getIdCita() {

        return idCita;
    }


    public void setIdCita(
            Integer idCita
    ) {

        this.idCita =
                idCita;
    }


    public Integer getIdEnfermero() {

        return idEnfermero;
    }


    public void setIdEnfermero(
            Integer idEnfermero
    ) {

        this.idEnfermero =
                idEnfermero;
    }


    public Short getPresionSistolica() {

        return presionSistolica;
    }


    public void setPresionSistolica(
            Short presionSistolica
    ) {

        this.presionSistolica =
                presionSistolica;
    }


    public Short getPresionDiastolica() {

        return presionDiastolica;
    }


    public void setPresionDiastolica(
            Short presionDiastolica
    ) {

        this.presionDiastolica =
                presionDiastolica;
    }


    public BigDecimal getTemperatura() {

        return temperatura;
    }


    public void setTemperatura(
            BigDecimal temperatura
    ) {

        this.temperatura =
                temperatura;
    }


    public BigDecimal getPeso() {

        return peso;
    }


    public void setPeso(
            BigDecimal peso
    ) {

        this.peso =
                peso;
    }


    public BigDecimal getTalla() {

        return talla;
    }


    public void setTalla(
            BigDecimal talla
    ) {

        this.talla =
                talla;
    }


    public Short getFrecuenciaCardiaca() {

        return frecuenciaCardiaca;
    }


    public void setFrecuenciaCardiaca(
            Short frecuenciaCardiaca
    ) {

        this.frecuenciaCardiaca =
                frecuenciaCardiaca;
    }


    public Boolean getEsEmergencia() {

        return esEmergencia;
    }


    public void setEsEmergencia(
            Boolean esEmergencia
    ) {

        this.esEmergencia =
                esEmergencia;
    }
}