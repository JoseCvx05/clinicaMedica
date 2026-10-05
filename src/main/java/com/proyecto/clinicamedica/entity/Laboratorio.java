package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;


/**
 * =========================================================
 * ENTIDAD: LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * Representa un laboratorio disponible dentro
 * del catálogo clínico.
 *
 * IMPORTANTE:
 *
 * - Esta entidad NO crea órdenes.
 * - No modifica citas.
 * - No modifica consultas médicas.
 * - Solo representa la tabla existente "laboratorio".
 *
 * =========================================================
 */
@Entity
@Table(
        name = "laboratorio"
)
public class Laboratorio {


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

    @Column(
            name = "descripcion"
    )
    private String descripcion;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private boolean activo = true;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Laboratorio() {
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
    // VALIDACIÓN
    // =====================================================

    public void validar() {

        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalStateException(
                    "El nombre del laboratorio es obligatorio."
            );
        }
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizar() {

        if (nombre != null) {

            nombre =
                    nombre.trim();
        }


        if (descripcion != null) {

            descripcion =
                    descripcion.trim();

            if (descripcion.isBlank()) {

                descripcion = null;
            }
        }
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


    public boolean isActivo() {

        return activo;
    }


    public void setActivo(
            boolean activo
    ) {

        this.activo = activo;
    }
}