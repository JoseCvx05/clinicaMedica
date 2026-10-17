
package com.proyecto.clinicamedica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =========================================================
 * CU-14 - CONTROLADOR DEL PANEL DE CATÁLOGOS
 * =========================================================
 *
 * Permite al Administrador acceder al panel general
 * de mantenimiento de los catálogos del sistema.
 *
 * SEGURIDAD:
 * - Ruta protegida mediante SecurityConfig.
 * - Acceso exclusivo al rol ADMINISTRADOR.
 *
 * ARQUITECTURA:
 * - No reemplaza controladores existentes.
 * - No modifica los servicios de mantenimiento.
 * - No realiza operaciones sobre PostgreSQL.
 * - Únicamente permite navegar hacia los catálogos.
 *
 * =========================================================
 */
@Controller
public class CatalogosController {

    // =====================================================
    // CU-14 - PANEL GENERAL DE CATÁLOGOS
    // =====================================================

    @GetMapping("/admin/catalogos")
    public String mostrarCatalogos() {

        return "admin/catalogos";

    }

}
