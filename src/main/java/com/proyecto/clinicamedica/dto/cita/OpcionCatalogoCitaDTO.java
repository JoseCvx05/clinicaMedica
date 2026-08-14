package com.proyecto.clinicamedica.dto.cita;

/**
 * =========================================================
 * DTO: OPCIÓN DE CATÁLOGO
 * =========================================================
 *
 * Utilizado por el wizard de CU-03 para mostrar:
 *
 * - sucursales;
 * - especialidades;
 * - médicos.
 *
 * Evita exponer entidades JPA completas en la vista.
 * =========================================================
 */
public record OpcionCatalogoCitaDTO(
        Integer id,
        String nombre
) {
}