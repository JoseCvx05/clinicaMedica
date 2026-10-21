
package com.proyecto.clinicamedica.exception;

import com.proyecto.clinicamedica.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.HttpRequestMethodNotSupportedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * MANEJADOR GLOBAL DE EXCEPCIONES
 * =========================================================
 *
 * Centraliza las respuestas de error de los endpoints REST.
 *
 * Evita colocar try/catch repetidos en cada Controller.
 *
 * Maneja:
 *
 * - Errores de Bean Validation.
 * - JSON inválido.
 * - Argumentos inválidos.
 * - Recursos no encontrados.
 * - Métodos HTTP no permitidos.
 * - Accesos denegados por seguridad.
 * - Errores inesperados.
 *
 * CU-15:
 *
 * - AccessDeniedException devuelve HTTP 403.
 * - No convierte accesos denegados en errores 500.
 * - No expone información sensible.
 *
 * Aplica:
 *
 * - SRP.
 * - Reutilización.
 * - Separación de responsabilidades.
 *
 * =========================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * =====================================================
     * ERRORES DE VALIDACIÓN
     * =====================================================
     *
     * Se ejecuta cuando un DTO anotado con @Valid
     * no supera alguna de sus validaciones.
     *
     * En CU-00 captura específicamente los mensajes
     * generados por @DpiValido.
     *
     * =====================================================
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String campo = null;

        String mensaje =
                "Los datos ingresados no son válidos.";

        /*
         * Recuperamos el primer error encontrado.
         *
         * Para el CU-00 tendremos únicamente el campo DPI,
         * pero este código será reutilizable en otros DTO.
         */

        if (!exception.getBindingResult()
                .getFieldErrors()
                .isEmpty()) {

            var errorCampo =
                    exception.getBindingResult()
                            .getFieldErrors()
                            .get(0);

            campo = errorCampo.getField();

            if (errorCampo.getDefaultMessage() != null) {

                mensaje =
                        errorCampo.getDefaultMessage();
            }
        }

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Error de validación",
                        mensaje,
                        campo,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * =====================================================
     * JSON INVÁLIDO
     * =====================================================
     *
     * Se ejecuta cuando el cliente envía información
     * JSON incompleta o con formato incorrecto.
     *
     * =====================================================
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Solicitud inválida",
                        "La información enviada no tiene un formato válido.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * =====================================================
     * ARGUMENTOS INVÁLIDOS
     * =====================================================
     *
     * Protege contra llamadas incorrectas a los Services
     * desde otras partes de la aplicación.
     *
     * =====================================================
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Solicitud inválida",
                        exception.getMessage(),
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * =====================================================
     * ACCESO DENEGADO - HTTP 403
     * =====================================================
     *
     * CU-15 - SEGURIDAD POR ROL Y SUCURSAL.
     *
     * Se ejecuta cuando un usuario autenticado
     * intenta realizar una operación no autorizada.
     *
     * Ejemplos:
     *
     * - Farmacéutico intenta registrar Ajuste+.
     * - Farmacéutico intenta registrar Ajuste-.
     * - Farmacéutico intenta operar en otra sucursal.
     * - Usuario intenta consultar información
     *   para la cual no posee permisos.
     *
     * IMPORTANTE:
     *
     * - Devuelve HTTP 403 Forbidden.
     * - No cambia los permisos.
     * - No permite continuar con la operación.
     * - No modifica PostgreSQL.
     * - No revela información interna de seguridad.
     * - No sustituye a Spring Security.
     *
     * =====================================================
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.FORBIDDEN.value(),
                        "Acceso denegado",
                        "No tiene permisos para realizar esta operación.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * =====================================================
     * RECURSO / RUTA NO ENCONTRADA
     * =====================================================
     *
     * Se ejecuta cuando se solicita una ruta que todavía
     * no existe dentro de la aplicación.
     *
     * Ejemplo durante el desarrollo:
     *
     * /registro
     *
     * mientras CU-02 todavía no se haya implementado.
     *
     * =====================================================
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "Recurso no encontrado",
                        "La página o recurso solicitado no se encuentra disponible.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * =====================================================
     * MÉTODO HTTP NO PERMITIDO
     * =====================================================
     *
     * CU-14 - SEGURIDAD DE OPERACIONES.
     *
     * Se ejecuta cuando una ruta existe, pero el método
     * HTTP utilizado no está permitido.
     *
     * Ejemplo:
     *
     * GET /admin/estados-cita/delete/1
     *
     * La desactivación requiere POST, por lo tanto
     * la solicitud GET debe responder HTTP 405.
     *
     * IMPORTANTE:
     *
     * - No ejecuta operaciones de modificación.
     * - No cambia registros en PostgreSQL.
     * - No reemplaza la seguridad de Spring Security.
     * - No reemplaza las validaciones del Service.
     * - Conserva los métodos permitidos en la cabecera
     *   Allow de la respuesta HTTP.
     *
     * =====================================================
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "Método HTTP no permitido",
                        "El método HTTP utilizado no está permitido para esta operación.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(exception.getHeaders())
                .body(response);
    }


    /**
     * =====================================================
     * CU-15 - CONFLICTO DE CONCURRENCIA
     * =====================================================
     *
     * Maneja modificaciones simultáneas detectadas
     * mediante @Version de JPA / Hibernate.
     *
     * - Devuelve HTTP 409 Conflict.
     * - No modifica inventarios.
     * - No intenta repetir la operación.
     * - Evita presentar el conflicto como error 500.
     *
     * =====================================================
     */
    @ExceptionHandler(
            ObjectOptimisticLockingFailureException.class
    )
    public ResponseEntity<ErrorResponse> manejarConflictoConcurrencia(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.CONFLICT.value(),
                        "Conflicto de concurrencia",
                        "El registro fue modificado por otra operación. " +
                                "Actualice la página e inténtelo nuevamente.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * =====================================================
     * ERROR INESPERADO
     * =====================================================
     *
     * Evitamos exponer:
     *
     * - Stack trace.
     * - SQL.
     * - Rutas internas.
     * - Clases Java.
     * - Información sensible.
     *
     * Este manejador se utiliza cuando ninguna excepción
     * específica anterior puede resolver el error.
     *
     * Los accesos denegados ya se manejan mediante
     * AccessDeniedException y devuelven HTTP 403.
     *
     * =====================================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(
            Exception exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno",
                        "Ocurrió un error inesperado al procesar la solicitud.",
                        null,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
