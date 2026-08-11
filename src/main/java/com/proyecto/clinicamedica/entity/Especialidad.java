package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * ENTIDAD: ESPECIALIDAD
 * =========================================================
 *
 * Representa las especialidades médicas disponibles
 * dentro del sistema.
 *
 * Ejemplos:
 * - Medicina General
 * - Pediatría
 * - Cardiología
 * - Traumatología
 *
 * El borrado de registros es lógico mediante el
 * campo activo.
 * =========================================================
 */
@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
public class Especialidad {

    /**
     * Identificador único.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;


    /**
     * Nombre de la especialidad.
     */
    @Column(
            name = "nombre",
            nullable = false,
            length = 200
    )
    private String nombre;


    /**
     * Descripción de la especialidad.
     *
     * En la base de datos es obligatoria.
     */
    @Column(
            name = "descripcion",
            nullable = false,
            length = 500
    )
    private String descripcion;


    /**
     * Estado lógico.
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
     * Garantiza que un nuevo registro quede activo
     * si no se establece explícitamente el estado.
     */
    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }
    }
}