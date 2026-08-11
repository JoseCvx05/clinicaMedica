package com.proyecto.clinicamedica.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * DTO: RESULTADO DE VALIDACIÓN DE USUARIO
 * =========================================================
 *
 * Contiene todos los errores detectados durante la
 * validación del formulario del CU-01.
 *
 * Permite:
 *
 * - Saber si el formulario es válido.
 * - Agregar errores por campo.
 * - Consultar si un campo tiene error.
 * - Recuperar el mensaje de un campo.
 * - Obtener la lista completa de errores.
 *
 * =========================================================
 */
public class ResultadoValidacionUsuario {

    // =====================================================
    // ERRORES
    // =====================================================

    private final List<ErrorValidacionUsuario>
            errores =
            new ArrayList<>();


    // =====================================================
    // AGREGAR ERROR
    // =====================================================

    /**
     * Agrega un error solamente si ese campo todavía
     * no tiene otro error registrado.
     *
     * Esto es importante porque queremos mostrar un solo
     * mensaje claro por campo.
     *
     * Ejemplo:
     *
     * nombreUsuario = "abc"
     *
     * Debe mostrar:
     *
     * "El usuario debe contener al menos 8 caracteres."
     *
     * y no varios mensajes al mismo tiempo.
     */
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


        if (tieneError(
                campo
        )) {

            return;
        }


        errores.add(
                new ErrorValidacionUsuario(
                        campo,
                        mensaje
                )
        );
    }


    // =====================================================
    // ES VÁLIDO
    // =====================================================

    /**
     * @return true cuando no existe ningún error.
     */
    public boolean esValido() {

        return errores.isEmpty();
    }


    // =====================================================
    // TIENE ERRORES
    // =====================================================

    public boolean tieneErrores() {

        return !errores.isEmpty();
    }


    // =====================================================
    // ERROR POR CAMPO
    // =====================================================

    /**
     * Permite verificar si un campo específico
     * contiene error.
     *
     * Ejemplo:
     *
     * resultado.tieneError("nombreUsuario")
     */
    public boolean tieneError(
            String campo
    ) {

        if (campo == null) {
            return false;
        }


        return errores
                .stream()
                .anyMatch(
                        error ->
                                campo.equals(
                                        error.campo()
                                )
                );
    }


    // =====================================================
    // OBTENER MENSAJE
    // =====================================================

    /**
     * Obtiene el mensaje asociado a un campo.
     *
     * Si no existe error para ese campo,
     * devuelve null.
     */
    public String obtenerMensaje(
            String campo
    ) {

        return buscarError(
                campo
        )
                .map(
                        ErrorValidacionUsuario::mensaje
                )
                .orElse(
                        null
                );
    }


    // =====================================================
    // BUSCAR ERROR
    // =====================================================

    public Optional<ErrorValidacionUsuario> buscarError(
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
    // OBTENER TODOS LOS ERRORES
    // =====================================================

    /**
     * Devuelve una lista de solo lectura.
     *
     * Desde fuera de esta clase no será posible hacer:
     *
     * getErrores().clear()
     *
     * y modificar accidentalmente el resultado.
     */
    public List<ErrorValidacionUsuario> getErrores() {

        return Collections.unmodifiableList(
                errores
        );
    }
}