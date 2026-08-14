package com.proyecto.clinicamedica.scheduler;

import com.proyecto.clinicamedica.service.ExpiracionPagoCitaService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * SCHEDULER: EXPIRACIÓN DE PAGO
 * =========================================================
 *
 * Revisa periódicamente las citas pendientes de pago
 * cuyo tiempo ya finalizó.
 * =========================================================
 */
@Component
public class ExpiracionPagoCitaScheduler {


    private final ExpiracionPagoCitaService
            expiracionPagoCitaService;


    public ExpiracionPagoCitaScheduler(
            ExpiracionPagoCitaService expiracionPagoCitaService
    ) {

        this.expiracionPagoCitaService =
                expiracionPagoCitaService;
    }


    @Scheduled(
            fixedDelayString =
                    "${cita.pago.limpieza-ms:60000}"
    )
    public void cancelarExpiradas() {

        expiracionPagoCitaService
                .cancelarCitasExpiradas();
    }
}