package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.service.JwtCookieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO DE COOKIE JWT
 * =========================================================
 *
 * Se encarga exclusivamente de crear y eliminar
 * la cookie utilizada para transportar el JWT.
 *
 * Características:
 *
 * - HttpOnly = true
 * - SameSite = Lax
 * - Path = /
 * - MaxAge = duración del JWT
 * - Secure configurable según ambiente
 *
 * El JWT no será almacenado en localStorage ni
 * manipulado directamente por JavaScript.
 * =========================================================
 */
@Service
public class JwtCookieServiceImpl
        implements JwtCookieService {

    private final String nombreCookie;

    private final boolean cookieSecure;


    public JwtCookieServiceImpl(

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

    @Override
    public ResponseCookie crearCookie(
            String token,
            Duration duracion
    ) {

        if (token == null
                || token.isBlank()) {

            throw new IllegalArgumentException(
                    "No se puede crear la cookie con un JWT vacío."
            );
        }


        if (duracion == null
                || duracion.isZero()
                || duracion.isNegative()) {

            throw new IllegalArgumentException(
                    "La duración de la cookie debe ser mayor que cero."
            );
        }


        return ResponseCookie
                .from(
                        nombreCookie,
                        token
                )

                /*
                 * JavaScript no podrá leer directamente
                 * el contenido de la cookie.
                 */
                .httpOnly(true)

                /*
                 * En localhost será false.
                 *
                 * En producción con HTTPS será true.
                 */
                .secure(
                        cookieSecure
                )

                /*
                 * Permite una política adecuada para
                 * navegación normal dentro del sistema.
                 */
                .sameSite(
                        "Lax"
                )

                /*
                 * La cookie estará disponible para
                 * todas las rutas del sistema.
                 */
                .path(
                        "/"
                )

                /*
                 * Misma duración que el JWT.
                 */
                .maxAge(
                        duracion
                )

                .build();
    }


    // =====================================================
    // ELIMINAR COOKIE
    // =====================================================

    @Override
    public ResponseCookie eliminarCookie() {

        return ResponseCookie
                .from(
                        nombreCookie,
                        ""
                )

                .httpOnly(true)

                .secure(
                        cookieSecure
                )

                .sameSite(
                        "Lax"
                )

                .path(
                        "/"
                )

                /*
                 * MaxAge = 0 hace que el navegador
                 * elimine inmediatamente la cookie.
                 */
                .maxAge(
                        Duration.ZERO
                )

                .build();
    }
}