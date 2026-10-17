
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.InventarioFarmaciaFormularioDTO;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;

import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;

import com.proyecto.clinicamedica.service.InventarioFarmaciaMantenimientoService;
import com.proyecto.clinicamedica.service.PermisoCatalogoService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;

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

import com.proyecto.clinicamedica.dto.InventarioFarmaciaEntradaDTO;
import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import org.springframework.web.bind.annotation.PathVariable;

import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;

import com.proyecto.clinicamedica.dto.InventarioFarmaciaAjusteDTO;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
/**
 * =========================================================
 * CU-14 - CONTROLLER: MANTENIMIENTO DE INVENTARIO
 * =========================================================
 *
 * Catálogo 8 de 9 - Inventario de Medicamentos.
 *
 * Responsabilidades:
 *
 * - Mostrar listado administrativo.
 * - Buscar inventarios por medicamento.
 * - Filtrar inventarios por sucursal.
 * - Paginar resultados.
 * - Mostrar formulario de creación.
 * - Validar datos recibidos.
 * - Registrar nuevos inventarios.
 * - Mostrar mensajes de resultado.
 *
 * SEGURIDAD:
 *
 * - Acceso administrativo protegido.
 * - Permisos específicos por operación.
 * - Validaciones definitivas en el Service.
 *
 * IMPORTANTE:
 *
 * - No descuenta medicamentos.
 * - No modifica despachos del CU-11.
 * - No elimina registros históricos.
 * - No permite cambios directos de existencias.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/inventario-farmacia")
public class InventarioFarmaciaMantenimientoController {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final InventarioFarmaciaMantenimientoService
            inventarioService;

    private final MedicamentoRepository
            medicamentoRepository;

    private final SucursalRepository
            sucursalRepository;

    private final PermisoCatalogoService
            permisoCatalogoService;


    // =====================================================
    // CU-14 - CONSULTAR INVENTARIO PARA ENTRADAS
    // =====================================================

    private final InventarioFarmaciaRepository
            inventarioRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================


    public InventarioFarmaciaMantenimientoController(
            InventarioFarmaciaMantenimientoService inventarioService,
            MedicamentoRepository medicamentoRepository,
            SucursalRepository sucursalRepository,
            PermisoCatalogoService permisoCatalogoService,
            InventarioFarmaciaRepository inventarioRepository
    ) {

        this.inventarioService = inventarioService;
        this.medicamentoRepository = medicamentoRepository;
        this.sucursalRepository = sucursalRepository;
        this.permisoCatalogoService = permisoCatalogoService;
        this.inventarioRepository = inventarioRepository;
    }


    // =====================================================
    // CU-14 - LISTADO ADMINISTRATIVO
    // =====================================================

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "busqueda",
                    defaultValue = ""
            )
            String busqueda,

            @RequestParam(
                    name = "idSucursal",
                    required = false
            )
            Integer idSucursal,

            @RequestParam(
                    name = "pagina",
                    defaultValue = "0"
            )
            int pagina,

            Model model
    ) {

        // -------------------------------------------------
        // VALIDAR PERMISO DE CONSULTA
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );


        // -------------------------------------------------
        // CONFIGURAR PAGINACIÓN
        // -------------------------------------------------

        int paginaSegura = Math.max(0, pagina);

        Pageable pageable = PageRequest.of(
                paginaSegura,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "medicamento.nombre"
                )
        );


        // -------------------------------------------------
        // CONSULTAR INVENTARIOS
        // -------------------------------------------------

        Page<InventarioFarmacia> resultado =
                inventarioService.listar(
                        busqueda,
                        idSucursal,
                        pageable
                );


        // -------------------------------------------------
        // ENVIAR RESULTADOS A LA VISTA
        // -------------------------------------------------

        model.addAttribute(
                "inventarios",
                resultado.getContent()
        );

        model.addAttribute(
                "paginaActual",
                paginaSegura
        );

        model.addAttribute(
                "totalPaginas",
                resultado.getTotalPages()
        );

        model.addAttribute(
                "totalElementos",
                resultado.getTotalElements()
        );


        // -------------------------------------------------
        // CONSERVAR FILTROS
        // -------------------------------------------------

        model.addAttribute(
                "busqueda",
                busqueda
        );

        model.addAttribute(
                "idSucursal",
                idSucursal
        );


        // -------------------------------------------------
        // SUCURSALES PARA EL FILTRO
        // -------------------------------------------------

        model.addAttribute(
                "sucursales",
                sucursalRepository
                        .findByActivoTrueOrderByNombreAsc()
        );


        return "admin/inventario-farmacia/listado";
    }


    // =====================================================
    // CU-14 - MOSTRAR FORMULARIO DE CREACIÓN
    // =====================================================

    @GetMapping("/nuevo")
    public String mostrarFormulario(
            Model model
    ) {

        // -------------------------------------------------
        // VALIDAR PERMISO DE CREACIÓN
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/create"
        );


        // -------------------------------------------------
        // PREPARAR FORMULARIO
        // -------------------------------------------------

        model.addAttribute(
                "formulario",
                new InventarioFarmaciaFormularioDTO()
        );


        // -------------------------------------------------
        // CARGAR CATÁLOGOS ACTIVOS
        // -------------------------------------------------

        cargarCatalogos(model);


        return "admin/inventario-farmacia/formulario";
    }


    // =====================================================
    // CU-14 - REGISTRAR INVENTARIO
    // =====================================================

    @PostMapping("/nuevo")
    public String crear(
            @Valid
            @ModelAttribute("formulario")
            InventarioFarmaciaFormularioDTO formulario,

            BindingResult errores,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // VERIFICAR PERMISO DE CREACIÓN
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/create"
        );


        // -------------------------------------------------
        // VALIDAR FORMULARIO
        // -------------------------------------------------

        if (errores.hasErrors()) {

            cargarCatalogos(model);

            return "admin/inventario-farmacia/formulario";
        }


        // -------------------------------------------------
        // EJECUTAR CREACIÓN EN EL SERVICE
        // -------------------------------------------------

        try {

            InventarioFarmacia inventario =
                    inventarioService.crear(
                            formulario.getIdMedicamento(),
                            formulario.getIdSucursal(),
                            formulario.getCantidadInicial()
                    );


            // ---------------------------------------------
            // MENSAJE DE ÉXITO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El inventario #"
                            + inventario.getId()
                            + " ha sido creado correctamente."
            );


            return "redirect:/admin/inventario-farmacia";


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            // ---------------------------------------------
            // ERRORES DE VALIDACIÓN DEL NEGOCIO
            // ---------------------------------------------

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


        } catch (DataIntegrityViolationException ex) {

            // ---------------------------------------------
            // RESTRICCIONES DE POSTGRESQL
            // ---------------------------------------------

            model.addAttribute(
                    "mensajeError",
                    "No fue posible registrar el inventario. "
                            + "Verifique que el medicamento no tenga "
                            + "un inventario registrado en esta sucursal."
            );
        }


        // -------------------------------------------------
        // RECARGAR FORMULARIO SIN PERDER DATOS
        // -------------------------------------------------

        cargarCatalogos(model);

        return "admin/inventario-farmacia/formulario";
    }


    // =====================================================
    // CU-14 - CARGAR CATÁLOGOS PARA FORMULARIOS
    // =====================================================
    //
    // Solamente muestra:
    //
    // - Medicamentos activos.
    // - Sucursales activas.
    //
    // El Service verifica nuevamente los registros
    // antes de realizar cualquier operación.
    //
    // =====================================================

    private void cargarCatalogos(
            Model model
    ) {

        model.addAttribute(
                "medicamentos",
                medicamentoRepository
                        .findByActivoTrueOrderByNombreAsc()
        );

        model.addAttribute(
                "sucursales",
                sucursalRepository
                        .findByActivoTrueOrderByNombreAsc()
        );
    }


    // =====================================================
    // CU-14 - MOSTRAR FORMULARIO DE ENTRADA
    // =====================================================

    @GetMapping("/{id}/entrada")
    public String mostrarEntrada(
            @PathVariable("id") Integer id,
            Model model
    ) {

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );

        model.addAttribute(
                "formulario",
                new InventarioFarmaciaEntradaDTO()
        );

        cargarDatosEntrada(id, model);

        return "admin/inventario-farmacia/entrada";
    }


    // =====================================================
    // CU-14 - REGISTRAR ENTRADA DE MEDICAMENTOS
    // =====================================================
    //
    // La modificación real de las existencias
    // se realiza exclusivamente en el Service.
    //
    // =====================================================

    @PostMapping("/{id}/entrada")
    public String registrarEntrada(
            @PathVariable("id") Integer id,

            @Valid
            @ModelAttribute("formulario")
            InventarioFarmaciaEntradaDTO formulario,

            BindingResult errores,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // VALIDAR PERMISOS
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );

        // -------------------------------------------------
        // VALIDAR FORMULARIO
        // -------------------------------------------------

        if (errores.hasErrors()) {

            cargarDatosEntrada(id, model);

            return "admin/inventario-farmacia/entrada";
        }

        // -------------------------------------------------
        // REGISTRAR ENTRADA
        // -------------------------------------------------

        try {

            inventarioService.registrarEntrada(
                    id,
                    formulario.getCantidad(),
                    formulario.getObservaciones()
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La entrada de medicamentos se registró "
                            + "correctamente."
            );

            return "redirect:/admin/inventario-farmacia";

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        cargarDatosEntrada(id, model);

        return "admin/inventario-farmacia/entrada";
    }


    // =====================================================
    // CU-14 - DATOS PARA FORMULARIO DE ENTRADA
    // =====================================================
    //
    // Envía únicamente datos simples a Thymeleaf.
    // Evita depender de relaciones LAZY en la vista.
    //
    // =====================================================

    private void cargarDatosEntrada(
            Integer idInventario,
            Model model
    ) {

        InventarioFarmacia inventario =
                inventarioRepository.findById(idInventario)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El inventario seleccionado no existe."
                                )
                        );

        model.addAttribute(
                "idInventario",
                inventario.getId()
        );

        model.addAttribute(
                "stockActual",
                inventario.getCantidadDisponible()
        );

        model.addAttribute(
                "activo",
                inventario.getActivo()
        );
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // MOSTRAR FORMULARIO DE AJUSTE
    // =====================================================
    //
    // - Exige permiso administrativo.
    // - Consulta el inventario seleccionado.
    // - Muestra las existencias actuales.
    // - Prepara el formulario de ajuste.
    //
    // No modifica existencias.
    //
    // =====================================================

    @GetMapping("/{id}/ajuste")
    public String mostrarAjuste(
            @PathVariable("id") Integer id,
            Model model
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );

        // -------------------------------------------------
        // 2. PREPARAR FORMULARIO
        // -------------------------------------------------

        model.addAttribute(
                "formulario",
                new InventarioFarmaciaAjusteDTO()
        );

        // -------------------------------------------------
        // 3. CARGAR INVENTARIO
        // -------------------------------------------------

        cargarDatosEntrada(id, model);

        // -------------------------------------------------
        // 4. MOSTRAR VISTA
        // -------------------------------------------------

        return "admin/inventario-farmacia/ajuste";
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // REGISTRAR AJUSTE DE EXISTENCIAS
    // =====================================================
    //
    // El controlador recibe:
    //
    // - Nuevo stock.
    // - Motivo obligatorio.
    //
    // El Service se encarga de:
    //
    // - Validar permisos.
    // - Bloquear el inventario.
    // - Validar el nuevo stock.
    // - Aplicar el ajuste.
    // - Registrar movimiento.
    // - Registrar auditoría.
    //
    // =====================================================

    @PostMapping("/{id}/ajuste")
    public String registrarAjuste(
            @PathVariable("id") Integer id,

            @Valid
            @ModelAttribute("formulario")
            InventarioFarmaciaAjusteDTO formulario,

            BindingResult errores,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );

        // -------------------------------------------------
        // 2. VALIDAR FORMULARIO
        // -------------------------------------------------

        if (errores.hasErrors()) {

            cargarDatosEntrada(id, model);

            return "admin/inventario-farmacia/ajuste";
        }

        // -------------------------------------------------
        // 3. EJECUTAR AJUSTE
        // -------------------------------------------------

        try {

            inventarioService.ajustarExistencias(
                    id,
                    formulario.getNuevoStock(),
                    formulario.getMotivo()
            );

            // ---------------------------------------------
            // MENSAJE DE ÉXITO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El ajuste de inventario se registró "
                            + "correctamente."
            );

            return "redirect:/admin/inventario-farmacia";

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            // ---------------------------------------------
            // ERROR DE VALIDACIÓN DEL BACKEND
            // ---------------------------------------------

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        // -------------------------------------------------
        // 4. RECARGAR INFORMACIÓN DEL INVENTARIO
        // -------------------------------------------------

        cargarDatosEntrada(id, model);

        return "admin/inventario-farmacia/ajuste";
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // DESACTIVAR INVENTARIO DE MEDICAMENTOS
    // =====================================================
    //
    // SEGURIDAD:
    //
    // - Solo permite solicitudes POST.
    // - Requiere permiso medicine-inventory/delete.
    // - El Service bloquea el inventario.
    // - No permite desactivar con existencias.
    // - No elimina registros físicamente.
    // - Registra auditoría.
    //
    // IMPORTANTE:
    //
    // No modifica las cantidades disponibles.
    // No altera los despachos históricos del CU-11.
    //
    // =====================================================

    @PostMapping("/{id}/desactivar")
    public String desactivarInventario(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO ADMINISTRATIVO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/delete"
        );


        // -------------------------------------------------
        // 2. EJECUTAR DESACTIVACIÓN LÓGICA
        // -------------------------------------------------

        try {

            inventarioService.desactivar(id);


            // ---------------------------------------------
            // 3. MENSAJE DE ÉXITO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El inventario ha sido desactivado "
                            + "correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {


            // ---------------------------------------------
            // 4. MOSTRAR ERROR DE VALIDACIÓN
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        // -------------------------------------------------
        // 5. REGRESAR AL LISTADO
        // -------------------------------------------------

        return "redirect:/admin/inventario-farmacia";
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // CONSULTAR HISTORIAL DE MOVIMIENTOS
    // =====================================================
    //
    // Permite al administrador consultar:
    //
    // - Movimiento inicial.
    // - Entradas de inventario.
    // - Ajustes positivos y negativos.
    // - Salidas registradas.
    //
    // SEGURIDAD:
    //
    // - Requiere medicine-inventory/read.
    // - Verifica existencia del inventario.
    // - Aplica paginación.
    // - No modifica existencias.
    //
    // =====================================================

    @GetMapping("/{id}/historial")
    public String mostrarHistorial(
            @PathVariable("id") Integer id,

            @RequestParam(
                    name = "pagina",
                    defaultValue = "0"
            )
            int pagina,

            Model model
    ) {

        // -------------------------------------------------
        // 1. VALIDAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );

        // -------------------------------------------------
        // 2. PREPARAR PAGINACIÓN
        // -------------------------------------------------

        int paginaSegura = Math.max(0, pagina);

        Pageable pageable = PageRequest.of(
                paginaSegura,
                10
        );

        // -------------------------------------------------
        // 3. CONSULTAR HISTORIAL DESDE EL SERVICE
        // -------------------------------------------------

        Page<MovimientoInventarioFarmacia> resultado =
                inventarioService.consultarHistorial(
                        id,
                        pageable
                );

        // -------------------------------------------------
        // 4. ENVIAR MOVIMIENTOS A THYMELEAF
        // -------------------------------------------------

        model.addAttribute(
                "movimientos",
                resultado.getContent()
        );

        model.addAttribute(
                "paginaActual",
                paginaSegura
        );

        model.addAttribute(
                "totalPaginas",
                resultado.getTotalPages()
        );

        model.addAttribute(
                "totalElementos",
                resultado.getTotalElements()
        );

        // -------------------------------------------------
        // 5. CARGAR INFORMACIÓN DEL INVENTARIO
        // -------------------------------------------------

        cargarDatosEntrada(id, model);

        // -------------------------------------------------
        // 6. MOSTRAR VISTA
        // -------------------------------------------------

        return "admin/inventario-farmacia/historial";
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // CONSULTAR RESUMEN MENSUAL DE INVENTARIO
    // =====================================================
    //
    // Permite:
    //
    // - Seleccionar mes y año.
    // - Consultar existencias iniciales.
    // - Consultar entradas y salidas.
    // - Consultar ajustes positivos y negativos.
    // - Consultar existencias finales.
    // - Consultar cantidad de movimientos.
    //
    // SEGURIDAD:
    // - Requiere medicine-inventory/read.
    // - No modifica existencias.
    // - No modifica despachos del CU-11.
    //
    // =====================================================

    @GetMapping("/{id}/resumen-mensual")
    public String mostrarResumenMensual(

            @PathVariable("id") Integer id,

            @RequestParam(
                    name = "mes",
                    required = false
            )
            String mes,

            Model model
    ) {

        // -------------------------------------------------
        // 1. VALIDAR PERMISO ADMINISTRATIVO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );

        // -------------------------------------------------
        // 2. CARGAR INFORMACIÓN DEL INVENTARIO
        // -------------------------------------------------

        cargarDatosEntrada(id, model);

        // -------------------------------------------------
        // 3. DEFINIR MES ACTUAL POR DEFECTO
        // -------------------------------------------------

        YearMonth periodo = YearMonth.now(
                ZoneId.of("America/Guatemala")
        );

        // -------------------------------------------------
        // 4. PROCESAR MES SELECCIONADO
        // -------------------------------------------------

        if (mes != null && !mes.isBlank()) {

            try {

                periodo = YearMonth.parse(
                        mes.trim()
                );

            } catch (DateTimeParseException ex) {

                model.addAttribute(
                        "mesSeleccionado",
                        mes
                );

                model.addAttribute(
                        "errorResumen",
                        "El mes seleccionado no es válido."
                );

                return "admin/inventario-farmacia/resumen-mensual";
            }
        }

        // -------------------------------------------------
        // 5. ENVIAR PERÍODO SELECCIONADO
        // -------------------------------------------------

        model.addAttribute(
                "mesSeleccionado",
                periodo.toString()
        );

        // -------------------------------------------------
        // 6. CONSULTAR RESUMEN DESDE EL SERVICE
        // -------------------------------------------------

        try {

            model.addAttribute(
                    "resumen",
                    inventarioService.obtenerResumenMensual(
                            id,
                            periodo
                    )
            );

        } catch (IllegalStateException ex) {

            model.addAttribute(
                    "errorResumen",
                    ex.getMessage()
            );
        }

        // -------------------------------------------------
        // 7. MOSTRAR REPORTE MENSUAL
        // -------------------------------------------------

        return "admin/inventario-farmacia/resumen-mensual";
    }



}
