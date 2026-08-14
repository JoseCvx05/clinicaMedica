package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.service.CatalogoCitaService;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: MÉDICO DISPONIBLE
 * =========================================================
 *
 * CU-03 - Paso 3.
 *
 * Verifica que el médico:
 *
 * - esté activo;
 * - tenga rol Médico;
 * - corresponda a la sucursal;
 * - corresponda a la especialidad.
 *
 * =========================================================
 */
@Component
public class ValidadorMedicoDisponibleCita
        implements ValidadorPasoCita {


    private final CatalogoCitaService
            catalogoCitaService;


    public ValidadorMedicoDisponibleCita(
            CatalogoCitaService catalogoCitaService
    ) {

        this.catalogoCitaService =
                catalogoCitaService;
    }


    @Override
    public PasoCita paso() {

        return PasoCita.MEDICO;
    }


    @Override
    public int orden() {

        return 20;
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


        if (resultado.tieneError(
                "idMedico"
        )) {

            return;
        }


        boolean valido =
                catalogoCitaService
                        .medicoDisponibleParaSeleccion(
                                formulario.getIdMedico(),
                                formulario.getIdSucursal(),
                                formulario.getIdEspecialidad()
                        );


        if (!valido) {

            resultado.agregarError(
                    "idMedico",

                    "El médico seleccionado no está disponible "
                            + "para la sucursal y especialidad seleccionadas."
            );
        }
    }
}