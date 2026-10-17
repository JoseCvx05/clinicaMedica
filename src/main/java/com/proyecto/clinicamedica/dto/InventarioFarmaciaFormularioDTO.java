
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * CU-14 - DTO: FORMULARIO DE INVENTARIO
 * =========================================================
 *
 * Catálogo 8 - Inventario de Medicamentos.
 *
 * Recibe los datos necesarios para registrar
 * un inventario de medicamentos.
 *
 * VALIDACIONES:
 *
 * - Medicamento obligatorio.
 * - Sucursal obligatoria.
 * - Cantidad inicial obligatoria.
 * - No se permiten cantidades negativas.
 *
 * IMPORTANTE:
 *
 * - El estado inicial será ACTIVO.
 * - La validación definitiva se realiza en el Service.
 * - No modifica directamente las existencias.
 * - No afecta los despachos del CU-11.
 *
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class InventarioFarmaciaFormularioDTO {

    // =====================================================
    // MEDICAMENTO
    // =====================================================

    @NotNull(message = "Debe seleccionar un medicamento.")
    @Positive(message = "Debe seleccionar un medicamento válido.")
    private Integer idMedicamento;


    // =====================================================
    // SUCURSAL
    // =====================================================

    @NotNull(message = "Debe seleccionar una sucursal.")
    @Positive(message = "Debe seleccionar una sucursal válida.")
    private Integer idSucursal;


    // =====================================================
    // CANTIDAD INICIAL
    // =====================================================

    @NotNull(message = "La cantidad inicial es obligatoria.")
    @Min(
            value = 0,
            message = "La cantidad inicial no puede ser negativa."
    )
    private Integer cantidadInicial;
}
