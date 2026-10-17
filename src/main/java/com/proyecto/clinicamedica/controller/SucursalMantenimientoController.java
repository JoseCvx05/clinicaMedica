
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.Sucursal;

import com.proyecto.clinicamedica.service.SucursalMantenimientoConsultaService;
import com.proyecto.clinicamedica.service.SucursalMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 * CU-14 - CONTROLADOR DE MANTENIMIENTO DE SUCURSALES
 * =========================================================
 *
 * Funcionalidades:
 *
 * - Listar sucursales.
 * - Buscar por nombre, dirección o descripción.
 * - Filtrar por estado.
 * - Paginar resultados.
 * - Mostrar formulario de creación.
 * - Crear sucursales.
 * - Mostrar formulario de edición.
 * - Actualizar sucursales.
 * - Desactivar sucursales.
 * - Mostrar mensajes y errores.
 *
 * SEGURIDAD:
 *
 * - Cada operación utiliza permisos específicos.
 * - Las modificaciones se delegan al Service.
 * - La eliminación lógica utiliza POST.
 * - No se permite modificar datos mediante GET.
 * - No se eliminan físicamente sucursales.
 *
 * PROTECCIÓN:
 *
 * El Service valida las dependencias relacionadas
 * con usuarios, citas, inventario y especialidades.
 *
 * Los formularios POST deberán cumplir con la
 * protección CSRF configurada en Spring Security.
 *
 * Este controlador no modifica directamente
 * otros módulos del sistema.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/sucursales")
public class SucursalMantenimientoController {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;

    private static final String VISTA_LISTADO =
            "admin/sucursales/listado";

    private static final String VISTA_FORMULARIO =
            "admin/sucursales/formulario";

    private static final String REDIRECCION_LISTADO =
            "redirect:/admin/sucursales";

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalMantenimientoConsultaService
            consultaService;

    private final SucursalMantenimientoService
            mantenimientoService;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SucursalMantenimientoController(
            SucursalMantenimientoConsultaService consultaService,
            SucursalMantenimientoService mantenimientoService,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.consultaService = consultaService;

        this.mantenimientoService = mantenimientoService;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTADO DE SUCURSALES
    // =====================================================
    //
    // GET /admin/sucursales
    //
    // Filtros:
    //
    // texto  -> Nombre, dirección o descripción.
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
                "branch/read"
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
        // CONSULTAR SUCURSALES
        // =================================================

        Page<Sucursal> pagina =
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
                "sucursales",
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
    // GET /admin/sucursales/create
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormularioCreacion(
            Model model
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/create"
        );

        // =================================================
        // PREPARAR FORMULARIO
        // =================================================

        model.addAttribute(
                "formulario",
                new FormularioSucursal()
        );

        prepararFormulario(
                model,
                false,
                null
        );

        return VISTA_FORMULARIO;
    }

    // =====================================================
    // CU-14 - CREAR SUCURSAL
    // =====================================================
    //
    // POST /admin/sucursales/create
    //
    // =====================================================

    @PostMapping("/create")
    public String crearSucursal(

            @Valid
            @ModelAttribute("formulario")
            FormularioSucursal formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/create"
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
        // CREAR SUCURSAL MEDIANTE SERVICE
        // =================================================

        try {

            Sucursal guardada =
                    mantenimientoService.crear(
                            formulario.getNombre(),
                            formulario.getTelefono(),
                            formulario.getDireccion(),
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

            // =============================================
            // ERROR DE INTEGRIDAD EN POSTGRESQL
            // =============================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo guardar la sucursal. "
                            + "Verifique que no exista otra "
                            + "sucursal activa con el mismo "
                            + "nombre y que los datos cumplan "
                            + "las restricciones del sistema."
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
    // CU-14 - MOSTRAR FORMULARIO DE EDICIÓN
    // =====================================================
    //
    // GET /admin/sucursales/edit/{id}
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
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/update"
        );

        try {

            // =============================================
            // BUSCAR SUCURSAL
            // =============================================
            //
            // El servicio de consulta también exige
            // branch/read para obtener sus datos.
            //
            // =============================================

            Sucursal sucursal =
                    consultaService.buscarPorId(id);

            // =============================================
            // PRECARGAR DATOS
            // =============================================

            FormularioSucursal formulario =
                    new FormularioSucursal();

            formulario.setNombre(
                    sucursal.getNombre()
            );

            formulario.setTelefono(
                    sucursal.getTelefono()
            );

            formulario.setDireccion(
                    sucursal.getDireccion()
            );

            formulario.setDescripcion(
                    sucursal.getDescripcion()
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
    // CU-14 - ACTUALIZAR SUCURSAL
    // =====================================================
    //
    // POST /admin/sucursales/edit/{id}
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String actualizarSucursal(

            @PathVariable("id")
            Integer id,

            @Valid
            @ModelAttribute("formulario")
            FormularioSucursal formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/update"
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

            Sucursal actualizada =
                    mantenimientoService.actualizar(
                            id,
                            formulario.getNombre(),
                            formulario.getTelefono(),
                            formulario.getDireccion(),
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

            // =============================================
            // PROTEGER INTEGRIDAD DE POSTGRESQL
            // =============================================

            model.addAttribute(
                    "mensajeError",
                    "No se pudo actualizar la sucursal. "
                            + "Verifique que el nombre no esté "
                            + "duplicado y que los datos "
                            + "sean válidos."
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

            // =============================================
            // ERRORES DE VALIDACIÓN
            // =============================================

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
    // CU-14 - DESACTIVAR SUCURSAL
    // =====================================================
    //
    // POST /admin/sucursales/delete/{id}
    //
    // REGLAS:
    //
    // - Exigir permiso branch/delete.
    // - No eliminar físicamente.
    // - Validar dependencias en el Service.
    // - Conservar registros históricos.
    // - Registrar auditoría desde el Service.
    //
    // =====================================================

    @PostMapping("/delete/{id}")
    public String desactivarSucursal(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/delete"
        );

        try {

            // =============================================
            // BUSCAR NOMBRE DE SUCURSAL
            // =============================================
            //
            // Esta consulta también exige branch/read.
            //
            // =============================================

            Sucursal sucursal =
                    consultaService.buscarPorId(id);

            // =============================================
            // DESACTIVACIÓN LÓGICA
            // =============================================

            mantenimientoService.desactivar(id);

            // =============================================
            // MENSAJE DE CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El registro "
                            + sucursal.getNombre()
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
                    "No se pudo desactivar la sucursal "
                            + "porque existe una restricción "
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
            Integer idSucursal
    ) {

        model.addAttribute(
                "modoEdicion",
                modoEdicion
        );

        model.addAttribute(
                "idSucursal",
                idSucursal
        );

        model.addAttribute(
                "tituloFormulario",
                modoEdicion
                        ? "Editar Sucursal"
                        : "Crear Sucursal"
        );

        model.addAttribute(
                "accionFormulario",
                modoEdicion
                        ? "/admin/sucursales/edit/"
                        + idSucursal
                        : "/admin/sucursales/create"
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
    // DTO INTERNO - FORMULARIO DE SUCURSAL
    // =====================================================
    //
    // Validaciones iniciales.
    //
    // El Service realiza una segunda validación.
    //
    // El estado no se modifica desde este formulario.
    //
    // La creación establece activo = true.
    //
    // La desactivación requiere una operación separada.
    //
    // =====================================================

    public static class FormularioSucursal {

        // ================================================
        // NOMBRE
        // ================================================

        @NotBlank(
                message = "El nombre es obligatorio."
        )
        @Size(
                max = 100,
                message = "El nombre no puede exceder los 100 caracteres."
        )
        private String nombre;

        // ================================================
        // TELÉFONO
        // ================================================
        //
        // Opcional.
        //
        // Si se ingresa, debe contener exactamente
        // ocho dígitos numéricos.
        //
        // ================================================

        @Pattern(
                regexp = "^$|^[0-9]{8}$",
                message = "El teléfono debe tener exactamente 8 dígitos."
        )
        private String telefono;

        // ================================================
        // DIRECCIÓN
        // ================================================
        //
        // Opcional.
        //
        // ================================================

        @Size(
                max = 500,
                message = "La dirección no puede exceder los 500 caracteres."
        )
        private String direccion;

        // ================================================
        // DESCRIPCIÓN
        // ================================================
        //
        // Opcional.
        //
        // ================================================

        @Size(
                max = 250,
                message = "La descripción no puede exceder los 250 caracteres."
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

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }

        public String getDireccion() {
            return direccion;
        }

        public void setDireccion(String direccion) {
            this.direccion = direccion;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
    }
}
