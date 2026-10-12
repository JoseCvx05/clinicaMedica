package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.CitaPacienteService;
import com.proyecto.clinicamedica.service.PacienteMedicamentosService;
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

    private final PacienteMedicamentosService pacienteMedicamentosService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PacienteDashboardController(
            CitaPacienteService citaPacienteService,
            PacienteMedicamentosService pacienteMedicamentosService
    ) {

        this.citaPacienteService =
                citaPacienteService;

        this.pacienteMedicamentosService =
                pacienteMedicamentosService;
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


    // =====================================================
    // CU-11 - MIS MEDICAMENTOS
    // =====================================================
    //
    // Muestra el historial de medicamentos
    // realmente entregados en Farmacia.
    //
    // SEGURIDAD:
    //
    // - Ruta protegida por ROLE_PACIENTE.
    // - El Service identifica al paciente autenticado.
    // - No recibe IDs de otros pacientes desde la URL.
    // - Solo consulta despachos CONFIRMADOS.
    //
    // No modifica inventario, recetas ni despachos.
    //
    // =====================================================

    @GetMapping("/paciente/medicamentos")
    public String mostrarMisMedicamentos(
            Model model
    ) {

        model.addAttribute(
                "medicamentos",
                pacienteMedicamentosService
                        .listarMisMedicamentos()
        );

        return "paciente/mis-medicamentos";
    }

}