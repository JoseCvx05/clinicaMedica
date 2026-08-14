package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: SUCURSAL
 * =========================================================
 *
 * CU-03 - Paso 1.
 *
 * Valida que el paciente haya seleccionado
 * una sucursal antes de avanzar.
 *
 * No valida todavía si existen especialidades.
 * Esa es una condición diferente correspondiente
 * al flujo alterno FA01.
 * =========================================================
 */
@Component
public class ValidadorSucursalCita
        implements ValidadorPasoCita {


    @Override
    public PasoCita paso() {

        return PasoCita.SUCURSAL;
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


        if (formulario.getIdSucursal() == null) {

            resultado.agregarError(
                    "idSucursal",
                    "Debe seleccionar una sucursal para continuar."
            );
        }
    }
}