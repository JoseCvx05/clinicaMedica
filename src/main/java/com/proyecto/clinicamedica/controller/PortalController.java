package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.SucursalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =========================================================
 * CONTROLADOR MVC: PORTAL PÚBLICO
 * =========================================================
 *
 * Controla la página principal pública correspondiente
 * al CU-00 - Visualización del Portal Web.
 *
 * Responsabilidades:
 *
 * - Atender el acceso al portal.
 * - Obtener los catálogos necesarios.
 * - Entregar los datos a Thymeleaf.
 *
 * NO contiene:
 *
 * - Consultas directas a PostgreSQL.
 * - Lógica de verificación de DPI.
 * - Lógica de autenticación.
 * - Lógica de caché.
 *
 * Las dependencias se realizan mediante interfaces,
 * aplicando inversión de dependencias (SOLID).
 * =========================================================
 */
@Controller
public class PortalController {

    private final EspecialidadService especialidadService;
    private final SucursalService sucursalService;


    /**
     * Inyección de dependencias por constructor.
     */
    public PortalController(
            EspecialidadService especialidadService,
            SucursalService sucursalService
    ) {
        this.especialidadService = especialidadService;
        this.sucursalService = sucursalService;
    }


    /**
     * =====================================================
     * GET /
     * GET /portal
     * =====================================================
     *
     * Muestra la página principal del portal.
     *
     * Las especialidades y sucursales se obtienen
     * mediante Services cuyos métodos utilizan caché.
     */
    @GetMapping({"/", "/portal"})
    public String mostrarPortal(Model model) {

        /*
         * Catálogo de especialidades activas.
         *
         * EspecialidadServiceImpl utiliza:
         *
         * @Cacheable(CacheConfig.ESPECIALIDADES)
         */
        model.addAttribute(
                "especialidades",
                especialidadService.listarActivas()
        );


        /*
         * Catálogo de sucursales activas.
         *
         * SucursalServiceImpl utiliza:
         *
         * @Cacheable(CacheConfig.SUCURSALES)
         */
        model.addAttribute(
                "sucursales",
                sucursalService.listarActivas()
        );


        /*
         * Nombre del archivo Thymeleaf:
         *
         * templates/portal.html
         */
        return "portal";
    }
}