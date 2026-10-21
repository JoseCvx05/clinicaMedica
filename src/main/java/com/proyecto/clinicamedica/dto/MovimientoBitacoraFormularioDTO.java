
package com.proyecto.clinicamedica.dto;

import java.math.BigDecimal;

/**
 * =========================================================
 * CU-15 - DTO: FORMULARIO DE MOVIMIENTOS DE INVENTARIO
 * =========================================================
 *
 * RN-CU13-01
 *
 * Recibe los datos del formulario:
 *
 * - Medicamento.
 * - Sucursal.
 * - Tipo de movimiento (0 a 5).
 * - Cantidad.
 * - Costo unitario.
 * - Número de referencia.
 * - Motivo / Notas.
 *
 * IMPORTANTE:
 *
 * - El tipo 6 (DESPACHO) no se registra manualmente.
 * - No se recibe el stock anterior ni el stock nuevo.
 * - No se recibe el usuario responsable.
 * - El backend obtiene esos datos desde PostgreSQL
 *   y desde el usuario autenticado.
 * - Las validaciones específicas se realizan
 *   posteriormente en el Service.
 *
 * =========================================================
 */
public class MovimientoBitacoraFormularioDTO {

    // =====================================================
    // MEDICAMENTO
    // =====================================================

    private Integer idMedicamento;

    // =====================================================
    // SUCURSAL
    // =====================================================

    private Integer idSucursal;

    // =====================================================
    // TIPO DE MOVIMIENTO
    // =====================================================

    private Integer tipoMovimiento;

    // =====================================================
    // CANTIDAD
    // =====================================================

    private Integer cantidad;

    // =====================================================
    // COSTO UNITARIO
    // =====================================================

    private BigDecimal costoUnitario;

    // =====================================================
    // NÚMERO DE REFERENCIA
    // =====================================================

    private String numeroReferencia;

    // =====================================================
    // MOTIVO / NOTAS
    // =====================================================

    private String motivoNotas;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraFormularioDTO() {
    }

    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getIdMedicamento() {
        return idMedicamento;
    }

    public void setIdMedicamento(Integer idMedicamento) {
        this.idMedicamento = idMedicamento;
    }

    public Integer getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(Integer idSucursal) {
        this.idSucursal = idSucursal;
    }

    public Integer getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(Integer tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public String getNumeroReferencia() {
        return numeroReferencia;
    }

    public void setNumeroReferencia(String numeroReferencia) {
        this.numeroReferencia = numeroReferencia;
    }

    public String getMotivoNotas() {
        return motivoNotas;
    }

    public void setMotivoNotas(String motivoNotas) {
        this.motivoNotas = motivoNotas;
    }
}
