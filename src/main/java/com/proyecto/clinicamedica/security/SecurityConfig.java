package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.service.JwtCookieService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.web.SecurityFilterChain;


/**
 * =========================================================
 * CONFIGURACIÓN GENERAL DE SEGURIDAD
 * =========================================================
 *
 * Define:
 *
 * - Rutas públicas.
 * - Portal del paciente.
 * - Portal interno.
 * - Módulos administrativos.
 * - CU-05 Recepción.
 * - CU-06 Caja.
 * - CU-07 Enfermería.
 * - JWT desde cookie HttpOnly.
 * - Autorización mediante roles.
 * - CSRF.
 * - Logout.
 *
 * =========================================================
 */
@Configuration
public class SecurityConfig {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JwtCookieBearerTokenResolver
            jwtCookieBearerTokenResolver;

    private final JwtCookieService
            jwtCookieService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SecurityConfig(

            JwtCookieBearerTokenResolver
                    jwtCookieBearerTokenResolver,

            JwtCookieService
                    jwtCookieService
    ) {

        this.jwtCookieBearerTokenResolver =
                jwtCookieBearerTokenResolver;

        this.jwtCookieService =
                jwtCookieService;
    }


    // =====================================================
    // PASSWORD ENCODER
    // =====================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    // =====================================================
    // CONVERSIÓN DE ROLES DEL JWT
    // =====================================================

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(

            JwtRolGrantedAuthoritiesConverter
                    jwtRolGrantedAuthoritiesConverter
    ) {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();


        converter.setJwtGrantedAuthoritiesConverter(
                jwtRolGrantedAuthoritiesConverter
        );


        return converter;
    }


    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(

            HttpSecurity http,

            JwtAuthenticationConverter
                    jwtAuthenticationConverter

    ) throws Exception {


        http


                // =========================================
                // AUTORIZACIÓN DE RUTAS
                // =========================================

                .authorizeHttpRequests(

                        auth -> auth


                                // =========================
                                // RUTAS PÚBLICAS
                                // =========================

                                .requestMatchers(

                                        "/",
                                        "/portal",

                                        "/login",
                                        "/login-interno",

                                        "/registro",

                                        "/error",

                                        "/api/public/**",

                                        /*
                                         * Solamente el endpoint
                                         * utilizado para iniciar
                                         * sesión interna es público.
                                         *
                                         * NO hacemos público:
                                         *
                                         * /api/interno/**
                                         */
                                        "/api/interno/login",

                                        "/css/**",
                                        "/js/**",
                                        "/img/**",

                                        "/favicon.ico"
                                )

                                .permitAll()


                                // =========================
                                // CU-01
                                // ADMINISTRACIÓN
                                // =========================
                                //
                                // Exclusivo:
                                // ADMINISTRADOR
                                // =========================

                                .requestMatchers(
                                        "/admin/**"
                                )

                                .hasRole(
                                        "ADMINISTRADOR"
                                )


                                // =========================
                                // PORTAL DEL PACIENTE
                                // =========================
                                //
                                // Exclusivo:
                                // PACIENTE
                                // =========================

                                .requestMatchers(
                                        "/paciente/**"
                                )

                                .hasRole(
                                        "PACIENTE"
                                )


                                // =========================
                                // CU-05
                                // RECEPCIÓN
                                // =========================
                                //
                                // Exclusivo:
                                // RECEPCIONISTA
                                //
                                // Incluye:
                                //
                                // - Búsqueda de citas.
                                // - Registro de llegada.
                                // - Walk-in.
                                // - Emergencias.
                                // - Reasignación de médico.
                                // - Consulta de estado.
                                //
                                // IMPORTANTE:
                                //
                                // Debe estar ANTES de:
                                //
                                // /interno/**
                                //
                                // porque Spring Security
                                // evalúa las reglas en orden.
                                // =========================

                                .requestMatchers(
                                        "/interno/recepcion/**"
                                )

                                .hasRole(
                                        "RECEPCIONISTA"
                                )


                                // =========================
                                // CU-06
                                // CAJA
                                // =========================
                                //
                                // Exclusivo:
                                // CAJERO
                                //
                                // Incluye:
                                //
                                // - Búsqueda de citas.
                                // - Cobro presencial.
                                // - Efectivo.
                                // - Tarjeta.
                                // - POS.
                                // - Recibos.
                                // - Reimpresión.
                                //
                                // También debe estar ANTES
                                // del matcher /interno/**.
                                // =========================

                                .requestMatchers(
                                        "/interno/caja/**"
                                )

                                .hasRole(
                                        "CAJERO"
                                )


                                // =========================
                                // CU-07
                                // ENFERMERÍA
                                // =========================
                                //
                                // Exclusivo:
                                // ENFERMERO
                                //
                                // Incluye:
                                //
                                // - Panel de Enfermería.
                                // - Pacientes presentes.
                                // - Llamado de pacientes.
                                // - Toma de signos vitales.
                                // - Registro de emergencia.
                                // - Alertas clínicas.
                                //
                                // También debe estar ANTES
                                // de /interno/**.
                                // =========================

                                .requestMatchers(
                                        "/interno/enfermeria/**"
                                )

                                .hasRole(
                                        "ENFERMERO"
                                )


                                // =========================
                                // PORTAL INTERNO GENERAL
                                // =========================
                                //
                                // Permite acceder únicamente
                                // a las rutas internas que NO
                                // tienen una regla específica
                                // definida anteriormente.
                                //
                                // Ejemplo:
                                //
                                // /interno/dashboard
                                //
                                // Las rutas específicas:
                                //
                                // /interno/recepcion/**
                                // /interno/caja/**
                                // /interno/enfermeria/**
                                //
                                // ya fueron protegidas arriba.
                                // =========================

                                .requestMatchers(
                                        "/interno/**"
                                )

                                .hasAnyRole(

                                        "MEDICO",

                                        "ENFERMERO",

                                        "RECEPCIONISTA",

                                        "CAJERO",

                                        "LABORATORISTA",

                                        "FARMACEUTICO",

                                        "ADMINISTRADOR"
                                )


                                // =========================
                                // RESTO DEL SISTEMA
                                // =========================

                                .anyRequest()
                                .authenticated()
                )


                // =========================================
                // CSRF
                // =========================================

                .csrf(

                        csrf -> csrf

                                .ignoringRequestMatchers(

                                        /*
                                         * Endpoints REST
                                         * públicos.
                                         */
                                        "/api/public/**",


                                        /*
                                         * Excluimos únicamente
                                         * el POST encargado del
                                         * inicio de sesión interno.
                                         *
                                         * NO excluimos:
                                         *
                                         * /api/interno/**
                                         */
                                        "/api/interno/login"
                                )
                )


                // =========================================
                // JWT / RESOURCE SERVER
                // =========================================

                .oauth2ResourceServer(

                        oauth2 -> oauth2


                                // =========================
                                // JWT DESDE COOKIE
                                // =========================

                                .bearerTokenResolver(
                                        jwtCookieBearerTokenResolver
                                )


                                // =========================
                                // CONVERSIÓN DE ROLES
                                // =========================

                                .jwt(

                                        jwt -> jwt

                                                .jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter
                                                )
                                )
                )


                // =========================================
                // STATELESS
                // =========================================
                //
                // No utilizamos HttpSession para mantener
                // autenticación.
                //
                // La autenticación depende del JWT.
                // =========================================

                .sessionManagement(

                        session -> session

                                .sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )


                // =========================================
                // LOGOUT
                // =========================================

                .logout(

                        logout -> logout


                                // =========================
                                // ENDPOINTS DE LOGOUT
                                // =========================
                                //
                                // Paciente:
                                //
                                // POST /logout
                                //
                                // Interno:
                                //
                                // POST /logout-interno
                                // =========================

                                .logoutRequestMatcher(

                                        request -> {


                                            // =================
                                            // SOLO POST
                                            // =================

                                            if (!"POST"
                                                    .equalsIgnoreCase(
                                                            request.getMethod()
                                                    )) {

                                                return false;
                                            }


                                            String ruta =
                                                    request
                                                            .getServletPath();


                                            return "/logout"
                                                    .equals(
                                                            ruta
                                                    )

                                                    ||

                                                    "/logout-interno"
                                                            .equals(
                                                                    ruta
                                                            );
                                        }
                                )


                                // =========================
                                // ELIMINAR COOKIE JWT
                                // =========================

                                .addLogoutHandler(

                                        (
                                                request,
                                                response,
                                                authentication

                                        ) -> {


                                            response.addHeader(

                                                    HttpHeaders.SET_COOKIE,

                                                    jwtCookieService
                                                            .eliminarCookie()
                                                            .toString()
                                            );
                                        }
                                )


                                // =========================
                                // REDIRECCIÓN DESPUÉS
                                // DEL LOGOUT
                                // =========================

                                .logoutSuccessHandler(

                                        (
                                                request,
                                                response,
                                                authentication

                                        ) -> {


                                            String ruta =
                                                    request
                                                            .getServletPath();


                                            // =================
                                            // LOGIN INTERNO
                                            // =================

                                            if ("/logout-interno"
                                                    .equals(
                                                            ruta
                                                    )) {

                                                response.sendRedirect(
                                                        "/login-interno"
                                                );

                                                return;
                                            }


                                            // =================
                                            // LOGIN PACIENTE
                                            // =================

                                            response.sendRedirect(
                                                    "/login"
                                            );
                                        }
                                )

                                .permitAll()
                )


                // =========================================
                // LOGIN TRADICIONAL DESACTIVADO
                // =========================================
                //
                // El sistema utiliza JWT.
                // =========================================

                .formLogin(
                        form -> form.disable()
                )


                // =========================================
                // HTTP BASIC DESACTIVADO
                // =========================================

                .httpBasic(
                        basic -> basic.disable()
                );


        // =================================================
        // CONSTRUIR CONFIGURACIÓN
        // =================================================

        return http.build();
    }
}