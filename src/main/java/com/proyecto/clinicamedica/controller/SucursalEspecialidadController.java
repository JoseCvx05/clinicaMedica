
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.Sucursal;

import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.SucursalEspecialidadService;
import com.proyecto.clinicamedica.service.SucursalService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;


/**
 * =========================================================
 * CONTROLADOR: CONFIGURACIÓN DE SEDES Y ESPECIALIDADES
 * =========================================================
 *
 * CU-13 - Configuración de Sedes y Especialidades.
 *
 * Rutas:
 *
 * GET  /branch-specialty
 * GET  /branch-specialty/create
 * POST /branch-specialty/create
 *
 * Responsabilidades:
 *
 * - Mostrar asignaciones registradas.
 * - Permitir búsqueda por ID.
 * - Implementar paginación.
 * - Mostrar formulario de asignación.
 * - Cargar sucursales activas.
 * - Cargar especialidades activas.
 * - Validar campos del formulario.
 * - Registrar nuevas asignaciones.
 * - Mostrar mensajes de éxito y error.
 *
 * SEGURIDAD:
 *
 * Spring Security protege /branch-specialty/**
 * y exige ROLE_ADMINISTRADOR.
 *
 * Además, el servicio comprueba que el usuario
 * autenticado sea un Administrador activo.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/branch-specialty")
public class SucursalEspecialidadController {


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_PAGINA = 10;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalEspecialidadService
            sucursalEspecialidadService;

    private final SucursalService sucursalService;

    private final EspecialidadService especialidadService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SucursalEspecialidadController(
            SucursalEspecialidadService sucursalEspecialidadService,
            SucursalService sucursalService,
            EspecialidadService especialidadService
    ) {

        this.sucursalEspecialidadService =
                sucursalEspecialidadService;

        this.sucursalService =
                sucursalService;

        this.especialidadService =
                especialidadService;
    }


    // =====================================================
    // CU-13 - LISTADO DE ASIGNACIONES
    // =====================================================
    //
    // GET /branch-specialty
    //
    // Permite:
    //
    // - Listar asignaciones.
    // - Filtrar por ID.
    // - Paginar resultados.
    // - Consultar relaciones activas e inactivas.
    //
    // =====================================================

    @GetMapping
    public String mostrarListado(

            @RequestParam(
                    name = "id",
                    required = false
            )
            Integer id,

            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            Model model
    ) {

        // ================================================
        // BÚSQUEDA POR ID
        // ================================================

        if (id != null) {

            List<SucursalEspecialidadService.ResumenAsignacion>
                    resultados = sucursalEspecialidadService
                    .buscarPorId(id)
                    .stream()
                    .toList();

            model.addAttribute(
                    "asignaciones",
                    resultados
            );

            model.addAttribute(
                    "idBusqueda",
                    id
            );

            model.addAttribute(
                    "paginaActual",
                    0
            );

            model.addAttribute(
                    "totalPaginas",
                    1
            );

            model.addAttribute(
                    "filtrando",
                    true
            );

            return "branch-specialty/listado";
        }


        // ================================================
        // PAGINACIÓN NORMAL
        // ================================================

        int paginaActual =
                Math.max(page, 0);

        Pageable pageable =
                PageRequest.of(
                        paginaActual,
                        TAMANIO_PAGINA,
                        Sort.by(
                                Sort.Direction.ASC,
                                "id"
                        )
                );


        Page<SucursalEspecialidadService.ResumenAsignacion>
                pagina = sucursalEspecialidadService
                .listar(pageable);


        // ================================================
        // DATOS PARA THYMELEAF
        // ================================================

        model.addAttribute(
                "asignaciones",
                pagina.getContent()
        );

        model.addAttribute(
                "paginaActual",
                paginaActual
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
                "filtrando",
                false
        );


        return "branch-specialty/listado";
    }


    // =====================================================
    // CU-13 - MOSTRAR FORMULARIO
    // =====================================================
    //
    // GET /branch-specialty/create
    //
    // Carga únicamente sucursales y especialidades
    // activas desde los servicios existentes.
    //
    // =====================================================

    @GetMapping("/create")
    public String mostrarFormulario(
            Model model
    ) {

        model.addAttribute(
                "formulario",
                new FormularioAsignacion()
        );

        cargarCatalogos(model);

        return "branch-specialty/crear";
    }


    // =====================================================
    // CU-13 - GUARDAR ASIGNACIÓN
    // =====================================================
    //
    // POST /branch-specialty/create
    //
    // Valida campos obligatorios y delega la creación
    // en SucursalEspecialidadService.
    //
    // =====================================================

    @PostMapping("/create")
    public String guardarAsignacion(

            @Valid
            @ModelAttribute("formulario")
            FormularioAsignacion formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // ================================================
        // FA03 - CAMPOS INVÁLIDOS
        // ================================================

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "errores",
                    bindingResult
                            .getFieldErrors()
                            .stream()
                            .map(
                                    error -> error.getDefaultMessage()
                            )
                            .distinct()
                            .toList()
            );

            cargarCatalogos(model);

            return "branch-specialty/crear";
        }


        // ================================================
        // CREAR ASIGNACIÓN
        // ================================================

        try {

            sucursalEspecialidadService
                    .asignar(

                            formulario.getIdSucursal(),

                            formulario.getIdEspecialidad(),

                            formulario.getPrecioConsulta()
                    );


            // ============================================
            // MENSAJE DE ÉXITO
            // ============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Especialidad asignada a la sede correctamente."
            );


            return "redirect:/branch-specialty";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // ============================================
            // FA05 - DUPLICADOS Y OTRAS VALIDACIONES
            // ============================================

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            cargarCatalogos(model);

            return "branch-specialty/crear";
        }
    }




    // =====================================================
    // CU-13 - FA02: ELIMINAR ASIGNACIÓN
    // =====================================================
    //
    // POST /branch-specialty/delete/{id}
    //
    // La eliminación es lógica.
    //
    // El Service:
    // - Verifica al Administrador.
    // - Bloquea la asignación.
    // - Protege las citas existentes.
    // - Desactiva sin borrar físicamente.
    // - Registra la auditoría.
    //
    // Se utiliza POST para evitar eliminaciones
    // mediante enlaces GET.
    // =====================================================

    @PostMapping("/delete/{id}")
    public String eliminarAsignacion(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // ================================================
        // INTENTAR DESACTIVAR
        // ================================================

        try {

            sucursalEspecialidadService
                    .deleteBranchSpecialty(id);

            // ============================================
            // MENSAJE DE ÉXITO
            // ============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La asignación de especialidad "
                            + "se desactivó correctamente."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // ============================================
            // MENSAJE DE ERROR
            // ============================================
            //
            // Se utiliza el mensaje validado
            // por el Service.
            // ============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        // ================================================
        // REGRESAR AL LISTADO
        // ================================================

        return "redirect:/branch-specialty";
    }


    // =====================================================
    // CU-13 - REACTIVAR ASIGNACIÓN
    // =====================================================
    //
    // POST /branch-specialty/reactivate/{id}
    //
    // Permite al Administrador volver a habilitar
    // una asignación previamente desactivada.
    //
    // SEGURIDAD:
    //
    // - Solo Administrador.
    // - La sede debe estar activa.
    // - La especialidad debe estar activa.
    // - No se crean registros duplicados.
    // - Conserva el precio registrado.
    // - Registra auditoría.
    //
    // La validación se realiza en el Service.
    // =====================================================

    @PostMapping("/reactivate/{id}")
    public String reactivarAsignacion(

            @PathVariable("id")
            Integer id,

            RedirectAttributes redirectAttributes
    ) {

        // ================================================
        // INTENTAR REACTIVAR
        // ================================================

        try {

            sucursalEspecialidadService
                    .reactivarAsignacion(id);

            // ============================================
            // MENSAJE DE ÉXITO
            // ============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La asignación de especialidad "
                            + "se reactivó correctamente."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // ============================================
            // MENSAJE DE ERROR
            // ============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        // ================================================
        // REGRESAR AL LISTADO
        // ================================================

        return "redirect:/branch-specialty";
    }


    // =====================================================
    // CARGAR CATÁLOGOS ACTIVOS
    // =====================================================
    //
    // Se utiliza al abrir el formulario y cuando
    // alguna validación falla.
    //
    // Esto evita perder las opciones del dropdown.
    //
    // =====================================================

    private void cargarCatalogos(
            Model model
    ) {

        List<Sucursal> sucursales =
                sucursalService
                        .listarActivas();

        List<Especialidad> especialidades =
                especialidadService
                        .listarActivas();


        model.addAttribute(
                "sucursales",
                sucursales
        );

        model.addAttribute(
                "especialidades",
                especialidades
        );
    }


    // =====================================================
    // DTO INTERNO - FORMULARIO DE ASIGNACIÓN
    // =====================================================
    //
    // Los campos son obligatorios.
    //
    // El precio se conserva porque la entidad
    // SucursalEspecialidad lo necesita y otros
    // módulos utilizan ese valor para cobros.
    //
    // =====================================================

    public static class FormularioAsignacion {


        // ================================================
        // SEDE
        // ================================================

        @NotNull(
                message = "Debe seleccionar una sede."
        )
        private Integer idSucursal;


        // ================================================
        // ESPECIALIDAD
        // ================================================

        @NotNull(
                message = "Debe seleccionar una especialidad."
        )
        private Integer idEspecialidad;


        // ================================================
        // PRECIO DE CONSULTA
        // ================================================

        @NotNull(
                message = "Debe indicar el precio de consulta."
        )

        @DecimalMin(
                value = "0.01",
                message = "El precio debe ser mayor que cero."
        )

        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio debe tener máximo "
                        + "8 dígitos enteros y 2 decimales."
        )
        private BigDecimal precioConsulta;


        // ================================================
        // GETTERS Y SETTERS
        // ================================================

        public Integer getIdSucursal() {
            return idSucursal;
        }

        public void setIdSucursal(
                Integer idSucursal
        ) {
            this.idSucursal = idSucursal;
        }


        public Integer getIdEspecialidad() {
            return idEspecialidad;
        }

        public void setIdEspecialidad(
                Integer idEspecialidad
        ) {
            this.idEspecialidad = idEspecialidad;
        }


        public BigDecimal getPrecioConsulta() {
            return precioConsulta;
        }

        public void setPrecioConsulta(
                BigDecimal precioConsulta
        ) {
            this.precioConsulta = precioConsulta;
        }
    }


    // =====================================================
    // CU-14 - CATÁLOGO 9
    // MOSTRAR FORMULARIO DE EDICIÓN DE PRECIO
    // =====================================================
    //
    // GET /branch-specialty/edit/{id}
    //
    // Permite consultar la asignación seleccionada
    // y mostrar su precio de consulta actual.
    //
    // No modifica información en PostgreSQL.
    //
    // =====================================================

    @GetMapping("/edit/{id}")
    public String mostrarEdicionPrecio(

            @PathVariable("id") Integer id,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // 1. BUSCAR ASIGNACIÓN
        // -------------------------------------------------

        SucursalEspecialidadService.ResumenAsignacion asignacion =
                sucursalEspecialidadService
                        .buscarPorId(id)
                        .orElse(null);

        // -------------------------------------------------
        // 2. VERIFICAR EXISTENCIA
        // -------------------------------------------------

        if (asignacion == null) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "La asignación seleccionada no existe."
            );

            return "redirect:/branch-specialty";
        }

        // -------------------------------------------------
        // 3. VERIFICAR ESTADO
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(asignacion.activo())) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No puede modificar el precio de una "
                            + "asignación inactiva."
            );

            return "redirect:/branch-specialty";
        }

        // -------------------------------------------------
        // 4. PREPARAR FORMULARIO
        // -------------------------------------------------

        model.addAttribute(
                "asignacion",
                asignacion
        );

        model.addAttribute(
                "nuevoPrecio",
                asignacion.precioConsulta()
        );

        // -------------------------------------------------
        // 5. MOSTRAR VISTA
        // -------------------------------------------------

        return "branch-specialty/editar-precio";
    }


    // =====================================================
    // CU-14 - CATÁLOGO 9
    // GUARDAR NUEVO PRECIO DE CONSULTA
    // =====================================================
    //
    // POST /branch-specialty/edit/{id}
    //
    // SEGURIDAD:
    //
    // - El servicio valida al Administrador activo.
    // - No permite precios inválidos.
    // - Protege citas antiguas pendientes de pago.
    // - Utiliza bloqueo en PostgreSQL.
    // - Registra auditoría.
    //
    // =====================================================

    @PostMapping("/edit/{id}")
    public String guardarEdicionPrecio(

            @PathVariable("id") Integer id,

            @RequestParam(
                    name = "nuevoPrecio",
                    required = false
            )
            BigDecimal nuevoPrecio,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // 1. INTENTAR ACTUALIZAR PRECIO
        // -------------------------------------------------

        try {

            sucursalEspecialidadService
                    .actualizarPrecioConsulta(
                            id,
                            nuevoPrecio
                    );

            // ---------------------------------------------
            // 2. MENSAJE DE ÉXITO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El precio de consulta se actualizó "
                            + "correctamente."
            );

            return "redirect:/branch-specialty";

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // ---------------------------------------------
            // 3. MENSAJE DE ERROR
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            return "redirect:/branch-specialty/edit/" + id;
        }
    }

}
