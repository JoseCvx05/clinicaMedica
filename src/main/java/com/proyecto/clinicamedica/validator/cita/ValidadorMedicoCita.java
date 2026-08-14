package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: MÉDICO
 * =========================================================
 *
 * CU-03 - Paso 3.
 *
 * Valida que exista un médico seleccionado.
 *
 * La comprobación de que el médico:
 *
 * - esté activo;
 * - tenga rol Médico;
 * - pertenezca a la sucursal;
 * - corresponda a la especialidad;
 *
 * será responsabilidad de un Service especializado.
 * =========================================================
 */
@Component
public class ValidadorMedicoCita
        implements ValidadorPasoCita {


    @Override
    public PasoCita paso() {

        return PasoCita.MEDICO;
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


        if (formulario.getIdMedico() == null) {

            /*
             * El documento de CU-03 exige seleccionar
             * médico, pero las reglas consolidadas no
             * proporcionan un mensaje específico para
             * médico vacío.
             *
             * Por eso este texto es una decisión de
             * implementación.
             */
            resultado.agregarError(
                    "idMedico",
                    "Debe seleccionar un médico para continuar."
            );
        }
    }
}