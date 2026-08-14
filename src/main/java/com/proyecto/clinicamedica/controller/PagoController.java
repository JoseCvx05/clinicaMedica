package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.pago.PagoFormularioDTO;

import com.proyecto.clinicamedica.service.PagoService;

import com.proyecto.clinicamedica.validator.pago.PagoFormularioValidator;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


/**
 * =========================================================
 * CONTROLLER: PAGO EN LÍNEA
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Responsabilidades:
 *
 * - Mostrar la pantalla de pago.
 * - Generar la idempotency key.
 * - Ejecutar las validaciones del formulario.
 * - Coordinar el procesamiento del pago.
 * - Manejar FA01, FA02 y FA03.
 * - Mostrar el comprobante.
 *
 * La lógica financiera permanece en PagoService.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/paciente/pagos")
public class PagoController {


    // =====================================================
    // VISTAS
    // =====================================================

    private static final String VISTA_PAGO =
            "paciente/pago-consulta";


    private static final String VISTA_CONFIRMACION =
            "paciente/pago-exitoso";


    // =====================================================
    // REDIRECCIÓN CU-03
    // =====================================================
    //
    // Si tu endpoint actual de agendamiento utiliza
    // otra ruta, cambia únicamente esta constante.
    // =====================================================

    private static final String URL_AGENDAR_CITA =
            "/paciente/citas/nueva";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final PagoService pagoService;

    private final PagoFormularioValidator
            pagoFormularioValidator;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PagoController(
            PagoService pagoService,
            PagoFormularioValidator pagoFormularioValidator
    ) {

        this.pagoService =
                pagoService;

        this.pagoFormularioValidator =
                pagoFormularioValidator;
    }


    // =====================================================
    // GET - PANTALLA DE PAGO
    // =====================================================

    @GetMapping("/cita/{idCita}")
    public String mostrarPago(
            @PathVariable Integer idCita,
            Model model
    ) {

        PagoFormularioDTO formulario =
                new PagoFormularioDTO();


        // =================================================
        // RNF-016 - IDEMPOTENCIA
        // =================================================

        formulario.setIdempotencyKey(
                UUID.randomUUID()
        );


        model.addAttribute(
                "formulario",
                formulario
        );


        return cargarPantallaPago(
                idCita,
                model
        );
    }


    // =====================================================
    // POST - PROCESAR PAGO
    // =====================================================

    @PostMapping("/cita/{idCita}")
    public String procesarPago(
            @PathVariable Integer idCita,

            @ModelAttribute("formulario")
            PagoFormularioDTO formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // FA01 - VALIDAR FORMULARIO
        // =================================================

        pagoFormularioValidator.validate(
                formulario,
                bindingResult
        );


        if (bindingResult.hasErrors()) {

            return cargarPantallaPago(
                    idCita,
                    model
            );
        }


        // =================================================
        // PROCESAR CU-04
        // =================================================

        PagoService.ResultadoPago resultado =
                pagoService.procesarPago(
                        idCita,
                        formulario
                );


        // =================================================
        // PAGO APROBADO
        // =================================================

        if (resultado.exitoso()) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            return "redirect:/paciente/pagos/confirmacion/"
                    + resultado.numeroTransaccion();
        }


        // =================================================
        // FA02 - TIEMPO EXPIRADO
        // =================================================

        if (resultado.expirado()) {

            model.addAttribute(
                    "pagoExpirado",
                    true
            );


            model.addAttribute(
                    "mensajePago",
                    resultado.mensaje()
            );


            model.addAttribute(
                    "urlRedireccionExpirado",
                    URL_AGENDAR_CITA
            );


            return VISTA_PAGO;
        }


        // =================================================
        // PAGO EN PROCESAMIENTO
        // =================================================

        if (resultado.estado()
                == PagoService.EstadoResultadoPago.EN_PROCESO) {

            model.addAttribute(
                    "pagoEnProceso",
                    true
            );


            model.addAttribute(
                    "mensajePago",
                    resultado.mensaje()
            );


            /*
             * Se conserva la MISMA idempotency key.
             *
             * No queremos crear un nuevo intento mientras
             * otro pago continúa procesándose.
             */

            return cargarPantallaPago(
                    idCita,
                    model
            );
        }


        // =================================================
        // FA03 - RECHAZO / ERROR
        // =================================================
        //
        // El intento anterior ya terminó.
        //
        // Para permitir un nuevo intento real debemos
        // generar una NUEVA idempotency key.
        // =================================================

        formulario.setIdempotencyKey(
                UUID.randomUUID()
        );


        /*
         * Limpiar datos sensibles antes de devolver
         * nuevamente la vista.
         *
         * No queremos volver a renderizar PAN ni CVV.
         */

        formulario.setNumeroTarjeta(
                null
        );


        formulario.setCvv(
                null
        );


        model.addAttribute(
                "mensajePago",
                resultado.mensaje()
        );


        model.addAttribute(
                "pagoRechazado",
                true
        );


        return cargarPantallaPago(
                idCita,
                model
        );
    }


    // =====================================================
    // GET - COMPROBANTE
    // =====================================================

    @GetMapping("/confirmacion/{numeroTransaccion}")
    public String mostrarConfirmacion(
            @PathVariable String numeroTransaccion,
            Model model
    ) {

        PagoService.ComprobantePago comprobante =
                pagoService
                        .obtenerComprobante(
                                numeroTransaccion
                        );


        model.addAttribute(
                "comprobante",
                comprobante
        );


        return VISTA_CONFIRMACION;
    }


    // =====================================================
    // CARGAR PANTALLA
    // =====================================================

    private String cargarPantallaPago(
            Integer idCita,
            Model model
    ) {
        model.addAttribute(
                "pagoExpirado",
                false
        );


        model.addAttribute(
                "pagoEnProceso",
                false
        );

        try {

            PagoService.ResumenPago resumen =
                    pagoService
                            .obtenerResumen(
                                    idCita
                            );


            model.addAttribute(
                    "resumen",
                    resumen
            );


            return VISTA_PAGO;

        } catch (IllegalStateException ex) {

            // =================================================
            // FA02
            // =================================================

            if (ex.getMessage() != null
                    && ex.getMessage()
                    .startsWith(
                            "El tiempo para confirmar su cita ha expirado."
                    )) {

                model.addAttribute(
                        "pagoExpirado",
                        true
                );


                model.addAttribute(
                        "mensajePago",
                        ex.getMessage()
                );


                model.addAttribute(
                        "urlRedireccionExpirado",
                        URL_AGENDAR_CITA
                );


                return VISTA_PAGO;
            }


            throw ex;
        }
    }
}