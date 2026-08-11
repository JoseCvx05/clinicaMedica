package com.proyecto.clinicamedica.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =========================================================
 * CONTROLADOR MVC: DASHBOARD INTERNO
 * =========================================================
 *
 * Punto de entrada para el personal interno autenticado.
 *
 * La seguridad real de los módulos se controla mediante
 * Spring Security.
 *
 * Este controlador únicamente envía a la vista información
 * necesaria para adaptar visualmente el dashboard según
 * las autoridades del usuario autenticado.
 * =========================================================
 */
@Controller
public class InternoDashboardController {

    /**
     * Muestra el dashboard principal del personal interno.
     *
     * @param authentication autenticación obtenida del JWT
     * @param model datos enviados a Thymeleaf
     *
     * @return templates/interno/dashboard.html
     */
    @GetMapping("/interno/dashboard")
    public String mostrarDashboard(
            Authentication authentication,
            Model model
    ) {

        // =================================================
        // NOMBRE DEL USUARIO AUTENTICADO
        // =================================================

        String nombreUsuario =
                authentication.getName();


        // =================================================
        // VERIFICAR SI ES ADMINISTRADOR
        // =================================================

        boolean esAdministrador =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(
                                GrantedAuthority::getAuthority
                        )
                        .anyMatch(
                                "ROLE_ADMINISTRADOR"::equals
                        );


        // =================================================
        // DATOS PARA THYMELEAF
        // =================================================

        model.addAttribute(
                "nombreUsuario",
                nombreUsuario
        );


        model.addAttribute(
                "esAdministrador",
                esAdministrador
        );


        return "interno/dashboard";
    }
}