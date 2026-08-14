package com.proyecto.clinicamedica.dto.pago;

import java.util.UUID;


/**
 * =========================================================
 * DTO: FORMULARIO DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Contiene únicamente los datos necesarios para enviar
 * una solicitud de pago a la pasarela.
 *
 * IMPORTANTE:
 *
 * Los siguientes datos son transitorios:
 *
 * - numeroTarjeta
 * - nombreTitular
 * - vencimiento
 * - cvv
 *
 * NO deben persistirse en PostgreSQL.
 *
 * El CVV tampoco debe escribirse en logs.
 *
 * =========================================================
 */
public class PagoFormularioDTO {


    // =====================================================
    // DATOS DE TARJETA
    // =====================================================

    private String numeroTarjeta;

    private String nombreTitular;

    private String vencimiento;

    private String cvv;


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================

    /**
     * UUID generado por el servidor al abrir el formulario.
     *
     * Permite que un reenvío accidental de la misma
     * solicitud no produzca un segundo cobro.
     */
    private UUID idempotencyKey;


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(
            String numeroTarjeta
    ) {
        this.numeroTarjeta =
                numeroTarjeta;
    }


    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(
            String nombreTitular
    ) {
        this.nombreTitular =
                nombreTitular;
    }


    public String getVencimiento() {
        return vencimiento;
    }

    public void setVencimiento(
            String vencimiento
    ) {
        this.vencimiento =
                vencimiento;
    }


    public String getCvv() {
        return cvv;
    }

    public void setCvv(
            String cvv
    ) {
        this.cvv =
                cvv;
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