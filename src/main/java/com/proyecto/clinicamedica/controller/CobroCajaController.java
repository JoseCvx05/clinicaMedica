package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.caja.CajaBusquedaDTO;
import com.proyecto.clinicamedica.dto.caja.CobroCajaDTO;

import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import com.proyecto.clinicamedica.service.CobroCajaPagoService;
import com.proyecto.clinicamedica.service.CobroCajaService;
import com.proyecto.clinicamedica.service.ComprobanteCajaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


/**
 * =========================================================
 * CONTROLADOR: COBRO DE CONSULTA EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Responsabilidades:
 *
 * - Mostrar el módulo de Caja.
 * - Recibir criterios de búsqueda.
 * - Mostrar citas Pendiente de pago.
 * - Recibir la solicitud de cobro.
 * - Redirigir al comprobante.
 * - Permitir consultar comprobantes anteriores.
 *
 * No contiene reglas de negocio.
 *
 * Utiliza patrón:
 *
 * POST -> REDIRECT -> GET
 *
 * para evitar reenvío accidental de formularios.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/caja")
public class CobroCajaController {


    // =====================================================
    // VISTAS
    // =====================================================

    private static final String VISTA_CAJA =
            "caja/caja";

    private static final String VISTA_COMPROBANTE =
            "caja/comprobante";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CobroCajaService
            cobroCajaService;

    private final CobroCajaPagoService
            cobroCajaPagoService;

    private final ComprobanteCajaService
            comprobanteCajaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CobroCajaController(

            CobroCajaService cobroCajaService,

            CobroCajaPagoService cobroCajaPagoService,

            ComprobanteCajaService comprobanteCajaService
    ) {

        this.cobroCajaService =
                cobroCajaService;

        this.cobroCajaPagoService =
                cobroCajaPagoService;

        this.comprobanteCajaService =
                comprobanteCajaService;
    }


    // =====================================================
    // PANTALLA PRINCIPAL DE CAJA
    // =====================================================

    @GetMapping
    public String mostrarCaja(
            Model model
    ) {

        if (!model.containsAttribute(
                "busqueda"
        )) {

            model.addAttribute(
                    "busqueda",
                    new CajaBusquedaDTO()
            );
        }


        return VISTA_CAJA;
    }


    // =====================================================
    // BUSCAR CITA
    // =====================================================

    @PostMapping("/buscar")
    public String buscar(

            @ModelAttribute("busqueda")
            CajaBusquedaDTO busqueda,

            RedirectAttributes redirectAttributes
    ) {

        CobroCajaService.ResultadoBusquedaCaja resultado =
                cobroCajaService
                        .buscar(
                                busqueda
                        );


        // =================================================
        // CONSERVAR CRITERIO
        // =================================================

        redirectAttributes.addFlashAttribute(
                "busqueda",
                busqueda
        );


        // =================================================
        // CITA ENCONTRADA
        // =================================================

        if (resultado.cita() != null) {

            return "redirect:/interno/caja/cita/"
                    + resultado.cita()
                    .numeroCita();
        }


        // =================================================
        // ERROR / SIN RESULTADOS
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeError",
                resultado.mensaje()
        );


        return "redirect:/interno/caja";
    }


    // =====================================================
    // MOSTRAR CITA PARA COBRO
    // =====================================================

    @GetMapping("/cita/{idCita}")
    public String mostrarCita(

            @PathVariable("idCita")
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        CajaBusquedaDTO busqueda =
                new CajaBusquedaDTO();


        busqueda.setTipoBusqueda(
                CajaBusquedaDTO.TIPO_CITA
        );


        busqueda.setValorBusqueda(
                String.valueOf(
                        idCita
                )
        );


// =================================================
// FORMULARIO DEL BUSCADOR
// =================================================

        model.addAttribute(
                "busqueda",
                busqueda
        );


        CobroCajaService.ResultadoBusquedaCaja resultado =
                cobroCajaService
                        .buscar(
                                busqueda
                        );


        // =================================================
        // YA NO ESTÁ PENDIENTE
        // =================================================

        if (resultado.cita() == null) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    resultado.mensaje()
            );


            return "redirect:/interno/caja";
        }


        // =================================================
        // DETALLE
        // =================================================

        model.addAttribute(
                "resultado",
                resultado
        );


        // =================================================
        // FORMULARIO DE COBRO
        // =================================================

        CobroCajaDTO cobro =
                new CobroCajaDTO();


        cobro.setIdCita(
                idCita
        );


        /*
         * La llave se genera en servidor.
         *
         * Un doble clic sobre Registrar Pago enviará
         * el mismo UUID.
         */
        cobro.setIdempotencyKey(
                UUID.randomUUID()
        );


        model.addAttribute(
                "cobro",
                cobro
        );


        model.addAttribute(
                "tiposCobro",
                TipoCobroCaja.values()
        );


        return VISTA_CAJA;
    }


    // =====================================================
    // REGISTRAR PAGO
    // =====================================================

    @PostMapping("/cobrar")
    public String cobrar(

            @ModelAttribute("cobro")
            CobroCajaDTO formulario,

            RedirectAttributes redirectAttributes
    ) {

        Integer idCita =
                formulario.getIdCita();


        try {

            CobroCajaPagoService.ResultadoCobroCaja resultado =
                    cobroCajaPagoService
                            .registrarPago(
                                    formulario
                            );


            // =============================================
            // PAGO APROBADO
            // =============================================

            if (resultado.exitoso()) {

                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        resultado.mensaje()
                );


                if (resultado.detalle() != null) {

                    redirectAttributes.addFlashAttribute(
                            "detallePago",
                            resultado.detalle()
                    );
                }


                return "redirect:/interno/caja/comprobante/"
                        + resultado.numeroTransaccion();
            }


            // =============================================
            // RECHAZO / ERROR
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    resultado.mensaje()
            );


            /*
             * Al volver mediante GET se genera una nueva
             * idempotency key.
             *
             * Esto permite que FA04 pueda intentar:
             *
             * - otra tarjeta;
             * - efectivo.
             */
            if (idCita != null
                    && idCita > 0) {

                return "redirect:/interno/caja/cita/"
                        + idCita;
            }


            return "redirect:/interno/caja";


        } catch (RuntimeException ex) {

            /*
             * No exponemos detalles técnicos al Cajero.
             */

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No fue posible registrar el pago. "
                            + "Verifique el estado de la cita "
                            + "e intente nuevamente."
            );


            if (idCita != null
                    && idCita > 0) {

                return "redirect:/interno/caja/cita/"
                        + idCita;
            }


            return "redirect:/interno/caja";
        }
    }


    // =====================================================
    // COMPROBANTE POR NÚMERO DE TRANSACCIÓN
    // =====================================================

    @GetMapping("/comprobante/{numeroTransaccion}")
    public String mostrarComprobante(

            @PathVariable("numeroTransaccion")
            String numeroTransaccion,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ComprobanteCajaService.ComprobanteCaja comprobante =
                    comprobanteCajaService
                            .obtener(
                                    numeroTransaccion
                            );


            model.addAttribute(
                    "comprobante",
                    comprobante
            );


            return VISTA_COMPROBANTE;


        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/interno/caja";
        }
    }


    // =====================================================
    // RNF-033 - REIMPRIMIR COMPROBANTE
    // =====================================================

    @GetMapping("/comprobante")
    public String buscarComprobante(

            @RequestParam(
                    value = "numeroTransaccion",
                    required = false
            )
            String numeroTransaccion,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        String numero =
                numeroTransaccion == null
                        ? ""
                        : numeroTransaccion.trim();


        if (numero.isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Debe ingresar el número de transacción "
                            + "del comprobante."
            );


            return "redirect:/interno/caja";
        }


        try {

            ComprobanteCajaService.ComprobanteCaja comprobante =
                    comprobanteCajaService
                            .obtener(
                                    numero
                            );


            model.addAttribute(
                    "comprobante",
                    comprobante
            );


            return VISTA_COMPROBANTE;


        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/interno/caja";
        }
    }
}