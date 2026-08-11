package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;
import com.proyecto.clinicamedica.service.UsuarioMantenimientoService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;


/**
 * =========================================================
 * CONTROLLER: CREACIÓN DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA01 Crear Usuario.
 *
 * Responsabilidad:
 *
 * - Mostrar formulario.
 * - Cargar catálogos.
 * - Recibir creación.
 * - Mostrar errores.
 * - Redirigir al listado.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioCreacionController {

    private static final Set<String> ROLES_INTERNOS =
            Set.of(
                    "Médico",
                    "Enfermero",
                    "Recepcionista",
                    "Cajero",
                    "Laboratorista",
                    "Farmacéutico",
                    "Administrador"
            );


    private final UsuarioMantenimientoService
            usuarioMantenimientoService;

    private final RolService
            rolService;

    private final SucursalService
            sucursalService;

    private final EspecialidadService
            especialidadService;


    public UsuarioCreacionController(
            UsuarioMantenimientoService usuarioMantenimientoService,
            RolService rolService,
            SucursalService sucursalService,
            EspecialidadService especialidadService
    ) {

        this.usuarioMantenimientoService =
                usuarioMantenimientoService;

        this.rolService =
                rolService;

        this.sucursalService =
                sucursalService;

        this.especialidadService =
                especialidadService;
    }


    // =====================================================
    // FA01 - MOSTRAR FORMULARIO
    // =====================================================

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(
            Model model
    ) {

        UsuarioFormularioDTO formulario =
                new UsuarioFormularioDTO();


        // Estado predeterminado.
        formulario.setActivo(
                true
        );


        model.addAttribute(
                "usuarioFormulario",
                formulario
        );


        model.addAttribute(
                "modo",
                "crear"
        );


        cargarCatalogos(
                model
        );


        return "admin/usuarios/formulario";
    }


    // =====================================================
    // FA01 - CREAR USUARIO
    // =====================================================

    @PostMapping("/nuevo")
    public String crearUsuario(
            @ModelAttribute("usuarioFormulario")
            UsuarioFormularioDTO formulario,

            Authentication authentication,

            HttpServletRequest request,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        String ejecutor =
                obtenerUsuarioAutenticado(
                        authentication
                );


        String direccionIp =
                obtenerDireccionIp(
                        request
                );


        ResultadoValidacionUsuario resultado =
                usuarioMantenimientoService
                        .crearUsuario(
                                formulario,
                                ejecutor,
                                direccionIp
                        );


        // =================================================
        // ERRORES
        // =================================================

        if (resultado.tieneErrores()) {

            /*
             * No devolvemos la contraseña nuevamente
             * al navegador.
             */
            formulario.setContrasena(
                    null
            );


            model.addAttribute(
                    "resultadoValidacion",
                    resultado
            );


            model.addAttribute(
                    "modo",
                    "crear"
            );


            cargarCatalogos(
                    model
            );


            return "admin/usuarios/formulario";
        }


        // =================================================
        // ÉXITO
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Usuario creado correctamente."
        );


        return "redirect:/admin/usuarios";
    }


    // =====================================================
    // CATÁLOGOS
    // =====================================================

    private void cargarCatalogos(
            Model model
    ) {

        model.addAttribute(
                "roles",
                obtenerRolesInternos()
        );


        model.addAttribute(
                "sucursales",
                sucursalService.listarActivas()
        );


        model.addAttribute(
                "especialidades",
                especialidadService.listarActivas()
        );
    }


    private List<Rol> obtenerRolesInternos() {

        return rolService
                .listarActivos()
                .stream()
                .filter(
                        rol ->
                                rol != null
                                        && rol.getNombre() != null
                                        && ROLES_INTERNOS
                                        .stream()
                                        .anyMatch(
                                                permitido ->
                                                        permitido.equalsIgnoreCase(
                                                                rol.getNombre()
                                                        )
                                        )
                )
                .toList();
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


        return authentication
                .getName();
    }


    // =====================================================
    // IP
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