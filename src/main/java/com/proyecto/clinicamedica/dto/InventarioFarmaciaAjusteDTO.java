
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * CU-14 - DTO: AJUSTE DE INVENTARIO DE FARMACIA
 * =========================================================
 *
 * Catálogo 8 de 9 - Inventario de Medicamentos.
 *
 * Permite recibir los datos necesarios para
 * realizar ajustes controlados de existencias.
 *
 * VALIDACIONES:
 *
 * - Las nuevas existencias son obligatorias.
 * - No se permiten existencias negativas.
 * - El motivo del ajuste es obligatorio.
 * - El motivo tiene un máximo de 500 caracteres.
 *
 * SEGURIDAD:
 *
 * - El inventario se identifica desde la ruta.
 * - El Service verifica los permisos administrativos.
 * - El Service bloquea el inventario.
 * - El Service valida medicamento y sucursal activos.
 * - El Service impide ajustes sin cambios.
 * - El Service registra movimiento y auditoría.
 *
 * IMPORTANTE:
 *
 * - No modifica directamente PostgreSQL.
 * - No afecta despachos históricos del CU-11.
 * - El stock final se valida en el backend.
 *
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class InventarioFarmaciaAjusteDTO {

    // =====================================================
    // NUEVAS EXISTENCIAS
    // =====================================================

    @NotNull(
            message = "Debe indicar las nuevas existencias."
    )
    @Min(
            value = 0,
            message = "Las existencias no pueden ser negativas."
    )
    private Integer nuevoStock;


    // =====================================================
    // MOTIVO OBLIGATORIO
    // =====================================================

    @NotBlank(
            message = "Debe indicar el motivo del ajuste."
    )
    @Size(
            max = 500,
            message = "El motivo no puede superar los 500 caracteres."
    )
    private String motivo;
}
