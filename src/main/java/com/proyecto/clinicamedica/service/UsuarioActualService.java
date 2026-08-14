package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;


/**
 * =========================================================
 * SERVICIO: USUARIO ACTUAL
 * =========================================================
 *
 * Responsabilidad:
 *
 * - Obtener la autenticación actual de Spring Security.
 * - Identificar al usuario autenticado.
 * - Recuperar únicamente un usuario activo.
 *
 * Centraliza esta lógica para evitar repetir
 * SecurityContextHolder en diferentes servicios.
 *
 * =========================================================
 */
@Service
public class UsuarioActualService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final UsuarioService usuarioService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioActualService(
            UsuarioService usuarioService
    ) {

        this.usuarioService =
                usuarioService;
    }


    // =====================================================
    // OBTENER USUARIO ACTUAL
    // =====================================================

    public Usuario obtenerUsuarioActual() {

        // =================================================
        // AUTENTICACIÓN
        // =================================================

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        // =================================================
        // VALIDAR AUTENTICACIÓN
        // =================================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new IllegalStateException(
                    "No existe un usuario autenticado."
            );
        }


        // =================================================
        // NOMBRE DE USUARIO
        // =================================================

        String nombreUsuario =
                authentication.getName();


        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalStateException(
                    "No existe un usuario autenticado."
            );
        }


        // =================================================
        // BUSCAR USUARIO ACTIVO
        // =================================================

        return usuarioService
                .buscarActivoPorNombreUsuario(
                        nombreUsuario
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No se encontró el usuario autenticado."
                                )
                );
    }
}