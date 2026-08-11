package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * DTO: ERROR DE VALIDACIÓN DE USUARIO
 * =========================================================
 *
 * Representa un error asociado a un campo específico
 * del formulario del CU-01.
 *
 * Ejemplo:
 *
 * campo:
 * nombreUsuario
 *
 * mensaje:
 * El usuario debe contener al menos 8 caracteres.
 *
 * =========================================================
 */
public record ErrorValidacionUsuario(

        String campo,

        String mensaje

) {
}