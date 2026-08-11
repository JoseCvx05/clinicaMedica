package com.proyecto.clinicamedica.controller;

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
 * La ruta:
 *
 * /paciente/**
 *
 * se encuentra protegida por Spring Security y exige:
 *
 * ROLE_PACIENTE
 *
 * El usuario autenticado se obtiene directamente
 * del JWT previamente validado por Spring Security.
 * =========================================================
 */
@Controller
public class PacienteDashboardController {

    /**
     * Muestra el dashboard principal del paciente.
     *
     * @param jwt JWT autenticado y validado
     * @param model modelo utilizado por Thymeleaf
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
}