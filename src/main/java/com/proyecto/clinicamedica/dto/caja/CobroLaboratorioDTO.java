package com.proyecto.clinicamedica.dto.caja;

import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * =========================================================
 * DTO: COBRO DE LABORATORIO EN CAJA
 * =========================================================
 *
 * CU-09 / CU-16.
 *
 * Contiene únicamente los datos que el Cajero puede
 * introducir durante el cobro de una orden de laboratorio.
 *
 * IMPORTANTE:
 *
 * NO contiene:
 *
 * - Monto total de la orden.
 * - Nombre de exámenes.
 * - Precio individual de exámenes.
 * - Número completo de tarjeta.
 * - CVV.
 * - Fecha de vencimiento.
 *
 * El monto total siempre se calcula nuevamente
 * en backend utilizando:
 *
 * OrdenLaboratorio
 *      -> DetalleOrdenLaboratorio
 *      -> precioExamen
 *
 * Esto evita manipulación del monto desde el navegador.
 *
 * =========================================================
 */
public class CobroLaboratorioDTO {


    // =====================================================
    // ORDEN DE LABORATORIO
    // =====================================================

    /**
     * Orden de laboratorio que se pretende cobrar.
     */
    private Integer idOrdenLaboratorio;


    // =====================================================
    // MÉTODO DE COBRO
    // =====================================================
    //
    // Valores permitidos actualmente:
    //
    // EFECTIVO
    // VISA
    // MASTERCARD
    // DEBITO
    //
    // =====================================================

    private TipoCobroCaja tipoCobro;


    // =====================================================
    // EFECTIVO
    // =====================================================

    /**
     * Cantidad entregada por el paciente.
     *
     * Solo aplica cuando:
     *
     * tipoCobro = EFECTIVO
     */
    private BigDecimal montoRecibido;


    // =====================================================
    // TARJETA PRESENCIAL
    // =====================================================

    /**
     * Últimos cuatro dígitos utilizados únicamente
     * como referencia de la transacción presencial.
     *
     * Nunca se recibe el número completo de tarjeta.
     */
    private String ultimos4Tarjeta;


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================

    /**
     * Identificador único del intento de cobro.
     *
     * Evita que:
     *
     * - un doble clic;
     * - recargar;
     * - reenviar el formulario;
     *
     * genere dos cobros de la misma solicitud.
     *
     * El valor será generado por el servidor.
     */
    private UUID idempotencyKey;


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


    public TipoCobroCaja getTipoCobro() {

        return tipoCobro;
    }


    public void setTipoCobro(
            TipoCobroCaja tipoCobro
    ) {

        this.tipoCobro =
                tipoCobro;
    }


    public BigDecimal getMontoRecibido() {

        return montoRecibido;
    }


    public void setMontoRecibido(
            BigDecimal montoRecibido
    ) {

        this.montoRecibido =
                montoRecibido;
    }


    public String getUltimos4Tarjeta() {

        return ultimos4Tarjeta;
    }


    public void setUltimos4Tarjeta(
            String ultimos4Tarjeta
    ) {

        this.ultimos4Tarjeta =
                ultimos4Tarjeta;
    }


    public UUID getIdempotencyKey() {

        return idempotencyKey;
    }


    public void setIdempotencyKey(
            UUID idempotencyKey
    ) {

        this.idempotencyKey =
                idempotencyKey;
    }
}