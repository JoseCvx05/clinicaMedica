package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.OrdenLaboratorioService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


/**
 * =========================================================
 * CONTROLLER: ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01.
 * CU-09 - FA01 Orden externa.
 *
 * Responsabilidades:
 *
 * - Mostrar el formulario de orden.
 * - Mostrar catálogo de exámenes.
 * - Recibir únicamente IDs de exámenes.
 * - Recibir indicador de orden externa.
 * - Delegar TODA la lógica al Service.
 * - Mostrar mensajes de éxito/error.
 *
 * SEGURIDAD:
 *
 * Las rutas están dentro de:
 *
 * /interno/medico/**
 *
 * por lo que quedan protegidas por la configuración
 * existente para ROLE_MEDICO.
 *
 * La seguridad real de propiedad de cita, consulta,
 * Médico y sucursal se vuelve a validar en el Service.
 *
 * =========================================================
 */
@Controller
@RequestMapping(
        "/interno/medico"
)
public class OrdenLaboratorioController {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final OrdenLaboratorioService
            ordenLaboratorioService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public OrdenLaboratorioController(
            OrdenLaboratorioService ordenLaboratorioService
    ) {

        this.ordenLaboratorioService =
                ordenLaboratorioService;
    }


    // =====================================================
    // MOSTRAR FORMULARIO
    // =====================================================

    @GetMapping(
            "/citas/{idCita}/laboratorio"
    )
    public String mostrarFormulario(

            @PathVariable
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // VALIDAR CONTEXTO Y CARGAR CATÁLOGO
            // =============================================

            OrdenLaboratorioService.PreparacionOrden
                    preparacion =

                    ordenLaboratorioService
                            .prepararOrden(
                                    idCita
                            );


            // =============================================
            // MODELO
            // =============================================

            model.addAttribute(
                    "idCita",
                    preparacion.idCita()
            );


            model.addAttribute(
                    "idConsulta",
                    preparacion.idConsulta()
            );


            model.addAttribute(
                    "examenes",
                    preparacion.examenes()
            );


            /*
             * CU-09 - FA01.
             *
             * Si venimos por primera vez al formulario,
             * la orden se considera interna por defecto.
             *
             * Si regresamos después de un error,
             * permitimos conservar el valor enviado.
             */
            if (!model.containsAttribute(
                    "ordenExterna"
            )) {

                model.addAttribute(
                        "ordenExterna",
                        false
                );
            }


            return "medico/orden-laboratorio";

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
    // GUARDAR ORDEN
    // =====================================================

    @PostMapping(
            "/citas/{idCita}/laboratorio"
    )
    public String guardarOrden(

            @PathVariable
            Integer idCita,

            @RequestParam(
                    name = "idsExamenes",
                    required = false
            )
            List<Integer> idsExamenes,

            @RequestParam(
                    name = "observaciones",
                    required = false
            )
            String observaciones,

            /*
             * CU-09 - FA01
             *
             * Checkbox marcado:
             * true
             *
             * Checkbox no marcado:
             * el navegador no envía el parámetro,
             * por lo que utilizamos false.
             */
            @RequestParam(
                    name = "ordenExterna",
                    defaultValue = "false"
            )
            boolean ordenExterna,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // CREAR ORDEN
            // =============================================

            OrdenLaboratorioService.ResultadoOrden
                    resultado =

                    ordenLaboratorioService
                            .crearOrden(

                                    idCita,

                                    idsExamenes,

                                    observaciones,

                                    ordenExterna
                            );


            // =============================================
            // MENSAJE DE ÉXITO
            // =============================================
            //
            // El Service decide qué mensaje corresponde:
            //
            // - Orden interna:
            //   conserva mensaje exacto CU-08.
            //
            // - Orden externa:
            //   utiliza el mensaje correspondiente a FA01.
            //
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "numeroOrdenLaboratorio",
                            resultado.numeroOrden()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "examenesOrdenLaboratorio",
                            resultado.examenes()
                    );


            return "redirect:/interno/medico";

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // =============================================
            // CONSERVAR DATOS ÚTILES DEL FORMULARIO
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "ordenExterna",
                            ordenExterna
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "observacionesIngresadas",
                            observaciones
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "idsExamenesSeleccionados",
                            idsExamenes
                    );


            /*
             * Regresamos al formulario.
             *
             * Al volver:
             *
             * - valida nuevamente el Médico;
             * - valida nuevamente la cita;
             * - valida nuevamente la consulta;
             * - vuelve a leer el catálogo desde PostgreSQL;
             * - no confía en información enviada desde HTML.
             */
            return "redirect:/interno/medico/citas/"
                    + idCita
                    + "/laboratorio";
        }
    }
}