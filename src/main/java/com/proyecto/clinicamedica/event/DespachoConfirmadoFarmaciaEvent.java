package com.proyecto.clinicamedica.event;
/**
 * =========================================================
 * EVENTO: DESPACHO CONFIRMADO EN FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 * FA02 - Sustitución de Medicamentos.
 *
 * Representa la confirmación de un despacho.
 *
 * Este evento permitirá procesar notificaciones
 * después de confirmar la transacción.
 *
 * IMPORTANTE:
 *
 * - No envía correos.
 * - No modifica inventario.
 * - No registra despachos.
 * - Solo transporta el ID del despacho confirmado.
 *
 * =========================================================
 */
public record DespachoConfirmadoFarmaciaEvent(
        Integer idDespacho
) {

    // =====================================================
    // VALIDACIÓN DEL IDENTIFICADOR
    // =====================================================

    public DespachoConfirmadoFarmaciaEvent {

        if (idDespacho == null || idDespacho <= 0) {

            throw new IllegalArgumentException(
                    "El evento requiere un ID de despacho válido."
            );
        }
    }
}
