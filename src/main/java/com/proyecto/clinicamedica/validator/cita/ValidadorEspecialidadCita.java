package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: ESPECIALIDAD
 * =========================================================
 *
 * CU-03 - Paso 2.
 *
 * Valida que el paciente haya seleccionado una
 * especialidad antes de avanzar.
 *
 * La relación entre sucursal y especialidad se comprobará
 * posteriormente mediante un Service especializado.
 * =========================================================
 */
@Component
public class ValidadorEspecialidadCita
        implements ValidadorPasoCita {


    @Override
    public PasoCita paso() {

        return PasoCita.ESPECIALIDAD;
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


        if (formulario.getIdEspecialidad() == null) {

            resultado.agregarError(
                    "idEspecialidad",
                    "Debe seleccionar una especialidad médica para continuar."
            );
        }
    }
}