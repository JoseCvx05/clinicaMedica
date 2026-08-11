package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * ENTIDAD: ROL
 * =========================================================
 *
 * Representa los roles disponibles dentro del sistema.
 *
 * Ejemplos:
 * - Paciente
 * - Médico
 * - Enfermero
 * - Recepcionista
 * - Cajero
 * - Laboratorista
 * - Farmacéutico
 * - Administrador
 *
 * Los registros no se eliminan físicamente.
 * Para realizar borrado lógico se utiliza el campo activo.
 * =========================================================
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    /**
     * Identificador único del rol.
     */
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;

    /**
     * Nombre del rol.
     */
    @Column(
            name = "nombre",
            nullable = false,
            length = 200
    )
    private String nombre;

    /**
     * Descripción opcional del rol.
     */
    @Column(
            name = "descripcion",
            length = 500
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
     * Se ejecuta antes de insertar el registro.
     *
     * Garantiza que un nuevo rol quede activo
     * cuando no se haya especificado un estado.
     */
    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }
    }
}