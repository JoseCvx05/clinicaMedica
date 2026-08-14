package com.proyecto.clinicamedica.scheduler;

import com.proyecto.clinicamedica.service.ReintentoNotificacionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * =========================================================
 * SCHEDULER: REINTENTO DE NOTIFICACIONES
 * =========================================================
 *
 * Ejecuta periódicamente el procesamiento de correos
 * pendientes o fallidos que todavía admiten reintentos.
 *
 * La lógica de negocio pertenece a:
 *
 * ReintentoNotificacionService
 *
 * Este componente únicamente determina CUÁNDO ejecutar
 * dicha lógica.
 *
 * =========================================================
 */
@Component
public class ReintentoNotificacionScheduler {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final ReintentoNotificacionService
            reintentoNotificacionService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ReintentoNotificacionScheduler(
            ReintentoNotificacionService reintentoNotificacionService
    ) {

        this.reintentoNotificacionService =
                reintentoNotificacionService;
    }


    // =====================================================
    // PROCESAR NOTIFICACIONES
    // =====================================================

    /**
     * Por defecto se ejecuta cada 60 segundos.
     *
     * El valor puede modificarse desde
     * application.properties.
     */
    @Scheduled(
            fixedDelayString =
                    "${notificacion.reintento.intervalo-ms:60000}"
    )
    public void procesarNotificacionesPendientes() {

        reintentoNotificacionService
                .procesarPendientes();
    }
}