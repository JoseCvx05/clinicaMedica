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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;


/**
 * =========================================================
 * CONTROLLER: EDICIÓN DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA04 Editar Usuario.
 *
 * Responsabilidad:
 *
 * - Mostrar formulario de edición.
 * - Cargar datos actuales.
 * - Cargar catálogos.
 * - Procesar actualización.
 * - Mostrar errores.
 * - Redirigir al listado.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioEdicionController {


    // =====================================================
    // ROLES INTERNOS
    // =====================================================

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


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioMantenimientoService
            usuarioMantenimientoService;

    private final RolService
            rolService;

    private final SucursalService
            sucursalService;

    private final EspecialidadService
            especialidadService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioEdicionController(
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
    // FA04 - MOSTRAR FORMULARIO DE EDICIÓN
    // =====================================================

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            UsuarioFormularioDTO formulario =
                    usuarioMantenimientoService
                            .obtenerUsuarioParaEditar(
                                    id
                            );


            model.addAttribute(
                    "usuarioFormulario",
                    formulario
            );


            model.addAttribute(
                    "modo",
                    "editar"
            );


            cargarCatalogos(
                    model,
                    formulario.getIdRol()
            );

            return "admin/usuarios/formulario";


        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/admin/usuarios";
        }
    }


    // =====================================================
    // FA04 - PROCESAR ACTUALIZACIÓN
    // =====================================================

    @PostMapping("/{id}/editar")
    public String actualizarUsuario(
            @PathVariable Integer id,

            @ModelAttribute("usuarioFormulario")
            UsuarioFormularioDTO formulario,

            Authentication authentication,

            HttpServletRequest request,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        /*
         * El ID utilizado siempre será el de la URL.
         *
         * Así evitamos que alguien manipule manualmente
         * el campo hidden del formulario.
         */
        formulario.setId(
                id
        );


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
        // ACTUALIZAR
        // =================================================

        ResultadoValidacionUsuario resultado =
                usuarioMantenimientoService
                        .actualizarUsuario(
                                formulario,
                                ejecutor,
                                direccionIp
                        );


        // =================================================
        // FA06 - ERRORES
        // =================================================

        if (resultado.tieneErrores()) {

            /*
             * La contraseña nunca debe regresar
             * nuevamente al navegador.
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
                    "editar"
            );


            cargarCatalogos(
                    model,
                    formulario.getIdRol()
            );


            return "admin/usuarios/formulario";
        }


        // =================================================
        // ACTUALIZACIÓN EXITOSA
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Usuario actualizado correctamente."
        );


        return "redirect:/admin/usuarios";
    }


    // =====================================================
    // CARGAR CATÁLOGOS
    // =====================================================

    private void cargarCatalogos(
            Model model,
            Integer idRolActual
    ) {

        model.addAttribute(
                "roles",
                obtenerRolesEdicion(
                        idRolActual
                )
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


    // =====================================================
    // OBTENER ROLES INTERNOS
    // =====================================================

    private List<Rol> obtenerRolesEdicion(
            Integer idRolActual
    ) {

        return rolService
                .listarActivos()
                .stream()
                .filter(
                        rol -> {

                            if (rol == null
                                    || rol.getNombre() == null) {

                                return false;
                            }


                            boolean esRolInterno =
                                    ROLES_INTERNOS
                                            .stream()
                                            .anyMatch(
                                                    permitido ->
                                                            permitido.equalsIgnoreCase(
                                                                    rol.getNombre()
                                                            )
                                            );


                            boolean esRolActual =
                                    idRolActual != null
                                            && idRolActual.equals(
                                            rol.getId()
                                    );


                            /*
                             * Los roles internos siempre aparecen.
                             *
                             * Si el usuario ya posee otro rol,
                             * como Paciente, también mostramos
                             * únicamente su rol actual.
                             */
                            return esRolInterno
                                    || esRolActual;
                        }
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