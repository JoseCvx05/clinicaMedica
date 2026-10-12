
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * =========================================================
 * ENTIDAD: INVENTARIO DE FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Controla la cantidad de medicamentos disponibles
 * en cada sucursal del sistema hospitalario.
 *
 * REGLAS PRINCIPALES:
 *
 * - Cada inventario pertenece a un medicamento.
 * - Cada inventario pertenece a una sucursal.
 * - No puede existir la misma combinación de
 *   medicamento y sucursal más de una vez.
 * - La cantidad disponible nunca puede ser negativa.
 * - No se permite descontar más del stock disponible.
 * - Se utiliza @Version para control de concurrencia.
 *
 * IMPORTANTE:
 *
 * Las autorizaciones, validaciones de recetas y
 * operaciones de despacho se realizarán en el Service.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "inventario_farmacia",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventario_farmacia_medicamento_sucursal",
                        columnNames = {
                                "id_medicamento",
                                "id_sucursal"
                        }
                )
        }
)
public class InventarioFarmacia {

    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // MEDICAMENTO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_medicamento",
            nullable = false
    )
    private Medicamento medicamento;


    // =====================================================
    // SUCURSAL
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_sucursal",
            nullable = false
    )
    private Sucursal sucursal;


    // =====================================================
    // CANTIDAD DISPONIBLE
    // =====================================================

    @Column(
            name = "cantidad_disponible",
            nullable = false
    )
    private Integer cantidadDisponible = 0;


    // =====================================================
    // CONTROL DE CONCURRENCIA
    // RNF-025
    // =====================================================

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;


    // =====================================================
    // CONSTRUCTOR VACÍO
    // =====================================================

    public InventarioFarmacia() {
    }


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public InventarioFarmacia(
            Medicamento medicamento,
            Sucursal sucursal,
            Integer cantidadDisponible
    ) {

        this.medicamento = medicamento;
        this.sucursal = sucursal;
        this.cantidadDisponible = cantidadDisponible;

        validar();
    }


    // =====================================================
    // VALIDACIONES JPA
    // =====================================================

    @PrePersist
    private void prepararCreacion() {
        validar();
    }

    @PreUpdate
    private void prepararActualizacion() {
        validar();
    }


    // =====================================================
    // VALIDACIONES BACKEND
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // MEDICAMENTO OBLIGATORIO
        // -------------------------------------------------

        if (medicamento == null
                || medicamento.getId() == null) {

            throw new IllegalStateException(
                    "El inventario debe tener un medicamento válido."
            );
        }


        // -------------------------------------------------
        // SUCURSAL OBLIGATORIA
        // -------------------------------------------------

        if (sucursal == null
                || sucursal.getId() == null) {

            throw new IllegalStateException(
                    "El inventario debe pertenecer a una sucursal válida."
            );
        }


        // -------------------------------------------------
        // CANTIDAD OBLIGATORIA
        // -------------------------------------------------

        if (cantidadDisponible == null) {

            throw new IllegalStateException(
                    "La cantidad disponible del inventario es obligatoria."
            );
        }


        // -------------------------------------------------
        // NO PERMITIR INVENTARIO NEGATIVO
        // -------------------------------------------------

        if (cantidadDisponible < 0) {

            throw new IllegalStateException(
                    "La cantidad disponible del inventario no puede ser negativa."
            );
        }
    }


    // =====================================================
    // DESCONTAR EXISTENCIAS
    // =====================================================

    public void descontar(
            Integer cantidad
    ) {

        validar();

        // -------------------------------------------------
        // CANTIDAD VÁLIDA
        // -------------------------------------------------

        if (cantidad == null || cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad a despachar debe ser mayor a cero."
            );
        }


        // -------------------------------------------------
        // VERIFICAR STOCK SUFICIENTE
        // -------------------------------------------------

        if (cantidad > cantidadDisponible) {

            throw new IllegalStateException(
                    "Stock insuficiente. El stock actual es "
                            + cantidadDisponible
                            + " unidades."
            );
        }


        // -------------------------------------------------
        // DESCONTAR
        // -------------------------------------------------

        cantidadDisponible =
                cantidadDisponible - cantidad;

        validar();
    }


    // =====================================================
    // AGREGAR EXISTENCIAS
    // =====================================================

    public void agregar(
            Integer cantidad
    ) {

        validar();

        if (cantidad == null || cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad a ingresar debe ser mayor a cero."
            );
        }

        try {

            cantidadDisponible = Math.addExact(
                    cantidadDisponible,
                    cantidad
            );

        } catch (ArithmeticException ex) {

            throw new IllegalStateException(
                    "La cantidad ingresada supera el límite permitido del inventario."
            );
        }

        validar();
    }


    // =====================================================
    // CONSULTAR STOCK DISPONIBLE
    // =====================================================

    public boolean tieneStockSuficiente(
            Integer cantidad
    ) {

        return cantidadDisponible != null
                && cantidad != null
                && cantidad > 0
                && cantidadDisponible >= cantidad;
    }


    // =====================================================
    // VERIFICAR STOCK MÍNIMO
    // RN-CU10-03
    // =====================================================

    public boolean alcanzoStockMinimo() {

        if (medicamento == null
                || medicamento.getStockMinimo() == null
                || cantidadDisponible == null) {

            return false;
        }

        return cantidadDisponible
                <= medicamento.getStockMinimo();
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(
            Medicamento medicamento
    ) {
        this.medicamento = medicamento;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal = sucursal;
    }

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(
            Integer cantidadDisponible
    ) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public Long getVersion() {
        return version;
    }
}
