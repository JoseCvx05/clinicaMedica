
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * =========================================================
 * CU-15 - ENTIDAD: DETALLE DE BITÁCORA DE INVENTARIO
 * =========================================================
 *
 * Complementa MovimientoInventarioFarmacia.
 *
 * Conserva la compatibilidad con:
 *
 * - CU-11: Despacho de medicamentos.
 * - CU-14: Inventario de farmacia.
 *
 * Almacena:
 *
 * - Tipo específico de movimiento (0 a 6).
 * - Costo unitario de compra.
 * - Estado activo / inactivo del registro.
 * - Versión para control de concurrencia.
 *
 * Los movimientos históricos permanecen intactos.
 *
 * No modifica existencias directamente.
 *
 * =========================================================
 */
@Entity
@Table(name = "movimiento_inventario_bitacora_detalle")
public class MovimientoBitacoraDetalle {

    // =====================================================
    // IDENTIFICADOR Y MOVIMIENTO HISTÓRICO
    // =====================================================

    @Id
    @Column(name = "id_movimiento")
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @MapsId
    @JoinColumn(
            name = "id_movimiento",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bitacora_detalle_movimiento"
            )
    )
    private MovimientoInventarioFarmacia movimiento;

    // =====================================================
    // TIPO DE MOVIMIENTO
    // =====================================================
    //
    // Códigos:
    // 0 - Compra
    // 1 - Devolución
    // 2 - Venta
    // 3 - Reclamo
    // 4 - Ajuste+
    // 5 - Ajuste-
    // 6 - Despacho
    //
    // Puede ser NULL únicamente para representar
    // un movimiento histórico anterior al CU-15.
    // =====================================================

    @Column(name = "codigo_tipo")
    private Integer codigoTipo;

    // =====================================================
    // COSTO UNITARIO
    // =====================================================
    //
    // Obligatorio únicamente para compras.
    // =====================================================

    @Column(
            name = "costo_unitario",
            precision = 12,
            scale = 2
    )
    private BigDecimal costoUnitario;

    // =====================================================
    // ESTADO DEL REGISTRO
    // =====================================================
    //
    // Desactivar la bitácora NO revierte el stock.
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo = true;

    // =====================================================
    // CONCURRENCIA
    // =====================================================

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    // =====================================================
    // CONSTRUCTOR JPA
    // =====================================================

    protected MovimientoBitacoraDetalle() {
    }

    // =====================================================
    // CONSTRUCTOR PARA MOVIMIENTOS DEL CU-15
    // =====================================================

    public MovimientoBitacoraDetalle(
            MovimientoInventarioFarmacia movimiento,
            TipoMovimientoBitacora tipo,
            BigDecimal costoUnitario
    ) {

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de movimiento."
            );
        }

        this.movimiento = movimiento;
        this.codigoTipo = tipo.getCodigo();
        this.costoUnitario = costoUnitario;
        this.activo = true;

        validar();
    }

    // =====================================================
    // MOVIMIENTOS HISTÓRICOS SIN TIPO ESPECÍFICO
    // =====================================================

    public static MovimientoBitacoraDetalle paraHistorico(
            MovimientoInventarioFarmacia movimiento
    ) {

        MovimientoBitacoraDetalle detalle =
                new MovimientoBitacoraDetalle();

        detalle.movimiento = movimiento;
        detalle.activo = true;

        detalle.validar();

        return detalle;
    }

    // =====================================================
    // VALIDACIONES
    // =====================================================

    @PrePersist
    @PreUpdate
    private void validar() {

        if (movimiento == null
                || movimiento.getId() == null) {

            throw new IllegalStateException(
                    "El movimiento de inventario no es válido."
            );
        }

        if (activo == null) {

            throw new IllegalStateException(
                    "El estado del movimiento es obligatorio."
            );
        }

        TipoMovimientoBitacora tipo = null;

        if (codigoTipo != null) {
            tipo = TipoMovimientoBitacora.desdeCodigo(
                    codigoTipo
            );
        }

        if (tipo == TipoMovimientoBitacora.COMPRA
                && costoUnitario == null) {

            throw new IllegalArgumentException(
                    "El costo unitario es obligatorio para compras"
            );
        }

        if (costoUnitario != null) {

            if (costoUnitario.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                throw new IllegalArgumentException(
                        "El costo unitario debe ser mayor a 0"
                );
            }

            if (costoUnitario.scale() > 2) {

                throw new IllegalArgumentException(
                        "El costo unitario debe tener máximo 2 decimales"
                );
            }

            if (costoUnitario.precision()
                    - costoUnitario.scale() > 10) {

                throw new IllegalArgumentException(
                        "El costo unitario supera el límite permitido."
                );
            }
        }

        if (tipo != null
                && tipo != TipoMovimientoBitacora.COMPRA
                && costoUnitario != null) {

            throw new IllegalArgumentException(
                    "El costo unitario solo se permite para compras."
            );
        }
    }

    // =====================================================
    // ACTIVAR / DESACTIVAR REGISTRO
    // =====================================================

    public void cambiarEstado(boolean activo) {
        this.activo = activo;
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public MovimientoInventarioFarmacia getMovimiento() {
        return movimiento;
    }

    public Integer getCodigoTipo() {
        return codigoTipo;
    }

    public TipoMovimientoBitacora getTipo() {

        if (codigoTipo == null) {
            return null;
        }

        return TipoMovimientoBitacora.desdeCodigo(
                codigoTipo
        );
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Long getVersion() {
        return version;
    }
}
