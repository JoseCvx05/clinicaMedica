package com.proyecto.clinicamedica.service;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * =========================================================
 * SERVICIO: COOKIE JWT
 * =========================================================
 *
 * Define el contrato encargado de preparar la cookie
 * utilizada para transportar el JWT entre el navegador
 * y el servidor.
 *
 * El JWT NO será guardado en:
 *
 * - localStorage
 * - sessionStorage
 * - HTML
 * - JavaScript
 *
 * Se utilizará una cookie HttpOnly.
 *
 * Esto permite que JavaScript no tenga acceso directo
 * al token.
 * =========================================================
 */
public interface JwtCookieService {

    /**
     * Crea la cookie que contiene el JWT.
     *
     * @param token JWT generado
     * @param duracion duración del token
     * @return cookie preparada para enviarse al navegador
     */
    ResponseCookie crearCookie(
            String token,
            Duration duracion
    );


    /**
     * Crea una cookie expirada para eliminar
     * la sesión JWT del navegador.
     *
     * Se utilizará posteriormente al cerrar sesión.
     *
     * @return cookie con duración cero
     */
    ResponseCookie eliminarCookie();
}