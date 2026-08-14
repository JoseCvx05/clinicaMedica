package com.proyecto.clinicamedica.dto.cita;

/**
 * =========================================================
 * RESULTADO DE VALIDACIÓN DE DOCUMENTO
 * =========================================================
 *
 * CU-03 Agendar Citas.
 * =========================================================
 */
public record ResultadoValidacionDocumentoCita(

        boolean valido,

        String mensaje

) {


    public static ResultadoValidacionDocumentoCita documentoValido() {

        return new ResultadoValidacionDocumentoCita(
                true,
                null
        );
    }


    public static ResultadoValidacionDocumentoCita documentoInvalido(
            String mensaje
    ) {

        return new ResultadoValidacionDocumentoCita(
                false,
                mensaje
        );
    }
}