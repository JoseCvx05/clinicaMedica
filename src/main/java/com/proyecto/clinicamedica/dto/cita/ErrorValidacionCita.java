package com.proyecto.clinicamedica.dto.cita;

/**
 * =========================================================
 * ERROR DE VALIDACIÓN - CU-03
 * =========================================================
 *
 * Representa un error asociado a un campo o condición
 * específica del wizard de agendamiento.
 *
 * Ejemplo:
 *
 * campo   = "idSucursal"
 * mensaje = "Debe seleccionar una sucursal para continuar."
 *
 * =========================================================
 */
public record ErrorValidacionCita(
        String campo,
        String mensaje
) {
}