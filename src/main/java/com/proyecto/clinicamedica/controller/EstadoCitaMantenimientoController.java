
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.EstadoCita;

import com.proyecto.clinicamedica.service.EstadoCitaMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.EstadoCitaMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;

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

import java.util.List;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE ESTADOS DE CITA
 * =========================================================
 *
 * Funcionalidades:
 *
 * - Listar estados de cita.
 * - Buscar por nombre o descripción.
 * - Filtrar por estado activo e inactivo.
 * - Paginar resultados.
 * - Mostrar formulario de creación.
 * - Crear nuevos estados.
 * - Mostrar formulario de edición.
 * - Actualizar estados existentes.
 * - Desactivar mediante eliminación lógica.
 * - Mostrar mensajes de éxito y errores.
 *
 * SEGURIDAD:
 *
 * - appointment-status/read
 * - appointment-status/create
 * - appointment-status/update
 * - appointment-status/delete
 *
 * Las operaciones de modificación se delegan
 * al EstadoCitaMantenimientoService.
 *
 * El Service protege:
 *
 * - Estados críticos del sistema.
 * - Citas activas e históricas.
 * - Integridad de los registros.
 * - Auditoría.
 * - Caché de estados de cita.
 *
 * IMPORTANTE:
 *
 * - GET no modifica registros.
 * - Las operaciones de escritura utilizan POST.
 * - Los formularios deben incluir CSRF.
 * - No se eliminan físicamente los registros.
 * - No se modifica el orden del flujo de citas.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/estados-cita")
public class EstadoCitaMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/estados-cita/listado";

    private static final String VISTA_FORMULARIO =
            "admin/estados-cita/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/estados-cita";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EstadoCitaMantenimientoConsultaService
            consultaService;

    private final EstadoCitaMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EstadoCitaMantenimientoController(
            EstadoCitaMantenimientoConsultaService consultaService,
            EstadoCitaMantenimientoService mantenimientoService,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.consultaService = consultaService;

        this.mantenimientoService = mantenimientoService;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR ESTADOS DE CITA
    // =====================================================
    //
    // GET /admin/estados-cita
    //
    // Filtros:
    //
    // texto  -> Nombre o descripción.
    // estado -> Todos, activos, inactivos.
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
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/read"
        );

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
        // CONSULTAR ESTADOS
        // =================================================

        Page<EstadoCita> pagina =
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
                "estadosCita",
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
    // CU-14 - FORMULARIO DE CREACIÓN
    // =====================================================
    //
    // GET /admin/estados-cita/create
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        permisoCatalogoService.exigirPermiso(
                "appointment-status/create"
        );

        model.addAttribute(
                "formulario",
                new FormularioEstadoCita()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - CREAR ESTADO DE CITA
    // =====================================================
    //
    // POST /admin/estados-cita/create
    //
    // =====================================================

    @PostMapping("/create")
    public String crearEstadoCita(

            @Valid
            @ModelAttribute("formulario")
            FormularioEstadoCita formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/create"
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
        // CREAR MEDIANTE SERVICE
        // =================================================

        try {

            EstadoCita guardado =
                    mantenimientoService.crear(
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + guardado.getNombre()
                            + " ha sido creado exitosamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            // =============================================
            // PROTECCIÓN DE INTEGRIDAD EN POSTGRESQL
            // =============================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar el estado de cita. "
                            + "Verifique que no exista otro "
                            + "registro activo con el mismo "
                            + "nombre y que los datos sean válidos."
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

            // =============================================
            // ERRORES DE VALIDACIÓN DEL SERVICE
            // =============================================

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
    // CU-14 - FORMULARIO DE EDICIÓN
    // =====================================================
    //
    // GET /admin/estados-cita/edit/{id}
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
                "appointment-status/update"
        );

        try {

            // =============================================
            // CONSULTAR ESTADO EXISTENTE
            // =============================================

            EstadoCita estadoCita =
                    consultaService.buscarPorId(id);

            // =============================================
            // PRECARGAR FORMULARIO
            // =============================================

            FormularioEstadoCita formulario =
                    new FormularioEstadoCita();

            formulario.setNombre(
                    estadoCita.getNombre()
            );

            formulario.setDescripcion(
                    estadoCita.getDescripcion()
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
    // CU-14 - ACTUALIZAR ESTADO DE CITA
    // =====================================================
    //
    // POST /admin/estados-cita/edit/{id}
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarEstadoCita(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioEstadoCita formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "appointment-status/update"
        );

        // =================================================
        // VALIDAR FORMULARIO
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

            EstadoCita actualizado =
                    mantenimientoService.actualizar(
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

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar el estado de cita. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos "
                            + "cumplan las restricciones."
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
    // CU-14 - DESACTIVAR ESTADO DE CITA
    // =====================================================
    //
    // POST /admin/estados-cita/delete/{id}
    //
    // - Validación de permisos.
    // - Protección de estados críticos.
    // - Protección de citas relacionadas.
    // - Desactivación lógica.
    // - Auditoría desde el Service.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarEstadoCita(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        permisoCatalogoService.exigirPermiso(
                "appointment-status/delete"
        );

        try {

            // =============================================
            // CONSULTAR NOMBRE DEL REGISTRO
            // =============================================

            EstadoCita estado =
                    consultaService.buscarPorId(id);

            String nombre =
                    estado.getNombre();

            // =============================================
            // DESACTIVAR MEDIANTE SERVICE
            // =============================================

            mantenimientoService.desactivar(id);

            // =============================================
            // MOSTRAR CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + nombre
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

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo desactivar el estado "
                            + "porque existen restricciones "
                            + "de integridad en PostgreSQL."
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
            Integer idEstadoCita
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idEstadoCita",
                idEstadoCita
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Estado de Cita"
                        : "Crear Estado de Cita"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/estados-cita/edit/"
                        + idEstadoCita
                        : "/admin/estados-cita/create"
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
    // DTO INTERNO - FORMULARIO ESTADO DE CITA
    // =====================================================
    //
    // Validaciones del CU-14.
    //
    // El Service también valida:
    //
    // - Nombres reservados.
    // - Estados críticos.
    // - Duplicados.
    // - Citas relacionadas.
    //
    // El estado y el orden del flujo no son
    // modificables desde este formulario.
    //
    // =====================================================

    public static class FormularioEstadoCita {

        // ================================================
        // NOMBRE
        // ================================================

        @NotBlank(
                message = "El nombre es obligatorio."
        )
        @Size(
                max = 50,
                message = "El nombre no puede exceder los 50 caracteres."
        )
        private String nombre;

        // ================================================
        // DESCRIPCIÓN
        // ================================================

        @Size(
                max = 200,
                message = "La descripción no puede exceder los 200 caracteres."
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
