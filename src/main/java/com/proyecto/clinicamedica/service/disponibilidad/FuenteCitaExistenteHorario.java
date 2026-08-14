package com.proyecto.clinicamedica.service.disponibilidad;

import com.proyecto.clinicamedica.repository.CitaRepository;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * FUENTE DE OCUPACIÓN: CITA EXISTENTE
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Determina si el médico ya posee una cita registrada
 * que se cruza con el intervalo solicitado.
 *
 * Las citas canceladas no bloquean disponibilidad.
 * =========================================================
 */
@Component
public class FuenteCitaExistenteHorario
        implements FuenteOcupacionHorario {


    private final CitaRepository
            citaRepository;


    public FuenteCitaExistenteHorario(
            CitaRepository citaRepository
    ) {

        this.citaRepository =
                citaRepository;
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


        return citaRepository
                .existeCitaSolapada(
                        idMedico,
                        inicio,
                        fin,
                        OffsetDateTime.now()
                );
    }
}