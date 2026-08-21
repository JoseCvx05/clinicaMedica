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

                .authorizeHttpRequests(auth -> auth

                        // ================================================
                        // RUTAS PÚBLICAS
                        // ================================================

                        .requestMatchers(
                                "/",
                                "/portal",
                                "/login",
                                "/login-interno",
                                "/registro",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/favicon.ico",
                                "/api/public/**",
                                "/api/interno/login"
                        )
                        .permitAll()


                        // ================================================
                        // ADMINISTRACIÓN
                        // ================================================

                        .requestMatchers(
                                "/admin/**"
                        )
                        .hasRole(
                                "ADMINISTRADOR"
                        )


                        // ================================================
                        // CU-05 - RECEPCIÓN
                        // SOLO RECEPCIONISTA
                        // ================================================

                        .requestMatchers(
                                "/interno/recepcion/**"
                        )
                        .hasRole(
                                "RECEPCIONISTA"
                        )


                        // ================================================
                        // CU-06 - CAJA
                        // SOLO CAJERO
                        // ================================================

                        .requestMatchers(
                                "/interno/caja/**"
                        )
                        .hasRole(
                                "CAJERO"
                        )


                        // ================================================
                        // PACIENTE
                        // ================================================

                        .requestMatchers(
                                "/paciente/**"
                        )
                        .hasRole(
                                "PACIENTE"
                        )


                        // ================================================
                        // RESTO DEL ÁREA INTERNA
                        // ================================================
                        //
                        // ESTA REGLA TIENE QUE IR DESPUÉS DE
                        // RECEPCIÓN Y CAJA.
                        // ================================================

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


                        // ================================================
                        // CUALQUIER OTRA RUTA
                        // ================================================

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

                                // =========================================
                                // ACEPTAR LOS DOS ENDPOINTS DE LOGOUT
                                // =========================================

                                .logoutRequestMatcher(
                                        request -> {

                                            if (!"POST".equalsIgnoreCase(
                                                    request.getMethod()
                                            )) {

                                                return false;
                                            }

                                            String ruta =
                                                    request.getServletPath();

                                            return "/logout".equals(ruta)
                                                    || "/logout-interno".equals(ruta);
                                        }
                                )


                                // =========================================
                                // ELIMINAR COOKIE JWT
                                // =========================================

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


                                // =========================================
                                // REDIRECCIÓN DESPUÉS DEL LOGOUT
                                // =========================================

                                .logoutSuccessHandler(
                                        (
                                                request,
                                                response,
                                                authentication
                                        ) -> {

                                            String ruta =
                                                    request.getServletPath();


                                            // LOGIN INTERNO
                                            if ("/logout-interno".equals(ruta)) {

                                                response.sendRedirect(
                                                        "/login-interno"
                                                );

                                                return;
                                            }


                                            // LOGIN PACIENTE
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