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

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * =========================================================
 * ENTIDAD: DETALLE RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 *
 * Representa cada medicamento prescrito dentro
 * de una receta médica.
 *
 * Guarda:
 *
 * - Medicamento del catálogo.
 * - Cantidad prescrita.
 * - Dosis.
 * - Frecuencia.
 * - Duración.
 * - Indicaciones especiales.
 *
 * También conserva snapshot histórico de:
 *
 * - Nombre del medicamento.
 * - Precio unitario.
 * - Unidad.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "detalle_receta_medica"
)
public class DetalleRecetaMedica {


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
    // RECETA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_receta_medica",
            nullable = false
    )
    private RecetaMedica recetaMedica;


    // =====================================================
    // MEDICAMENTO DEL CATÁLOGO
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
    // SNAPSHOT: NOMBRE
    // =====================================================

    @Column(
            name = "nombre_medicamento",
            nullable = false,
            length = 200,
            updatable = false
    )
    private String nombreMedicamento;


    // =====================================================
    // CANTIDAD PRESCRITA
    // =====================================================

    @Column(
            name = "cantidad",
            nullable = false
    )
    private Integer cantidad;


    // =====================================================
    // DOSIS
    // =====================================================

    @Column(
            name = "dosis",
            nullable = false,
            length = 150
    )
    private String dosis;


    // =====================================================
    // FRECUENCIA
    // =====================================================

    @Column(
            name = "frecuencia",
            nullable = false,
            length = 150
    )
    private String frecuencia;


    // =====================================================
    // DURACIÓN
    // =====================================================

    @Column(
            name = "duracion",
            nullable = false,
            length = 150
    )
    private String duracion;


    // =====================================================
    // INDICACIONES ESPECIALES
    // =====================================================

    @Column(
            name = "indicaciones_especiales",
            length = 1000
    )
    private String indicacionesEspeciales;


    // =====================================================
    // SNAPSHOT: PRECIO
    // =====================================================

    @Column(
            name = "precio_unitario",
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal precioUnitario;


    // =====================================================
    // SNAPSHOT: UNIDAD
    // =====================================================

    @Column(
            name = "unidad",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String unidad;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DetalleRecetaMedica() {
    }


    // =====================================================
    // CONSTRUCTOR ÚTIL
    // =====================================================

    public DetalleRecetaMedica(
            RecetaMedica recetaMedica,
            Medicamento medicamento,
            Integer cantidad,
            String dosis,
            String frecuencia,
            String duracion,
            String indicacionesEspeciales
    ) {

        this.recetaMedica =
                recetaMedica;

        this.medicamento =
                medicamento;

        this.cantidad =
                cantidad;

        this.dosis =
                dosis;

        this.frecuencia =
                frecuencia;

        this.duracion =
                duracion;

        this.indicacionesEspeciales =
                indicacionesEspeciales;
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        normalizar();

        validar();

        copiarSnapshot();
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        if (dosis != null) {

            dosis =
                    dosis.trim();
        }


        if (frecuencia != null) {

            frecuencia =
                    frecuencia.trim();
        }


        if (duracion != null) {

            duracion =
                    duracion.trim();
        }


        if (indicacionesEspeciales != null) {

            indicacionesEspeciales =
                    indicacionesEspeciales.trim();


            if (indicacionesEspeciales.isBlank()) {

                indicacionesEspeciales =
                        null;
            }
        }
    }


    // =====================================================
    // VALIDACIÓN
    // =====================================================

    public void validar() {


        // -------------------------------------------------
        // RECETA
        // -------------------------------------------------

        if (recetaMedica == null
                || recetaMedica.getId() == null) {

            throw new IllegalStateException(
                    "El medicamento debe pertenecer a una receta médica guardada."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO
        // -------------------------------------------------

        if (medicamento == null
                || medicamento.getId() == null) {

            throw new IllegalStateException(
                    "El medicamento es obligatorio."
            );
        }


        if (!medicamento.estaActivo()) {

            throw new IllegalStateException(
                    "No se puede recetar un medicamento inactivo."
            );
        }


        medicamento.validar();


        // -------------------------------------------------
        // CANTIDAD
        // -------------------------------------------------

        if (cantidad == null
                || cantidad <= 0) {

            throw new IllegalStateException(
                    "La cantidad prescrita debe ser mayor a cero."
            );
        }


        // -------------------------------------------------
        // DOSIS
        // -------------------------------------------------

        if (dosis == null
                || dosis.isBlank()) {

            throw new IllegalStateException(
                    "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
            );
        }


        if (dosis.length() > 150) {

            throw new IllegalStateException(
                    "La dosis no puede superar los 150 caracteres."
            );
        }


        // -------------------------------------------------
        // FRECUENCIA
        // -------------------------------------------------

        if (frecuencia == null
                || frecuencia.isBlank()) {

            throw new IllegalStateException(
                    "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
            );
        }


        if (frecuencia.length() > 150) {

            throw new IllegalStateException(
                    "La frecuencia no puede superar los 150 caracteres."
            );
        }


        // -------------------------------------------------
        // DURACIÓN
        // -------------------------------------------------

        if (duracion == null
                || duracion.isBlank()) {

            throw new IllegalStateException(
                    "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
            );
        }


        if (duracion.length() > 150) {

            throw new IllegalStateException(
                    "La duración no puede superar los 150 caracteres."
            );
        }


        // -------------------------------------------------
        // INDICACIONES
        // -------------------------------------------------

        if (indicacionesEspeciales != null
                && indicacionesEspeciales.length() > 1000) {

            throw new IllegalStateException(
                    "Las indicaciones especiales no pueden superar los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // DATOS DEL CATÁLOGO
        // -------------------------------------------------

        if (medicamento.getNombre() == null
                || medicamento.getNombre().isBlank()) {

            throw new IllegalStateException(
                    "El medicamento seleccionado no posee un nombre válido."
            );
        }


        if (medicamento.getPrecio() == null
                || medicamento
                .getPrecio()
                .compareTo(
                        BigDecimal.ZERO
                ) < 0) {

            throw new IllegalStateException(
                    "El medicamento seleccionado no posee un precio válido."
            );
        }


        if (medicamento.getUnidad() == null
                || medicamento.getUnidad().isBlank()) {

            throw new IllegalStateException(
                    "El medicamento seleccionado no posee una unidad válida."
            );
        }
    }


    // =====================================================
    // SNAPSHOT HISTÓRICO
    // =====================================================

    private void copiarSnapshot() {

        nombreMedicamento =
                medicamento
                        .getNombre()
                        .trim();


        precioUnitario =
                medicamento
                        .getPrecio()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        unidad =
                medicamento
                        .getUnidad()
                        .trim();
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


    public RecetaMedica getRecetaMedica() {

        return recetaMedica;
    }


    public void setRecetaMedica(
            RecetaMedica recetaMedica
    ) {

        this.recetaMedica =
                recetaMedica;
    }


    public Medicamento getMedicamento() {

        return medicamento;
    }


    public void setMedicamento(
            Medicamento medicamento
    ) {

        this.medicamento =
                medicamento;
    }


    public String getNombreMedicamento() {

        return nombreMedicamento;
    }


    public Integer getCantidad() {

        return cantidad;
    }


    public void setCantidad(
            Integer cantidad
    ) {

        this.cantidad =
                cantidad;
    }


    public String getDosis() {

        return dosis;
    }


    public void setDosis(
            String dosis
    ) {

        this.dosis =
                dosis;
    }


    public String getFrecuencia() {

        return frecuencia;
    }


    public void setFrecuencia(
            String frecuencia
    ) {

        this.frecuencia =
                frecuencia;
    }


    public String getDuracion() {

        return duracion;
    }


    public void setDuracion(
            String duracion
    ) {

        this.duracion =
                duracion;
    }


    public String getIndicacionesEspeciales() {

        return indicacionesEspeciales;
    }


    public void setIndicacionesEspeciales(
            String indicacionesEspeciales
    ) {

        this.indicacionesEspeciales =
                indicacionesEspeciales;
    }


    public BigDecimal getPrecioUnitario() {

        return precioUnitario;
    }


    public String getUnidad() {

        return unidad;
    }
}