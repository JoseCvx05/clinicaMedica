package com.proyecto.clinicamedica.service.disponibilidad;

import com.proyecto.clinicamedica.repository.EventoAgendaRepository;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * FUENTE DE OCUPACIÓN: EVENTO DE AGENDA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Comprueba si un evento registrado en la agenda
 * del médico bloquea el intervalo solicitado.
 * =========================================================
 */
@Component
public class FuenteEventoAgendaHorario
        implements FuenteOcupacionHorario {


    private final EventoAgendaRepository
            eventoAgendaRepository;


    public FuenteEventoAgendaHorario(
            EventoAgendaRepository eventoAgendaRepository
    ) {

        this.eventoAgendaRepository =
                eventoAgendaRepository;
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


        if (!fin.isAfter(
                inicio
        )) {

            return true;
        }


        return eventoAgendaRepository
                .existeEventoSolapado(
                        idMedico,
                        inicio,
                        fin
                );
    }
}