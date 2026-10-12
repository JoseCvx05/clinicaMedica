
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.MedicoAutorizacionesConsultaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * =========================================================
 * CONTROLADOR: LISTADO DE AUTORIZACIONES MEDICAS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite al medico consultar los medicamentos
 * controlados de sus propias recetas.
 *
 * SEGURIDAD:
 *
 * - Ruta protegida dentro de /interno/medico/**.
 * - No recibe el ID del medico desde HTML.
 * - No recibe sucursales desde el navegador.
 * - El Service obtiene al usuario autenticado.
 * - Solo muestra recetas permitidas.
 * - No registra autorizaciones mediante GET.
 *
 * IMPORTANTE:
 *
 * Este controlador NO modifica:
 *
 * - Consultas medicas.
 * - Recetas.
 * - Inventarios.
 * - Despachos.
 * - Pagos.
 *
 * =========================================================
 */
@Controller
@RequestMapping(
        "/interno/medico/recetas"
)
public class MedicoAutorizacionesListadoController {

    // =====================================================
    // SERVICE
    // =====================================================

    private final MedicoAutorizacionesConsultaService
            consultaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MedicoAutorizacionesListadoController(
            MedicoAutorizacionesConsultaService consultaService
    ) {

        this.consultaService = consultaService;
    }


    // =====================================================
    // MOSTRAR AUTORIZACIONES
    // =====================================================

    @GetMapping("/autorizaciones")
    public String mostrarAutorizaciones(
            Model model
    ) {

        // -------------------------------------------------
        // CONSULTAR RECETAS DEL MEDICO AUTENTICADO
        // -------------------------------------------------

        List<MedicoAutorizacionesConsultaService.RecetaAutorizacion>
                recetas = consultaService.consultarMisRecetas();


        // -------------------------------------------------
        // CALCULAR RESUMEN
        // -------------------------------------------------

        int pendientes = recetas.stream()
                .mapToInt(
                        MedicoAutorizacionesConsultaService
                                .RecetaAutorizacion::pendientes
                )
                .sum();


        int autorizados = recetas.stream()
                .mapToInt(
                        MedicoAutorizacionesConsultaService
                                .RecetaAutorizacion::autorizados
                )
                .sum();


        // -------------------------------------------------
        // DATOS PARA THYMELEAF
        // -------------------------------------------------

        model.addAttribute(
                "recetas",
                recetas
        );

        model.addAttribute(
                "totalRecetas",
                recetas.size()
        );

        model.addAttribute(
                "totalPendientes",
                pendientes
        );

        model.addAttribute(
                "totalAutorizados",
                autorizados
        );


        // -------------------------------------------------
        // VISTA DEL MEDICO
        // -------------------------------------------------

        return "medico/autorizaciones-controlados";
    }
}
