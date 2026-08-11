package com.proyecto.clinicamedica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =========================================================
 * CONTROLADOR MVC: LOGIN DEL PERSONAL INTERNO
 * =========================================================
 *
 * Se encarga exclusivamente de mostrar la pantalla
 * de inicio de sesión para el personal interno.
 *
 * La autenticación real NO se realiza aquí.
 *
 * El envío de credenciales será procesado por:
 *
 * POST /api/interno/login
 *
 * utilizando:
 *
 * TipoAcceso.INTERNO
 *
 * =========================================================
 */
@Controller
public class LoginInternoVistaController {

    /**
     * Muestra la pantalla de inicio de sesión
     * para el personal interno del hospital.
     *
     * @return templates/login-interno.html
     */
    @GetMapping("/login-interno")
    public String mostrarLoginInterno() {

        return "login-interno";
    }
}