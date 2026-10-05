package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.ConsultaMedicaFormulario;

import com.proyecto.clinicamedica.service.Cie10Service;
import com.proyecto.clinicamedica.service.ConsultaMedicaService;
import com.proyecto.clinicamedica.service.PanelMedicoService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


/**
 * =========================================================
 * CONTROLLER: MÉDICO
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Rutas protegidas por Spring Security:
 *
 * /interno/medico/**
 *
 * Rol requerido:
 *
 * ROLE_MEDICO
 *
 * Responsabilidades:
 *
 * - Mostrar panel del Médico.
 * - Iniciar una consulta.
 * - Ejecutar TTS mediante mensaje enviado a la vista.
 * - Abrir expediente clínico.
 * - Autocompletar CIE-10.
 * - Guardar consulta En curso.
 * - Finalizar consulta.
 * - FA05: impedir cierre sin diagnóstico.
 * - FA06: marcar No Asistió.
 *
 * La lógica de negocio NO pertenece a este Controller.
 * Se delega a los Services correspondientes.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/medico")
public class MedicoController {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final PanelMedicoService
            panelMedicoService;

    private final ConsultaMedicaService
            consultaMedicaService;

    private final Cie10Service
            cie10Service;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MedicoController(

            PanelMedicoService panelMedicoService,

            ConsultaMedicaService consultaMedicaService,

            Cie10Service cie10Service
    ) {

        this.panelMedicoService =
                panelMedicoService;

        this.consultaMedicaService =
                consultaMedicaService;

        this.cie10Service =
                cie10Service;
    }


    // =====================================================
    // PANEL DEL MÉDICO
    // =====================================================

    /**
     * Muestra:
     *
     * - En Espera de Consulta.
     * - En Consulta Médica.
     * - Evaluados - Pendiente de cierre.
     */
    @GetMapping
    public String mostrarPanel(
            Model model
    ) {

        try {

            PanelMedicoService.PanelMedico panel =
                    panelMedicoService
                            .obtenerPanel();


            model.addAttribute(
                    "panel",
                    panel
            );


            model.addAttribute(
                    "enEspera",
                    panel.enEspera()
            );


            model.addAttribute(
                    "enConsulta",
                    panel.enConsulta()
            );


            model.addAttribute(
                    "evaluados",
                    panel.evaluados()
            );


            return "medico/panel";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "medico/panel";
        }
    }


    // =====================================================
    // INICIAR CONSULTA
    // =====================================================

    /**
     * CU-08 - Flujo normal.
     *
     * En Espera
     *      ↓
     * Consulta Médica
     *
     * También crea ConsultaMedica versión 1.
     */
    @PostMapping(
            "/citas/{idCita}/iniciar"
    )
    public String iniciarConsulta(

            @PathVariable
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaService.InicioConsulta resultado =
                    consultaMedicaService
                            .iniciarConsulta(
                                    idCita
                            );


            // =============================================
            // MENSAJE VISUAL
            // =============================================

            redirectAttributes
                    .addFlashAttribute(

                            "mensajeExito",

                            "Consulta médica iniciada "
                                    + "correctamente para "
                                    + resultado.nombrePaciente()
                                    + "."
                    );


            // =============================================
            // MENSAJE TTS
            // =============================================
            //
            // Será leído por SpeechSynthesis
            // dentro de panel.html.
            // =============================================

            redirectAttributes
                    .addFlashAttribute(

                            "mensajeTts",

                            resultado.mensajeTts()
                    );


            // =============================================
            // ABRIR CONSULTA DIRECTAMENTE
            // =============================================

            return "redirect:/interno/medico/consulta/"
                    + resultado.numeroCita();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/medico";
        }
    }


    // =====================================================
    // FA06 - NO ASISTIÓ
    // =====================================================

    @PostMapping(
            "/citas/{idCita}/no-asistio"
    )
    public String marcarNoAsistio(

            @PathVariable
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaService.ResultadoNoAsistio resultado =
                    consultaMedicaService
                            .marcarNoAsistio(
                                    idCita
                            );


            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );
        }


        return "redirect:/interno/medico";
    }


    // =====================================================
    // ABRIR EXPEDIENTE / CONSULTA
    // =====================================================

    /**
     * Carga:
     *
     * - Datos del paciente.
     * - Cita.
     * - Signos vitales.
     * - Motivo inicial.
     * - Información clínica capturada.
     * - CIE-10.
     * - Estado.
     * - Versión.
     */
    @GetMapping(
            "/consulta/{idCita}"
    )
    public String abrirConsulta(

            @PathVariable
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaService.ExpedienteConsulta expediente =
                    consultaMedicaService
                            .obtenerExpediente(
                                    idCita
                            );


            ConsultaMedicaFormulario formulario =
                    construirFormulario(
                            expediente
                    );


            model.addAttribute(
                    "expediente",
                    expediente
            );


            model.addAttribute(
                    "formulario",
                    formulario
            );


            return "medico/consulta";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/medico";
        }
    }


    // =====================================================
    // GUARDAR CONSULTA EN CURSO
    // =====================================================

    @PostMapping(
            "/consulta/guardar"
    )
    public String guardarConsulta(

            @Valid
            ConsultaMedicaFormulario formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDACIONES DEL DTO
        // =================================================

        if (bindingResult.hasErrors()) {

            return volverAConsultaConErrores(
                    formulario,
                    model
            );
        }


        try {

            ConsultaMedicaService.ResultadoGuardado resultado =
                    consultaMedicaService
                            .guardarEnCurso(
                                    formulario
                            );


            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            return "redirect:/interno/medico/consulta/"
                    + formulario.getIdCita();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return volverAConsultaConErrores(
                    formulario,
                    model
            );
        }
    }


    // =====================================================
    // FINALIZAR CONSULTA
    // =====================================================

    /**
     * Aplica:
     *
     * RN-CU08-01
     * RN-CU08-02
     * FA05
     *
     * Después de finalizar:
     *
     * La cita permanece en "Consulta Médica".
     *
     * ConsultaMedica pasa a:
     *
     * FINALIZADA
     *
     * y aparecerá en:
     *
     * Evaluados - Pendiente de cierre.
     */
    @PostMapping(
            "/consulta/finalizar"
    )
    public String finalizarConsulta(

            @Valid
            ConsultaMedicaFormulario formulario,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDACIONES DTO
        // =================================================

        if (bindingResult.hasErrors()) {

            return volverAConsultaConErrores(
                    formulario,
                    model
            );
        }


        try {

            ConsultaMedicaService.ResultadoGuardado resultado =
                    consultaMedicaService
                            .finalizarConsulta(
                                    formulario
                            );


            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            // =============================================
            // VOLVER AL PANEL
            //
            // Ahora debe aparecer en:
            //
            // Evaluados - Pendiente de cierre
            // =============================================

            return "redirect:/interno/medico";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return volverAConsultaConErrores(
                    formulario,
                    model
            );
        }
    }

    // =====================================================
// FINALIZAR ATENCIÓN
// =====================================================

    /**
     * CU-08 - Cierre definitivo de la atención.
     *
     * Precondiciones validadas por el Service:
     *
     * - Médico autenticado.
     * - Cita asignada al Médico.
     * - Cita en "Consulta Médica".
     * - Consulta clínica FINALIZADA.
     * - Firma digital existente.
     * - Firma digital criptográficamente válida.
     *
     * Resultado:
     *
     * Consulta Médica
     *        ↓
     * Atención Finalizada
     */
    @PostMapping(
            "/citas/{idCita}/finalizar-atencion"
    )
    public String finalizarAtencion(

            @PathVariable
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaService.ResultadoAtencionFinalizada resultado =
                    consultaMedicaService
                            .finalizarAtencion(
                                    idCita
                            );


            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );
        }


        return "redirect:/interno/medico";
    }


    // =====================================================
    // AUTOCOMPLETADO CIE-10
    // =====================================================

    /**
     * Ejemplo:
     *
     * GET
     * /interno/medico/cie10?texto=hip
     *
     * Devuelve máximo 15 resultados.
     */
    @GetMapping(
            "/cie10"
    )
    @ResponseBody
    public List<Cie10Service.SugerenciaCie10>
    buscarCie10(

            @RequestParam(
                    name = "texto",
                    defaultValue = ""
            )
            String texto
    ) {

        return cie10Service
                .buscarSugerencias(
                        texto
                );
    }


    // =====================================================
    // CONSTRUIR FORMULARIO DESDE EXPEDIENTE
    // =====================================================

    private ConsultaMedicaFormulario construirFormulario(

            ConsultaMedicaService.ExpedienteConsulta expediente
    ) {

        ConsultaMedicaFormulario formulario =
                new ConsultaMedicaFormulario();


        formulario.setIdCita(
                expediente.numeroCita()
        );


        formulario.setMotivoVisita(
                expediente.motivoVisita()
        );


        formulario.setHallazgosClinicos(
                expediente.hallazgosClinicos()
        );


        formulario.setIdCie10(
                expediente.idCie10()
        );


        formulario.setDiagnostico(
                expediente.diagnostico()
        );


        formulario.setPlanTratamiento(
                expediente.planTratamiento()
        );


        formulario.setNotasAdicionales(
                expediente.notasAdicionales()
        );


        return formulario;
    }


    // =====================================================
// CU-08 FA02
// IR A AGENDAR CITA DE SEGUIMIENTO
// =====================================================

    @GetMapping(
            "/citas/{idCita}/seguimiento"
    )
    public String irAgendarSeguimiento(

            @PathVariable
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // OBTENER CONSULTA REAL DE LA CITA
            // =============================================

            ConsultaMedicaService.ExpedienteConsulta expediente =
                    consultaMedicaService
                            .obtenerExpediente(
                                    idCita
                            );


            // =============================================
            // VALIDAR ID DE CONSULTA
            // =============================================

            if (expediente.idConsulta() == null
                    || expediente.idConsulta() <= 0) {

                throw new IllegalStateException(
                        "La cita no posee una consulta médica válida."
                );
            }


            // =============================================
            // ENVIAR AL WIZARD DE SEGUIMIENTO
            // =============================================

            return "redirect:/interno/medico/citas/agendar/seguimiento/"
                    + expediente.idConsulta();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/medico";
        }
    }

    // =====================================================
    // VOLVER A CONSULTA CON ERRORES
    // =====================================================

    /**
     * Si existen errores de validación:
     *
     * - conserva lo escrito por el Médico;
     * - vuelve a cargar el expediente;
     * - muestra nuevamente consulta.html.
     */
    private String volverAConsultaConErrores(

            ConsultaMedicaFormulario formulario,

            Model model
    ) {

        try {

            if (formulario == null
                    || formulario.getIdCita() == null) {

                model.addAttribute(
                        "mensajeError",
                        "No fue posible identificar la cita."
                );


                return "medico/consulta";
            }


            ConsultaMedicaService.ExpedienteConsulta expediente =
                    consultaMedicaService
                            .obtenerExpediente(
                                    formulario.getIdCita()
                            );


            model.addAttribute(
                    "expediente",
                    expediente
            );


            /*
             * IMPORTANTE:
             *
             * No reconstruimos formulario desde BD.
             *
             * Conservamos el objeto recibido para que
             * el Médico no pierda lo que había escrito.
             */
            model.addAttribute(
                    "formulario",
                    formulario
            );


            return "medico/consulta";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "medico/consulta";
        }
    }
}