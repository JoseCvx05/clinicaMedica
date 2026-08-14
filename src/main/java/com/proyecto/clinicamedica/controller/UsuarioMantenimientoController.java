package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.TipoFiltroUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;

import com.proyecto.clinicamedica.entity.Rol;

import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;
import com.proyecto.clinicamedica.service.UsuarioMantenimientoService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
 * CONTROLADOR MVC: MANTENIMIENTO DE USUARIOS
 * =========================================================
 *
 * CU-01 - Mantenimiento de Usuarios.
 *
 * Responsabilidades:
 *
 * - Listar usuarios.
 * - Buscar y filtrar usuarios.
 * - Mostrar formulario de creación.
 * - Crear usuarios.
 * - Mostrar formulario de edición.
 * - Actualizar usuarios.
 * - Preparar catálogos para Thymeleaf.
 *
 * La lógica de negocio permanece en:
 *
 * UsuarioMantenimientoService.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioMantenimientoController {


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

    public UsuarioMantenimientoController(
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
    // LISTAR USUARIOS
    // =====================================================

    @GetMapping
    public String listarUsuarios(
            @ModelAttribute("busqueda")
            UsuarioBusquedaDTO busqueda,

            Model model
    ) {

        Page<UsuarioListadoDTO> paginaUsuarios;


        try {

            paginaUsuarios =
                    usuarioMantenimientoService
                            .listarUsuarios(
                                    busqueda
                            );

        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "errorBusqueda",
                    ex.getMessage()
            );


            paginaUsuarios =
                    Page.empty(
                            PageRequest.of(
                                    0,
                                    obtenerTamanioSeguro(
                                            busqueda.getTamanio()
                                    )
                            )
                    );
        }


        // =================================================
        // USUARIOS
        // =================================================

        model.addAttribute(
                "paginaUsuarios",
                paginaUsuarios
        );


        model.addAttribute(
                "usuarios",
                paginaUsuarios.getContent()
        );


        // =================================================
        // TIPOS DE FILTRO
        // =================================================

        model.addAttribute(
                "tiposFiltro",
                TipoFiltroUsuario.values()
        );


        // =================================================
        // ROLES
        // =================================================

        model.addAttribute(
                "roles",
                obtenerRolesFormulario(
                        null
                )
        );


        // =================================================
        // SUCURSALES
        // =================================================

        model.addAttribute(
                "sucursales",
                sucursalService.listarActivas()
        );


        // =================================================
        // TAMAÑOS DE PÁGINA
        // =================================================

        model.addAttribute(
                "tamaniosPagina",
                List.of(
                        10,
                        20,
                        25,
                        50
                )
        );


        // =================================================
        // PAGINACIÓN
        // =================================================

        model.addAttribute(
                "paginaActual",
                paginaUsuarios.getNumber()
        );


        model.addAttribute(
                "totalPaginas",
                paginaUsuarios.getTotalPages()
        );


        model.addAttribute(
                "totalRegistros",
                paginaUsuarios.getTotalElements()
        );


        model.addAttribute(
                "tamanioActual",
                paginaUsuarios.getSize()
        );


        return "admin/usuarios/listado";
    }


    // =====================================================
    // FA01 - MOSTRAR FORMULARIO DE CREACIÓN
    // =====================================================

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(
            Model model
    ) {

        UsuarioFormularioDTO formulario =
                new UsuarioFormularioDTO();


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


        cargarCatalogosFormulario(
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
             * Nunca devolvemos nuevamente la contraseña
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


            cargarCatalogosFormulario(
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


            cargarCatalogosFormulario(
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
    // FA04 - ACTUALIZAR USUARIO
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
         * Siempre utilizamos el ID recibido en la URL.
         *
         * No confiamos en un ID enviado desde el formulario.
         */
        formulario.setId(
                id
        );


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
                        .actualizarUsuario(
                                formulario,
                                ejecutor,
                                direccionIp
                        );


        // =================================================
        // ERRORES
        // =================================================

        if (resultado.tieneErrores()) {

            /*
             * Nunca devolvemos la contraseña nuevamente
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
                    "editar"
            );


            cargarCatalogosFormulario(
                    model,
                    formulario.getIdRol()
            );


            return "admin/usuarios/formulario";
        }


        // =================================================
        // ÉXITO
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Usuario actualizado correctamente."
        );


        return "redirect:/admin/usuarios";
    }


    // =====================================================
    // CARGAR CATÁLOGOS - CREACIÓN
    // =====================================================

    private void cargarCatalogosFormulario(
            Model model
    ) {

        cargarCatalogosFormulario(
                model,
                null
        );
    }


    // =====================================================
    // CARGAR CATÁLOGOS - CREACIÓN / EDICIÓN
    // =====================================================

    private void cargarCatalogosFormulario(
            Model model,
            Integer idRolActual
    ) {

        model.addAttribute(
                "roles",
                obtenerRolesFormulario(
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
    // OBTENER ROLES PERMITIDOS
    // =====================================================

    private List<Rol> obtenerRolesFormulario(
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
                                                            permitido
                                                                    .equalsIgnoreCase(
                                                                            rol.getNombre()
                                                                    )
                                            );


                            boolean esRolActual =
                                    idRolActual != null
                                            && idRolActual.equals(
                                            rol.getId()
                                    );


                            /*
                             * CREAR:
                             * solamente roles internos.
                             *
                             * EDITAR:
                             * roles internos + rol actual.
                             */
                            return esRolInterno
                                    || esRolActual;
                        }
                )
                .toList();
    }


    // =====================================================
    // OBTENER USUARIO AUTENTICADO
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
    // OBTENER DIRECCIÓN IP
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


        /*
         * La columna direccion_ip permite máximo
         * 45 caracteres.
         */
        if (direccionIp.length() > 45) {

            return direccionIp.substring(
                    0,
                    45
            );
        }


        return direccionIp;
    }


    // =====================================================
    // TAMAÑO DE PÁGINA SEGURO
    // =====================================================

    private int obtenerTamanioSeguro(
            int tamanio
    ) {

        return switch (tamanio) {

            case 10, 20, 25, 50 ->
                    tamanio;

            default ->
                    20;
        };
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

        String nombreUsuarioEliminado;

        try {

            nombreUsuarioEliminado =
                    usuarioMantenimientoService
                            .obtenerUsuarioParaEditar(
                                    id
                            )
                            .getNombreUsuario();

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            return "redirect:/admin/usuarios";
        }


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
}