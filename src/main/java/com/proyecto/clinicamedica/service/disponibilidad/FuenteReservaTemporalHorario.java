package com.proyecto.clinicamedica.service.disponibilidad;

import com.proyecto.clinicamedica.repository.ReservaTemporalCitaRepository;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * FUENTE DE OCUPACIÓN: RESERVA TEMPORAL
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Determina si un intervalo está temporalmente reservado
 * por otro proceso de agendamiento.
 *
 * Una reserva únicamente bloquea disponibilidad cuando:
 *
 * - activa = true;
 * - todavía no ha expirado;
 * - su intervalo se cruza con el solicitado.
 *
 * =========================================================
 */
@Component
public class FuenteReservaTemporalHorario
        implements FuenteOcupacionHorario {


    private final ReservaTemporalCitaRepository
            reservaTemporalCitaRepository;


    public FuenteReservaTemporalHorario(
            ReservaTemporalCitaRepository reservaTemporalCitaRepository
    ) {

        this.reservaTemporalCitaRepository =
                reservaTemporalCitaRepository;
    }


    // =====================================================
    // ¿ESTÁ OCUPADO?
    // =====================================================

    @Override
    public boolean estaOcupado(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        if (idMedico == null
                || inicio == null
                || fin == null) {

            return true;
        }


        /*
         * Un intervalo inválido nunca debe considerarse
         * disponible.
         */
        if (!fin.isAfter(inicio)) {

            return true;
        }


        return reservaTemporalCitaRepository
                .existeReservaActivaSolapada(
                        idMedico,
                        inicio,
                        fin,
                        OffsetDateTime.now()
                );
    }
}