package com.proyecto.clinicamedica.dto;

import com.proyecto.clinicamedica.validation.DpiValido;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * DTO: VERIFICACIÓN DE DPI
 * =========================================================
 *
 * Representa la información enviada por el Usuario
 * Externo desde el modal "Verificar Registro".
 *
 * CU-00:
 *
 * El usuario ingresa su DPI para determinar si:
 *
 * - Ya está registrado como paciente.
 * - No se encuentra registrado.
 * - Pertenece a un usuario interno.
 *
 * La validación RN-GLOBAL-001 se encuentra centralizada
 * mediante la anotación @DpiValido.
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class VerificacionDpiRequest {

    /**
     * DPI ingresado por el Usuario Externo.
     *
     * Reglas:
     *
     * - Obligatorio.
     * - Exactamente 13 dígitos.
     * - Únicamente números.
     */
    @DpiValido
    private String dpi;
}