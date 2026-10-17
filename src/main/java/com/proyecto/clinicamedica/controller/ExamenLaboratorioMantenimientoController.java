
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.ExamenLaboratorio;

import com.proyecto.clinicamedica.service.ExamenLaboratorioMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.ExamenLaboratorioMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * =========================================================
 * CU-14 - CONTROLLER:
 * MANTENIMIENTO DE EXÁMENES DE LABORATORIO
 * =========================================================
 *
 * Catálogo 5 de 9.
 *
 * FUNCIONALIDADES:
 *
 * - Listar exámenes de laboratorio.
 * - Buscar por nombre, descripción o laboratorio.
 * - Filtrar por laboratorio.
 * - Filtrar por estado activo/inactivo.
 * - Paginar resultados.
 * - Registrar nuevos exámenes.
 * - Editar exámenes existentes.
 * - Actualizar precios del catálogo.
 * - Desactivar mediante eliminación lógica.
 * - Mostrar errores de validación.
 * - Mostrar mensajes de confirmación.
 *
 * SEGURIDAD:
 *
 * - lab-exam/read
 * - lab-exam/create
 * - lab-exam/update
 * - lab-exam/delete
 *
 * REGLAS:
 *
 * - Nombre obligatorio y único entre activos.
 * - Descripción opcional.
 * - Precio obligatorio y mayor a cero.
 * - Laboratorio obligatorio y activo.
 * - Rango de referencia opcional.
 * - Unidad opcional.
 *
 * IMPORTANTE:
 *
 * - No elimina físicamente los exámenes.
 * - No modifica órdenes médicas.
 * - No actualiza precios históricos.
 * - No modifica firmas digitales.
 * - No modifica los CU-08 ni CU-09.
 *
 * Las operaciones críticas, auditoría y protección
 * de dependencias corresponden al Service.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/examenes-laboratorio")
public class ExamenLaboratorioMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/examenes-laboratorio/listado";

    private static final String VISTA_FORMULARIO =
            "admin/examenes-laboratorio/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/examenes-laboratorio";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ExamenLaboratorioMantenimientoConsultaService
            consultaService;

    private final ExamenLaboratorioMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ExamenLaboratorioMantenimientoController(

            ExamenLaboratorioMantenimientoConsultaService consultaService,

            ExamenLaboratorioMantenimientoService mantenimientoService,

            PermisoCatalogoService permisoCatalogoService
    ) {

        this.consultaService =
                consultaService;

        this.mantenimientoService =
                mantenimientoService;

        this.permisoCatalogoService =
                permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR Y BUSCAR EXÁMENES
    // =====================================================
    //
    // GET /admin/examenes-laboratorio
    //
    // Permite:
    //
    // - Búsqueda por texto.
    // - Filtro activo/inactivo.
    // - Filtro por laboratorio.
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
                    name = "idLaboratorio",
                    required = false
            )
            Integer idLaboratorio,

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
                "lab-exam/read"
        );

        // =================================================
        // FILTRAR ESTADO
        // =================================================

        Boolean activo = null;

        if ("activos".equalsIgnoreCase(estado)) {

            activo = true;

        } else if ("inactivos".equalsIgnoreCase(estado)) {

            activo = false;
        }

        // =================================================
        // CONSULTAR EXÁMENES
        // =================================================

        Page<ExamenLaboratorio> pagina =
                consultaService.listar(

                        texto,

                        activo,

                        idLaboratorio,

                        page,

                        TAMANIO_PAGINA
                );

        // =================================================
        // RESULTADOS
        // =================================================

        model.addAttribute(
                "examenes",
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

        // =================================================
        // MANTENER FILTROS
        // =================================================

        model.addAttribute(
                "textoBusqueda",
                texto
        );

        model.addAttribute(
                "estadoFiltro",
                estado
        );

        model.addAttribute(
                "idLaboratorioFiltro",
                idLaboratorio
        );

        // =================================================
        // LABORATORIOS DISPONIBLES PARA FILTROS
        // =================================================

        model.addAttribute(
                "laboratoriosFiltro",
                consultaService.listarLaboratoriosParaFiltro()
        );

        return VISTA_LISTADO;
    }

    // =====================================================
    // CU-14 - FORMULARIO DE CREACIÓN
    // =====================================================
    //
    // GET /admin/examenes-laboratorio/create
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/create"
        );

        // =================================================
        // PREPARAR FORMULARIO
        // =================================================

        model.addAttribute(
                "formulario",
                new FormularioExamenLaboratorio()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - CREAR EXAMEN
    // =====================================================
    //
    // POST /admin/examenes-laboratorio/create
    //
    // =====================================================

    @PostMapping("/create")
    public String crearExamen(

            @Valid
            @ModelAttribute("formulario")
            FormularioExamenLaboratorio formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/create"
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
        // CREAR EXAMEN MEDIANTE SERVICE
        // =================================================

        try {

            ExamenLaboratorio guardado =
                    mantenimientoService.crear(

                            formulario.getNombre(),

                            formulario.getDescripcion(),

                            formulario.getPrecio(),

                            formulario.getRangoReferencia(),

                            formulario.getUnidad(),

                            formulario.getIdLaboratorio()
                    );

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El examen "
                            + guardado.getNombre()
                            + " ha sido creado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            // =============================================
            // RESTRICCIONES DE POSTGRESQL
            // =============================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar el examen. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos "
                            + "cumplan las restricciones."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // =============================================
            // REGLAS DEL SERVICE
            // =============================================

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        // =================================================
        // CONSERVAR FORMULARIO Y MENSAJES
        // =================================================

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - FORMULARIO DE EDICIÓN
    // =====================================================
    //
    // GET /admin/examenes-laboratorio/edit/{id}
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
                "lab-exam/update"
        );

        try {

            // =============================================
            // CONSULTAR EXAMEN EXISTENTE
            // =============================================

            ExamenLaboratorio examen =
                    consultaService.buscarPorId(id);

            // =============================================
            // PRECARGAR FORMULARIO
            // =============================================

            FormularioExamenLaboratorio formulario =
                    new FormularioExamenLaboratorio();

            formulario.setNombre(
                    examen.getNombre()
            );

            formulario.setDescripcion(
                    examen.getDescripcion()
            );

            formulario.setPrecio(
                    examen.getPrecio()
            );

            formulario.setRangoReferencia(
                    examen.getRangoReferencia()
            );

            formulario.setUnidad(
                    examen.getUnidad()
            );

            formulario.setIdLaboratorio(
                    examen.getLaboratorio() == null
                            ? null
                            : examen.getLaboratorio().getId()
            );

            // =============================================
            // MODELO
            // =============================================

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
    // CU-14 - ACTUALIZAR EXAMEN
    // =====================================================
    //
    // POST /admin/examenes-laboratorio/edit/{id}
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarExamen(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioExamenLaboratorio formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/update"
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

            ExamenLaboratorio actualizado =
                    mantenimientoService.actualizar(

                            id,

                            formulario.getNombre(),

                            formulario.getDescripcion(),

                            formulario.getPrecio(),

                            formulario.getRangoReferencia(),

                            formulario.getUnidad(),

                            formulario.getIdLaboratorio()
                    );

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El examen "
                            + actualizado.getNombre()
                            + " ha sido actualizado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar el examen. "
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

        // =================================================
        // REGRESAR AL FORMULARIO CON LOS DATOS
        // =================================================

        prepararFormulario(
                model,
                true,
                id
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - DESACTIVAR EXAMEN
    // =====================================================
    //
    // POST /admin/examenes-laboratorio/delete/{id}
    //
    // Eliminación lógica.
    //
    // El Service protege los exámenes asociados
    // a órdenes clínicas existentes.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarExamen(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/delete"
        );

        try {

            // =============================================
            // CONSULTAR NOMBRE
            // =============================================

            ExamenLaboratorio examen =
                    consultaService.buscarPorId(id);

            String nombre =
                    examen.getNombre();

            // =============================================
            // DESACTIVACIÓN LÓGICA
            // =============================================

            mantenimientoService.desactivar(id);

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El examen "
                            + nombre
                            + " ha sido desactivado correctamente."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo desactivar el examen "
                            + "debido a restricciones "
                            + "de integridad en PostgreSQL."
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
    //
    // Utilizado en creación y edición.
    //
    // Carga:
    //
    // - Modo de edición.
    // - Identificador.
    // - Título.
    // - Ruta de guardado.
    // - Laboratorios activos.
    //
    // =====================================================

    private void prepararFormulario(

            Model model,

            boolean modoEdicion,

            Integer idExamen
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idExamen",
                idExamen
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Examen de Laboratorio"
                        : "Crear Examen de Laboratorio"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/examenes-laboratorio/edit/"
                        + idExamen
                        : "/admin/examenes-laboratorio/create"
        );

        // =================================================
        // LABORATORIOS DISPONIBLES
        // =================================================

        model.addAttribute(
                "laboratoriosActivos",
                consultaService.listarLaboratoriosActivos()
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
                                error ->
                                        error.getDefaultMessage() == null
                                                ? "Uno de los campos no es válido."
                                                : error.getDefaultMessage()
                        )
                        .distinct()
                        .toList();

        model.addAttribute(
                "errores",
                errores
        );
    }

    // =====================================================
    // DTO INTERNO - FORMULARIO DE EXÁMENES
    // =====================================================
    //
    // Los campos coinciden con las reglas del CU-14.
    //
    // La validación real se realiza nuevamente
    // desde ExamenLaboratorioMantenimientoService.
    //
    // El ID y el estado activo/inactivo no se
    // reciben como campos editables.
    //
    // =====================================================

    public static class FormularioExamenLaboratorio {

        // ================================================
        // NOMBRE
        // ================================================

        @NotBlank(
                message = "El nombre es obligatorio."
        )
        @Size(
                max = 200,
                message = "El nombre no puede superar los 200 caracteres."
        )
        private String nombre;

        // ================================================
        // DESCRIPCIÓN
        // ================================================

        @Size(
                max = 500,
                message = "La descripción no puede superar los 500 caracteres."
        )
        private String descripcion;

        // ================================================
        // PRECIO
        // ================================================

        @NotNull(
                message = "El precio base es obligatorio."
        )
        @DecimalMin(
                value = "0.01",
                message = "El precio base debe ser mayor a 0."
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio base debe tener precisión 10,2."
        )
        private BigDecimal precio;

        // ================================================
        // RANGO DE REFERENCIA
        // ================================================

        @Size(
                max = 255,
                message = "El rango de referencia no puede superar los 255 caracteres."
        )
        private String rangoReferencia;

        // ================================================
        // UNIDAD
        // ================================================
        //
        // El detalle histórico de las órdenes
        // admite máximo 100 caracteres.
        //
        // ================================================

        @Size(
                max = 100,
                message = "La unidad no puede superar los 100 caracteres."
        )
        private String unidad;

        // ================================================
        // LABORATORIO
        // ================================================

        @NotNull(
                message = "Debe seleccionar un laboratorio."
        )
        @Min(
                value = 1,
                message = "Debe seleccionar un laboratorio válido."
        )
        private Integer idLaboratorio;

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

        public BigDecimal getPrecio() {
            return precio;
        }

        public void setPrecio(BigDecimal precio) {
            this.precio = precio;
        }

        public String getRangoReferencia() {
            return rangoReferencia;
        }

        public void setRangoReferencia(
                String rangoReferencia
        ) {
            this.rangoReferencia = rangoReferencia;
        }

        public String getUnidad() {
            return unidad;
        }

        public void setUnidad(String unidad) {
            this.unidad = unidad;
        }

        public Integer getIdLaboratorio() {
            return idLaboratorio;
        }

        public void setIdLaboratorio(
                Integer idLaboratorio
        ) {
            this.idLaboratorio = idLaboratorio;
        }
    }
}
