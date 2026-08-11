package com.proyecto.clinicamedica.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * RESOLVER JWT DESDE COOKIE
 * =========================================================
 *
 * Spring Security Resource Server normalmente busca
 * el Bearer Token en el encabezado Authorization.
 *
 * En este proyecto el JWT se almacena en una cookie
 * HttpOnly, por lo que necesitamos indicar a Spring
 * Security cómo obtenerlo.
 *
 * Esta clase:
 *
 * - Busca la cookie CLINICA_AUTH.
 * - Obtiene el JWT.
 * - Entrega el token a Spring Security.
 *
 * NO:
 *
 * - Valida el JWT.
 * - Decodifica el JWT.
 * - Verifica la firma.
 *
 * Esas tareas las realizará JwtDecoder.
 * =========================================================
 */
@Component
public class JwtCookieBearerTokenResolver
        implements BearerTokenResolver {

    private final String nombreCookie;


    public JwtCookieBearerTokenResolver(

            @Value(
                    "${app.security.jwt-cookie-name:CLINICA_AUTH}"
            )
            String nombreCookie
    ) {

        if (nombreCookie == null
                || nombreCookie.isBlank()) {

            throw new IllegalStateException(
                    "El nombre de la cookie JWT "
                            + "no puede estar vacío."
            );
        }

        this.nombreCookie =
                nombreCookie.trim();
    }


    // =====================================================
    // RESOLVER TOKEN
    // =====================================================

    @Override
    public String resolve(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }


        Cookie[] cookies =
                request.getCookies();


        if (cookies == null) {
            return null;
        }


        for (Cookie cookie : cookies) {

            if (cookie == null) {
                continue;
            }


            if (!nombreCookie.equals(
                    cookie.getName()
            )) {
                continue;
            }


            String token =
                    cookie.getValue();


            if (token == null
                    || token.isBlank()) {

                return null;
            }


            return token.trim();
        }


        return null;
    }
}