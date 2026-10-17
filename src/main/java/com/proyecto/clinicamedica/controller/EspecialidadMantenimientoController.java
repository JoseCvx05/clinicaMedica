
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.Especialidad;

import com.proyecto.clinicamedica.service.EspecialidadMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.EspecialidadMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * =========================================================
 * CU-14 - CONTROLADOR DE MANTENIMIENTO DE ESPECIALIDADES
 * =========================================================
 *
 * Funcionalidades:
 *
 * - Listar especialidades.
 * - Buscar por nombre o descripción.
 * - Filtrar por estado.
 * - Paginar resultados.
 * - Mostrar formulario de creación.
 * - Crear especialidades.
 * - Mostrar formulario de edición.
 * - Actualizar especialidades.
 * - Desactivar especialidades.
 * - Mostrar mensajes y errores.
 *
 * SEGURIDAD:
 *
 * - Cada operación utiliza permisos específicos.
 * - Las modificaciones se delegan al Service.
 * - La eliminación lógica utiliza POST.
 * - No se permite modificar datos mediante GET.
 *
 * Las vistas Thymeleaf deberán incluir el token CSRF
 * en todos los formularios POST.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/especialidades")
public class EspecialidadMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/especialidades/listado";

    private static final String VISTA_FORMULARIO =
            "admin/especialidades/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/especialidades";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EspecialidadMantenimientoConsultaService
            consultaService;

    private final EspecialidadMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EspecialidadMantenimientoController(
            EspecialidadMantenimientoConsultaService consultaService,
            EspecialidadMantenimientoService mantenimientoService,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.consultaService = consultaService;

        this.mantenimientoService = mantenimientoService;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTADO DE ESPECIALIDADES
    // =====================================================
    //
    // GET /admin/especialidades
    //
    // Filtros:
    //
    // texto  -> Nombre o descripción.
    // estado -> todos, activos, inactivos.
    // page   -> Página de resultados.
    //
    // =====================================================

    @GetMapping
    public String mostrarListado(

            @RequestParam(
                    name = "texto",
                    defaultValue = ""
            )
            String texto,

            @RequestParam(
                    name = "estado",
                    defaultValue = "todos"
            )
            String estado,

            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            Model model
    ) {

        // =================================================
        // INTERPRETAR FILTRO
        // =================================================

        Boolean activo = null;

        if ("activos".equalsIgnoreCase(estado)) {

            activo = true;

        } else if ("inactivos".equalsIgnoreCase(estado)) {

            activo = false;
        }

        // =================================================
        // CONSULTAR LISTADO
        // =================================================

        Page<Especialidad> pagina =
                consultaService.listar(
                        texto,
                        activo,
                        page,
                        TAMANIO_PAGINA
                );

        // =================================================
        // DATOS PARA THYMELEAF
        // =================================================

        model.addAttribute(
                "especialidades",
                pagina.getContent()
        );

        model.addAttribute(
                "paginaActual",
                pagina.getNumber()
        );

        model.addAttribute(
                "totalPaginas",
                pagina.getTotalPages()
        );

        model.addAttribute(
                "totalRegistros",
                pagina.getTotalElements()
        );

        model.addAttribute(
                "textoBusqueda",
                texto
        );

        model.addAttribute(
                "estadoFiltro",
                estado
        );

        return VISTA_LISTADO;
    }

    // =====================================================
    // CU-14 - MOSTRAR FORMULARIO DE CREACIÓN
    // =====================================================
    //
    // GET /admin/especialidades/create
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        permisoCatalogoService.exigirPermiso(
                "specialty/create"
        );

        model.addAttribute(
                "formulario",
                new FormularioEspecialidad()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }


    // =====================================================
    // CU-14 - CREAR ESPECIALIDAD
    // =====================================================

    @PostMapping("/create")
    public String crearEspecialidad(

            @Valid
            @ModelAttribute("formulario")
            FormularioEspecialidad formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "specialty/create"
        );

        // =================================================
        // VALIDACIONES DEL FORMULARIO
        // =================================================

        if (bindingResult.hasErrors()) {

            cargarErrores(
                    bindingResult,
                    model
            );

            prepararFormulario(
                    model,
                    false,
                    null
            );

            return VISTA_FORMULARIO;
        }

        // =================================================
        // GUARDAR MEDIANTE SERVICE
        // =================================================

        try {

            Especialidad guardada =
                    mantenimientoService.crear(
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + guardada.getNombre()
                            + " ha sido creado exitosamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            // =================================================
            // RESTRICCIÓN DE INTEGRIDAD EN POSTGRESQL
            // =================================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar el registro. "
                            + "Ya existe una especialidad activa "
                            + "con ese nombre o se incumplió "
                            + "una restricción de la base de datos."
            );

            prepararFormulario(
                    model,
                    false,
                    null
            );

            return VISTA_FORMULARIO;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // =================================================
            // ERRORES DE VALIDACIÓN
            // =================================================

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
    // CU-14 - MOSTRAR FORMULARIO DE EDICIÓN
    // =====================================================
    //
    // GET /admin/especialidades/edit/{id}
    //
    // =====================================================

    @GetMapping("/edit/{id}")
    public String mostrarFormularioEdicion(

            @PathVariable("id")
            Integer id,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "specialty/update"
        );

        try {

            Especialidad especialidad =
                    consultaService.buscarPorId(id);

            FormularioEspecialidad formulario =
                    new FormularioEspecialidad();

            formulario.setNombre(
                    especialidad.getNombre()
            );

            formulario.setDescripcion(
                    especialidad.getDescripcion()
            );

            model.addAttribute(
                    "formulario",
                    formulario
            );

            prepararFormulario(
                    model,
                    true,
                    id
            );

            return VISTA_FORMULARIO;

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            return REDIRECCION_LISTADO;
        }
    }


    // =====================================================
    // CU-14 - ACTUALIZAR ESPECIALIDAD
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarEspecialidad(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioEspecialidad formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "specialty/update"
        );

        // =================================================
        // VALIDACIONES DEL FORMULARIO
        // =================================================

        if (bindingResult.hasErrors()) {

            cargarErrores(
                    bindingResult,
                    model
            );

            prepararFormulario(
                    model,
                    true,
                    id
            );

            return VISTA_FORMULARIO;
        }

        // =================================================
        // ACTUALIZAR MEDIANTE SERVICE
        // =================================================

        try {

            Especialidad actualizada =
                    mantenimientoService.actualizar(
                            id,
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + actualizada.getNombre()
                            + " ha sido actualizado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            // =================================================
            // RESTRICCIÓN DE INTEGRIDAD EN POSTGRESQL
            // =================================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar el registro. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos sean válidos."
            );

            prepararFormulario(
                    model,
                    true,
                    id
            );

            return VISTA_FORMULARIO;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // =================================================
            // ERRORES DE VALIDACIÓN
            // =================================================

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
    // CU-14 - DESACTIVAR ESPECIALIDAD
    // =====================================================
    //
    // POST /admin/especialidades/delete/{id}
    //
    // La desactivación es lógica.
    //
    // No se elimina físicamente el registro.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarEspecialidad(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "specialty/delete"
        );

        try {

            // Consultar el nombre para mostrar el mensaje
            Especialidad especialidad =
                    consultaService.buscarPorId(id);

            // El Service verifica todas las dependencias.
            mantenimientoService.desactivar(id);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + especialidad.getNombre()
                            + " ha sido eliminado correctamente."
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
    // PREPARAR FORMULARIO
    // =====================================================

    private void prepararFormulario(
            Model model,
            boolean modoEdicion,
            Integer idEspecialidad
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idEspecialidad",
                idEspecialidad
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Especialidad"
                        : "Crear Especialidad"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/especialidades/edit/"
                        + idEspecialidad
                        : "/admin/especialidades/create"
        );
    }

    // =====================================================
    // CARGAR ERRORES DE VALIDACIÓN
    // =====================================================

    private void cargarErrores(
            BindingResult bindingResult,
            Model model
    ) {

        List<String> errores =
                bindingResult
                        .getFieldErrors()
                        .stream()
                        .map(
                                error -> error.getDefaultMessage()
                        )
                        .distinct()
                        .toList();

        model.addAttribute(
                "errores",
                errores
        );
    }

    // =====================================================
    // DTO INTERNO - FORMULARIO DE ESPECIALIDAD
    // =====================================================
    //
    // Validaciones iniciales del formulario.
    //
    // El Service también valida los datos.
    //
    // =====================================================

    public static class FormularioEspecialidad {

        // ================================================
        // NOMBRE
        // ================================================

        @NotBlank(
                message = "El nombre es obligatorio."
        )
        @Size(
                max = 200,
                message = "El nombre no puede exceder los 200 caracteres."
        )
        private String nombre;

        // ================================================
        // DESCRIPCIÓN
        // ================================================

        @NotBlank(
                message = "La descripción es obligatoria."
        )
        @Size(
                max = 500,
                message = "La descripción no puede exceder los 500 caracteres."
        )
        private String descripcion;

        // ================================================
        // GETTERS Y SETTERS
        // ================================================

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
    }
}
