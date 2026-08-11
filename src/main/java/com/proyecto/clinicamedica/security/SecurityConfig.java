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
 * - JWT desde cookie HttpOnly.
 * - Autorización mediante roles.
 * - CSRF.
 * - Logout.
 * =========================================================
 */
@Configuration
public class SecurityConfig {

    private final JwtCookieBearerTokenResolver
            jwtCookieBearerTokenResolver;

    private final JwtCookieService
            jwtCookieService;


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
                                         * de LOGIN interno es público.
                                         *
                                         * No hacemos público todo:
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
                                // Solamente Administrador.
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
                                         * Endpoint REST público
                                         * utilizado por CU-00.
                                         */
                                        "/api/public/**",

                                        /*
                                         * Excluimos únicamente
                                         * el POST de autenticación
                                         * interna.
                                         *
                                         * NO excluimos todo
                                         * /api/interno/**.
                                         */
                                        "/api/interno/login"
                                )
                )


                // =========================================
                // JWT / RESOURCE SERVER
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

                                .logoutUrl(
                                        "/logout"
                                )

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

                                .logoutSuccessUrl(
                                        "/"
                                )

                                .permitAll()
                )


                // =========================================
                // LOGIN TRADICIONAL DESACTIVADO
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