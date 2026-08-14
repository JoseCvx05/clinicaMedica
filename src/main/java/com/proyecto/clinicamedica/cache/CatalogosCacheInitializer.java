package com.proyecto.clinicamedica.cache;

import com.proyecto.clinicamedica.dto.cita.OpcionCatalogoCitaDTO;

import com.proyecto.clinicamedica.service.CatalogoCitaService;
import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * =========================================================
 * PRECARGA GENERAL DE CATÁLOGOS EN CACHÉ
 * =========================================================
 *
 * Se ejecuta cuando Spring Boot termina de iniciar.
 *
 * Precarga:
 *
 * - Roles.
 * - Especialidades.
 * - Sucursales.
 * - Sucursales disponibles para citas.
 * - Especialidades disponibles por sucursal para citas.
 *
 * Centraliza toda la precarga de catálogos en una sola
 * clase para evitar inicializadores duplicados.
 * =========================================================
 */
@Component
public class CatalogosCacheInitializer {


    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    CatalogosCacheInitializer.class
            );


    private final RolService rolService;

    private final EspecialidadService especialidadService;

    private final SucursalService sucursalService;

    private final CatalogoCitaService catalogoCitaService;


    public CatalogosCacheInitializer(
            RolService rolService,
            EspecialidadService especialidadService,
            SucursalService sucursalService,
            CatalogoCitaService catalogoCitaService
    ) {

        this.rolService =
                rolService;

        this.especialidadService =
                especialidadService;

        this.sucursalService =
                sucursalService;

        this.catalogoCitaService =
                catalogoCitaService;
    }


    /**
     * Precarga los catálogos después de que la aplicación
     * haya iniciado correctamente.
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

            // =================================================
            // CATÁLOGOS GENERALES
            // =================================================

            int totalRoles =
                    rolService
                            .listarActivos()
                            .size();


            int totalEspecialidades =
                    especialidadService
                            .listarActivas()
                            .size();


            int totalSucursales =
                    sucursalService
                            .listarActivas()
                            .size();


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


            // =================================================
            // CATÁLOGOS ESPECÍFICOS DE CITAS
            // =================================================

            List<OpcionCatalogoCitaDTO> sucursalesCita =
                    catalogoCitaService
                            .listarSucursales();


            int relacionesEspecialidadCargadas =
                    0;


            for (
                    OpcionCatalogoCitaDTO sucursal :
                    sucursalesCita
            ) {

                List<OpcionCatalogoCitaDTO> especialidades =
                        catalogoCitaService
                                .listarEspecialidades(
                                        sucursal.id()
                                );


                relacionesEspecialidadCargadas +=
                        especialidades.size();
            }


            LOGGER.info(
                    "Sucursales de citas cargadas en caché: {}",
                    sucursalesCita.size()
            );


            LOGGER.info(
                    "Relaciones sucursal-especialidad "
                            + "cargadas en caché: {}",
                    relacionesEspecialidadCargadas
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