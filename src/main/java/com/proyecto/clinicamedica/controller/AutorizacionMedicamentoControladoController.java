
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.AutorizacionMedicamentoControladoService;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * =========================================================
 * CONTROLADOR: AUTORIZACION MEDICAMENTO CONTROLADO
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 * RNF-017 - Autorizacion adicional.
 *
 * Este controlador es independiente del CU-08.
 *
 * Permite:
 *
 * - Solicitar autorizacion de un medicamento controlado.
 * - Mostrar confirmacion o error al medico.
 *
 * SEGURIDAD:
 *
 * - Ruta dentro de /interno/medico/**.
 * - No recibe el ID del medico desde HTML.
 * - No recibe la sucursal desde HTML.
 * - No recibe la cantidad autorizada desde HTML.
 * - Toda autorizacion se valida en el Service.
 *
 * IMPORTANTE:
 *
 * Spring Security debe proteger esta ruta
 * con el rol MEDICO y CSRF habilitado.
 *
 * =========================================================
 */
@Controller
@RequestMapping(
        "/interno/medico/recetas"
)
public class AutorizacionMedicamentoControladoController {

    // =====================================================
    // SERVICE
    // =====================================================

    private final AutorizacionMedicamentoControladoService
            autorizacionService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AutorizacionMedicamentoControladoController(
            AutorizacionMedicamentoControladoService
                    autorizacionService
    ) {

        this.autorizacionService =
                autorizacionService;
    }


    // =====================================================
    // AUTORIZAR MEDICAMENTO CONTROLADO
    // =====================================================

    @PostMapping(
            "/{idReceta}/medicamentos/{idDetalleReceta}/autorizar"
    )
    public String autorizarMedicamento(

            @PathVariable
            Integer idReceta,

            @PathVariable
            Integer idDetalleReceta,

            @RequestParam(
                    name = "observaciones",
                    required = false
            )
            String observaciones,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // AUTORIZACION EN EL BACKEND
            // =============================================

            AutorizacionMedicamentoControladoService
                    .ResultadoAutorizacion resultado =

                    autorizacionService.autorizar(

                            idReceta,

                            idDetalleReceta,

                            observaciones
                    );


            // =============================================
            // MENSAJE DE CONFIRMACION
            // =============================================

            redirectAttributes.addFlashAttribute(

                    "mensajeExito",

                    "Medicamento controlado autorizado "
                            + "correctamente. "
                            + "Receta #"
                            + resultado.idReceta()
                            + ". Cantidad autorizada: "
                            + resultado.cantidadAutorizada()
                            + "."
            );


            // =============================================
            // REGRESAR AL PANEL MEDICO
            // =============================================

            return "redirect:/interno/medico/recetas/autorizaciones";

        } catch (
                IllegalArgumentException
                |
                IllegalStateException ex
        ) {

            // =============================================
            // MOSTRAR ERROR DE VALIDACION
            // =============================================

            redirectAttributes.addFlashAttribute(

                    "mensajeError",

                    ex.getMessage()
            );


            return "redirect:/interno/medico/recetas/autorizaciones";
        }
    }
}
