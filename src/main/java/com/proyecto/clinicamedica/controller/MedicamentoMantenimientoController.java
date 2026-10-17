
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.Medicamento;

import com.proyecto.clinicamedica.service.MedicamentoMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.MedicamentoMantenimientoService;
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
 * CU-14 - CONTROLLER: MANTENIMIENTO DE MEDICAMENTOS
 * =========================================================
 *
 * Catálogo 6 de 9.
 *
 * FUNCIONALIDADES:
 *
 * - Listar medicamentos registrados.
 * - Buscar por nombre, descripción o unidad.
 * - Filtrar por estado activo/inactivo.
 * - Filtrar medicamentos controlados.
 * - Paginar resultados.
 * - Registrar medicamentos.
 * - Editar medicamentos.
 * - Actualizar precios.
 * - Configurar stock mínimo.
 * - Administrar indicador de controlado.
 * - Desactivar mediante eliminación lógica.
 * - Mostrar errores y confirmaciones.
 *
 * SEGURIDAD:
 *
 * - medicine/read
 * - medicine/create
 * - medicine/update
 * - medicine/delete
 *
 * REGLAS:
 *
 * - Nombre obligatorio, máximo 200.
 * - Descripción obligatoria, máximo 500.
 * - Precio mayor a cero, precisión 10,2.
 * - Unidad obligatoria, máximo 50.
 * - Indicador controlado obligatorio.
 * - Stock mínimo opcional, no negativo.
 *
 * PROTECCIÓN:
 *
 * - No elimina físicamente medicamentos.
 * - No modifica recetas médicas.
 * - No cambia precios históricos.
 * - No descuenta inventario.
 * - No modifica despachos.
 *
 * Las reglas de negocio, transacciones,
 * auditoría y dependencias se validan
 * nuevamente desde el Service.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/medicamentos")
public class MedicamentoMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/medicamentos/listado";

    private static final String VISTA_FORMULARIO =
            "admin/medicamentos/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/medicamentos";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MedicamentoMantenimientoConsultaService
            consultaService;

    private final MedicamentoMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MedicamentoMantenimientoController(

            MedicamentoMantenimientoConsultaService consultaService,

            MedicamentoMantenimientoService mantenimientoService,

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
    // CU-14 - LISTAR MEDICAMENTOS
    // =====================================================
    //
    // GET /admin/medicamentos
    //
    // Permite:
    //
    // - Buscar medicamentos.
    // - Filtrar activos e inactivos.
    // - Filtrar controlados y no controlados.
    // - Paginar resultados.
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
                    name = "controlado",
                    defaultValue = "todos"
            )
            String controlado,

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
                "medicine/read"
        );

        // =================================================
        // FILTRO DE ESTADO
        // =================================================

        Boolean activo = null;

        if ("activos".equalsIgnoreCase(estado)) {

            activo = true;

        } else if ("inactivos".equalsIgnoreCase(estado)) {

            activo = false;
        }

        // =================================================
        // FILTRO DE MEDICAMENTO CONTROLADO
        // =================================================

        Boolean esControlado = null;

        if ("controlados".equalsIgnoreCase(controlado)) {

            esControlado = true;

        } else if ("no-controlados".equalsIgnoreCase(controlado)) {

            esControlado = false;
        }

        // =================================================
        // CONSULTAR MEDICAMENTOS
        // =================================================

        Page<Medicamento> pagina =
                consultaService.listar(

                        texto,

                        activo,

                        esControlado,

                        page,

                        TAMANIO_PAGINA
                );

        // =================================================
        // RESULTADOS
        // =================================================

        model.addAttribute(
                "medicamentos",
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
        // CONSERVAR FILTROS
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
                "controladoFiltro",
                controlado
        );

        return VISTA_LISTADO;
    }

    // =====================================================
    // CU-14 - FORMULARIO DE CREACIÓN
    // =====================================================
    //
    // GET /admin/medicamentos/create
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
                "medicine/create"
        );

        // =================================================
        // FORMULARIO VACÍO
        // =================================================

        model.addAttribute(
                "formulario",
                new FormularioMedicamento()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - CREAR MEDICAMENTO
    // =====================================================
    //
    // POST /admin/medicamentos/create
    //
    // =====================================================

    @PostMapping("/create")
    public String crearMedicamento(

            @Valid
            @ModelAttribute("formulario")
            FormularioMedicamento formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/create"
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
                    false,
                    null
            );

            return VISTA_FORMULARIO;
        }

        // =================================================
        // CREAR MEDIANTE SERVICE
        // =================================================

        try {

            Medicamento guardado =
                    mantenimientoService.crear(

                            formulario.getNombre(),

                            formulario.getDescripcion(),

                            formulario.getPrecio(),

                            formulario.getUnidad(),

                            formulario.getEsControlado(),

                            formulario.getStockMinimo()
                    );

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El medicamento "
                            + guardado.getNombre()
                            + " ha sido creado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar el medicamento. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos "
                            + "sean válidos."
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
        // CONSERVAR DATOS DEL FORMULARIO
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
    // GET /admin/medicamentos/edit/{id}
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
                "medicine/update"
        );

        try {

            // =============================================
            // BUSCAR MEDICAMENTO
            // =============================================

            Medicamento medicamento =
                    consultaService.buscarPorId(id);

            // =============================================
            // PRECARGAR FORMULARIO
            // =============================================

            FormularioMedicamento formulario =
                    new FormularioMedicamento();

            formulario.setNombre(
                    medicamento.getNombre()
            );

            formulario.setDescripcion(
                    medicamento.getDescripcion()
            );

            formulario.setPrecio(
                    medicamento.getPrecio()
            );

            formulario.setUnidad(
                    medicamento.getUnidad()
            );

            formulario.setEsControlado(
                    medicamento.getEsControlado()
            );

            formulario.setStockMinimo(
                    medicamento.getStockMinimo()
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
    // CU-14 - ACTUALIZAR MEDICAMENTO
    // =====================================================
    //
    // POST /admin/medicamentos/edit/{id}
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarMedicamento(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioMedicamento formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/update"
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

            Medicamento actualizado =
                    mantenimientoService.actualizar(

                            id,

                            formulario.getNombre(),

                            formulario.getDescripcion(),

                            formulario.getPrecio(),

                            formulario.getUnidad(),

                            formulario.getEsControlado(),

                            formulario.getStockMinimo()
                    );

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El medicamento "
                            + actualizado.getNombre()
                            + " ha sido actualizado correctamente."
            );

            return REDIRECCION_LISTADO;

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar el medicamento. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos "
                            + "sean válidos."
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
        // CONSERVAR FORMULARIO
        // =================================================

        prepararFormulario(
                model,
                true,
                id
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - DESACTIVAR MEDICAMENTO
    // =====================================================
    //
    // POST /admin/medicamentos/delete/{id}
    //
    // Eliminación lógica.
    //
    // El Service verifica:
    //
    // - Stock disponible en todas las sucursales.
    // - Recetas activas con cantidades pendientes.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarMedicamento(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/delete"
        );

        try {

            // =============================================
            // CONSULTAR NOMBRE
            // =============================================

            Medicamento medicamento =
                    consultaService.buscarPorId(id);

            String nombre =
                    medicamento.getNombre();

            // =============================================
            // DESACTIVACIÓN LÓGICA
            // =============================================

            mantenimientoService.desactivar(id);

            // =============================================
            // CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El medicamento "
                            + nombre
                            + " ha sido desactivado correctamente."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo desactivar el medicamento "
                            + "debido a restricciones de "
                            + "integridad en PostgreSQL."
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

            Integer idMedicamento
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idMedicamento",
                idMedicamento
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Medicamento"
                        : "Crear Medicamento"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/medicamentos/edit/"
                        + idMedicamento
                        : "/admin/medicamentos/create"
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
    // DTO INTERNO - FORMULARIO DE MEDICAMENTOS
    // =====================================================
    //
    // Permite recibir únicamente campos administrables.
    //
    // No se reciben:
    //
    // - ID como campo editable.
    // - Estado activo/inactivo.
    // - Cantidades disponibles de inventario.
    // - Despachos.
    // - Recetas.
    //
    // =====================================================

    public static class FormularioMedicamento {

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

        @NotBlank(
                message = "La descripción es obligatoria."
        )
        @Size(
                max = 500,
                message = "La descripción no puede superar los 500 caracteres."
        )
        private String descripcion;

        // ================================================
        // PRECIO
        // ================================================

        @NotNull(
                message = "El precio es obligatorio."
        )
        @DecimalMin(
                value = "0.01",
                message = "El precio debe ser mayor a 0."
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio debe tener precisión 10,2."
        )
        private BigDecimal precio;

        // ================================================
        // UNIDAD
        // ================================================

        @NotBlank(
                message = "La unidad es obligatoria."
        )
        @Size(
                max = 50,
                message = "La unidad no puede superar los 50 caracteres."
        )
        private String unidad;

        // ================================================
        // MEDICAMENTO CONTROLADO
        // ================================================

        @NotNull(
                message = "Debe indicar si el medicamento es controlado."
        )
        private Boolean esControlado;

        // ================================================
        // STOCK MÍNIMO
        // ================================================

        @Min(
                value = 0,
                message = "El stock mínimo no puede ser negativo."
        )
        private Integer stockMinimo;

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

        public String getUnidad() {
            return unidad;
        }

        public void setUnidad(String unidad) {
            this.unidad = unidad;
        }

        public Boolean getEsControlado() {
            return esControlado;
        }

        public void setEsControlado(
                Boolean esControlado
        ) {
            this.esControlado = esControlado;
        }

        public Integer getStockMinimo() {
            return stockMinimo;
        }

        public void setStockMinimo(
                Integer stockMinimo
        ) {
            this.stockMinimo = stockMinimo;
        }
    }
}
