package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ERROR DE VALIDACIÓN - REGISTRO EXTERNO
 * =========================================================
 *
 * Representa un error asociado a un campo específico
 * del formulario de CU-02.
 *
 * Ejemplo:
 *
 * campo   = "dpi"
 * mensaje = "El DPI debe contener exactamente 13 dígitos..."
 *
 * =========================================================
 */
public record ErrorValidacionRegistroExterno(
        String campo,
        String mensaje
) {
}