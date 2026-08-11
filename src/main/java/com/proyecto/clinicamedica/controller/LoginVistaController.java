package com.proyecto.clinicamedica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =========================================================
 * CONTROLADOR MVC: LOGIN DE PACIENTES
 * =========================================================
 *
 * Se encarga únicamente de mostrar la vista de inicio
 * de sesión del portal de pacientes.
 *
 * La autenticación NO se realiza aquí.
 *
 * El envío de credenciales será realizado posteriormente
 * mediante:
 *
 * POST /api/public/login
 *
 * =========================================================
 */
@Controller
public class LoginVistaController {

    /**
     * Muestra la pantalla de inicio de sesión.
     *
     * @return vista templates/login.html
     */
    @GetMapping("/login")
    public String mostrarLogin() {

        return "login";
    }
}