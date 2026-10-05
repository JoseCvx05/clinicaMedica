package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;


/**
 * =========================================================
 * ENTIDAD: MEDICAMENTO
 * =========================================================
 *
 * Catálogo de medicamentos utilizado por:
 *
 * - CU-08 FA04 - Receta Médica.
 * - Módulo posterior de Farmacia / Despacho.
 *
 * IMPORTANTE:
 *
 * Esta entidad representa únicamente el catálogo.
 * No modifica inventario ni descuenta existencias.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "medicamento"
)
public class Medicamento {


    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private Integer id;


    // =====================================================
    // NOMBRE
    // =====================================================

    @Column(
            name = "nombre",
            nullable = false,
            length = 200
    )
    private String nombre;


    // =====================================================
    // DESCRIPCIÓN
    // =====================================================

    @Column(
            name = "descripcion",
            nullable = false,
            length = 500
    )
    private String descripcion;


    // =====================================================
    // PRECIO
    // =====================================================

    @Column(
            name = "precio",
            nullable = false
    )
    private BigDecimal precio;


    // =====================================================
    // UNIDAD
    // =====================================================

    @Column(
            name = "unidad",
            nullable = false,
            length = 50
    )
    private String unidad;


    // =====================================================
    // MEDICAMENTO CONTROLADO
    // =====================================================

    @Column(
            name = "es_controlado",
            nullable = false
    )
    private Boolean esControlado =
            false;


    // =====================================================
    // STOCK MÍNIMO
    // =====================================================

    @Column(
            name = "stock_minimo"
    )
    private Integer stockMinimo;


    // =====================================================
    // ACTIVO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo =
            true;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Medicamento() {
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        if (activo == null) {

            activo =
                    true;
        }


        if (esControlado == null) {

            esControlado =
                    false;
        }


        normalizar();

        validar();
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    private void prepararActualizacion() {

        normalizar();

        validar();
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        if (nombre != null) {

            nombre =
                    nombre.trim();
        }


        if (descripcion != null) {

            descripcion =
                    descripcion.trim();
        }


        if (unidad != null) {

            unidad =
                    unidad.trim();
        }
    }


    // =====================================================
    // VALIDAR
    // =====================================================

    public void validar() {


        // -------------------------------------------------
        // NOMBRE
        // -------------------------------------------------

        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalStateException(
                    "El nombre del medicamento es obligatorio."
            );
        }


        if (nombre.length() > 200) {

            throw new IllegalStateException(
                    "El nombre del medicamento no puede superar los 200 caracteres."
            );
        }


        // -------------------------------------------------
        // DESCRIPCIÓN
        // -------------------------------------------------

        if (descripcion == null
                || descripcion.isBlank()) {

            throw new IllegalStateException(
                    "La descripción del medicamento es obligatoria."
            );
        }


        if (descripcion.length() > 500) {

            throw new IllegalStateException(
                    "La descripción del medicamento no puede superar los 500 caracteres."
            );
        }


        // -------------------------------------------------
        // PRECIO
        // -------------------------------------------------

        if (precio == null) {

            throw new IllegalStateException(
                    "El precio del medicamento es obligatorio."
            );
        }


        if (precio.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalStateException(
                    "El precio del medicamento no puede ser negativo."
            );
        }


        // -------------------------------------------------
        // UNIDAD
        // -------------------------------------------------

        if (unidad == null
                || unidad.isBlank()) {

            throw new IllegalStateException(
                    "La unidad del medicamento es obligatoria."
            );
        }


        if (unidad.length() > 50) {

            throw new IllegalStateException(
                    "La unidad del medicamento no puede superar los 50 caracteres."
            );
        }


        // -------------------------------------------------
        // CONTROLADO
        // -------------------------------------------------

        if (esControlado == null) {

            throw new IllegalStateException(
                    "Debe indicarse si el medicamento es controlado."
            );
        }


        // -------------------------------------------------
        // STOCK MÍNIMO
        // -------------------------------------------------

        if (stockMinimo != null
                && stockMinimo < 0) {

            throw new IllegalStateException(
                    "El stock mínimo del medicamento no puede ser negativo."
            );
        }


        // -------------------------------------------------
        // ACTIVO
        // -------------------------------------------------

        if (activo == null) {

            throw new IllegalStateException(
                    "El estado del medicamento es obligatorio."
            );
        }
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaActivo() {

        return Boolean.TRUE.equals(
                activo
        );
    }


    public boolean esControlado() {

        return Boolean.TRUE.equals(
                esControlado
        );
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    public void setId(
            Integer id
    ) {

        this.id = id;
    }


    public String getNombre() {

        return nombre;
    }


    public void setNombre(
            String nombre
    ) {

        this.nombre = nombre;
    }


    public String getDescripcion() {

        return descripcion;
    }


    public void setDescripcion(
            String descripcion
    ) {

        this.descripcion = descripcion;
    }


    public BigDecimal getPrecio() {

        return precio;
    }


    public void setPrecio(
            BigDecimal precio
    ) {

        this.precio = precio;
    }


    public String getUnidad() {

        return unidad;
    }


    public void setUnidad(
            String unidad
    ) {

        this.unidad = unidad;
    }


    public Boolean getEsControlado() {

        return esControlado;
    }


    public void setEsControlado(
            Boolean esControlado
    ) {

        this.esControlado = esControlado;
    }


    public Integer getStockMinimo() {

        return stockMinimo;
    }


    public void setStockMinimo(
            Integer stockMinimo
    ) {

        this.stockMinimo = stockMinimo;
    }


    public Boolean getActivo() {

        return activo;
    }


    public void setActivo(
            Boolean activo
    ) {

        this.activo = activo;
    }
}