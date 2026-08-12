package com.proyecto.clinicamedica.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * RESULTADO DE VALIDACIÓN - REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Centraliza los errores detectados por los distintos
 * validadores polimórficos.
 *
 * Permite:
 *
 * - agregar errores;
 * - consultar si existen errores;
 * - consultar si un campo tiene error;
 * - recuperar el mensaje de un campo.
 *
 * =========================================================
 */
public class ResultadoValidacionRegistroExterno {

    private final List<ErrorValidacionRegistroExterno>
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
         * Solo dejamos el primer error de cada campo.
         *
         * Esto permite respetar el orden de validación:
         *
         * obligatorio
         *      ↓
         * longitud
         *      ↓
         * formato
         *
         * y evita mostrar tres mensajes simultáneos
         * debajo del mismo input.
         */
        if (tieneError(campo)) {

            return;
        }


        errores.add(
                new ErrorValidacionRegistroExterno(
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

        return buscarError(campo)
                .isPresent();
    }


    // =====================================================
    // OBTENER MENSAJE
    // =====================================================

    public String obtenerMensaje(
            String campo
    ) {

        return buscarError(campo)
                .map(
                        ErrorValidacionRegistroExterno::mensaje
                )
                .orElse(null);
    }


    // =====================================================
    // BUSCAR ERROR
    // =====================================================

    public Optional<ErrorValidacionRegistroExterno>
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

    public List<ErrorValidacionRegistroExterno>
    getErrores() {

        return Collections.unmodifiableList(
                errores
        );
    }
}