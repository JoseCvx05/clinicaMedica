package com.proyecto.clinicamedica.dto.cita;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * RESULTADO DE VALIDACIÓN - CU-03
 * =========================================================
 *
 * Centraliza todos los errores encontrados durante
 * el wizard de agendamiento de citas.
 *
 * Permite:
 *
 * - agregar errores;
 * - consultar si existen errores;
 * - buscar errores por campo;
 * - obtener mensajes específicos;
 * - manejar errores generales del paso.
 *
 * =========================================================
 */
public class ResultadoValidacionCita {


    private final List<ErrorValidacionCita>
            errores =
            new ArrayList<>();


    // =====================================================
    // AGREGAR ERROR
    // =====================================================

    public void agregarError(
            String campo,
            String mensaje
    ) {

        if (campo == null
                || campo.isBlank()
                || mensaje == null
                || mensaje.isBlank()) {

            return;
        }


        /*
         * Conservamos solamente el primer error
         * asociado a cada campo.
         *
         * Esto evita mostrar varios mensajes
         * contradictorios debajo del mismo input.
         */
        if (tieneError(campo)) {

            return;
        }


        errores.add(
                new ErrorValidacionCita(
                        campo,
                        mensaje
                )
        );
    }


    // =====================================================
    // ¿ES VÁLIDO?
    // =====================================================

    public boolean esValido() {

        return errores.isEmpty();
    }


    // =====================================================
    // ¿TIENE ERRORES?
    // =====================================================

    public boolean tieneErrores() {

        return !errores.isEmpty();
    }


    // =====================================================
    // ¿CAMPO TIENE ERROR?
    // =====================================================

    public boolean tieneError(
            String campo
    ) {

        return buscarError(
                campo
        ).isPresent();
    }


    // =====================================================
    // OBTENER MENSAJE
    // =====================================================

    public String obtenerMensaje(
            String campo
    ) {

        return buscarError(
                campo
        )
                .map(
                        ErrorValidacionCita::mensaje
                )
                .orElse(null);
    }


    // =====================================================
    // BUSCAR ERROR
    // =====================================================

    public Optional<ErrorValidacionCita>
    buscarError(
            String campo
    ) {

        if (campo == null) {

            return Optional.empty();
        }


        return errores
                .stream()
                .filter(
                        error ->
                                campo.equals(
                                        error.campo()
                                )
                )
                .findFirst();
    }


    // =====================================================
    // LISTA DE ERRORES
    // =====================================================

    public List<ErrorValidacionCita>
    getErrores() {

        return Collections.unmodifiableList(
                errores
        );
    }
}