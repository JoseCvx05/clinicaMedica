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
 *
 * Responsabilidades:
 *
 * - Mostrar el formulario de orden.
 * - Mostrar catálogo de exámenes.
 * - Recibir únicamente IDs de exámenes.
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

                                    observaciones
                            );


            // =============================================
            // MENSAJE EXACTO FA01
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

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            /*
             * Regresamos al formulario.
             *
             * Al volver a entrar:
             *
             * - vuelve a validar Médico;
             * - vuelve a validar cita;
             * - vuelve a validar consulta;
             * - vuelve a leer catálogo desde PostgreSQL.
             */
            return "redirect:/interno/medico/citas/"
                    + idCita
                    + "/laboratorio";
        }
    }
}