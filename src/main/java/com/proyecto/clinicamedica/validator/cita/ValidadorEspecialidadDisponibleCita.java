package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.OpcionCatalogoCitaDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.service.CatalogoCitaService;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: ESPECIALIDAD DISPONIBLE
 * =========================================================
 *
 * CU-03 - Paso 2.
 *
 * Comprueba:
 *
 * - que la especialidad pertenezca a la sucursal;
 * - que existan médicos disponibles para la combinación.
 *
 * Implementa la primera parte de FA02.
 * =========================================================
 */
@Component
public class ValidadorEspecialidadDisponibleCita
        implements ValidadorPasoCita {


    private final CatalogoCitaService
            catalogoCitaService;


    public ValidadorEspecialidadDisponibleCita(
            CatalogoCitaService catalogoCitaService
    ) {

        this.catalogoCitaService =
                catalogoCitaService;
    }


    @Override
    public PasoCita paso() {

        return PasoCita.ESPECIALIDAD;
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
                "idEspecialidad"
        )) {

            return;
        }


        Integer idSucursal =
                formulario.getIdSucursal();

        Integer idEspecialidad =
                formulario.getIdEspecialidad();


        // =================================================
        // ESPECIALIDAD DEBE PERTENECER A LA SUCURSAL
        // =================================================

        if (!catalogoCitaService
                .especialidadDisponibleEnSucursal(
                        idSucursal,
                        idEspecialidad
                )) {

            resultado.agregarError(
                    "idEspecialidad",

                    "La especialidad seleccionada no está "
                            + "disponible en la sucursal seleccionada."
            );

            return;
        }


        // =================================================
        // FA02 - SIN MÉDICOS
        // =================================================

        if (catalogoCitaService
                .listarMedicos(
                        idSucursal,
                        idEspecialidad
                )
                .isEmpty()) {

            resultado.agregarError(
                    "idEspecialidad",

                    construirMensajeSinDisponibilidad(
                            formulario
                    )
            );
        }
    }


    // =====================================================
    // MENSAJE FA02
    // =====================================================

    private String construirMensajeSinDisponibilidad(
            CitaWizardDTO formulario
    ) {

        String especialidad =
                buscarNombreEspecialidad(
                        formulario.getIdSucursal(),
                        formulario.getIdEspecialidad()
                );


        String sucursal =
                buscarNombreSucursal(
                        formulario.getIdSucursal()
                );


        return "No se encontraron horarios disponibles para la "
                + "especialidad "
                + especialidad
                + " en la Sede "
                + sucursal
                + ". Por favor, seleccione otra especialidad o sede.";
    }


    private String buscarNombreSucursal(
            Integer idSucursal
    ) {

        return catalogoCitaService
                .listarSucursales()
                .stream()
                .filter(
                        opcion ->
                                idSucursal.equals(
                                        opcion.id()
                                )
                )
                .map(
                        OpcionCatalogoCitaDTO::nombre
                )
                .findFirst()
                .orElse("seleccionada");
    }


    private String buscarNombreEspecialidad(
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
                                idEspecialidad.equals(
                                        opcion.id()
                                )
                )
                .map(
                        OpcionCatalogoCitaDTO::nombre
                )
                .findFirst()
                .orElse("seleccionada");
    }
}