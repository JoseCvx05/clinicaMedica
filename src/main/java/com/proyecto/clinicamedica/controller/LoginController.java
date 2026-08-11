package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.EstadoLogin;
import com.proyecto.clinicamedica.dto.LoginRequest;
import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.service.AutenticacionService;
import com.proyecto.clinicamedica.service.JwtCookieService;
import com.proyecto.clinicamedica.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * =========================================================
 * CONTROLADOR REST: LOGIN DE PACIENTES
 * =========================================================
 *
 * Recibe las credenciales del portal y coordina:
 *
 * 1. Autenticación.
 * 2. Validación del resultado.
 * 3. Generación del JWT.
 * 4. Creación de la cookie HttpOnly.
 * 5. Respuesta al frontend.
 *
 * La lógica de autenticación NO se encuentra aquí.
 * =========================================================
 */
@RestController
@RequestMapping("/api/public")
public class LoginController {

    private final AutenticacionService autenticacionService;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;


    public LoginController(
            AutenticacionService autenticacionService,
            JwtService jwtService,
            JwtCookieService jwtCookieService
    ) {
        this.autenticacionService = autenticacionService;
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
    }


    // =====================================================
    // INICIAR SESIÓN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {

        // =================================================
        // 1. AUTENTICAR
        // =================================================

        ResultadoAutenticacion resultado =
                autenticacionService.autenticar(
                        request.getNombreUsuario(),
                        request.getContrasena()
                );


        LoginResponse respuesta =
                resultado.getRespuesta();


        // =================================================
        // 2. FA06 - CREDENCIALES INCORRECTAS
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CREDENCIALES_INCORRECTAS) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(respuesta);
        }


        // =================================================
        // 3. FA07 - CUENTA BLOQUEADA
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CUENTA_BLOQUEADA) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(respuesta);
        }


        // =================================================
        // 4. FA09 - ROL NO AUTORIZADO
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.ROL_NO_AUTORIZADO) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(respuesta);
        }


        // =================================================
        // 5. AUTENTICACIÓN EXITOSA
        // =================================================

        if (respuesta.getEstado()
                != EstadoLogin.AUTENTICADO) {

            throw new IllegalStateException(
                    "El resultado de autenticación no es válido."
            );
        }


        Usuario usuario =
                resultado.getUsuarioAutenticado();


        if (usuario == null) {

            throw new IllegalStateException(
                    "No se encontró el usuario autenticado."
            );
        }


        // =================================================
        // 6. GENERAR JWT
        // =================================================

        String token =
                jwtService.generarToken(
                        usuario
                );


        // =================================================
        // 7. CREAR COOKIE HTTPONLY
        // =================================================

        ResponseCookie cookie =
                jwtCookieService.crearCookie(
                        token,
                        jwtService.obtenerDuracionToken()
                );


        // =================================================
        // 8. ENVIAR COOKIE + RESPUESTA
        // =================================================

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .body(
                        respuesta
                );
    }
}