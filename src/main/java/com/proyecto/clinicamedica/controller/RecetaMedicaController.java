package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.RecetaMedicaService;

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
 * CONTROLADOR: RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04.
 *
 * Permite al Médico:
 *
 * - Abrir el formulario de receta.
 * - Consultar catálogo de medicamentos.
 * - Registrar uno o varios medicamentos.
 * - Guardar la receta médica.
 *
 * SEGURIDAD:
 *
 * - Las rutas están dentro de /interno/medico/**.
 * - El ID del Médico NO se recibe desde HTML.
 * - La sucursal NO se recibe desde HTML.
 * - El Service vuelve a validar usuario, rol, cita,
 *   consulta, sucursal y firma digital.
 *
 * =========================================================
 */
@Controller
@RequestMapping(
        "/interno/medico"
)
public class RecetaMedicaController {


    // =====================================================
    // SERVICE
    // =====================================================

    private final RecetaMedicaService
            recetaMedicaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RecetaMedicaController(
            RecetaMedicaService recetaMedicaService
    ) {

        this.recetaMedicaService =
                recetaMedicaService;
    }


    // =====================================================
    // MOSTRAR FORMULARIO
    // =====================================================

    @GetMapping(
            "/citas/{idCita}/receta"
    )
    public String mostrarFormulario(

            @PathVariable
            Integer idCita,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // VALIDAR Y PREPARAR
            // =============================================

            RecetaMedicaService.PreparacionReceta
                    preparacion =
                    recetaMedicaService
                            .prepararReceta(
                                    idCita
                            );


            // =============================================
            // MODELO
            // =============================================

            model.addAttribute(
                    "preparacion",
                    preparacion
            );


            model.addAttribute(
                    "idCita",
                    preparacion.idCita()
            );


            model.addAttribute(
                    "idConsulta",
                    preparacion.idConsulta()
            );


            model.addAttribute(
                    "medicamentos",
                    preparacion.medicamentos()
            );


            return "medico/receta-medica";


        } catch (
                IllegalArgumentException
                |
                IllegalStateException ex
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
    // GUARDAR RECETA
    // =====================================================

    @PostMapping(
            "/citas/{idCita}/receta"
    )
    public String guardarReceta(

            @PathVariable
            Integer idCita,


            // =============================================
            // MEDICAMENTOS
            // =============================================

            @RequestParam(
                    name = "idMedicamento",
                    required = false
            )
            List<Integer> idsMedicamentos,


            // =============================================
            // CANTIDADES
            // =============================================

            @RequestParam(
                    name = "cantidad",
                    required = false
            )
            List<Integer> cantidades,


            // =============================================
            // DOSIS
            // =============================================

            @RequestParam(
                    name = "dosis",
                    required = false
            )
            List<String> dosis,


            // =============================================
            // FRECUENCIAS
            // =============================================

            @RequestParam(
                    name = "frecuencia",
                    required = false
            )
            List<String> frecuencias,


            // =============================================
            // DURACIONES
            // =============================================

            @RequestParam(
                    name = "duracion",
                    required = false
            )
            List<String> duraciones,


            // =============================================
            // INDICACIONES
            // =============================================

            @RequestParam(
                    name = "indicaciones",
                    required = false
            )
            List<String> indicaciones,


            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // CREAR RECETA
            // =============================================

            RecetaMedicaService.ResultadoReceta
                    resultado =
                    recetaMedicaService
                            .crearReceta(

                                    idCita,

                                    idsMedicamentos,

                                    cantidades,

                                    dosis,

                                    frecuencias,

                                    duraciones,

                                    indicaciones
                            );


            // =============================================
            // MENSAJE EXACTO FA04
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            // =============================================
            // VOLVER AL PANEL
            // =============================================

            return "redirect:/interno/medico";


        } catch (
                IllegalArgumentException
                |
                IllegalStateException ex
        ) {

            // =============================================
            // CONSERVAR MENSAJE
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            // =============================================
            // CONSERVAR DATOS INGRESADOS
            // =============================================

            if (idsMedicamentos != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "idsMedicamentosIngresados",
                                idsMedicamentos
                        );
            }


            if (cantidades != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "cantidadesIngresadas",
                                cantidades
                        );
            }


            if (dosis != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "dosisIngresadas",
                                dosis
                        );
            }


            if (frecuencias != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "frecuenciasIngresadas",
                                frecuencias
                        );
            }


            if (duraciones != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "duracionesIngresadas",
                                duraciones
                        );
            }


            if (indicaciones != null) {

                redirectAttributes
                        .addFlashAttribute(
                                "indicacionesIngresadas",
                                indicaciones
                        );
            }


            // =============================================
            // VOLVER AL FORMULARIO
            // =============================================

            return "redirect:/interno/medico/citas/"
                    + idCita
                    + "/receta";
        }
    }
}