package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.OpcionCatalogoCitaDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.service.CatalogoCitaService;
import com.proyecto.clinicamedica.service.DisponibilidadCitaService;

import org.springframework.stereotype.Component;


/**
 * =========================================================
 * VALIDADOR: DISPONIBILIDAD REAL DEL HORARIO
 * =========================================================
 *
 * CU-03 - Paso 4.
 *
 * Se ejecuta después de validar que exista una fecha
 * futura.
 *
 * Implementa la segunda parte de FA02.
 * =========================================================
 */
@Component
public class ValidadorDisponibilidadHorarioCita
        implements ValidadorPasoCita {


    private final DisponibilidadCitaService
            disponibilidadCitaService;

    private final CatalogoCitaService
            catalogoCitaService;


    public ValidadorDisponibilidadHorarioCita(
            DisponibilidadCitaService disponibilidadCitaService,
            CatalogoCitaService catalogoCitaService
    ) {

        this.disponibilidadCitaService =
                disponibilidadCitaService;

        this.catalogoCitaService =
                catalogoCitaService;
    }


    @Override
    public PasoCita paso() {

        return PasoCita.FECHA_HORA;
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
                "fechaHoraInicio"
        )) {

            return;
        }


        boolean disponible =
                disponibilidadCitaService
                        .estaDisponible(
                                formulario.getIdMedico(),
                                formulario.getFechaHoraInicio(),
                                formulario.getFechaHoraFin()
                        );


        if (!disponible) {

            resultado.agregarError(
                    "fechaHoraInicio",
                    construirMensajeFA02(
                            formulario
                    )
            );
        }
    }


    private String construirMensajeFA02(
            CitaWizardDTO formulario
    ) {

        String sucursal =
                buscarSucursal(
                        formulario.getIdSucursal()
                );


        String especialidad =
                buscarEspecialidad(
                        formulario.getIdSucursal(),
                        formulario.getIdEspecialidad()
                );


        return "No se encontraron horarios disponibles para la "
                + "especialidad "
                + especialidad
                + " en la Sede "
                + sucursal
                + ". Por favor, seleccione otra especialidad o sede.";
    }


    private String buscarSucursal(
            Integer idSucursal
    ) {

        return catalogoCitaService
                .listarSucursales()
                .stream()
                .filter(
                        opcion ->
                                idSucursal != null
                                        && idSucursal.equals(
                                        opcion.id()
                                )
                )
                .map(
                        OpcionCatalogoCitaDTO::nombre
                )
                .findFirst()
                .orElse(
                        "seleccionada"
                );
    }


    private String buscarEspecialidad(
            Integer idSucursal,
            Integer idEspecialidad
    ) {

        return catalogoCitaService
                .listarEspecialidades(
                        idSucursal
                )
                .stream()
                .filter(
                        opcion ->
                                idEspecialidad != null
                                        && idEspecialidad.equals(
                                        opcion.id()
                                )
                )
                .map(
                        OpcionCatalogoCitaDTO::nombre
                )
                .findFirst()
                .orElse(
                        "seleccionada"
                );
    }
}