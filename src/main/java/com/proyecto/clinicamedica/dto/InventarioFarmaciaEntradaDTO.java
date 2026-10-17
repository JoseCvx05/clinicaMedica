
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * CU-14 - DTO: ENTRADA DE INVENTARIO DE FARMACIA
 * =========================================================
 *
 * Catálogo 8 de 9 - Inventario de Medicamentos.
 *
 * Permite recibir los datos necesarios para
 * registrar entradas de medicamentos.
 *
 * VALIDACIONES:
 *
 * - La cantidad es obligatoria.
 * - La cantidad debe ser mayor a cero.
 * - Las observaciones son opcionales.
 * - Las observaciones tienen máximo 500 caracteres.
 *
 * SEGURIDAD:
 *
 * - El inventario se identifica desde la ruta.
 * - El Service valida los permisos administrativos.
 * - El Service verifica medicamento y sucursal.
 * - El Service bloquea el inventario.
 * - El Service registra movimiento y auditoría.
 *
 * IMPORTANTE:
 *
 * No modifica directamente las existencias.
 * No afecta los despachos históricos del CU-11.
 *
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class InventarioFarmaciaEntradaDTO {

    // =====================================================
    // CANTIDAD A INGRESAR
    // =====================================================

    @NotNull(
            message = "La cantidad de entrada es obligatoria."
    )
    @Positive(
            message = "La cantidad debe ser mayor a cero."
    )
    private Integer cantidad;


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    @Size(
            max = 500,
            message = "Las observaciones no pueden superar los 500 caracteres."
    )
    private String observaciones;
}
