package com.proyecto.clinicamedica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * DTO: RESPUESTA DE ERROR
 * =========================================================
 *
 * Representa los errores controlados que el backend
 * enviará al frontend.
 *
 * Se utilizará de forma general para los diferentes
 * casos de uso del sistema.
 *
 * Ejemplos:
 *
 * - Error de validación.
 * - Recurso no encontrado.
 * - Solicitud incorrecta.
 * - Error interno controlado.
 *
 * Evita enviar al navegador:
 *
 * - Stack traces.
 * - Nombres de clases internas.
 * - Información sensible.
 * =========================================================
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Fecha y hora en la que ocurrió el error.
     */
    private OffsetDateTime timestamp;


    /**
     * Código HTTP.
     *
     * Ejemplos:
     * 400
     * 401
     * 403
     * 404
     * 500
     */
    private int status;


    /**
     * Nombre general del error.
     *
     * Ejemplo:
     * "Error de validación"
     */
    private String error;


    /**
     * Mensaje que podrá mostrarse al usuario.
     */
    private String mensaje;


    /**
     * Campo que produjo el error.
     *
     * Ejemplo:
     * "dpi"
     *
     * Puede ser null cuando el error no pertenece
     * específicamente a un campo.
     */
    private String campo;


    /**
     * Ruta donde ocurrió el error.
     */
    private String path;
}