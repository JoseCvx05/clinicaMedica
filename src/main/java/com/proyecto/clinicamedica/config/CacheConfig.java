package com.proyecto.clinicamedica.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * =========================================================
 * CONFIGURACIÓN DE CACHÉ
 * =========================================================
 *
 * Configura Caffeine como proveedor de caché local
 * para los catálogos del sistema.
 *
 * Los catálogos serán precargados al iniciar
 * la aplicación.
 * =========================================================
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Nombres centralizados de las cachés.
     *
     * Conforme avancemos con los casos de uso podremos
     * agregar otros catálogos a esta lista.
     */
    public static final String ROLES = "roles";
    public static final String ESPECIALIDADES = "especialidades";
    public static final String SUCURSALES = "sucursales";
    public static final String ESTADOS_CITA = "estadosCita";


    /**
     * Configura el administrador de caché Caffeine.
     */
    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager();

        /*
         * Definimos explícitamente las cachés permitidas.
         */
        cacheManager.setCacheNames(
                List.of(
                        ROLES,
                        ESPECIALIDADES,
                        SUCURSALES,
                        ESTADOS_CITA
                )
        );

        /*
         * Configuración general de Caffeine.
         *
         * maximumSize evita que una caché pueda crecer
         * indefinidamente por error.
         *
         * recordStats permitirá consultar estadísticas
         * posteriormente durante pruebas.
         */
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .recordStats()
        );

        return cacheManager;
    }
}