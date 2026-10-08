package com.proyecto.clinicamedica.dto;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * DTO: RESULTADO RECIBIDO DESDE LIS
 * =========================================================
 *
 * RNF-030
 *
 * Representa un resultado normalizado recibido desde
 * un analizador de laboratorio o desde un sistema LIS.
 *
 * IMPORTANTE:
 *
 * Este DTO NO depende de:
 *
 * - HL7.
 * - ASTM.
 * - REST.
 * - Marca del analizador.
 * - Fabricante.
 *
 * Un adaptador futuro será responsable de convertir
 * el formato propio del analizador a este formato
 * interno del sistema.
 *
 * De esta manera CU-09 no queda acoplado a una
 * tecnología o proveedor específico.
 *
 * =========================================================
 */
public record ResultadoLisDTO(

        // =================================================
        // IDENTIFICADOR EXTERNO
        // =================================================
        //
        // Identificador único del mensaje enviado por LIS.
        //
        // Permitirá implementar idempotencia para evitar
        // procesar dos veces el mismo resultado.
        //
        String idMensajeLis,


        // =================================================
        // ANALIZADOR
        // =================================================
        //
        // Ejemplo:
        //
        // ANALIZADOR-HEMATOLOGIA-01
        //
        String identificadorAnalizador,


        // =================================================
        // ORDEN
        // =================================================
        //
        // Debe corresponder al número de orden generado
        // por nuestro sistema.
        //
        String numeroOrden,


        // =================================================
        // EXAMEN
        // =================================================
        //
        // Se utilizará el código histórico almacenado en
        // DetalleOrdenLaboratorio.
        //
        // No se buscará únicamente por nombre porque
        // podría cambiar o escribirse de forma diferente.
        //
        String codigoExamen,


        // =================================================
        // RESULTADO
        // =================================================

        String valorResultado,


        String unidadResultado,


        OffsetDateTime fechaResultado,


        // =================================================
        // FUERA DE RANGO
        // =================================================
        //
        // Puede venir informado por el analizador.
        //
        // Posteriormente Laboratorio podrá revisar este
        // indicador antes de validar/publicar.
        //
        Boolean fueraRango,


        // =================================================
        // OBSERVACIONES
        // =================================================

        String observaciones

) {


    // =====================================================
    // VALIDACIÓN BÁSICA DEL MENSAJE LIS
    // =====================================================

    public void validar() {

        if (idMensajeLis == null
                || idMensajeLis.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El identificador del mensaje LIS es obligatorio."
            );
        }


        if (idMensajeLis.trim().length() > 150) {

            throw new IllegalArgumentException(
                    "El identificador del mensaje LIS "
                            + "no puede superar los 150 caracteres."
            );
        }


        if (identificadorAnalizador == null
                || identificadorAnalizador.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El identificador del analizador es obligatorio."
            );
        }


        if (identificadorAnalizador.trim().length() > 150) {

            throw new IllegalArgumentException(
                    "El identificador del analizador "
                            + "no puede superar los 150 caracteres."
            );
        }


        if (numeroOrden == null
                || numeroOrden.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El número de orden recibido desde LIS es obligatorio."
            );
        }


        if (numeroOrden.trim().length() > 40) {

            throw new IllegalArgumentException(
                    "El número de orden recibido desde LIS "
                            + "no es válido."
            );
        }


        if (codigoExamen == null
                || codigoExamen.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El código del examen recibido desde LIS es obligatorio."
            );
        }


        if (codigoExamen.trim().length() > 30) {

            throw new IllegalArgumentException(
                    "El código del examen recibido desde LIS "
                            + "no es válido."
            );
        }


        if (valorResultado == null
                || valorResultado.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El valor del resultado recibido desde LIS es obligatorio."
            );
        }


        if (valorResultado.trim().length() > 255) {

            throw new IllegalArgumentException(
                    "El resultado recibido desde LIS "
                            + "supera la longitud permitida."
            );
        }


        if (unidadResultado != null
                && unidadResultado.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "La unidad recibida desde LIS "
                            + "supera la longitud permitida."
            );
        }


        if (fechaResultado == null) {

            throw new IllegalArgumentException(
                    "La fecha del resultado recibido desde LIS es obligatoria."
            );
        }


        if (observaciones != null
                && observaciones.trim().length() > 5000) {

            throw new IllegalArgumentException(
                    "Las observaciones recibidas desde LIS "
                            + "superan la longitud permitida."
            );
        }
    }
}