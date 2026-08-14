package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;


/**
 * =========================================================
 * ENTIDAD: FORMA DE PAGO
 * =========================================================
 *
 * Catálogo de formas de pago aceptadas por el sistema.
 *
 * Ejemplos:
 *
 * - Efectivo
 * - Tarjeta de crédito
 * - Tarjeta de débito
 *
 * CU-04 utilizará únicamente las formas de pago
 * correspondientes a tarjetas.
 *
 * =========================================================
 */
@Entity
@Table(name = "forma_pago")
public class FormaPago {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // NOMBRE
    // =====================================================

    @Column(
            name = "nombre",
            nullable = false,
            length = 50
    )
    private String nombre;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo =
            true;


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public String getNombre() {
        return nombre;
    }

    public void setNombre(
            String nombre
    ) {
        this.nombre = nombre;
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