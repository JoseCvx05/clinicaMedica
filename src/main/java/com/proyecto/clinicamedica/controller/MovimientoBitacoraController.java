
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.MovimientoBitacoraFormularioDTO;
import com.proyecto.clinicamedica.dto.MovimientoBitacoraListadoDTO;

import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;

import com.proyecto.clinicamedica.service.MovimientoBitacoraCatalogoService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraConsultaService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraDetalleConsultaService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraRegistroService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraSeguridadService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraPrevisualizacionService;
import com.proyecto.clinicamedica.service.MovimientoBitacoraEstadoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * CU-15 - CONTROLLER: BITÁCORA DE INVENTARIO
 * =========================================================
 *
 * Funcionalidades:
 *
 * 1. Listar movimientos.
 * 2. Aplicar filtros de búsqueda.
 * 3. Ver detalle de movimientos.
 * 4. Mostrar formulario Nuevo Movimiento.
 * 5. Registrar movimiento manual.
 * 6. Previsualizar stock.
 * 7. Activar / Desactivar movimientos.
 *
 * SEGURIDAD:
 *
 * - Rutas protegidas mediante SecurityConfig.
 * - Validación de rol y sucursal en los Services.
 * - Cambio de estado únicamente mediante POST.
 * - Protección CSRF.
 *
 * IMPORTANTE:
 *
 * - No modifica inventario directamente.
 * - No elimina movimientos históricos.
 * - No modifica los despachos del CU-11.
 * - Conserva las operaciones del CU-14.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/bitacora-inventario")
public class MovimientoBitacoraController {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MovimientoBitacoraConsultaService
            consultaService;

    private final MovimientoBitacoraDetalleConsultaService
            detalleConsultaService;

    private final MovimientoBitacoraCatalogoService
            catalogoService;

    private final MovimientoBitacoraRegistroService
            registroService;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    private final MovimientoBitacoraPrevisualizacionService
            previsualizacionService;

    private final MovimientoBitacoraEstadoService
            estadoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraController(
            MovimientoBitacoraConsultaService consultaService,
            MovimientoBitacoraDetalleConsultaService detalleConsultaService,
            MovimientoBitacoraCatalogoService catalogoService,
            MovimientoBitacoraRegistroService registroService,
            MovimientoBitacoraSeguridadService seguridadService,
            MovimientoBitacoraPrevisualizacionService previsualizacionService,
            MovimientoBitacoraEstadoService estadoService
    ) {

        this.consultaService = consultaService;

        this.detalleConsultaService = detalleConsultaService;

        this.catalogoService = catalogoService;

        this.registroService = registroService;

        this.seguridadService = seguridadService;

        this.previsualizacionService = previsualizacionService;

        this.estadoService = estadoService;
    }

    // =====================================================
    // FLUJO NORMAL - LISTAR MOVIMIENTOS
    // =====================================================

    @GetMapping
    public String listarMovimientos(

            @RequestParam(required = false)
            Integer idMedicamento,

            @RequestParam(required = false)
            Integer idSucursal,

            @RequestParam(required = false)
            Integer codigoTipo,

            @RequestParam(required = false)
            String referencia,

            @RequestParam(required = false)
            Integer idUsuario,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            @RequestParam(defaultValue = "0")
            int pagina,

            Model model
    ) {

        Page<MovimientoBitacoraListadoDTO> paginaMovimientos;

        try {

            paginaMovimientos =
                    consultaService.buscarMovimientosListado(
                            idMedicamento,
                            idSucursal,
                            codigoTipo,
                            referencia,
                            idUsuario,
                            fechaInicio,
                            fechaFin,
                            pagina
                    );

        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "errorBusqueda",
                    ex.getMessage()
            );

            paginaMovimientos =
                    Page.empty(PageRequest.of(0, 20));
        }

        // =================================================
        // RESULTADOS
        // =================================================

        model.addAttribute(
                "paginaMovimientos",
                paginaMovimientos
        );

        model.addAttribute(
                "movimientos",
                paginaMovimientos.getContent()
        );

        model.addAttribute(
                "tiposMovimiento",
                TipoMovimientoBitacora.values()
        );


        // =================================================
        // CU-15 - CATÁLOGOS PARA FILTROS DEL HISTORIAL
        // =================================================
        //
        // Incluye registros históricos.
        // Respeta la sucursal autorizada del usuario.
        // No modifica las consultas existentes.
        // =================================================

        model.addAttribute(
                "medicamentosFiltro",
                catalogoService.listarMedicamentosConMovimientos()
        );

        model.addAttribute(
                "sucursalesFiltro",
                catalogoService.listarSucursalesConMovimientos()
        );

        model.addAttribute(
                "usuariosFiltro",
                catalogoService.listarUsuariosConMovimientos()
        );


        // =================================================
        // CONSERVAR FILTROS
        // =================================================

        model.addAttribute("idMedicamento", idMedicamento);
        model.addAttribute("idSucursal", idSucursal);
        model.addAttribute("codigoTipo", codigoTipo);
        model.addAttribute("referencia", referencia);
        model.addAttribute("idUsuario", idUsuario);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        // =================================================
        // PAGINACIÓN
        // =================================================

        model.addAttribute(
                "paginaActual",
                paginaMovimientos.getNumber()
        );

        model.addAttribute(
                "totalPaginas",
                paginaMovimientos.getTotalPages()
        );

        model.addAttribute(
                "totalRegistros",
                paginaMovimientos.getTotalElements()
        );

        return "interno/bitacora-inventario/listado";
    }

    // =====================================================
    // FLUJO NORMAL - VER DETALLE
    // =====================================================

    @GetMapping("/detalle/{idMovimiento}")
    public String verDetalle(

            @PathVariable
            Long idMovimiento,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            MovimientoBitacoraDetalleConsultaService.DetalleMovimiento
                    detalle =
                    detalleConsultaService.obtenerDetalle(
                            idMovimiento
                    );

            model.addAttribute(
                    "detalle",
                    detalle
            );

            return "interno/bitacora-inventario/detalle";

        } catch (IllegalArgumentException | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );

            return "redirect:/interno/bitacora-inventario";
        }
    }

    // =====================================================
    // NUEVO MOVIMIENTO - MOSTRAR FORMULARIO
    // =====================================================

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(
            Model model
    ) {

        MovimientoBitacoraFormularioDTO formulario =
                new MovimientoBitacoraFormularioDTO();

        Integer sucursalRestringida =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        if (sucursalRestringida != null) {

            formulario.setIdSucursal(
                    sucursalRestringida
            );
        }

        model.addAttribute(
                "formulario",
                formulario
        );

        cargarCatalogosFormulario(model);

        return "interno/bitacora-inventario/nuevo";
    }

    // =====================================================
    // CU-15 - PREVISUALIZAR STOCK
    // =====================================================
    //
    // Solo lectura:
    // No actualiza inventarios.
    // No registra movimientos.
    //
    // =====================================================

    @GetMapping("/previsualizar")
    @ResponseBody
    public ResponseEntity<?> previsualizarMovimiento(

            @RequestParam(required = false)
            Integer idMedicamento,

            @RequestParam(required = false)
            Integer idSucursal,

            @RequestParam(required = false)
            Integer codigoTipo,

            @RequestParam(required = false)
            Integer cantidad
    ) {

        try {

            MovimientoBitacoraPrevisualizacionService.VistaStock
                    resultado =
                    previsualizacionService.previsualizar(
                            idMedicamento,
                            idSucursal,
                            codigoTipo,
                            cantidad
                    );

            return ResponseEntity.ok(
                    resultado
            );

        } catch (IllegalArgumentException | IllegalStateException ex) {

            String mensaje =
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "No fue posible consultar el inventario.";

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "mensaje",
                            mensaje
                    )
            );
        }
    }

    // =====================================================
    // NUEVO MOVIMIENTO - GUARDAR
    // =====================================================

    @PostMapping("/nuevo")
    public String registrarMovimiento(

            @ModelAttribute("formulario")
            MovimientoBitacoraFormularioDTO formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDAR ERRORES DE CONVERSIÓN
        // =================================================

        if (bindingResult.hasErrors()) {

            String mensaje =
                    "Verifique los datos ingresados en el formulario.";

            if (bindingResult.hasFieldErrors("cantidad")) {

                mensaje =
                        "La cantidad debe ser un número entero positivo.";
            }

            model.addAttribute(
                    "errorFormulario",
                    mensaje
            );

            cargarCatalogosFormulario(model);

            return "interno/bitacora-inventario/nuevo";
        }

        // =================================================
        // REGISTRAR MOVIMIENTO TRANSACCIONAL
        // =================================================

        try {

            MovimientoBitacoraRegistroService.ResultadoMovimiento
                    resultado =
                    registroService.registrarMovimiento(
                            formulario
                    );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    resultado.mensaje()
            );

            if (resultado.alertaStockMinimo() != null) {

                redirectAttributes.addFlashAttribute(
                        "alertaStockMinimo",
                        resultado.alertaStockMinimo()
                );
            }

            return "redirect:/interno/bitacora-inventario";

        } catch (IllegalArgumentException | IllegalStateException ex) {

            model.addAttribute(
                    "errorFormulario",
                    ex.getMessage()
            );

            cargarCatalogosFormulario(model);

            return "interno/bitacora-inventario/nuevo";
        }
    }

    // =====================================================
    // CU-15 - ACTIVAR / DESACTIVAR MOVIMIENTO
    // =====================================================
    //
    // NUEVA FUNCIONALIDAD
    //
    // POST /interno/bitacora-inventario/estado/{id}
    //
    // activar = true  -> Activar
    // activar = false -> Desactivar
    //
    // SEGURIDAD:
    //
    // - Exclusivamente POST.
    // - CSRF validado por Spring Security.
    // - Usuario autorizado verificado en Service.
    // - Sucursal obtenida de PostgreSQL.
    //
    // IMPORTANTE:
    //
    // - No modifica cantidades.
    // - No elimina movimientos.
    // - No revierte despachos.
    // - Registra auditoría.
    //
    // =====================================================

    @PostMapping("/estado/{idMovimiento}")
    public String cambiarEstadoMovimiento(

            @PathVariable
            Long idMovimiento,

            @RequestParam("activar")
            boolean activar,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // 1. CAMBIAR ESTADO DESDE EL SERVICE
            // =============================================

            MovimientoBitacoraEstadoService.ResultadoEstado
                    resultado =
                    estadoService.cambiarEstado(
                            idMovimiento,
                            activar
                    );

            // =============================================
            // 2. MENSAJE DE CONFIRMACIÓN
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    resultado.mensaje()
            );

        } catch (IllegalArgumentException | IllegalStateException ex) {

            // =============================================
            // 3. ERRORES DE NEGOCIO
            // =============================================
            //
            // No capturamos errores de autorización.
            // Spring Security debe rechazarlos.
            //
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }

        // =================================================
        // 4. REGRESAR AL LISTADO
        // =================================================

        return "redirect:/interno/bitacora-inventario";
    }

    // =====================================================
    // CARGAR CATÁLOGOS DEL FORMULARIO
    // =====================================================

    private void cargarCatalogosFormulario(
            Model model
    ) {

        // =================================================
        // MEDICAMENTOS ACTIVOS
        // =================================================

        model.addAttribute(
                "medicamentos",
                catalogoService.listarMedicamentosActivos()
        );

        // =================================================
        // SUCURSALES AUTORIZADAS
        // =================================================

        model.addAttribute(
                "sucursales",
                catalogoService.listarSucursalesAutorizadas()
        );

        // =================================================
        // TIPOS PERMITIDOS SEGÚN ROL
        // =================================================

        Integer sucursalRestringida =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        List<TipoMovimientoBitacora> tiposPermitidos;

        if (sucursalRestringida == null) {

            // Administrador: ajustes de inventario.

            tiposPermitidos = List.of(
                    TipoMovimientoBitacora.AJUSTE_POSITIVO,
                    TipoMovimientoBitacora.AJUSTE_NEGATIVO
            );

        } else {

            // Farmacéutico: operaciones manuales.

            tiposPermitidos = List.of(
                    TipoMovimientoBitacora.COMPRA,
                    TipoMovimientoBitacora.DEVOLUCION,
                    TipoMovimientoBitacora.VENTA,
                    TipoMovimientoBitacora.RECLAMO
            );
        }

        model.addAttribute(
                "tiposPermitidos",
                tiposPermitidos
        );

        model.addAttribute(
                "sucursalRestringida",
                sucursalRestringida
        );
    }
}
