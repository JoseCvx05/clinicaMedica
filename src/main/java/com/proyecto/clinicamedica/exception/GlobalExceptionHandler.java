package com.proyecto.clinicamedica.exception;

import com.proyecto.clinicamedica.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
 * Actualmente maneja:
 *
 * - Errores de Bean Validation.
 * - JSON inválido.
 * - Argumentos inválidos.
 * - Errores inesperados.
 *
 * Posteriormente podremos agregar excepciones específicas
 * de autenticación, autorización, recursos inexistentes,
 * duplicados, etc.
 *
 * Aplica:
 *
 * - SRP.
 * - Reutilización.
 * - Separación de responsabilidades.
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
     * Ejemplo:
     *
     * {
     *    "dpi":
     *
     * JSON incompleto o mal formado.
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
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse>
    manejarArgumentoInvalido(
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
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    manejarErrorGeneral(
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
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(response);
    }
}