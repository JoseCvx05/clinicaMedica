package com.proyecto.clinicamedica.dto.caja;

import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * =========================================================
 * DTO: COBRO EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Contiene únicamente los datos que el Cajero puede
 * introducir durante un cobro.
 *
 * IMPORTANTE:
 *
 * NO contiene:
 *
 * - Número completo de tarjeta.
 * - CVV.
 * - Fecha de vencimiento.
 * - Monto total de la consulta.
 *
 * El monto total siempre será calculado nuevamente
 * en backend a partir de:
 *
 * Cita
 *   -> Sucursal
 *   -> Especialidad
 *   -> precioConsulta
 *
 * Esto evita manipulación desde el navegador.
 *
 * =========================================================
 */
public class CobroCajaDTO {


    // =====================================================
    // CITA
    // =====================================================

    /**
     * Cita que se pretende cobrar.
     */
    private Integer idCita;


    // =====================================================
    // MÉTODO DE COBRO
    // =====================================================

    /**
     * EFECTIVO
     * VISA
     * MASTERCARD
     * DEBITO
     */
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
     * Evita que un doble clic o reenvío del formulario
     * produzca dos cobros.
     *
     * El valor será generado por el servidor.
     */
    private UUID idempotencyKey;


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