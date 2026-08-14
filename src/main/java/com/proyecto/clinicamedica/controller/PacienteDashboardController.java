package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.CitaPacienteService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;


/**
 * =========================================================
 * CONTROLADOR MVC: DASHBOARD DEL PACIENTE
 * =========================================================
 *
 * Controla las vistas privadas correspondientes
 * al portal del paciente.
 *
 * Las rutas:
 *
 * /paciente/**
 *
 * se encuentran protegidas por Spring Security y exigen:
 *
 * ROLE_PACIENTE
 *
 * =========================================================
 */
@Controller
public class PacienteDashboardController {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaPacienteService citaPacienteService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PacienteDashboardController(
            CitaPacienteService citaPacienteService
    ) {

        this.citaPacienteService =
                citaPacienteService;
    }


    // =====================================================
    // DASHBOARD DEL PACIENTE
    // =====================================================

    /**
     * Muestra el dashboard principal del paciente.
     *
     * @param jwt   JWT autenticado y validado.
     * @param model modelo utilizado por Thymeleaf.
     *
     * @return templates/paciente/dashboard.html
     */
    @GetMapping("/paciente/dashboard")
    public String mostrarDashboard(
            @AuthenticationPrincipal Jwt jwt,
            Model model
    ) {

        // =================================================
        // NOMBRE DE USUARIO AUTENTICADO
        // =================================================

        String nombreUsuario =
                jwt.getSubject();


        // =================================================
        // INFORMACIÓN PARA LA VISTA
        // =================================================

        model.addAttribute(
                "nombreUsuario",
                nombreUsuario
        );


        return "paciente/dashboard";
    }


    // =====================================================
    // MIS CITAS
    // =====================================================

    /**
     * Muestra únicamente las citas pertenecientes
     * al paciente actualmente autenticado.
     *
     * @param model modelo utilizado por Thymeleaf.
     *
     * @return templates/paciente/mis-citas.html
     */
    @GetMapping("/paciente/citas")
    public String mostrarMisCitas(
            Model model
    ) {

        model.addAttribute(
                "citas",
                citaPacienteService
                        .listarMisCitas()
        );


        return "paciente/mis-citas";
    }
}