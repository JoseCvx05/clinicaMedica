
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * CU-14 - DTO: FORMULARIO DE ROLES
 * =========================================================
 *
 * Catálogo 7 de 9.
 *
 * Recibe los datos para crear y actualizar roles.
 *
 * VALIDACIONES:
 * - Nombre obligatorio.
 * - Nombre máximo 200 caracteres.
 * - Descripción opcional.
 * - Descripción máximo 500 caracteres.
 *
 * La seguridad, los duplicados y la protección
 * de roles esenciales se validan en el Service.
 *
 * No permite modificar el ID ni el estado
 * directamente desde el formulario.
 *
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class RolFormularioDTO {

    // =====================================================
    // NOMBRE
    // =====================================================

    @NotBlank(
            message = "El nombre es obligatorio."
    )
    @Size(
            max = 200,
            message = "El nombre no puede superar 200 caracteres."
    )
    private String nombre;

    // =====================================================
    // DESCRIPCIÓN
    // =====================================================

    @Size(
            max = 500,
            message = "La descripción no puede superar 500 caracteres."
    )
    private String descripcion;
}
