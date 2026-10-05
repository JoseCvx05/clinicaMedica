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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * =========================================================
 * ENTIDAD: DETALLE ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 *
 * Representa cada examen incluido dentro de una orden.
 *
 * Además conserva un snapshot histórico de:
 *
 * - Nombre del examen.
 * - Precio.
 * - Rango de referencia.
 * - Unidad.
 *
 * Esto evita que cambios posteriores en el catálogo
 * modifiquen la información de órdenes ya emitidas.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "detalle_orden_laboratorio",

        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_detalle_orden_examen",
                        columnNames = {
                                "id_orden_laboratorio",
                                "id_examen_laboratorio"
                        }
                )
        }
)
public class DetalleOrdenLaboratorio {


    // =====================================================
    // IDENTIFICADOR
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
    // ORDEN
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_orden_laboratorio",
            nullable = false
    )
    private OrdenLaboratorio ordenLaboratorio;


    // =====================================================
    // EXAMEN DEL CATÁLOGO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_examen_laboratorio",
            nullable = false
    )
    private ExamenLaboratorio examenLaboratorio;


    // =====================================================
    // SNAPSHOT: NOMBRE
    // =====================================================

    @Column(
            name = "nombre_examen",
            nullable = false,
            length = 255
    )
    private String nombreExamen;


    // =====================================================
    // SNAPSHOT: PRECIO
    // =====================================================

    @Column(
            name = "precio_examen",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal precioExamen;


    // =====================================================
    // SNAPSHOT: RANGO
    // =====================================================

    @Column(
            name = "rango_referencia",
            length = 255
    )
    private String rangoReferencia;


    // =====================================================
    // SNAPSHOT: UNIDAD
    // =====================================================

    @Column(
            name = "unidad",
            length = 100
    )
    private String unidad;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DetalleOrdenLaboratorio() {
    }


    // =====================================================
    // CONSTRUCTOR ÚTIL
    // =====================================================

    public DetalleOrdenLaboratorio(
            OrdenLaboratorio ordenLaboratorio,
            ExamenLaboratorio examenLaboratorio
    ) {

        this.ordenLaboratorio =
                ordenLaboratorio;

        this.examenLaboratorio =
                examenLaboratorio;
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        validar();

        copiarDatosHistoricos();
    }


    // =====================================================
    // VALIDAR
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // ORDEN
        // -------------------------------------------------

        if (ordenLaboratorio == null
                || ordenLaboratorio.getId() == null) {

            throw new IllegalStateException(
                    "El detalle debe pertenecer a una orden de laboratorio guardada."
            );
        }


        // -------------------------------------------------
        // EXAMEN
        // -------------------------------------------------

        if (examenLaboratorio == null
                || examenLaboratorio.getId() == null) {

            throw new IllegalStateException(
                    "Debe seleccionar un examen de laboratorio válido."
            );
        }


        // -------------------------------------------------
        // EXAMEN ACTIVO
        // -------------------------------------------------

        if (!examenLaboratorio.estaActivo()) {

            throw new IllegalStateException(
                    "No se puede agregar un examen de laboratorio inactivo."
            );
        }


        // -------------------------------------------------
        // LABORATORIO ACTIVO
        // -------------------------------------------------

        if (examenLaboratorio.getLaboratorio() == null
                || examenLaboratorio.getLaboratorio().getId() == null) {

            throw new IllegalStateException(
                    "El examen seleccionado no pertenece a un laboratorio válido."
            );
        }


        if (!examenLaboratorio
                .getLaboratorio()
                .estaActivo()) {

            throw new IllegalStateException(
                    "El laboratorio asociado al examen se encuentra inactivo."
            );
        }


        // -------------------------------------------------
        // NOMBRE
        // -------------------------------------------------

        if (examenLaboratorio.getNombre() == null
                || examenLaboratorio.getNombre().isBlank()) {

            throw new IllegalStateException(
                    "El examen seleccionado no posee un nombre válido."
            );
        }


        if (examenLaboratorio
                .getNombre()
                .trim()
                .length() > 255) {

            throw new IllegalStateException(
                    "El nombre del examen supera la longitud permitida."
            );
        }


        // -------------------------------------------------
        // PRECIO
        // -------------------------------------------------

        if (examenLaboratorio.getPrecio() == null) {

            throw new IllegalStateException(
                    "El examen seleccionado no posee un precio válido."
            );
        }


        if (examenLaboratorio
                .getPrecio()
                .compareTo(
                        BigDecimal.ZERO
                ) < 0) {

            throw new IllegalStateException(
                    "El precio del examen no puede ser negativo."
            );
        }


        // -------------------------------------------------
        // RANGO
        // -------------------------------------------------

        if (examenLaboratorio.getRangoReferencia() != null
                && examenLaboratorio
                .getRangoReferencia()
                .trim()
                .length() > 255) {

            throw new IllegalStateException(
                    "El rango de referencia supera la longitud permitida."
            );
        }


        // -------------------------------------------------
        // UNIDAD
        // -------------------------------------------------

        if (examenLaboratorio.getUnidad() != null
                && examenLaboratorio
                .getUnidad()
                .trim()
                .length() > 100) {

            throw new IllegalStateException(
                    "La unidad del examen supera la longitud permitida."
            );
        }
    }


    // =====================================================
    // COPIAR SNAPSHOT HISTÓRICO
    // =====================================================

    private void copiarDatosHistoricos() {

        nombreExamen =
                examenLaboratorio
                        .getNombre()
                        .trim();


        precioExamen =
                examenLaboratorio
                        .getPrecio()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        rangoReferencia =
                limpiarOpcional(
                        examenLaboratorio
                                .getRangoReferencia()
                );


        unidad =
                limpiarOpcional(
                        examenLaboratorio
                                .getUnidad()
                );
    }


    // =====================================================
    // LIMPIAR TEXTO OPCIONAL
    // =====================================================

    private String limpiarOpcional(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isBlank()
                ? null
                : limpio;
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


    public OrdenLaboratorio getOrdenLaboratorio() {

        return ordenLaboratorio;
    }


    public void setOrdenLaboratorio(
            OrdenLaboratorio ordenLaboratorio
    ) {

        this.ordenLaboratorio =
                ordenLaboratorio;
    }


    public ExamenLaboratorio getExamenLaboratorio() {

        return examenLaboratorio;
    }


    public void setExamenLaboratorio(
            ExamenLaboratorio examenLaboratorio
    ) {

        this.examenLaboratorio =
                examenLaboratorio;
    }


    public String getNombreExamen() {

        return nombreExamen;
    }


    public BigDecimal getPrecioExamen() {

        return precioExamen;
    }


    public String getRangoReferencia() {

        return rangoReferencia;
    }


    public String getUnidad() {

        return unidad;
    }
}