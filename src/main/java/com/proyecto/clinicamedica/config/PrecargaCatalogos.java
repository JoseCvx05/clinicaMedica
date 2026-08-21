package com.proyecto.clinicamedica.config;

import com.proyecto.clinicamedica.service.CatalogoEstadoCitaService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import org.springframework.stereotype.Component;


/**
 * =========================================================
 * PRECARGA DE CATÁLOGOS
 * =========================================================
 *
 * Ejecuta la carga inicial de los catálogos que deben
 * estar disponibles en caché cuando inicia el sistema.
 *
 * =========================================================
 */
@Component
public class PrecargaCatalogos
        implements ApplicationRunner {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final CatalogoEstadoCitaService
            catalogoEstadoCitaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PrecargaCatalogos(
            CatalogoEstadoCitaService catalogoEstadoCitaService
    ) {

        this.catalogoEstadoCitaService =
                catalogoEstadoCitaService;
    }


    // =====================================================
    // PRECARGAR
    // =====================================================

    @Override
    public void run(
            ApplicationArguments args
    ) {


        catalogoEstadoCitaService
                .listarEstadosActivos();


        System.out.println(
                ">>> CATÁLOGO ESTADOS_CITA PRECARGADO EN CACHÉ"
        );
    }
}