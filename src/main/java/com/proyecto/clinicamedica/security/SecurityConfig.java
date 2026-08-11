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
 * CONFIGURACIÓN DE SEGURIDAD
 * =========================================================
 *
 * Configura:
 *
 * - Rutas públicas.
 * - Rutas protegidas.
 * - JWT desde cookie HttpOnly.
 * - Autorización mediante roles.
 * - Cierre de sesión.
 * - Protección CSRF.
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
    // JWT AUTHENTICATION CONVERTER
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

                                .requestMatchers(
                                        "/",
                                        "/portal",
                                        "/login",
                                        "/registro",
                                        "/error",

                                        "/api/public/**",

                                        "/css/**",
                                        "/js/**",
                                        "/img/**",
                                        "/favicon.ico"
                                )
                                .permitAll()


                                // -------------------------
                                // PORTAL PACIENTE
                                // -------------------------

                                .requestMatchers(
                                        "/paciente/**"
                                )
                                .hasRole(
                                        "PACIENTE"
                                )


                                // -------------------------
                                // RESTO
                                // -------------------------

                                .anyRequest()
                                .authenticated()
                )


                // =========================================
                // CSRF
                // =========================================

                .csrf(
                        csrf -> csrf
                                .ignoringRequestMatchers(
                                        "/api/public/**"
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
                // CERRAR SESIÓN
                // =========================================
                //
                // Spring Security procesa:
                //
                // POST /logout
                //
                // Nuestro LogoutHandler adicional
                // elimina CLINICA_AUTH mediante
                // JwtCookieService.
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
                // SIN SESIÓN DE AUTENTICACIÓN TRADICIONAL
                // =========================================

                .sessionManagement(
                        session -> session
                                .sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
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