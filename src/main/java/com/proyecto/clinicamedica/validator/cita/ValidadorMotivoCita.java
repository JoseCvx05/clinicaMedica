package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: MOTIVO DE CONSULTA
 * =========================================================
 *
 * CU-03 - Paso 5.
 *
 * El motivo:
 *
 * - es obligatorio;
 * - mínimo 10 caracteres;
 * - máximo 2000 caracteres.
 *
 * =========================================================
 */
@Component
public class ValidadorMotivoCita
        implements ValidadorPasoCita {


    @Override
    public PasoCita paso() {

        return PasoCita.CONFIRMACION;
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


        String motivo =
                limpiar(
                        formulario.getMotivoConsulta()
                );


        int longitud =
                motivo.length();


        if (longitud < 10
                || longitud > 2000) {

            resultado.agregarError(
                    "motivoConsulta",

                    "El motivo debe contener entre 10 y 2000 "
                            + "caracteres. Usted ingresó "
                            + longitud
                            + " caracteres."
            );
        }
    }


    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }
}