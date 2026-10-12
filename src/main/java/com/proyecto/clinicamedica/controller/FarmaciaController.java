
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.SolicitudDespachoFarmaciaDTO;

import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.service.FarmaciaConsultaRecetaService;
import com.proyecto.clinicamedica.service.FarmaciaDetalleRecetaService;
import com.proyecto.clinicamedica.service.FarmaciaDespachoService;
import com.proyecto.clinicamedica.service.FarmaciaNoAdquiridoService;
import com.proyecto.clinicamedica.service.FarmaciaSeguridadService;
import com.proyecto.clinicamedica.service.FarmaciaVistaPreviaMontoService;
import com.proyecto.clinicamedica.service.FarmaciaResumenDespachoService;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import jakarta.validation.Valid;

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
import java.util.Locale;
import java.util.UUID;

/**
 * =========================================================
 * CONTROLLER: FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Funciones:
 *
 * - Mostrar el panel de Farmacia.
 * - Buscar recetas por ID de receta.
 * - Buscar recetas por ID de consulta.
 * - Consultar medicamentos de una receta.
 * - Recibir solicitudes de despacho.
 * - Confirmar entrega de medicamentos.
 * - Registrar cuando el paciente no adquiere.
 * - Mostrar mensajes al farmacéutico.
 *
 * SEGURIDAD:
 *
 * - Rutas protegidas por Spring Security.
 * - Los Services validan usuario y rol.
 * - Se verifica la sucursal autorizada.
 * - No se confía en IDs de sucursal del navegador.
 * - Los formularios POST utilizan CSRF.
 *
 * IMPORTANTE:
 *
 * Este Controller no modifica inventario.
 * No calcula precios.
 * No registra pagos.
 *
 * La lógica de negocio permanece en los Services.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/farmacia")
public class FarmaciaController {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FarmaciaSeguridadService
            seguridadService;

    private final FarmaciaConsultaRecetaService
            consultaRecetaService;

    private final FarmaciaDetalleRecetaService
            detalleRecetaService;

    private final FarmaciaDespachoService
            despachoService;

    private final FarmaciaNoAdquiridoService
            noAdquiridoService;

    private final FarmaciaResumenDespachoService
            resumenDespachoService;

    // =====================================================
// CU-11 - VISTA PREVIA DEL MONTO
// =====================================================

    private final FarmaciaVistaPreviaMontoService
            vistaPreviaMontoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaController(
            FarmaciaSeguridadService seguridadService,
            FarmaciaConsultaRecetaService consultaRecetaService,
            FarmaciaDetalleRecetaService detalleRecetaService,
            FarmaciaDespachoService despachoService,
            FarmaciaNoAdquiridoService noAdquiridoService,
            FarmaciaResumenDespachoService resumenDespachoService,
            FarmaciaVistaPreviaMontoService vistaPreviaMontoService
    ) {

        this.seguridadService =
                seguridadService;

        this.consultaRecetaService =
                consultaRecetaService;

        this.detalleRecetaService =
                detalleRecetaService;

        this.despachoService =
                despachoService;

        this.noAdquiridoService =
                noAdquiridoService;

        this.resumenDespachoService =
                resumenDespachoService;

        this.vistaPreviaMontoService =
                vistaPreviaMontoService;
    }


    // =====================================================
    // PANTALLA PRINCIPAL DE FARMACIA
    // =====================================================


    // =====================================================
    // PANTALLA PRINCIPAL DE FARMACIA
    // =====================================================

    @GetMapping({"", "/"})
    public String mostrarPanel(
            Model model
    ) {

        prepararModeloBase(model);


        // ---------------------------------------------
        // CU-11 - CARGAR RESUMEN DESPUÉS DEL DESPACHO
        // ---------------------------------------------
        //
        // El ID proviene del RedirectAttributes
        // registrado por el controlador después
        // de confirmar el despacho.
        //
        // El servicio comprobará la sucursal
        // del farmacéutico desde PostgreSQL.
        //
        // ---------------------------------------------

        Object idRegistrado =
                model.getAttribute(
                        "idDespachoRegistrado"
                );

        if (idRegistrado instanceof Integer idDespacho) {

            cargarResumenDespacho(
                    idDespacho,
                    model
            );
        }


        return "farmacia/panel";
    }



    // =====================================================
    // BUSCAR RECETAS
    // =====================================================
    //
    // Permite buscar por:
    //
    // RECETA   -> ID de receta médica.
    // CONSULTA -> ID de consulta médica.
    //
    // No recibe ni utiliza una sucursal indicada
    // por el navegador.
    //
    // =====================================================

    @GetMapping("/buscar")
    public String buscarRecetas(

            @RequestParam("tipoBusqueda")
            String tipoBusqueda,

            @RequestParam("valorBusqueda")
            String valorBusqueda,

            Model model
    ) {

        // -------------------------------------------------
        // PREPARAR PANEL Y VALIDAR FARMACÉUTICO
        // -------------------------------------------------

        prepararModeloBase(model);

        // -------------------------------------------------
        // CONSERVAR CRITERIOS EN LA VISTA
        // -------------------------------------------------

        model.addAttribute(
                "tipoBusqueda",
                tipoBusqueda
        );

        model.addAttribute(
                "valorBusqueda",
                valorBusqueda
        );

        model.addAttribute(
                "busquedaRealizada",
                true
        );


        try {

            // ---------------------------------------------
            // VALIDAR IDENTIFICADOR
            // ---------------------------------------------

            Integer identificador =
                    convertirIdentificador(valorBusqueda);


            // ---------------------------------------------
            // VALIDAR TIPO DE BÚSQUEDA
            // ---------------------------------------------

            if (tipoBusqueda == null
                    || tipoBusqueda.isBlank()) {

                throw new IllegalArgumentException(
                        "Debe seleccionar un tipo de búsqueda."
                );
            }


            String tipoNormalizado =
                    tipoBusqueda.trim()
                            .toUpperCase(Locale.ROOT);


            // ---------------------------------------------
            // CONSULTAR RECETAS EN POSTGRESQL
            // ---------------------------------------------

            List<FarmaciaConsultaRecetaService.ResumenReceta>
                    resultados;


            switch (tipoNormalizado) {

                case "RECETA" ->

                        resultados =
                                consultaRecetaService
                                        .buscarPorIdReceta(
                                                identificador
                                        );

                case "CONSULTA" ->

                        resultados =
                                consultaRecetaService
                                        .buscarPorIdConsulta(
                                                identificador
                                        );

                default -> throw new IllegalArgumentException(
                        "El tipo de búsqueda seleccionado no es válido."
                );
            }


            // ---------------------------------------------
            // ENVIAR RESULTADOS A LA VISTA
            // ---------------------------------------------

            model.addAttribute(
                    "resultadosRecetas",
                    resultados
            );


            // ---------------------------------------------
            // NINGUNA RECETA ENCONTRADA
            // ---------------------------------------------

            if (resultados.isEmpty()) {

                model.addAttribute(
                        "mensajeInformacion",
                        "No se encontraron recetas médicas "
                                + "activas para el criterio indicado "
                                + "en su sucursal."
                );

                return "farmacia/panel";
            }


            // ---------------------------------------------
            // SI EXISTE UNA SOLA RECETA
            // MOSTRAR AUTOMÁTICAMENTE SUS MEDICAMENTOS
            // ---------------------------------------------

            if (resultados.size() == 1) {

                seleccionarDetalleReceta(
                        resultados.get(0).idReceta(),
                        model
                );
            }


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        return "farmacia/panel";
    }


    // =====================================================
    // CONSULTAR UNA RECETA SELECCIONADA
    // =====================================================
    //
    // Esta ruta permite seleccionar una receta
    // cuando la búsqueda devuelve varios resultados.
    //
    // El Service vuelve a comprobar la sucursal.
    //
    // =====================================================

    @GetMapping("/receta/{idReceta}")
    public String seleccionarReceta(

            @PathVariable
            Integer idReceta,

            Model model
    ) {

        // -------------------------------------------------
        // PREPARAR PANEL Y VALIDAR FARMACÉUTICO
        // -------------------------------------------------

        prepararModeloBase(model);


        try {

            // ---------------------------------------------
            // BUSCAR RECETA AUTORIZADA
            // ---------------------------------------------

            List<FarmaciaConsultaRecetaService.ResumenReceta>
                    resultados =
                    consultaRecetaService
                            .buscarPorIdReceta(
                                    idReceta
                            );


            // ---------------------------------------------
            // VALIDAR EXISTENCIA Y ACCESO
            // ---------------------------------------------

            if (resultados.isEmpty()) {

                throw new IllegalArgumentException(
                        "No se encontró una receta médica "
                                + "activa para esta sucursal."
                );
            }


            // ---------------------------------------------
            // MOSTRAR RESULTADO
            // ---------------------------------------------

            model.addAttribute(
                    "resultadosRecetas",
                    resultados
            );


            model.addAttribute(
                    "busquedaRealizada",
                    true
            );


            // ---------------------------------------------
            // CONSULTAR MEDICAMENTOS DE LA RECETA
            // ---------------------------------------------

            seleccionarDetalleReceta(
                    idReceta,
                    model
            );


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        return "farmacia/panel";
    }


    // =====================================================
    // PREPARAR DATOS BASE DEL PANEL
    // =====================================================
    //
    // Se reutiliza en las tres rutas GET.
    //
    // Así evitamos repetir lógica y mantenemos
    // los mismos datos del usuario autenticado.
    //
    // =====================================================

    private void prepararModeloBase(
            Model model
    ) {

        // -------------------------------------------------
        // VALIDAR FARMACÉUTICO AUTORIZADO
        // -------------------------------------------------

        Usuario farmaceutico =
                seguridadService
                        .obtenerFarmaceuticoAutorizado();


        // -------------------------------------------------
        // NOMBRE DEL FARMACÉUTICO
        // -------------------------------------------------

        model.addAttribute(
                "nombreFarmaceutico",
                farmaceutico.getNombreUsuario()
        );


        // -------------------------------------------------
        // SUCURSAL AUTORIZADA
        // -------------------------------------------------

        model.addAttribute(
                "idSucursal",
                farmaceutico
                        .getSucursal()
                        .getId()
        );


        // -------------------------------------------------
        // DTO DE DESPACHO
        // -------------------------------------------------

        if (!model.containsAttribute("solicitud")) {

            SolicitudDespachoFarmaciaDTO solicitud =
                    new SolicitudDespachoFarmaciaDTO();

            solicitud.setClaveOperacion(
                    UUID.randomUUID()
            );

            model.addAttribute(
                    "solicitud",
                    solicitud
            );
        }


        // -------------------------------------------------
        // CLAVE DEL FLUJO NO ADQUIRIDO
        // -------------------------------------------------

        if (!model.containsAttribute(
                "claveOperacionNoAdquirido"
        )) {

            model.addAttribute(
                    "claveOperacionNoAdquirido",
                    UUID.randomUUID()
            );
        }
    }


    // =====================================================
    // CONVERTIR IDENTIFICADOR DE BÚSQUEDA
    // =====================================================

    private Integer convertirIdentificador(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe ingresar un número de receta o consulta."
            );
        }


        try {

            Integer id =
                    Integer.valueOf(
                            valor.trim()
                    );


            if (id <= 0) {

                throw new IllegalArgumentException(
                        "El identificador debe ser mayor a cero."
                );
            }


            return id;

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Debe ingresar un identificador numérico válido."
            );
        }
    }


    // =====================================================
    // CARGAR DETALLE DE UNA RECETA
    // =====================================================
    //
    // Reutilizamos FarmaciaDetalleRecetaService.
    //
    // Este método no registra despachos,
    // no reserva existencias ni descuenta stock.
    //
    // =====================================================

    private void seleccionarDetalleReceta(
            Integer idReceta,
            Model model
    ) {

        // -------------------------------------------------
        // CONSULTAR DETALLE AUTORIZADO
        // -------------------------------------------------

        FarmaciaDetalleRecetaService.DetalleReceta detalle =
                detalleRecetaService
                        .consultarDetalle(
                                idReceta
                        );


        // -------------------------------------------------
        // ENVIAR DATOS AL HTML
        // -------------------------------------------------

        model.addAttribute(
                "recetaSeleccionada",
                detalle
        );


        // -------------------------------------------------
        // PRESELECCIONAR RECETA EN EL DTO
        // -------------------------------------------------

        SolicitudDespachoFarmaciaDTO solicitud =
                (SolicitudDespachoFarmaciaDTO)
                        model.getAttribute(
                                "solicitud"
                        );


        if (solicitud != null) {

            solicitud.setIdReceta(
                    detalle.idReceta()
            );
        }
    }


    // =====================================================
    // CU-11 - VISTA PREVIA DEL MONTO
    // =====================================================
    //
    // Calcula el monto total antes de confirmar.
    //
    // SEGURIDAD:
    //
    // - Recibe los datos mediante Spring MVC.
    // - Ejecuta las validaciones del backend.
    // - Consulta precios desde PostgreSQL.
    // - No registra despachos.
    // - No descuenta inventario.
    // - No envía correos.
    //
    // El formulario POST está protegido por CSRF.
    //
    // =====================================================

    @PostMapping("/vista-previa")
    public ResponseEntity<Map<String, String>>
    calcularVistaPrevia(

            @Valid
            @ModelAttribute("solicitud")
            SolicitudDespachoFarmaciaDTO solicitud,

            BindingResult bindingResult
    ) {

        // -------------------------------------------------
        // 1. VALIDAR EL DTO
        // -------------------------------------------------

        if (bindingResult.hasErrors()) {

            String mensaje =
                    bindingResult
                            .getAllErrors()
                            .get(0)
                            .getDefaultMessage();

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            mensaje == null
                                    ? "La solicitud no es válida."
                                    : mensaje
                    )
            );
        }


        // -------------------------------------------------
        // 2. CALCULAR EN EL BACKEND
        // -------------------------------------------------

        try {

            BigDecimal montoTotal =
                    vistaPreviaMontoService
                            .calcularVistaPrevia(
                                    solicitud
                            );


            // ---------------------------------------------
            // 3. DEVOLVER EL MONTO CALCULADO
            // ---------------------------------------------

            return ResponseEntity.ok(
                    Map.of(
                            "total",
                            montoTotal.toPlainString()
                    )
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            // ---------------------------------------------
            // VALIDACIONES DE NEGOCIO RECHAZADAS
            // ---------------------------------------------

            String mensaje =
                    ex.getMessage();

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            mensaje == null
                                    ? "No se pudo calcular el monto."
                                    : mensaje
                    )
            );
        }
    }



    // =====================================================
    // CONFIRMAR DESPACHO DE MEDICAMENTOS
    // =====================================================

    @PostMapping("/confirmar")
    public String confirmarDespacho(

            @Valid
            @ModelAttribute("solicitud")
            SolicitudDespachoFarmaciaDTO solicitud,

            BindingResult bindingResult,

            RedirectAttributes redirectAttributes
    ) {

        // -------------------------------------------------
        // VALIDACIONES DEL DTO
        // -------------------------------------------------

        if (bindingResult.hasErrors()) {

            String mensaje =
                    bindingResult
                            .getAllErrors()
                            .get(0)
                            .getDefaultMessage();


            redirectAttributes.addFlashAttribute(
                    "mensajeError",

                    mensaje == null
                            ? "La solicitud de despacho no es válida."
                            : mensaje
            );


            return "redirect:/interno/farmacia";
        }


        try {

            // ---------------------------------------------
            // EJECUTAR DESPACHO TRANSACCIONAL
            // ---------------------------------------------

            FarmaciaDespachoService.ResultadoDespacho resultado =
                    despachoService
                            .confirmarDespacho(
                                    solicitud
                            );


            // ---------------------------------------------
            // OPERACIÓN REPETIDA
            // ---------------------------------------------

            if (resultado.operacionRepetida()) {

                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        "Este despacho ya estaba registrado. "
                                + "No se descontó inventario nuevamente."
                );

            } else {

                // -----------------------------------------
                // DESPACHO EXITOSO
                // -----------------------------------------

                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        "Despacho registrado exitosamente. "
                                + resultado.medicamentosDespachados()
                                + " medicamento(s) despachado(s). "
                                + "Total: Q"
                                + resultado.montoTotal()
                );
            }


            // ---------------------------------------------
            // REFERENCIA DEL DESPACHO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "idDespachoRegistrado",
                    resultado.idDespacho()
            );


            // ---------------------------------------------
            // ALERTAS DE STOCK MÍNIMO
            // ---------------------------------------------

            if (!resultado
                    .alertasStockMinimo()
                    .isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "alertasStockMinimo",
                        resultado.alertasStockMinimo()
                );
            }


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        return "redirect:/interno/farmacia";
    }


    // =====================================================
    // FA03 - PACIENTE NO ADQUIERE MEDICAMENTOS
    // =====================================================

    @PostMapping("/no-adquirido")
    public String registrarNoAdquirido(

            @RequestParam("idReceta")
            Integer idReceta,

            @RequestParam("claveOperacion")
            UUID claveOperacion,

            @RequestParam(
                    value = "observaciones",
                    required = false
            )
            String observaciones,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // ---------------------------------------------
            // REGISTRAR DECISIÓN DEL PACIENTE
            // ---------------------------------------------

            FarmaciaNoAdquiridoService.ResultadoNoAdquirido resultado =
                    noAdquiridoService
                            .registrarNoAdquirido(
                                    idReceta,
                                    claveOperacion,
                                    observaciones
                            );


            // ---------------------------------------------
            // OPERACIÓN REPETIDA
            // ---------------------------------------------

            if (resultado.operacionRepetida()) {

                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        "Esta decisión del paciente "
                                + "ya estaba registrada."
                );

            } else {

                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        "Se registró correctamente que "
                                + "el paciente no adquirió "
                                + "los medicamentos. "
                                + "No se modificó el inventario."
                );
            }


            // ---------------------------------------------
            // REFERENCIA DEL REGISTRO
            // ---------------------------------------------

            redirectAttributes.addFlashAttribute(
                    "idDespachoRegistrado",
                    resultado.idDespacho()
            );


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        return "redirect:/interno/farmacia";
    }


    // =====================================================
    // CU-11 - CONSULTAR UN DESPACHO YA REGISTRADO
    // =====================================================
    //
    // Permite visualizar un despacho anterior
    // sin volver a ejecutar su confirmación.
    //
    // No modifica inventario ni envía correos.
    //
    // =====================================================

    @GetMapping("/despacho/{idDespacho}")
    public String consultarResumenDespacho(

            @PathVariable
            Integer idDespacho,

            Model model
    ) {

        prepararModeloBase(model);

        cargarResumenDespacho(
                idDespacho,
                model
        );

        return "farmacia/panel";
    }


    // =====================================================
    // CU-11 - CARGAR RESUMEN DESDE POSTGRESQL
    // =====================================================
    //
    // Reutilizado por:
    //
    // - Confirmación exitosa del despacho.
    // - Consulta de despachos anteriores.
    //
    // La seguridad de rol y sucursal
    // permanece en el backend.
    //
    // =====================================================

    private void cargarResumenDespacho(

            Integer idDespacho,

            Model model
    ) {

        try {

            FarmaciaResumenDespachoService.ResumenDespacho
                    resumen =
                    resumenDespachoService
                            .consultarResumen(
                                    idDespacho
                            );


            model.addAttribute(
                    "resumenDespacho",
                    resumen
            );


            model.addAttribute(
                    "idDespachoRegistrado",
                    resumen.idDespacho()
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }
    }

}
