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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
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
 * - Mostrar listado.
 * - Recibir filtros.
 * - Mostrar formulario de creación.
 * - Procesar creación.
 * - Preparar datos para Thymeleaf.
 *
 * NO realiza:
 *
 * - Consultas directas a PostgreSQL.
 * - Cifrado.
 * - Hash.
 * - BCrypt.
 * - Validaciones de negocio.
 * - Auditoría.
 *
 * Estas responsabilidades pertenecen al Service.
 *
 * Seguridad:
 *
 * /admin/**
 *
 * requiere ROLE_ADMINISTRADOR mediante SecurityConfig.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioMantenimientoController {

    // =====================================================
    // ROLES INTERNOS DISPONIBLES EN CU-01
    // =====================================================

    private static final Set<String>
            ROLES_INTERNOS =
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

            // =============================================
            // RN-CU01-01
            // =============================================

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
        // CATÁLOGO DE ROLES
        // =================================================

        model.addAttribute(
                "roles",
                obtenerRolesInternos()
        );


        // =================================================
        // CATÁLOGO DE SUCURSALES
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
        // INFORMACIÓN DE PAGINACIÓN
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
    // CARGAR CATÁLOGOS DEL FORMULARIO
    // =====================================================

    private void cargarCatalogosFormulario(
            Model model
    ) {

        // =================================================
        // ROLES INTERNOS
        // =================================================

        model.addAttribute(
                "roles",
                obtenerRolesInternos()
        );


        // =================================================
        // SUCURSALES ACTIVAS
        // =================================================

        model.addAttribute(
                "sucursales",
                sucursalService.listarActivas()
        );


        // =================================================
        // ESPECIALIDADES ACTIVAS
        // =================================================

        model.addAttribute(
                "especialidades",
                especialidadService.listarActivas()
        );
    }


    // =====================================================
    // OBTENER ROLES INTERNOS
    // =====================================================

    /**
     * El catálogo Rol puede contener Paciente.
     *
     * CU-01 administra usuarios internos, por lo que
     * solamente enviamos a la vista los roles permitidos.
     *
     * La capa Service vuelve a validar esto para evitar
     * manipulación manual de la petición.
     */
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
                                                        permitido
                                                                .equalsIgnoreCase(
                                                                        rol.getNombre()
                                                                )
                                        )
                )
                .toList();
    }


    // =====================================================
    // OBTENER IP
    // =====================================================

    private String obtenerDireccionIp(
            HttpServletRequest request
    ) {

        if (request == null) {

            return null;
        }


        /*
         * En desarrollo local utilizamos directamente
         * getRemoteAddr().
         *
         * No confiamos todavía en X-Forwarded-For enviado
         * por el navegador.
         *
         * Cuando publiquemos detrás de Azure configuraremos
         * correctamente los encabezados del proxy.
         */
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
    // TAMAÑO SEGURO
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
}