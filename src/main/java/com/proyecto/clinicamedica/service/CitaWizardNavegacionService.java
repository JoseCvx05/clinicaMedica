package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import org.springframework.stereotype.Service;


/**
 * =========================================================
 * SERVICIO: NAVEGACIÓN DEL WIZARD DE CITAS
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Controla los retrocesos entre pasos del wizard.
 *
 * Responsabilidades:
 *
 * - Liberar una reserva temporal cuando el paciente
 *   regresa desde un paso posterior.
 *
 * - Limpiar selecciones que ya no son válidas.
 *
 * - Actualizar el paso actual del wizard.
 *
 * =========================================================
 */
@Service
public class CitaWizardNavegacionService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final ReservaTemporalCitaService
            reservaTemporalCitaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CitaWizardNavegacionService(
            ReservaTemporalCitaService reservaTemporalCitaService
    ) {

        this.reservaTemporalCitaService =
                reservaTemporalCitaService;
    }


    // =====================================================
    // FA04 - REGRESAR A UN PASO
    // =====================================================

    public void regresarA(
            CitaWizardDTO wizard,
            PasoCita pasoDestino
    ) {

        if (wizard == null
                || pasoDestino == null) {

            return;
        }


        // =================================================
        // LIBERAR RESERVA TEMPORAL
        // =================================================

        liberarReservaSiExiste(
                wizard
        );


        // =================================================
        // LIMPIAR INFORMACIÓN POSTERIOR
        // =================================================

        switch (pasoDestino) {

            case SUCURSAL -> {

                wizard.setIdEspecialidad(
                        null
                );

                wizard.setIdMedico(
                        null
                );

                limpiarHorario(
                        wizard
                );

                limpiarConfirmacion(
                        wizard
                );
            }


            case ESPECIALIDAD -> {

                wizard.setIdMedico(
                        null
                );

                limpiarHorario(
                        wizard
                );

                limpiarConfirmacion(
                        wizard
                );
            }


            case MEDICO -> {

                limpiarHorario(
                        wizard
                );

                limpiarConfirmacion(
                        wizard
                );
            }


            case FECHA_HORA -> {

                /*
                 * La reserva anterior ya fue liberada.
                 *
                 * El paciente debe seleccionar nuevamente
                 * un horario disponible.
                 */
                limpiarHorario(
                        wizard
                );

                limpiarConfirmacion(
                        wizard
                );
            }


            case CONFIRMACION -> {

                /*
                 * No existen selecciones posteriores
                 * que deban limpiarse.
                 */
            }
        }


        // =================================================
        // ACTUALIZAR PASO
        // =================================================

        wizard.setPasoActual(
                pasoDestino
        );
    }


    // =====================================================
    // LIBERAR RESERVA
    // =====================================================

    private void liberarReservaSiExiste(
            CitaWizardDTO wizard
    ) {

        String token =
                wizard.getTokenReserva();


        if (token == null
                || token.isBlank()) {

            return;
        }


        reservaTemporalCitaService
                .liberar(
                        token
                );


        wizard.setTokenReserva(
                null
        );


        wizard.setFechaExpiracionReserva(
                null
        );
    }


    // =====================================================
    // LIMPIAR HORARIO
    // =====================================================

    private void limpiarHorario(
            CitaWizardDTO wizard
    ) {

        wizard.setFechaHoraInicio(
                null
        );


        wizard.setFechaHoraFin(
                null
        );
    }


    // =====================================================
    // LIMPIAR CONFIRMACIÓN
    // =====================================================

    private void limpiarConfirmacion(
            CitaWizardDTO wizard
    ) {

        wizard.setMotivoConsulta(
                null
        );
    }
}