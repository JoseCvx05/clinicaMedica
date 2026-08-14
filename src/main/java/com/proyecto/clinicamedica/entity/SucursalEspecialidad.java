package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

/**
 * =========================================================
 * ENTIDAD: SUCURSAL - ESPECIALIDAD
 * =========================================================
 *
 * Representa las especialidades habilitadas en cada
 * sucursal.
 *
 * CU-03:
 *
 * Paso 1 -> Sucursal
 * Paso 2 -> Especialidades disponibles en esa sucursal
 *
 * También permite detectar FA01 cuando una sucursal
 * no tiene especialidades activas configuradas.
 * =========================================================
 */
@Entity
@Table(
        name = "sucursal_especialidad",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_sucursal_especialidad",
                        columnNames = {
                                "id_sucursal",
                                "id_especialidad"
                        }
                )
        }
)
public class SucursalEspecialidad {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


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
    // ESPECIALIDAD
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_especialidad",
            nullable = false
    )
    private Especialidad especialidad;


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
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {

        this.sucursal =
                sucursal;
    }


    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(
            Especialidad especialidad
    ) {

        this.especialidad =
                especialidad;
    }


    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {

        this.activo =
                activo;
    }
}