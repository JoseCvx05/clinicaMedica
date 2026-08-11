package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.service.UsuarioMantenimientoService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * =========================================================
 * CONTROLLER: ELIMINACIÓN DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA05.
 *
 * Responsabilidad:
 *
 * - Recibir la solicitud de eliminación.
 * - Identificar al administrador autenticado.
 * - Obtener la IP.
 * - Delegar la eliminación lógica al Service.
 * - Mostrar el resultado.
 *
 * La eliminación real consiste en:
 *
 * activo = false
 *
 * Nunca se elimina físicamente el registro.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioEliminacionController {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final UsuarioMantenimientoService
            usuarioMantenimientoService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioEliminacionController(
            UsuarioMantenimientoService usuarioMantenimientoService
    ) {

        this.usuarioMantenimientoService =
                usuarioMantenimientoService;
    }


    // =====================================================
    // FA05 - ELIMINAR USUARIO
    // =====================================================

    @PostMapping("/{id}/eliminar")
    public String eliminarUsuario(
            @PathVariable Integer id,

            Authentication authentication,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // USUARIO EJECUTOR
        // =================================================

        String ejecutor =
                obtenerUsuarioAutenticado(
                        authentication
                );


        // =================================================
        // DIRECCIÓN IP
        // =================================================

        String direccionIp =
                obtenerDireccionIp(
                        request
                );

        // =================================================
// OBTENER NOMBRE ANTES DE ELIMINAR
// =================================================

        String nombreUsuarioEliminado =
                usuarioMantenimientoService
                        .obtenerUsuarioParaEditar(
                                id
                        )
                        .getNombreUsuario();



        // =================================================
        // ELIMINACIÓN LÓGICA
        // =================================================

        ResultadoValidacionUsuario resultado =
                usuarioMantenimientoService
                        .eliminarUsuario(
                                id,
                                ejecutor,
                                direccionIp
                        );


        // =================================================
        // ERROR
        // =================================================

        if (resultado.tieneErrores()) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    resultado.obtenerMensaje(
                            "usuario"
                    )
            );


            return "redirect:/admin/usuarios";
        }


        // =================================================
        // ÉXITO
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "El usuario "
                        + nombreUsuarioEliminado
                        + " ha sido eliminado correctamente."
        );

        return "redirect:/admin/usuarios";
    }


    // =====================================================
    // USUARIO AUTENTICADO
    // =====================================================

    private String obtenerUsuarioAutenticado(
            Authentication authentication
    ) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario autenticado."
            );
        }


        return authentication.getName();
    }


    // =====================================================
    // DIRECCIÓN IP
    // =====================================================

    private String obtenerDireccionIp(
            HttpServletRequest request
    ) {

        if (request == null) {

            return null;
        }


        String direccionIp =
                request.getRemoteAddr();


        if (direccionIp == null
                || direccionIp.isBlank()) {

            return null;
        }


        if (direccionIp.length() > 45) {

            return direccionIp.substring(
                    0,
                    45
            );
        }


        return direccionIp;
    }
}