
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * CU-14 - ENTIDAD: MOVIMIENTO DE INVENTARIO FARMACIA
 * =========================================================
 *
 * Catálogo 8 - Inventario de Medicamentos.
 *
 * Registra el historial de movimientos:
 *
 * - INICIAL: Existencias al crear el inventario.
 * - ENTRADA: Ingreso de medicamentos.
 * - SALIDA: Descuento de medicamentos.
 * - AJUSTE: Corrección controlada de existencias.
 *
 * Cada movimiento conserva:
 *
 * - Inventario afectado.
 * - Usuario responsable.
 * - Tipo de movimiento.
 * - Cantidad modificada.
 * - Existencias anteriores.
 * - Existencias nuevas.
 * - Origen de la operación.
 * - Referencia y observaciones.
 * - Fecha y hora.
 *
 * IMPORTANTE:
 *
 * - No modifica inventarios por sí misma.
 * - No sustituye la auditoría general.
 * - No modifica despachos históricos.
 * - Los movimientos se registrarán desde Services
 *   con permisos y transacciones.
 *
 * =========================================================
 */
@Entity
@Table(name = "movimiento_inventario_farmacia")
public class MovimientoInventarioFarmacia {

    // =====================================================
    // TIPOS DE MOVIMIENTO
    // =====================================================

    public enum TipoMovimiento {
        INICIAL,
        ENTRADA,
        SALIDA,
        AJUSTE
    }


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    // =====================================================
    // INVENTARIO AFECTADO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_inventario",
            nullable = false
    )
    private InventarioFarmacia inventario;


    // =====================================================
    // USUARIO RESPONSABLE
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_usuario",
            nullable = false
    )
    private Usuario usuario;


    // =====================================================
    // TIPO DE MOVIMIENTO
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_movimiento",
            nullable = false,
            length = 20
    )
    private TipoMovimiento tipoMovimiento;


    // =====================================================
    // CANTIDAD MODIFICADA
    // =====================================================

    @Column(
            name = "cantidad",
            nullable = false
    )
    private Integer cantidad;


    // =====================================================
    // EXISTENCIAS ANTERIORES
    // =====================================================

    @Column(
            name = "stock_anterior",
            nullable = false
    )
    private Integer stockAnterior;


    // =====================================================
    // EXISTENCIAS NUEVAS
    // =====================================================

    @Column(
            name = "stock_nuevo",
            nullable = false
    )
    private Integer stockNuevo;


    // =====================================================
    // ORIGEN DE LA OPERACIÓN
    // =====================================================

    @Column(
            name = "origen",
            nullable = false,
            length = 40
    )
    private String origen;


    // =====================================================
    // REFERENCIA A LA OPERACIÓN
    // =====================================================

    @Column(
            name = "id_referencia",
            length = 50
    )
    private String idReferencia;


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    @Column(
            name = "observaciones",
            length = 500
    )
    private String observaciones;


    // =====================================================
    // FECHA Y HORA
    // =====================================================
    //
    // PostgreSQL asigna CURRENT_TIMESTAMP.
    // Hibernate no modifica este campo.
    //
    // =====================================================

    @Column(
            name = "fecha_hora",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime fechaHora;


    // =====================================================
    // CONSTRUCTOR VACÍO JPA
    // =====================================================

    protected MovimientoInventarioFarmacia() {
    }


    // =====================================================
    // CONSTRUCTOR DE MOVIMIENTO
    // =====================================================

    public MovimientoInventarioFarmacia(
            InventarioFarmacia inventario,
            Usuario usuario,
            TipoMovimiento tipoMovimiento,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            String origen,
            String idReferencia,
            String observaciones
    ) {

        this.inventario = inventario;
        this.usuario = usuario;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.origen = origen;
        this.idReferencia = idReferencia;
        this.observaciones = observaciones;

        validar();
    }


    // =====================================================
    // VALIDACIONES ANTES DE GUARDAR
    // =====================================================

    @PrePersist
    private void validar() {

        if (inventario == null
                || inventario.getId() == null) {

            throw new IllegalStateException(
                    "El movimiento requiere un inventario válido."
            );
        }

        if (usuario == null
                || usuario.getId() == null) {

            throw new IllegalStateException(
                    "El movimiento requiere un usuario responsable."
            );
        }

        if (tipoMovimiento == null) {

            throw new IllegalStateException(
                    "El tipo de movimiento es obligatorio."
            );
        }

        if (cantidad == null || cantidad < 0) {

            throw new IllegalStateException(
                    "La cantidad del movimiento no es válida."
            );
        }

        if (stockAnterior == null
                || stockAnterior < 0
                || stockNuevo == null
                || stockNuevo < 0) {

            throw new IllegalStateException(
                    "Las existencias del movimiento no pueden ser negativas."
            );
        }

        if (origen == null
                || origen.isBlank()
                || origen.length() > 40) {

            throw new IllegalStateException(
                    "El origen del movimiento no es válido."
            );
        }

        if (idReferencia != null
                && idReferencia.length() > 50) {

            throw new IllegalStateException(
                    "La referencia del movimiento es demasiado larga."
            );
        }

        if (observaciones != null
                && observaciones.length() > 500) {

            throw new IllegalStateException(
                    "Las observaciones superan los 500 caracteres."
            );
        }

        // -------------------------------------------------
        // VALIDAR CONSISTENCIA DE CANTIDADES
        // -------------------------------------------------

        long diferencia =
                (long) stockNuevo - stockAnterior;

        switch (tipoMovimiento) {

            case INICIAL -> {

                if (stockAnterior != 0
                        || stockNuevo.longValue() != cantidad.longValue()) {

                    throw new IllegalStateException(
                            "El movimiento inicial es inconsistente."
                    );
                }
            }

            case ENTRADA -> {

                if (cantidad <= 0 || diferencia != cantidad) {

                    throw new IllegalStateException(
                            "La entrada de inventario es inconsistente."
                    );
                }
            }

            case SALIDA -> {

                if (cantidad <= 0 || -diferencia != cantidad) {

                    throw new IllegalStateException(
                            "La salida de inventario es inconsistente."
                    );
                }
            }

            case AJUSTE -> {

                if (cantidad <= 0
                        || Math.abs(diferencia) != cantidad) {

                    throw new IllegalStateException(
                            "El ajuste de inventario es inconsistente."
                    );
                }
            }
        }
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public InventarioFarmacia getInventario() {
        return inventario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public Integer getStockNuevo() {
        return stockNuevo;
    }

    public String getOrigen() {
        return origen;
    }

    public String getIdReferencia() {
        return idReferencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }
}
