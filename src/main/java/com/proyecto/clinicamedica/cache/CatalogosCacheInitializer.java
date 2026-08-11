package com.proyecto.clinicamedica.cache;

import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * PRECARGA DE CATÁLOGOS EN CACHÉ
 * =========================================================
 *
 * Se ejecuta automáticamente cuando Spring Boot termina
 * de iniciar correctamente.
 *
 * Su responsabilidad es precargar los catálogos requeridos
 * por el portal para que las primeras solicitudes de los
 * usuarios no tengan que consultar directamente PostgreSQL.
 *
 * Catálogos iniciales:
 *
 * - Roles
 * - Especialidades
 * - Sucursales
 *
 * Las dependencias se realizan mediante interfaces de
 * servicio, manteniendo inversión de dependencias (SOLID).
 * =========================================================
 */
@Component
public class CatalogosCacheInitializer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CatalogosCacheInitializer.class);

    private final RolService rolService;
    private final EspecialidadService especialidadService;
    private final SucursalService sucursalService;

    public CatalogosCacheInitializer(
            RolService rolService,
            EspecialidadService especialidadService,
            SucursalService sucursalService
    ) {
        this.rolService = rolService;
        this.especialidadService = especialidadService;
        this.sucursalService = sucursalService;
    }


    /**
     * Precarga los catálogos cuando la aplicación
     * se encuentra completamente iniciada.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void precargarCatalogos() {

        LOGGER.info(
                "================================================="
        );

        LOGGER.info(
                "Iniciando precarga de catálogos en caché..."
        );

        try {

            int totalRoles =
                    rolService.listarActivos().size();

            int totalEspecialidades =
                    especialidadService.listarActivas().size();

            int totalSucursales =
                    sucursalService.listarActivas().size();


            LOGGER.info(
                    "Roles cargados en caché: {}",
                    totalRoles
            );

            LOGGER.info(
                    "Especialidades cargadas en caché: {}",
                    totalEspecialidades
            );

            LOGGER.info(
                    "Sucursales cargadas en caché: {}",
                    totalSucursales
            );

            LOGGER.info(
                    "Precarga de catálogos finalizada correctamente."
            );

        } catch (Exception exception) {

            LOGGER.error(
                    "Error durante la precarga de catálogos.",
                    exception
            );
        }

        LOGGER.info(
                "================================================="
        );
    }
}