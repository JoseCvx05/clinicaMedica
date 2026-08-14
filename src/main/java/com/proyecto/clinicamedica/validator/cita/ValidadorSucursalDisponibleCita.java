package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.OpcionCatalogoCitaDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.service.CatalogoCitaService;

import org.springframework.stereotype.Component;

/**
 * =========================================================
 * VALIDADOR: SUCURSAL DISPONIBLE
 * =========================================================
 *
 * CU-03 - Paso 1.
 *
 * Comprueba:
 *
 * - que la sucursal exista y esté activa;
 * - que tenga especialidades configuradas.
 *
 * Implementa FA01.
 * =========================================================
 */
@Component
public class ValidadorSucursalDisponibleCita
        implements ValidadorPasoCita {


    private final CatalogoCitaService
            catalogoCitaService;


    public ValidadorSucursalDisponibleCita(
            CatalogoCitaService catalogoCitaService
    ) {

        this.catalogoCitaService =
                catalogoCitaService;
    }


    @Override
    public PasoCita paso() {

        return PasoCita.SUCURSAL;
    }


    @Override
    public int orden() {

        /*
         * Se ejecuta después de
         * ValidadorSucursalCita (orden 10).
         */
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


        /*
         * Si ya falló la obligatoriedad,
         * no seguimos consultando catálogos.
         */
        if (resultado.tieneError(
                "idSucursal"
        )) {

            return;
        }


        Integer idSucursal =
                formulario.getIdSucursal();


        // =================================================
        // SUCURSAL ACTIVA Y EXISTENTE
        // =================================================

        if (!catalogoCitaService
                .existeSucursalActiva(
                        idSucursal
                )) {

            resultado.agregarError(
                    "idSucursal",
                    "La sucursal seleccionada no está disponible."
            );

            return;
        }


        // =================================================
        // FA01
        // =================================================

        if (catalogoCitaService
                .listarEspecialidades(
                        idSucursal
                )
                .isEmpty()) {

            String nombreSucursal =
                    obtenerNombreSucursal(
                            idSucursal
                    );


            resultado.agregarError(
                    "idSucursal",

                    "No hay especialidades disponibles para la "
                            + "sucursal "
                            + nombreSucursal
                            + ". Seleccione otra sucursal."
            );
        }
    }


    // =====================================================
    // NOMBRE DE SUCURSAL
    // =====================================================

    private String obtenerNombreSucursal(
            Integer idSucursal
    ) {

        return catalogoCitaService
                .listarSucursales()
                .stream()
                .filter(
                        sucursal ->
                                idSucursal.equals(
                                        sucursal.id()
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