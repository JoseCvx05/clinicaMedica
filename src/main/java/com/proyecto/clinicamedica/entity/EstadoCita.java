package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

/**
 * =========================================================
 * ENTIDAD: ESTADO DE CITA
 * =========================================================
 *
 * Catálogo de estados utilizados por las citas.
 * =========================================================
 */
@Entity
@Table(name = "estado_cita")
public class EstadoCita {


    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    @Column(
            name = "nombre",
            nullable = false,
            length = 50
    )
    private String nombre;


    @Column(
            name = "descripcion",
            length = 200
    )
    private String descripcion;


    @Column(
            name = "orden_flujo"
    )
    private Short ordenFlujo;


    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo =
            true;


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


    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion
    ) {
        this.descripcion = descripcion;
    }


    public Short getOrdenFlujo() {
        return ordenFlujo;
    }

    public void setOrdenFlujo(
            Short ordenFlujo
    ) {
        this.ordenFlujo = ordenFlujo;
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