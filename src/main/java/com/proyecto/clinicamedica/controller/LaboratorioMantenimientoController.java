
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.Laboratorio;

import com.proyecto.clinicamedica.service.LaboratorioMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.LaboratorioMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.dao.DataIntegrityViolationException;
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

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * =========================================================
 * CU-14 - CONTROLLER: MANTENIMIENTO DE LABORATORIOS
 * =========================================================
 *
 * Catálogo 4 - Laboratorios.
 *
 * Funcionalidades:
 *
 * - Listar laboratorios.
 * - Buscar por nombre y descripción.
 * - Filtrar activos e inactivos.
 * - Paginar resultados.
 * - Crear laboratorios.
 * - Editar laboratorios.
 * - Desactivar mediante eliminación lógica.
 * - Mostrar errores de validación.
 * - Mostrar mensajes de confirmación.
 *
 * SEGURIDAD:
 *
 * - laboratory/read
 * - laboratory/create
 * - laboratory/update
 * - laboratory/delete
 *
 * REGLAS:
 *
 * - No se eliminan laboratorios físicamente.
 * - No se modifican órdenes de laboratorio.
 * - No se modifican exámenes relacionados.
 * - La desactivación verifica dependencias.
 * - La auditoría se registra desde el Service.
 *
 * IMPORTANTE:
 *
 * Este Controller es exclusivo del CU-14.
 *
 * No sustituye LaboratorioController del CU-09.
 * No altera los módulos CU-08 ni CU-09.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/laboratorios")
public class LaboratorioMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/laboratorios/listado";

    private static final String VISTA_FORMULARIO =
            "admin/laboratorios/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/laboratorios";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final LaboratorioMantenimientoConsultaService
            consultaService;

    private final LaboratorioMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LaboratorioMantenimientoController(
            LaboratorioMantenimientoConsultaService consultaService,
            LaboratorioMantenimientoService mantenimientoService,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.consultaService = consultaService;

        this.mantenimientoService = mantenimientoService;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR Y BUSCAR LABORATORIOS
    // =====================================================
    //
    // GET /admin/laboratorios
    //
    // Permite:
    //
    // - Búsqueda por texto.
    // - Filtro por estado.
    // - Paginación.
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
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/read"
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
        // CONSULTAR LABORATORIOS
        // =================================================

        Page<Laboratorio> pagina =
                consultaService.listar(
                        texto,
                        activo,
                        page,
                        TAMANIO_PAGINA
                );

        // =================================================
        // ATRIBUTOS PARA THYMELEAF
        // =================================================

        model.addAttribute(
                "laboratorios",
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
    // GET /admin/laboratorios/create
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        permisoCatalogoService.exigirPermiso(
                "laboratory/create"
        );

        model.addAttribute(
                "formulario",
                new FormularioLaboratorio()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - CREAR LABORATORIO
    // =====================================================
    //
    // POST /admin/laboratorios/create
    //
    // =====================================================

    @PostMapping("/create")
    public String crearLaboratorio(

            @Valid
            @ModelAttribute("formulario")
            FormularioLaboratorio formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/create"
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

            Laboratorio guardado =
                    mantenimientoService.crear(
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El laboratorio "
                            + guardado.getNombre()
                            + " ha sido creado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar el laboratorio. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos sean válidos."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - MOSTRAR FORMULARIO DE EDICIÓN
    // =====================================================
    //
    // GET /admin/laboratorios/edit/{id}
    //
    // =====================================================

    @GetMapping("/edit/{id}")
    public String mostrarFormularioEdicion(

            @PathVariable("id")
            Integer id,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/update"
        );

        try {

            // =============================================
            // BUSCAR LABORATORIO
            // =============================================

            Laboratorio laboratorio =
                    consultaService.buscarPorId(id);

            // =============================================
            // PREPARAR DATOS
            // =============================================

            FormularioLaboratorio formulario =
                    new FormularioLaboratorio();

            formulario.setNombre(
                    laboratorio.getNombre()
            );

            formulario.setDescripcion(
                    laboratorio.getDescripcion()
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
    // CU-14 - ACTUALIZAR LABORATORIO
    // =====================================================
    //
    // POST /admin/laboratorios/edit/{id}
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarLaboratorio(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioLaboratorio formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/update"
        );

        // =================================================
        // VALIDACIONES
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

            Laboratorio actualizado =
                    mantenimientoService.actualizar(
                            id,
                            formulario.getNombre(),
                            formulario.getDescripcion()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El laboratorio "
                            + actualizado.getNombre()
                            + " ha sido actualizado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar el laboratorio. "
                            + "Compruebe que el nombre no esté "
                            + "duplicado y que los datos sean válidos."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        prepararFormulario(
                model,
                true,
                id
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - DESACTIVAR LABORATORIO
    // =====================================================
    //
    // POST /admin/laboratorios/delete/{id}
    //
    // Eliminación lógica.
    //
    // El Service protege laboratorios con exámenes
    // asociados, tanto activos como históricos.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarLaboratorio(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/delete"
        );

        try {

            // =============================================
            // CONSULTAR NOMBRE
            // =============================================

            Laboratorio laboratorio =
                    consultaService.buscarPorId(id);

            String nombre =
                    laboratorio.getNombre();

            // =============================================
            // DESACTIVAR MEDIANTE SERVICE
            // =============================================

            mantenimientoService.desactivar(id);

            // =============================================
            // MENSAJE DE CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El laboratorio "
                            + nombre
                            + " ha sido desactivado correctamente."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se puede desactivar el laboratorio "
                            + "debido a restricciones de integridad."
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
            Integer idLaboratorio
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idLaboratorio",
                idLaboratorio
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Laboratorio"
                        : "Crear Laboratorio"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/laboratorios/edit/"
                        + idLaboratorio
                        : "/admin/laboratorios/create"
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
                bindingResult.getFieldErrors()
                        .stream()
                        .map(
                                error ->
                                        error.getDefaultMessage()
                        )
                        .distinct()
                        .toList();

        model.addAttribute(
                "errores",
                errores
        );
    }

    // =====================================================
    // FORMULARIO INTERNO
    // =====================================================
    //
    // Los límites coinciden con PostgreSQL:
    //
    // laboratorio.nombre       VARCHAR(255)
    // laboratorio.descripcion  VARCHAR(255)
    //
    // El estado activo y el ID no se modifican
    // desde el formulario.
    //
    // =====================================================

    public static class FormularioLaboratorio {

        // ================================================
        // NOMBRE
        // ================================================

        @NotBlank(
                message = "El nombre del laboratorio es obligatorio."
        )
        @Size(
                max = 255,
                message = "El nombre no puede superar los 255 caracteres."
        )
        private String nombre;

        // ================================================
        // DESCRIPCIÓN
        // ================================================

        @Size(
                max = 255,
                message = "La descripción no puede superar los 255 caracteres."
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
