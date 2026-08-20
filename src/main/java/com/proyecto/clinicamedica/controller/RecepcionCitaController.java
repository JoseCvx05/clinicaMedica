package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.recepcion.RecepcionBusquedaDTO;

import com.proyecto.clinicamedica.service.RecepcionCitaService;
import com.proyecto.clinicamedica.service.ReasignacionMedicoService;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import com.proyecto.clinicamedica.service.RegistroEmergenciaService;

/**
 * =========================================================
 * CONTROLLER: RECEPCIÓN Y VERIFICACIÓN DE CITA
 * =========================================================
 *
 * CU-05.
 *
 * Responsabilidades:
 *
 * - Mostrar pantalla de recepción.
 * - Buscar por No. cita / DPI.
 * - Mostrar resultados.
 * - Registrar llegada.
 * - FA07: reasignar médico.
 * - Consultar cambios de estado para actualización
 *   automática de la pantalla.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/recepcion")
public class RecepcionCitaController {


    // =====================================================
    // VISTAS
    // =====================================================

    private static final String VISTA_RECEPCION =
            "interno/recepcion/recepcion";


    private static final String VISTA_REASIGNACION =
            "interno/recepcion/reasignar-medico";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final RecepcionCitaService
            recepcionCitaService;


    private final ReasignacionMedicoService
            reasignacionMedicoService;

    private final RegistroEmergenciaService
            registroEmergenciaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RecepcionCitaController(

            RecepcionCitaService recepcionCitaService,

            ReasignacionMedicoService reasignacionMedicoService,

            RegistroEmergenciaService registroEmergenciaService
    ) {

        this.recepcionCitaService =
                recepcionCitaService;


        this.reasignacionMedicoService =
                reasignacionMedicoService;

        this.registroEmergenciaService =
                registroEmergenciaService;
    }


    // =====================================================
    // PANTALLA PRINCIPAL
    // =====================================================

    @GetMapping
    public String mostrarRecepcion(
            Model model
    ) {

        if (!model.containsAttribute(
                "busqueda"
        )) {

            model.addAttribute(
                    "busqueda",
                    new RecepcionBusquedaDTO()
            );
        }


        return VISTA_RECEPCION;
    }


    // =====================================================
    // BUSCAR
    // =====================================================

    @PostMapping("/buscar")
    public String buscar(

            @ModelAttribute("busqueda")
            RecepcionBusquedaDTO busqueda,

            Model model
    ) {

        RecepcionCitaService.ResultadoBusqueda resultado =
                recepcionCitaService
                        .buscar(
                                busqueda
                        );


        model.addAttribute(
                "resultado",
                resultado
        );


        return VISTA_RECEPCION;
    }


    // =====================================================
    // MOSTRAR UNA CITA
    // =====================================================

    @GetMapping("/cita/{idCita}")
    public String mostrarCita(

            @PathVariable("idCita")
            Integer idCita,

            Model model
    ) {

        RecepcionBusquedaDTO busqueda =
                new RecepcionBusquedaDTO();


        busqueda.setTipoBusqueda(
                RecepcionBusquedaDTO.TIPO_CITA
        );


        busqueda.setValorBusqueda(
                String.valueOf(
                        idCita
                )
        );


        model.addAttribute(
                "busqueda",
                busqueda
        );


        model.addAttribute(
                "resultado",
                recepcionCitaService
                        .consultarPorNumero(
                                idCita
                        )
        );


        return VISTA_RECEPCION;
    }


    // =====================================================
    // REGISTRAR LLEGADA
    // =====================================================

    @PostMapping("/cita/{idCita}/llegada")
    public String registrarLlegada(

            @PathVariable("idCita")
            Integer idCita,

            RedirectAttributes redirectAttributes
    ) {

        try {

            RecepcionCitaService.ResultadoLlegada resultado =
                    recepcionCitaService
                            .registrarLlegada(
                                    idCita
                            );


            if (resultado.exitoso()) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeExito",
                                resultado.mensaje()
                        );


                redirectAttributes
                        .addFlashAttribute(
                                "indicadorLlegada",
                                resultado.indicador()
                        );


                redirectAttributes
                        .addFlashAttribute(
                                "llegadaEmergencia",
                                resultado.emergencia()
                        );

            } else {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeError",
                                resultado.mensaje()
                        );
            }

        } catch (RuntimeException ex) {

            /*
             * FA09:
             * si ocurre un error inesperado al actualizar
             * la llegada, mostramos el mensaje requerido
             * en lugar de dejar un error 500 visible.
             */
            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            "Error al registrar la llegada"
                    );
        }


        return "redirect:/interno/recepcion/cita/"
                + idCita;
    }


    // =====================================================
    // FA07 - MOSTRAR REASIGNACIÓN DE MÉDICO
    // =====================================================

    @GetMapping("/cita/{idCita}/reasignar-medico")
    public String mostrarReasignacion(

            @PathVariable("idCita")
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ReasignacionMedicoService.DetalleReasignacion detalle =
                    reasignacionMedicoService
                            .obtenerDatos(
                                    idCita
                            );


            model.addAttribute(
                    "detalle",
                    detalle
            );


            return VISTA_REASIGNACION;


        } catch (IllegalArgumentException ex) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/recepcion/cita/"
                    + idCita;
        }
    }


    // =====================================================
    // FA07 - CONFIRMAR REASIGNACIÓN
    // =====================================================
    @PostMapping("/cita/{idCita}/reasignar-medico")
    public String confirmarReasignacion(

            @PathVariable("idCita")
            Integer idCita,

            @RequestParam(
                    value = "idNuevoMedico",
                    required = false
            )
            Integer idNuevoMedico,

            @RequestParam(
                    value = "motivo",
                    required = false
            )
            String motivo,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        ReasignacionMedicoService.ResultadoReasignacion resultado;


        try {

            resultado =
                    reasignacionMedicoService
                            .reasignar(

                                    idCita,

                                    idNuevoMedico,

                                    motivo,

                                    request.getRemoteAddr()
                            );


        } catch (RuntimeException ex) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            "Operación no permitida"
                    );


            return "redirect:/interno/recepcion/cita/"
                    + idCita
                    + "/reasignar-medico";
        }


        // =================================================
        // ÉXITO
        // =================================================

        if (resultado.exitoso()) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            return "redirect:/interno/recepcion/cita/"
                    + idCita;
        }


        // =================================================
        // ERROR
        // =================================================

        redirectAttributes
                .addFlashAttribute(
                        "mensajeError",
                        resultado.mensaje()
                );


        return "redirect:/interno/recepcion/cita/"
                + idCita
                + "/reasignar-medico";
    }


    // =====================================================
    // RNF-021 - CONSULTA AUTOMÁTICA DE ESTADO
    // =====================================================

    @GetMapping("/cita/{idCita}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarEstado(

            @PathVariable("idCita")
            Integer idCita
    ) {

        RecepcionCitaService.ResultadoBusqueda resultado =
                recepcionCitaService
                        .consultarPorNumero(
                                idCita
                        );


        if (resultado.cita() == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        Map<String, Object> respuesta =
                new LinkedHashMap<>();


        respuesta.put(
                "numeroCita",
                resultado.cita()
                        .numeroCita()
        );


        respuesta.put(
                "estado",
                resultado.cita()
                        .estado()
        );


        respuesta.put(
                "prioridad",
                resultado.cita()
                        .prioridad()
        );


        respuesta.put(
                "horaLlegada",

                resultado.cita()
                        .horaLlegada() == null

                        ? null

                        : resultado.cita()
                        .horaLlegada()
                        .toString()
        );


        respuesta.put(
                "puedeRegistrarLlegada",
                resultado.puedeRegistrarLlegada()
        );


        respuesta.put(
                "emergencia",
                resultado.cita()
                        .emergencia()
        );


        return ResponseEntity.ok(
                respuesta
        );
    }
    // =====================================================
// FA01 - REGISTRAR EMERGENCIA
// =====================================================

    @PostMapping("/emergencia")
    public String registrarEmergencia(

            @RequestParam(
                    value = "nombreCompleto",
                    required = false
            )
            String nombreCompleto,

            @RequestParam(
                    value = "dpi",
                    required = false
            )
            String dpi,

            RedirectAttributes redirectAttributes
    ) {

        try {

            RegistroEmergenciaService.ResultadoEmergencia resultado =
                    registroEmergenciaService
                            .registrar(
                                    nombreCompleto,
                                    dpi
                            );


            // =================================================
            // ERROR DE VALIDACIÓN
            // =================================================

            if (!resultado.exitoso()) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeError",
                                resultado.mensaje()
                        );


                /*
                 * Conservamos el nombre para que el
                 * recepcionista no tenga que escribirlo
                 * nuevamente.
                 */
                redirectAttributes
                        .addFlashAttribute(
                                "nombreEmergencia",
                                nombreCompleto
                        );


                redirectAttributes
                        .addFlashAttribute(
                                "dpiEmergencia",
                                dpi
                        );


                /*
                 * Indica a la vista que debe volver a abrir
                 * automáticamente el modal.
                 */
                redirectAttributes
                        .addFlashAttribute(
                                "abrirModalEmergencia",
                                true
                        );


                return "redirect:/interno/recepcion";
            }


            // =================================================
            // REGISTRO EXITOSO
            // =================================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "atencionEmergenciaRegistrada",
                            true
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "idAtencionEmergencia",
                            resultado.idAtencionEmergencia()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "nombrePacienteEmergencia",
                            resultado.nombrePaciente()
                    );


            return "redirect:/interno/recepcion";


        } catch (RuntimeException ex) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            "No fue posible registrar la emergencia."
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "nombreEmergencia",
                            nombreCompleto
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "dpiEmergencia",
                            dpi
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "abrirModalEmergencia",
                            true
                    );


            return "redirect:/interno/recepcion";
        }
    }
}