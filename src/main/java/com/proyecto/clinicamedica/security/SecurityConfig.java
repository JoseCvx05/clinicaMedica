package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.service.JwtCookieService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
 * - CU-09 Gestión de Laboratorio.
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
                                // CU-13
                                // CONFIGURACIÓN DE SEDES
                                // Y ESPECIALIDADES
                                // =========================
                                //
                                // Acceso exclusivo:
                                // ADMINISTRADOR
                                //
                                // Protege:
                                // - Listado de asignaciones.
                                // - Formulario de creación.
                                // - Registro de asignaciones.
                                // - Eliminación o desactivación.
                                // - Todas las rutas derivadas.
                                //
                                // =========================

                                .requestMatchers(
                                        "/branch-specialty",
                                        "/branch-specialty/**"
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
                                // Debe permanecer ANTES de:
                                //
                                // /interno/**
                                //
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
                                // Debe permanecer ANTES de:
                                //
                                // /interno/**
                                //
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
                                // Debe permanecer ANTES de:
                                //
                                // /interno/**
                                //
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
                                // CU-09
                                // GESTIÓN DE LABORATORIO
                                // =========================
                                //
                                // Exclusivo:
                                // LABORATORISTA
                                //
                                // Incluye:
                                //
                                // - Panel de órdenes.
                                // - Filtros.
                                // - Detalle de orden.
                                // - Registro de resultados.
                                // - Validación de resultados.
                                // - Publicación.
                                // - Finalización de órdenes.
                                //
                                // IMPORTANTE:
                                //
                                // Esta regla debe permanecer
                                // ANTES del matcher general:
                                //
                                // /interno/**
                                //
                                // =========================

                                .requestMatchers(
                                        "/interno/laboratorio",
                                        "/interno/laboratorio/**"
                                )

                                .hasRole(
                                        "LABORATORISTA"
                                )


                                // =========================
                                // CU-11
                                // DESPACHO DE MEDICAMENTOS
                                // =========================
                                //
                                // Acceso exclusivo:
                                // FARMACEUTICO
                                //
                                // Protege:
                                // - Panel de Farmacia.
                                // - Busqueda de recetas.
                                // - Consulta de inventario.
                                // - Registro de despachos.
                                // - Sustitucion de medicamentos.
                                // - Historial de despachos.
                                //
                                // Debe permanecer ANTES de:
                                // /interno/**
                                //
                                // =========================

                                .requestMatchers(
                                        "/interno/farmacia",
                                        "/interno/farmacia/**"
                                )

                                .hasRole(
                                        "FARMACEUTICO"
                                )



                                // =====================================================
// CU-15 - BITÁCORA DE MOVIMIENTOS DE INVENTARIO
// =====================================================
//
// Acceso autorizado:
// - FARMACEUTICO
// - ADMINISTRADOR
//
// Los ajustes de inventario requieren validación
// adicional del rol ADMINISTRADOR en el backend.
//
// Esta regla debe permanecer ANTES de /interno/**.
//
// No modifica la seguridad del CU-11.
// =====================================================

                                .requestMatchers(
                                        "/interno/bitacora-inventario",
                                        "/interno/bitacora-inventario/**"
                                )

                                .hasAnyRole(
                                        "FARMACEUTICO",
                                        "ADMINISTRADOR"
                                )

                                // =========================
                                // PORTAL INTERNO GENERAL
                                // =========================
                                //
                                // Permite acceder a rutas
                                // internas que NO poseen una
                                // regla específica anterior.
                                //
                                // Ejemplo:
                                //
                                // /interno/dashboard
                                //
                                // Las rutas específicas de:
                                //
                                // - Recepción
                                // - Caja
                                // - Enfermería
                                // - Médico
                                // - Laboratorio
                                //
                                // ya fueron protegidas arriba.
                                //
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
                // CSRF - PROTECCIÓN DE FORMULARIOS WEB
                // =========================================
                //
                // El sistema utiliza JWT desde cookie HttpOnly.
                //
                // Aunque Spring Security reconoce el JWT como
                // Bearer Token, las solicitudes del navegador
                // deben mantener protección CSRF.
                //
                // No se modifica la autenticación JWT.
                //
                // =========================================


                .csrf(
                        csrf -> csrf

                                // Guardar el token CSRF en cookie HttpOnly
                                // independiente de la sesión HTTP.
                                .csrfTokenRepository(
                                        new CookieCsrfTokenRepository()
                                )


                                // =========================================
                                // JWT STATELESS - ESTABILIDAD DEL TOKEN CSRF
                                // =========================================
                                //
                                // La autenticación JWT se procesa en cada petición.
                                //
                                // Evitamos que esa autenticación vuelva a generar
                                // continuamente el token CSRF del navegador.
                                //
                                // NO desactiva el filtro CSRF.
                                // NO elimina la validación del token en los POST.
                                //
                                // =========================================

                                .sessionAuthenticationStrategy(
                                        (authentication, request, response) -> {
                                            // No renovar el CSRF por cada petición JWT.
                                        }
                                )


                                .ignoringRequestMatchers(

                                        "/api/public/**",
                                        "/api/interno/login"
                                )

                                .withObjectPostProcessor(
                                        new ObjectPostProcessor<CsrfFilter>() {

                                            @Override
                                            public <O extends CsrfFilter> O postProcess(
                                                    O filtro
                                            ) {

                                                filtro.setRequireCsrfProtectionMatcher(
                                                        request -> {

                                                            String metodo =
                                                                    request.getMethod();

                                                            // Métodos de lectura
                                                            if ("GET".equalsIgnoreCase(metodo)
                                                                    || "HEAD".equalsIgnoreCase(metodo)
                                                                    || "OPTIONS".equalsIgnoreCase(metodo)
                                                                    || "TRACE".equalsIgnoreCase(metodo)) {

                                                                return false;
                                                            }

                                                            String ruta =
                                                                    request.getServletPath();

                                                            // Rutas públicas de API
                                                            if ("/api/public".equals(ruta)
                                                                    || ruta.startsWith("/api/public/")) {

                                                                return false;
                                                            }

                                                            // Login interno
                                                            if ("/api/interno/login".equals(ruta)) {

                                                                return false;
                                                            }

                                                            // POST, PUT, PATCH y DELETE
                                                            // requieren CSRF aunque exista
                                                            // una cookie JWT válida.
                                                            return true;
                                                        }
                                                );

                                                return filtro;
                                            }
                                        }
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
                                //
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