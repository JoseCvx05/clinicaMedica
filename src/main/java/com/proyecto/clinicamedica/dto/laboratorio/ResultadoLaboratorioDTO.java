package com.proyecto.clinicamedica.dto.laboratorio;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


/**
 * =========================================================
 * DTO: RESULTADO DE LABORATORIO
 * =========================================================
 *
 * CU-09 Gestión de Laboratorio.
 *
 * Recibe únicamente los datos enviados desde
 * el formulario de captura de resultados.
 *
 * IMPORTANTE:
 *
 * El servidor volverá a validar:
 *
 * - orden;
 * - detalle;
 * - Laboratorista;
 * - sucursal;
 * - estado EN_PROCESO;
 * - pago previo;
 * - publicación;
 * - longitudes;
 * - integridad del examen.
 *
 * =========================================================
 */
public class ResultadoLaboratorioDTO {


    // =====================================================
    // ORDEN
    // =====================================================

    private Integer idOrdenLaboratorio;


    // =====================================================
    // DETALLE / EXAMEN
    // =====================================================

    private Integer idDetalleOrdenLaboratorio;


    // =====================================================
    // VALOR RESULTADO
    // =====================================================

    private String valorResultado;


    // =====================================================
    // UNIDAD
    // =====================================================

    private String unidadResultado;


    // =====================================================
    // FECHA RESULTADO
    // =====================================================

    @DateTimeFormat(
            pattern = "yyyy-MM-dd'T'HH:mm"
    )
    private LocalDateTime fechaResultado;


    // =====================================================
    // FUERA DE RANGO
    // =====================================================

    private boolean fueraRango;


    // =====================================================
    // NOTAS
    // =====================================================

    private String notasResultado;


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getIdOrdenLaboratorio() {

        return idOrdenLaboratorio;
    }


    public void setIdOrdenLaboratorio(
            Integer idOrdenLaboratorio
    ) {

        this.idOrdenLaboratorio =
                idOrdenLaboratorio;
    }


    public Integer getIdDetalleOrdenLaboratorio() {

        return idDetalleOrdenLaboratorio;
    }


    public void setIdDetalleOrdenLaboratorio(
            Integer idDetalleOrdenLaboratorio
    ) {

        this.idDetalleOrdenLaboratorio =
                idDetalleOrdenLaboratorio;
    }


    public String getValorResultado() {

        return valorResultado;
    }


    public void setValorResultado(
            String valorResultado
    ) {

        this.valorResultado =
                valorResultado;
    }


    public String getUnidadResultado() {

        return unidadResultado;
    }


    public void setUnidadResultado(
            String unidadResultado
    ) {

        this.unidadResultado =
                unidadResultado;
    }


    public LocalDateTime getFechaResultado() {

        return fechaResultado;
    }


    public void setFechaResultado(
            LocalDateTime fechaResultado
    ) {

        this.fechaResultado =
                fechaResultado;
    }


    public boolean isFueraRango() {

        return fueraRango;
    }


    public void setFueraRango(
            boolean fueraRango
    ) {

        this.fueraRango =
                fueraRango;
    }


    public String getNotasResultado() {

        return notasResultado;
    }


    public void setNotasResultado(
            String notasResultado
    ) {

        this.notasResultado =
                notasResultado;
    }
}