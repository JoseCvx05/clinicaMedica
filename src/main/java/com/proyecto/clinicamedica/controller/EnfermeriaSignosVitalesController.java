package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.signos.RegistroSignosVitalesDTO;

import com.proyecto.clinicamedica.service.PanelEnfermeriaService;
import com.proyecto.clinicamedica.service.SignosVitalesService;

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


/**
 * =========================================================
 * CONTROLADOR: ENFERMERÍA - SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Responsabilidades:
 *
 * - Mostrar el panel de Enfermería.
 * - Llamar pacientes.
 * - Coordinar el anuncio TTS.
 * - Mostrar formulario de signos vitales.
 * - Recibir el registro de signos vitales.
 *
 * Las reglas clínicas permanecen en la capa Service.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/enfermeria")
public class EnfermeriaSignosVitalesController {


    // =====================================================
    // VISTAS
    // =====================================================

    private static final String VISTA_PANEL =
            "enfermeria/panel";

    private static final String VISTA_SIGNOS =
            "enfermeria/signos-vitales";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final PanelEnfermeriaService
            panelEnfermeriaService;

    private final SignosVitalesService
            signosVitalesService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EnfermeriaSignosVitalesController(

            PanelEnfermeriaService panelEnfermeriaService,

            SignosVitalesService signosVitalesService
    ) {

        this.panelEnfermeriaService =
                panelEnfermeriaService;

        this.signosVitalesService =
                signosVitalesService;
    }


    // =====================================================
    // PANEL
    // =====================================================

    @GetMapping
    public String mostrarPanel(
            Model model
    ) {

        PanelEnfermeriaService.PanelEnfermeria panel =
                panelEnfermeriaService
                        .obtenerPanel();


        model.addAttribute(
                "panel",
                panel
        );


        return VISTA_PANEL;
    }
    // =====================================================
// RN-GLOBAL-001
// BUSCAR PACIENTE POR DPI
// =====================================================

    @PostMapping("/buscar-dpi")
    public String buscarPorDpi(

            @RequestParam(
                    value = "dpi",
                    required = false
            )
            String dpi,

            Model model
    ) {

        PanelEnfermeriaService.ResultadoBusquedaDpi resultado =
                panelEnfermeriaService
                        .buscarPorDpi(
                                dpi
                        );


        // =================================================
        // ERROR DE VALIDACIÓN
        // =================================================

        if (!resultado.exitoso()) {

            model.addAttribute(
                    "panel",
                    panelEnfermeriaService
                            .obtenerPanel()
            );


            model.addAttribute(
                    "mensajeError",
                    resultado.mensaje()
            );


            model.addAttribute(
                    "dpiBusqueda",
                    dpi
            );


            return VISTA_PANEL;
        }


        // =================================================
        // RESULTADO
        // =================================================

        model.addAttribute(
                "panel",
                resultado.panel()
        );


        model.addAttribute(
                "dpiBusqueda",
                dpi
        );


        model.addAttribute(
                "filtroDpiActivo",
                true
        );


        if (resultado.mensaje() != null) {

            model.addAttribute(
                    "mensajeBusqueda",
                    resultado.mensaje()
            );
        }


        return VISTA_PANEL;
    }


    // =====================================================
    // LLAMAR Y TOMAR SIGNOS
    // =====================================================

    @PostMapping("/llamar/{idCita}")
    public String llamarPaciente(

            @PathVariable("idCita")
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            PanelEnfermeriaService.LlamadoEnfermeria llamado =
                    panelEnfermeriaService
                            .llamarYTomarSignos(
                                    idCita
                            );


            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Paciente "
                            + llamado.nombrePaciente()
                            + " llamado para toma de signos vitales."
            );


            /*
             * El navegador reproducirá este texto mediante
             * speechSynthesis después del redirect.
             */
            redirectAttributes.addFlashAttribute(
                    "mensajeTts",
                    llamado.mensajeTts()
            );


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );
        }


        return "redirect:/interno/enfermeria";
    }


    // =====================================================
    // FORMULARIO DE SIGNOS VITALES
    // =====================================================

    @GetMapping("/cita/{idCita}/signos")
    public String mostrarFormulario(

            @PathVariable("idCita")
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            SignosVitalesService.ContextoRegistroSignos contexto =
                    signosVitalesService
                            .obtenerContextoRegistro(
                                    idCita
                            );


            RegistroSignosVitalesDTO formulario =
                    new RegistroSignosVitalesDTO();


            formulario.setIdCita(
                    contexto.numeroCita()
            );


            /*
             * Se precarga porque el CU lo solicita,
             * pero el Service NO confiará en este valor.
             */
            formulario.setIdEnfermero(
                    contexto.idEnfermero()
            );


            formulario.setEsEmergencia(
                    contexto.emergenciaPrevia()
            );


            model.addAttribute(
                    "contexto",
                    contexto
            );


            model.addAttribute(
                    "formulario",
                    formulario
            );


            return VISTA_SIGNOS;


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/interno/enfermeria";
        }
    }


    // =====================================================
    // REGISTRAR SIGNOS VITALES
    // =====================================================

    @PostMapping("/signos")
    public String registrarSignos(

            @ModelAttribute("formulario")
            RegistroSignosVitalesDTO formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // ERRORES DE CONVERSIÓN
        // =================================================
        //
        // Ejemplo:
        //
        // temperatura = "abc"
        //
        // Spring no puede convertirlo a BigDecimal.
        // Debemos responder con el mensaje clínico correcto,
        // no con un error 400/500.
        // =================================================

        if (bindingResult.hasErrors()) {

            String mensaje =
                    resolverErrorBinding(
                            bindingResult
                    );


            return mostrarFormularioConError(

                    formulario,

                    mensaje,

                    model,

                    redirectAttributes
            );
        }


        // =================================================
        // REGISTRAR
        // =================================================

        try {

            SignosVitalesService.ResultadoRegistroSignos resultado =
                    signosVitalesService
                            .registrar(
                                    formulario
                            );


            if (!resultado.exitoso()) {

                return mostrarFormularioConError(

                        formulario,

                        resultado.mensaje(),

                        model,

                        redirectAttributes
                );
            }


            // =============================================
            // ÉXITO
            // =============================================

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    resultado.mensaje()
            );


            /*
             * Las alertas ya quedaron persistidas.
             *
             * También las mostramos al volver al panel
             * como recordatorio para Enfermería.
             */
            if (resultado.alertas() != null
                    && !resultado.alertas()
                    .isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "alertasRegistradas",
                        resultado.alertas()
                );
            }


            return "redirect:/interno/enfermeria";


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            return mostrarFormularioConError(

                    formulario,

                    ex.getMessage(),

                    model,

                    redirectAttributes
            );
        }
    }


    // =====================================================
    // VOLVER AL FORMULARIO CON ERROR
    // =====================================================

    private String mostrarFormularioConError(

            RegistroSignosVitalesDTO formulario,

            String mensaje,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        if (formulario == null
                || formulario.getIdCita() == null
                || formulario.getIdCita() <= 0) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensaje
            );


            return "redirect:/interno/enfermeria";
        }


        try {

            SignosVitalesService.ContextoRegistroSignos contexto =
                    signosVitalesService
                            .obtenerContextoRegistro(
                                    formulario.getIdCita()
                            );


            /*
             * El ID de Enfermero enviado por navegador
             * se reemplaza nuevamente por el real.
             */
            formulario.setIdEnfermero(
                    contexto.idEnfermero()
            );


            model.addAttribute(
                    "contexto",
                    contexto
            );


            model.addAttribute(
                    "formulario",
                    formulario
            );


            model.addAttribute(
                    "mensajeError",
                    mensaje
            );


            return VISTA_SIGNOS;


        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/interno/enfermeria";
        }
    }


    // =====================================================
    // MENSAJE PARA ERRORES DE BINDING
    // =====================================================

    private String resolverErrorBinding(
            BindingResult bindingResult
    ) {

        if (bindingResult.hasFieldErrors(
                "presionSistolica"
        )

                || bindingResult.hasFieldErrors(
                "presionDiastolica"
        )) {

            return "La presión arterial debe ingresarse en formato "
                    + "sistólica/diastólica (ej: 120/80) "
                    + "dentro de rangos válidos.";
        }


        if (bindingResult.hasFieldErrors(
                "temperatura"
        )) {

            return "La temperatura debe estar entre 34.0 y 42.0°C "
                    + "con un decimal.";
        }


        if (bindingResult.hasFieldErrors(
                "peso"
        )) {

            return "El peso debe estar entre 0.5 y 300 kg "
                    + "con dos decimales.";
        }


        if (bindingResult.hasFieldErrors(
                "talla"
        )) {

            return "La talla debe estar entre 30 y 250 cm "
                    + "con dos decimales.";
        }


        if (bindingResult.hasFieldErrors(
                "frecuenciaCardiaca"
        )) {

            return "La frecuencia cardíaca debe estar entre 30 y 220 "
                    + "latidos por minuto.";
        }


        return "Los datos ingresados no son válidos. "
                + "Verifique los campos e intente nuevamente.";
    }
}