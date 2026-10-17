
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * CU-14 - SERVICIO DE PERMISOS DE CATÁLOGOS
 * =========================================================
 *
 * Centraliza las autorizaciones específicas de los
 * catálogos administrativos.
 *
 * No modifica la autenticación JWT.
 * No modifica los roles registrados en PostgreSQL.
 *
 * Cada operación debe verificar su permiso antes
 * de realizar cambios en el sistema.
 *
 * =========================================================
 */
@Service
public class PermisoCatalogoService {

    // =====================================================
    // PERMISOS ASIGNADOS A CADA ROL
    // =====================================================

    private static final Map<String, Set<String>> PERMISOS_POR_ROL =
            Map.of(
                    "ADMINISTRADOR",
                    Set.of(

                            // Especialidades
                            "specialty/read",
                            "specialty/create",
                            "specialty/update",
                            "specialty/delete",

                            // Sucursales
                            "branch/read",
                            "branch/create",
                            "branch/update",
                            "branch/delete",


                            // Estados de cita
                            "appointment-status/read",
                            "appointment-status/create",
                            "appointment-status/update",
                            "appointment-status/delete",


                            // Laboratorios
                            "laboratory/read",
                            "laboratory/create",
                            "laboratory/update",
                            "laboratory/delete",

                            // Exámenes de laboratorio
                            "lab-exam/read",
                            "lab-exam/create",
                            "lab-exam/update",
                            "lab-exam/delete",



                            // Medicamentos
                            "medicine/read",
                            "medicine/create",
                            "medicine/update",
                            "medicine/delete",


                            // Roles
                            "role/read",
                            "role/create",
                            "role/update",
                            "role/delete",


                            // Inventario
                            "medicine-inventory/read",
                            "medicine-inventory/create",
                            "medicine-inventory/update",
                            "medicine-inventory/delete",


                            // Sucursal - Especialidad
                            "branch-specialty/create",
                            "branch-specialty/delete"
                    )
            );

    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final UsuarioActualService usuarioActualService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PermisoCatalogoService(
            UsuarioActualService usuarioActualService
    ) {
        this.usuarioActualService = usuarioActualService;
    }

    // =====================================================
    // VERIFICAR PERMISO ESPECÍFICO
    // =====================================================

    @Transactional(readOnly = true)
    public void exigirPermiso(String permiso) {

        // Verificar que la operación esté definida
        if (permiso == null || permiso.isBlank()) {
            throw new AccessDeniedException(
                    "La operación solicitada no está autorizada."
            );
        }

        // Obtener usuario autenticado y activo
        Usuario usuario = usuarioActualService
                .obtenerUsuarioActual();

        if (!Boolean.TRUE.equals(usuario.getActivo())
                || usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || !Boolean.TRUE.equals(
                usuario.getRol().getActivo()
        )) {

            throw new AccessDeniedException(
                    "No tiene permisos para realizar esta operación."
            );
        }

        // Identificar el rol actual
        String nombreRol = usuario.getRol()
                .getNombre()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Consultar los permisos asignados al rol
        Set<String> permisos = PERMISOS_POR_ROL
                .getOrDefault(
                        nombreRol,
                        Set.of()
                );

        // Denegar operaciones no autorizadas
        if (!permisos.contains(permiso)) {

            throw new AccessDeniedException(
                    "No tiene permisos para realizar esta operación."
            );
        }
    }
}
