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

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * =========================================================
 * CONTROLADOR: LOGIN DE PACIENTES
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Mostrar la vista de login del paciente.
 * - Recibir las credenciales mediante REST.
 * - Coordinar la autenticación.
 * - Generar el JWT.
 * - Crear la cookie HttpOnly.
 *
 * La lógica de autenticación permanece en
 * AutenticacionService.
 * =========================================================
 */
@Controller
public class LoginController {


    private final AutenticacionService
            autenticacionService;

    private final JwtService
            jwtService;

    private final JwtCookieService
            jwtCookieService;


    public LoginController(
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
    // MOSTRAR LOGIN
    // =====================================================

    @GetMapping("/login")
    public String mostrarLogin() {

        return "login";
    }


    // =====================================================
    // PROCESAR LOGIN
    // =====================================================

    @PostMapping("/api/public/login")
    @ResponseBody
    public ResponseEntity<LoginResponse> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {

        // =================================================
        // AUTENTICAR COMO PACIENTE
        // =================================================

        ResultadoAutenticacion resultado =
                autenticacionService.autenticar(
                        request.getNombreUsuario(),
                        request.getContrasena(),
                        TipoAcceso.PACIENTE
                );


        LoginResponse respuesta =
                resultado.getRespuesta();


        // =================================================
        // CREDENCIALES INCORRECTAS
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CREDENCIALES_INCORRECTAS) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            respuesta
                    );
        }


        // =================================================
        // CUENTA BLOQUEADA
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.CUENTA_BLOQUEADA) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            respuesta
                    );
        }


        // =================================================
        // ROL NO AUTORIZADO
        // =================================================

        if (respuesta.getEstado()
                == EstadoLogin.ROL_NO_AUTORIZADO) {

            return ResponseEntity
                    .status(
                            HttpStatus.FORBIDDEN
                    )
                    .body(
                            respuesta
                    );
        }


        // =================================================
        // VALIDAR RESULTADO EXITOSO
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
        // GENERAR JWT
        // =================================================

        String token =
                jwtService.generarToken(
                        usuario
                );


        // =================================================
        // CREAR COOKIE HTTPONLY
        // =================================================

        ResponseCookie cookie =
                jwtCookieService.crearCookie(
                        token,
                        jwtService.obtenerDuracionToken()
                );


        // =================================================
        // DEVOLVER RESPUESTA
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