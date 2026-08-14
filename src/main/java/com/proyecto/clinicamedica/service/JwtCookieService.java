package com.proyecto.clinicamedica.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;


/**
 * =========================================================
 * SERVICIO: COOKIE JWT
 * =========================================================
 *
 * Se encarga de crear y eliminar la cookie utilizada
 * para transportar el JWT entre el navegador y el servidor.
 *
 * Características:
 *
 * - HttpOnly = true
 * - SameSite = Lax
 * - Path = /
 * - MaxAge = duración del JWT
 * - Secure configurable según ambiente
 *
 * El JWT no se almacena en:
 *
 * - localStorage
 * - sessionStorage
 * - HTML
 * - JavaScript
 *
 * =========================================================
 */
@Service
public class JwtCookieService {


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private final String nombreCookie;

    private final boolean cookieSecure;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public JwtCookieService(

            @Value(
                    "${app.security.jwt-cookie-name:CLINICA_AUTH}"
            )
            String nombreCookie,

            @Value(
                    "${app.security.cookie-secure:false}"
            )
            boolean cookieSecure
    ) {

        if (nombreCookie == null
                || nombreCookie.isBlank()) {

            throw new IllegalStateException(
                    "El nombre de la cookie JWT no puede estar vacío."
            );
        }


        this.nombreCookie =
                nombreCookie.trim();


        this.cookieSecure =
                cookieSecure;
    }


    // =====================================================
    // CREAR COOKIE
    // =====================================================

    public ResponseCookie crearCookie(
            String token,
            Duration duracion
    ) {

        // =================================================
        // VALIDAR JWT
        // =================================================

        if (token == null
                || token.isBlank()) {

            throw new IllegalArgumentException(
                    "No se puede crear la cookie con un JWT vacío."
            );
        }


        // =================================================
        // VALIDAR DURACIÓN
        // =================================================

        if (duracion == null
                || duracion.isZero()
                || duracion.isNegative()) {

            throw new IllegalArgumentException(
                    "La duración de la cookie debe ser mayor que cero."
            );
        }


        // =================================================
        // CREAR COOKIE
        // =================================================

        return ResponseCookie
                .from(
                        nombreCookie,
                        token
                )

                /*
                 * JavaScript no puede leer directamente
                 * el contenido de la cookie.
                 */
                .httpOnly(
                        true
                )

                /*
                 * Localhost:
                 *
                 * false
                 *
                 * Producción HTTPS:
                 *
                 * true
                 */
                .secure(
                        cookieSecure
                )

                /*
                 * Política adecuada para la navegación
                 * normal dentro del sistema.
                 */
                .sameSite(
                        "Lax"
                )

                /*
                 * Disponible para todas las rutas
                 * de la aplicación.
                 */
                .path(
                        "/"
                )

                /*
                 * La cookie tendrá la misma duración
                 * especificada para la sesión JWT.
                 */
                .maxAge(
                        duracion
                )

                .build();
    }


    // =====================================================
    // ELIMINAR COOKIE
    // =====================================================

    public ResponseCookie eliminarCookie() {

        return ResponseCookie
                .from(
                        nombreCookie,
                        ""
                )

                .httpOnly(
                        true
                )

                .secure(
                        cookieSecure
                )

                .sameSite(
                        "Lax"
                )

                /*
                 * Debe utilizar el mismo Path que la
                 * cookie original para poder eliminarla.
                 */
                .path(
                        "/"
                )

                /*
                 * MaxAge = 0 solicita al navegador
                 * eliminar inmediatamente la cookie.
                 */
                .maxAge(
                        Duration.ZERO
                )

                .build();
    }
}