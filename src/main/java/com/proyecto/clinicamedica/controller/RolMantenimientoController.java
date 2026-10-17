
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.RolFormularioDTO;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.service.RolMantenimientoService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * =========================================================
 * CU-14 - CONTROLLER: MANTENIMIENTO DE ROLES
 * =========================================================
 *
 * Catálogo 7 de 9.
 *
 * Funcionalidades:
 *
 * - Listar roles.
 * - Buscar por nombre.
 * - Filtrar por estado.
 * - Paginación.
 * - Crear nuevos roles.
 * - Editar roles existentes.
 * - Desactivar roles lógicamente.
 *
 * SEGURIDAD:
 *
 * - Acceso exclusivo del Administrador.
 * - Spring Security protege /admin/**.
 * - El Service verifica permisos granulares.
 * - Los roles esenciales están protegidos.
 * - No se permite eliminación física.
 * - Los usuarios existentes no se modifican.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/roles")
public class RolMantenimientoController {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int TAMANO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/roles/listado";

    private static final String VISTA_FORMULARIO =
            "admin/roles/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/roles";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final RolMantenimientoService
            rolMantenimientoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RolMantenimientoController(
            RolMantenimientoService rolMantenimientoService
    ) {

        this.rolMantenimientoService =
                rolMantenimientoService;
    }

    // =====================================================
    // LISTAR ROLES
    // =====================================================

    @GetMapping
    public String listar(
            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String busqueda,

            @RequestParam(
                    required = false
            )
            Boolean activo,

            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            int pagina,

            Model model
    ) {

        // =================================================
        // VALIDAR PÁGINA
        // =================================================

        int paginaSegura =
                Math.max(0, pagina);

        // =================================================
        // PAGINACIÓN Y ORDEN
        // =================================================

        PageRequest pageable =
                PageRequest.of(
                        paginaSegura,
                        TAMANO_PAGINA,
                        Sort.by(
                                Sort.Direction.ASC,
                                "nombre"
                        )
                );

        // =================================================
        // CONSULTAR ROLES
        // =================================================

        Page<Rol> roles =
                rolMantenimientoService.listar(
                        busqueda,
                        activo,
                        pageable
                );

        // =================================================
        // DATOS PARA LA VISTA
        // =================================================

        model.addAttribute(
                "roles",
                roles
        );

        model.addAttribute(
                "busqueda",
                busqueda
        );

        model.addAttribute(
                "activo",
                activo
        );

        model.addAttribute(
                "paginaActual",
                paginaSegura
        );

        model.addAttribute(
                "totalPaginas",
                roles.getTotalPages()
        );

        model.addAttribute(
                "totalElementos",
                roles.getTotalElements()
        );

        return VISTA_LISTADO;
    }

    // =====================================================
    // FORMULARIO DE CREACIÓN
    // =====================================================

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        // Verificar autorización de consulta.
        rolMantenimientoService.listar(
                "",
                null,
                PageRequest.of(0, 1)
        );

        model.addAttribute(
                "formulario",
                new RolFormularioDTO()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CREAR ROL
    // =====================================================

    @PostMapping("/nuevo")
    public String crear(
            @Valid
            @ModelAttribute("formulario")
            RolFormularioDTO formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDACIONES DEL DTO
        // =================================================

        if (bindingResult.hasErrors()) {

            prepararFormulario(
                    model,
                    false,
                    null
            );

            return VISTA_FORMULARIO;
        }

        // =================================================
        // GUARDAR DESDE EL SERVICE
        // =================================================

        try {

            Rol creado =
                    rolMantenimientoService.crear(
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + creado.getNombre()
                            + " ha sido creado exitosamente."
            );

            return REDIRECCION_LISTADO;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            prepararFormulario(
                    model,
                    false,
                    null
            );

            return VISTA_FORMULARIO;
        }
    }

    // =====================================================
    // FORMULARIO DE EDICIÓN
    // =====================================================

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(
            @PathVariable("id")
            Integer id,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            Rol rol =
                    rolMantenimientoService.buscarPorId(
                            id
                    );

            if (!Boolean.TRUE.equals(
                    rol.getActivo()
            )) {

                redirectAttributes.addFlashAttribute(
                        "mensajeError",
                        "No se puede editar un rol inactivo."
                );

                return REDIRECCION_LISTADO;
            }

            // =================================================
            // PRECARGAR DATOS
            // =================================================

            RolFormularioDTO formulario =
                    new RolFormularioDTO();

            formulario.setNombre(
                    rol.getNombre()
            );

            formulario.setDescripcion(
                    rol.getDescripcion()
            );

            model.addAttribute(
                    "formulario",
                    formulario
            );

            prepararFormulario(
                    model,
                    true,
                    rol.getId()
            );

            return VISTA_FORMULARIO;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            return REDIRECCION_LISTADO;
        }
    }

    // =====================================================
    // ACTUALIZAR ROL
    // =====================================================

    @PostMapping("/{id}/editar")
    public String actualizar(
            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            RolFormularioDTO formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDACIONES DEL DTO
        // =================================================

        if (bindingResult.hasErrors()) {

            prepararFormulario(
                    model,
                    true,
                    id
            );

            return VISTA_FORMULARIO;
        }

        // =================================================
        // ACTUALIZAR DESDE EL SERVICE
        // =================================================

        try {

            Rol actualizado =
                    rolMantenimientoService.actualizar(
                            id,
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + actualizado.getNombre()
                            + " ha sido actualizado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            prepararFormulario(
                    model,
                    true,
                    id
            );

            return VISTA_FORMULARIO;
        }
    }

    // =====================================================
    // DESACTIVACIÓN LÓGICA
    // =====================================================

    @PostMapping("/{id}/desactivar")
    public String desactivar(
            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =================================================
            // PROTECCIONES EN EL SERVICE
            // =================================================

            rolMantenimientoService.desactivar(
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro ha sido eliminado correctamente."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        return REDIRECCION_LISTADO;
    }

    // =====================================================
    // PREPARAR DATOS DEL FORMULARIO
    // =====================================================

    private void prepararFormulario(
            Model model,
            boolean modoEdicion,
            Integer idRol
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idRol",
                idRol
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Rol"
                        : "Crear Rol"
        );
    }
}
