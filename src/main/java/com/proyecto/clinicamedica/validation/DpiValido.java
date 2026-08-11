package com.proyecto.clinicamedica.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * =========================================================
 * VALIDACIÓN PERSONALIZADA: DPI
 * =========================================================
 *
 * Permite aplicar de forma centralizada la regla
 * RN-GLOBAL-001.
 *
 * Reglas:
 *
 * 1. El DPI es obligatorio.
 * 2. Debe contener exactamente 13 caracteres.
 * 3. Debe contener únicamente números.
 *
 * Esta anotación puede reutilizarse en todos los
 * DTO de los casos de uso que requieran DPI.
 *
 * De esta manera evitamos repetir las mismas
 * validaciones en diferentes clases.
 * =========================================================
 */
@Documented
@Constraint(validatedBy = DpiValidator.class)
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface DpiValido {

    /**
     * Mensaje genérico requerido por Bean Validation.
     *
     * Los mensajes específicos serán generados por
     * DpiValidator según la regla que falle.
     */
    String message() default "El DPI ingresado no es válido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
