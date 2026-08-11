package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.EstadoLogin;
import com.proyecto.clinicamedica.dto.LoginRequest;
import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.security.TipoAcceso;
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
 * CONTROLADOR REST: LOGIN DEL PERSONAL INTERNO
 * =========================================================
 *
 * Atiende exclusivamente el inicio de sesión del
 * personal interno del hospital.
 *
 * Reutiliza:
 *
 * - AutenticacionService
 * - PasswordEncoder
 * - control de intentos
 * - bloqueo temporal
 * - JwtService
 * - JwtCookieService
 *
 * La diferencia respecto al login de pacientes es:
 *
 * TipoAcceso.INTERNO
 *
 * La autorización posterior de cada módulo se realiza
 * mediante los roles almacenados en el JWT.
 * =========================================================
 */
@RestController
@RequestMapping("/api/interno")
public class LoginInternoController {

    private final AutenticacionService
            autenticacionService;

    private final JwtService
            jwtService;

    private final JwtCookieService
            jwtCookieService;


    public LoginInternoController(
            AutenticacionService autenticacionService,
            JwtService jwtService,
            JwtCookieService jwtCookieService
    ) {

        this.autenticacionService =
                autenticacionService;

        this.jwtService =
                jwtService;

        this.jwtCookieService =
                jwtCookieService;
    }


    // =====================================================
    // LOGIN INTERNO
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {

        // =================================================
        // 1. AUTENTICAR COMO USUARIO INTERNO
        // =================================================

        ResultadoAutenticacion resultado =
                autenticacionService.autenticar(
                        request.getNombreUsuario(),
                        request.getContrasena(),
                        TipoAcceso.INTERNO
                );


        LoginResponse respuesta =
                resultado.getRespuesta();


        // =================================================
        // 2. CREDENCIALES INCORRECTAS
        // RN-GLOBAL-007
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CREDENCIALES_INCORRECTAS) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(respuesta);
        }


        // =================================================
        // 3. CUENTA BLOQUEADA
        // RN-GLOBAL-007
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CUENTA_BLOQUEADA) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(respuesta);
        }


        // =================================================
        // 4. USUARIO NO PERTENECE AL PERSONAL INTERNO
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.ROL_NO_AUTORIZADO) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(respuesta);
        }


        // =================================================
        // 5. RESULTADO INESPERADO
        // =================================================

        if (respuesta.getEstado()
                != EstadoLogin.AUTENTICADO) {

            throw new IllegalStateException(
                    "El resultado de autenticación interna no es válido."
            );
        }


        Usuario usuario =
                resultado.getUsuarioAutenticado();


        if (usuario == null) {

            throw new IllegalStateException(
                    "No se encontró el usuario interno autenticado."
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
        // 8. RESPUESTA
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