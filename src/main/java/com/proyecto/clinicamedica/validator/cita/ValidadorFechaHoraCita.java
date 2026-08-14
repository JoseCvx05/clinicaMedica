package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * VALIDADOR: FECHA Y HORA
 * =========================================================
 *
 * CU-03 - Paso 4.
 *
 * Valida:
 *
 * - fecha/hora obligatoria;
 * - fecha/hora futura.
 *
 * La disponibilidad real del horario será validada
 * posteriormente por DisponibilidadCitaService.
 * =========================================================
 */
@Component
public class ValidadorFechaHoraCita
        implements ValidadorPasoCita {


    @Override
    public PasoCita paso() {

        return PasoCita.FECHA_HORA;
    }


    @Override
    public int orden() {

        return 10;
    }


    @Override
    public void validar(
            CitaWizardDTO formulario,
            ResultadoValidacionCita resultado
    ) {

        if (formulario == null
                || resultado == null) {

            return;
        }


        OffsetDateTime fechaHora =
                formulario.getFechaHoraInicio();


        if (fechaHora == null
                || !fechaHora.isAfter(
                OffsetDateTime.now()
        )) {

            resultado.agregarError(
                    "fechaHoraInicio",

                    "Debe seleccionar una fecha y hora futuras. "
                            + "Las citas no pueden agendarse en "
                            + "fechas pasadas o presentes."
            );
        }
    }
}