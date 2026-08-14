package com.proyecto.clinicamedica.scheduler;

import com.proyecto.clinicamedica.service.ReservaTemporalCitaService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * SCHEDULER: RESERVAS TEMPORALES
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Libera periódicamente reservas cuyo tiempo
 * de confirmación ya venció.
 *
 * =========================================================
 */
@Component
public class ReservaTemporalCitaScheduler {


    private final ReservaTemporalCitaService
            reservaTemporalCitaService;


    public ReservaTemporalCitaScheduler(
            ReservaTemporalCitaService reservaTemporalCitaService
    ) {

        this.reservaTemporalCitaService =
                reservaTemporalCitaService;
    }


    @Scheduled(
            fixedDelayString =
                    "${cita.reserva.limpieza-ms:60000}"
    )
    public void liberarReservasExpiradas() {

        reservaTemporalCitaService
                .liberarExpiradas();
    }
}