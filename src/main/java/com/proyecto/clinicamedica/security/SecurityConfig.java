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
 * - CU-01 Administración.
 * - CU-05 Recepción.
 * - CU-06 Caja.
 * - CU-07 Enfermería.
 * - CU-08 Consulta Médica.
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
                // AUTORIZACIÓN
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

                                .requestMatchers(
                                        "/admin/**"
                                )

                                .hasRole(
                                        "ADMINISTRADOR"
                                )


                                // =========================
                                // PORTAL DEL PACIENTE
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

                                .requestMatchers(
                                        "/interno/enfermeria/**"
                                )

                                .hasRole(
                                        "ENFERMERO"
                                )


                                // =========================
                                // CU-08
                                // CONSULTA MÉDICA
                                // =========================
                                //
                                // Exclusivo:
                                // MEDICO
                                //
                                // Incluye:
                                //
                                // - Panel del Médico.
                                // - Inicio de consulta.
                                // - Expediente clínico.
                                // - CIE-10.
                                // - Guardar consulta.
                                // - Finalizar consulta.
                                // - No asistió.
                                // - Receta.
                                // - Laboratorio.
                                // - Seguimiento.
                                // - Finalización de atención.
                                //
                                // Debe permanecer ANTES de:
                                //
                                // /interno/**
                                //
                                // =========================

                                .requestMatchers(
                                        "/interno/medico/**"
                                )

                                .hasRole(
                                        "MEDICO"
                                )


                                // =========================
                                // PORTAL INTERNO GENERAL
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
                                // RESTO
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

                                        "/api/public/**",

                                        "/api/interno/login"
                                )
                )


                // =========================================
                // JWT
                // =========================================

                .oauth2ResourceServer(

                        oauth2 -> oauth2

                                .bearerTokenResolver(
                                        jwtCookieBearerTokenResolver
                                )

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


                                .logoutRequestMatcher(

                                        request -> {


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
                                // REDIRECCIÓN
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


                                            if ("/logout-interno"
                                                    .equals(
                                                            ruta
                                                    )) {

                                                response.sendRedirect(
                                                        "/login-interno"
                                                );

                                                return;
                                            }


                                            response.sendRedirect(
                                                    "/login"
                                            );
                                        }
                                )

                                .permitAll()
                )


                // =========================================
                // FORM LOGIN DESACTIVADO
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


        return http.build();
    }
}