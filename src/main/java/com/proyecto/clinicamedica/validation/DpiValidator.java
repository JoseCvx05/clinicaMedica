package com.proyecto.clinicamedica.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * =========================================================
 * VALIDADOR: DPI
 * =========================================================
 *
 * Implementa RN-GLOBAL-001.
 *
 * Reglas:
 *
 * 1. DPI obligatorio.
 * 2. Exactamente 13 caracteres.
 * 3. Únicamente números.
 *
 * Devuelve el mensaje específico correspondiente
 * a la primera regla que falle.
 * =========================================================
 */
public class DpiValidator
        implements ConstraintValidator<DpiValido, String> {

    private static final int LONGITUD_DPI = 13;

    private static final String MENSAJE_OBLIGATORIO =
            "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.";

    private static final String MENSAJE_NUMERICO =
            "El DPI debe contener únicamente números. "
                    + "No se permiten letras ni caracteres especiales.";


    /**
     * Valida el DPI recibido.
     */
    @Override
    public boolean isValid(
            String dpi,
            ConstraintValidatorContext context
    ) {

        // =================================================
        // REGLA 1: OBLIGATORIO
        // =================================================

        if (dpi == null || dpi.isBlank()) {

            agregarMensaje(
                    context,
                    MENSAJE_OBLIGATORIO
            );

            return false;
        }


        // =================================================
        // REGLA 2: EXACTAMENTE 13 CARACTERES
        // =================================================

        if (dpi.length() != LONGITUD_DPI) {

            String mensaje =
                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpi.length()
                            + " dígitos.";

            agregarMensaje(
                    context,
                    mensaje
            );

            return false;
        }


        // =================================================
        // REGLA 3: ÚNICAMENTE NÚMEROS
        // =================================================

        if (!dpi.matches("^[0-9]{13}$")) {

            agregarMensaje(
                    context,
                    MENSAJE_NUMERICO
            );

            return false;
        }


        return true;
    }


    /**
     * Reemplaza el mensaje genérico de la anotación
     * por el mensaje específico de la regla que falló.
     */
    private void agregarMensaje(
            ConstraintValidatorContext context,
            String mensaje
    ) {

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(
                        mensaje
                )
                .addConstraintViolation();
    }
}