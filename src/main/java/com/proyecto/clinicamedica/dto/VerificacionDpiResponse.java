package com.proyecto.clinicamedica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * =========================================================
 * DTO: RESPUESTA DE VERIFICACIÓN DE DPI
 * =========================================================
 *
 * Representa la respuesta generada por el sistema después
 * de verificar el DPI ingresado en el CU-00.
 *
 * Puede representar:
 *
 * - Paciente registrado.
 * - Usuario no registrado.
 * - Usuario interno.
 *
 * No expone información sensible como:
 *
 * - DPI.
 * - dpiHash.
 * - dpiCifrado.
 * - NIT.
 * - contraseña.
 * =========================================================
 */
@Getter
@AllArgsConstructor
public class VerificacionDpiResponse {

    /**
     * Resultado de la verificación.
     */
    private EstadoVerificacionDpi estado;


    /**
     * Mensaje que será mostrado al usuario.
     */
    private String mensaje;


    /**
     * Ruta a la que debe dirigirse el navegador.
     *
     * Puede ser null cuando el usuario debe permanecer
     * en el modal.
     */
    private String redireccion;
}