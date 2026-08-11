package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * ENTIDAD: SUCURSAL
 * =========================================================
 *
 * Representa las diferentes sedes o sucursales
 * disponibles dentro del sistema hospitalario.
 *
 * Ejemplos:
 * - Sucursal Central
 * - Sucursal Norte
 * - Sucursal Sur
 *
 * El borrado es lógico mediante el campo activo.
 * =========================================================
 */
@Entity
@Table(name = "sucursal")
@Getter
@Setter
@NoArgsConstructor
public class Sucursal {

    /**
     * Identificador único de la sucursal.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;


    /**
     * Nombre de la sucursal.
     */
    @Column(
            name = "nombre",
            nullable = false,
            length = 100
    )
    private String nombre;


    /**
     * Número telefónico de la sucursal.
     *
     * En la base de datos:
     * - Es opcional.
     * - Si existe, debe contener exactamente 8 dígitos.
     *
     * La validación de formato se realizará también
     * en los DTO cuando corresponda.
     */
    @Column(
            name = "telefono",
            length = 8
    )
    private String telefono;


    /**
     * Dirección física de la sucursal.
     */
    @Column(
            name = "direccion",
            length = 500
    )
    private String direccion;


    /**
     * Descripción general de la sucursal.
     */
    @Column(
            name = "descripcion",
            length = 250
    )
    private String descripcion;


    /**
     * Estado lógico del registro.
     *
     * true  = activo
     * false = inactivo
     */
    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo = true;


    /**
     * Garantiza el estado activo en nuevos registros
     * cuando no se haya establecido explícitamente.
     */
    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }
    }
}