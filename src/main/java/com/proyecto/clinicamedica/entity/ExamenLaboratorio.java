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

import java.math.BigDecimal;


/**
 * =========================================================
 * ENTIDAD: EXAMEN DE LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * Cada examen pertenece a un laboratorio.
 *
 * Relación PostgreSQL:
 *
 * examen_laboratorio.id_laboratorio
 *              ↓
 * laboratorio.id
 *
 * =========================================================
 */
@Entity
@Table(
        name = "examen_laboratorio"
)
public class ExamenLaboratorio {


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
    // LABORATORIO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_laboratorio",
            nullable = false
    )
    private Laboratorio laboratorio;


    // =====================================================
    // NOMBRE
    // =====================================================

    @Column(
            name = "nombre",
            nullable = false
    )
    private String nombre;



    // =====================================================
    // DESCRIPCIÓN
    // =====================================================
    //
    // CU-14:
    // Campo opcional.
    // Longitud máxima: 500 caracteres.
    //
    // =====================================================

    @Column(
            name = "descripcion",
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
    // RANGO DE REFERENCIA
    // =====================================================

    @Column(
            name = "rango_referencia"
    )
    private String rangoReferencia;


    // =====================================================
    // UNIDAD
    // =====================================================

    @Column(
            name = "unidad"
    )
    private String unidad;


    // =====================================================
    // ACTIVO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private boolean activo = true;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ExamenLaboratorio() {
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

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
    // VALIDAR
    // =====================================================

    public void validar() {

        if (laboratorio == null
                || laboratorio.getId() == null) {

            throw new IllegalStateException(
                    "El examen debe pertenecer a un laboratorio válido."
            );
        }


        if (!laboratorio.estaActivo()) {

            throw new IllegalStateException(
                    "No se puede utilizar un laboratorio inactivo."
            );
        }


        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalStateException(
                    "El nombre del examen de laboratorio es obligatorio."
            );
        }


        if (precio == null) {

            throw new IllegalStateException(
                    "El precio del examen de laboratorio es obligatorio."
            );
        }


        if (precio.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalStateException(
                    "El precio del examen de laboratorio no puede ser negativo."
            );
        }
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        nombre =
                limpiarTexto(
                        nombre
                );


        descripcion =
                limpiarTexto(
                        descripcion
                );


        rangoReferencia =
                limpiarTexto(
                        rangoReferencia
                );


        unidad =
                limpiarTexto(
                        unidad
                );
    }


    private String limpiarTexto(
            String texto
    ) {

        if (texto == null) {

            return null;
        }


        String limpio =
                texto.trim();


        return limpio.isBlank()
                ? null
                : limpio;
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaActivo() {

        return activo;
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


    public Laboratorio getLaboratorio() {

        return laboratorio;
    }


    public void setLaboratorio(
            Laboratorio laboratorio
    ) {

        this.laboratorio = laboratorio;
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


    public String getRangoReferencia() {

        return rangoReferencia;
    }


    public void setRangoReferencia(
            String rangoReferencia
    ) {

        this.rangoReferencia = rangoReferencia;
    }


    public String getUnidad() {

        return unidad;
    }


    public void setUnidad(
            String unidad
    ) {

        this.unidad = unidad;
    }


    public boolean isActivo() {

        return activo;
    }


    public void setActivo(
            boolean activo
    ) {

        this.activo = activo;
    }
}