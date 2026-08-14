package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.ReservaTemporalCita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.exception.ReservaCitaInvalidaException;
import com.proyecto.clinicamedica.exception.ReservaExpiradaException;

import com.proyecto.clinicamedica.model.cita.EstadoReservaTemporal;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;


/**
 * =========================================================
 * SERVICIO: CONFIRMACIÓN DE CITA
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Registra definitivamente una cita únicamente cuando
 * la reserva temporal continúa vigente.
 *
 * La cita se crea inicialmente con estado:
 *
 * "Pendiente de pago"
 *
 * =========================================================
 */
@Service
public class ConfirmacionCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ESTADO_PENDIENTE_PAGO =
            "Pendiente de pago";


    private static final String MENSAJE_RESERVA_EXPIRADA =
            "El tiempo para confirmar su cita ha expirado. "
                    + "El horario seleccionado ha sido liberado. "
                    + "Por favor, seleccione un nuevo horario.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final EstadoCitaRepository estadoCitaRepository;

    private final ReservaTemporalCitaService
            reservaTemporalCitaService;

    private final int duracionPagoMinutos;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ConfirmacionCitaService(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            ReservaTemporalCitaService reservaTemporalCitaService,

            @Value("${cita.pago.duracion-minutos:5}")
            int duracionPagoMinutos,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.reservaTemporalCitaService =
                reservaTemporalCitaService;

        this.duracionPagoMinutos =
                duracionPagoMinutos;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // CONFIRMAR CITA
    // =====================================================

    @Transactional
    public Cita confirmar(
            Usuario paciente,
            CitaWizardDTO wizard
    ) {

        // =================================================
        // VALIDAR ENTRADA
        // =================================================

        validarEntrada(
                paciente,
                wizard
        );


        // =================================================
        // VERIFICAR ESTADO DE LA RESERVA
        // =================================================

        EstadoReservaTemporal estadoReserva =
                reservaTemporalCitaService
                        .obtenerEstado(
                                wizard.getTokenReserva()
                        );


        // =================================================
        // RESERVA EXPIRADA
        // =================================================

        if (estadoReserva
                == EstadoReservaTemporal.EXPIRADA) {

            throw new ReservaExpiradaException(
                    MENSAJE_RESERVA_EXPIRADA
            );
        }


        // =================================================
        // RESERVA NO DISPONIBLE
        // =================================================

        if (estadoReserva
                != EstadoReservaTemporal.VIGENTE) {

            throw new ReservaCitaInvalidaException(
                    "La reserva temporal ya no está disponible."
            );
        }


        // =================================================
        // OBTENER RESERVA VIGENTE
        // =================================================

        ReservaTemporalCita reserva =
                reservaTemporalCitaService
                        .buscarVigente(
                                wizard.getTokenReserva()
                        )
                        .orElseThrow(
                                () ->
                                        new ReservaExpiradaException(
                                                MENSAJE_RESERVA_EXPIRADA
                                        )
                        );


        // =================================================
        // VALIDAR PROPIETARIO DE LA RESERVA
        // =================================================

        if (reserva.getPaciente() == null
                || reserva.getPaciente().getId() == null
                || !paciente
                .getId()
                .equals(
                        reserva
                                .getPaciente()
                                .getId()
                )) {

            throw new ReservaCitaInvalidaException(
                    "La reserva no pertenece al paciente autenticado."
            );
        }


        // =================================================
        // OBTENER ESTADO "PENDIENTE DE PAGO"
        // =================================================

        EstadoCita estadoPendientePago =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_PENDIENTE_PAGO
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita "
                                                        + "'Pendiente de pago'."
                                        )
                        );


        // =================================================
        // CREAR CITA
        // =================================================

        Cita cita =
                new Cita();


        cita.setPaciente(
                paciente
        );


        /*
         * Los datos críticos se obtienen directamente
         * de la reserva temporal validada.
         *
         * No confiamos en valores enviados nuevamente
         * desde el navegador.
         */
        cita.setMedico(
                reserva.getMedico()
        );


        cita.setSucursal(
                reserva.getSucursal()
        );


        cita.setEspecialidad(
                reserva.getEspecialidad()
        );


        cita.setFechaHoraCita(
                reserva.getFechaHoraInicio()
        );


        cita.setFechaHoraFin(
                reserva.getFechaHoraFin()
        );


        // =================================================
        // ESTADO
        // =================================================

        cita.setEstadoCita(
                estadoPendientePago
        );


        // =================================================
        // MOTIVO
        // =================================================

        cita.setMotivoConsulta(
                wizard
                        .getMotivoConsulta()
                        .trim()
        );


        // =================================================
        // DATOS GENERALES
        // =================================================

        cita.setPrioridad(
                "Normal"
        );


        cita.setCanalOrigen(
                "Portal Web"
        );


        cita.setEsSeguimiento(
                false
        );


        // =================================================
        // AUDITORÍA
        // =================================================

        cita.setCreadoPor(
                paciente
        );


        // =================================================
        // EXPIRACIÓN DEL PAGO
        // =================================================

        cita.setFechaExpiracionPago(
                OffsetDateTime
                        .now(
                                zonaHoraria
                        )
                        .plusMinutes(
                                duracionPagoMinutos
                        )
        );


        // =================================================
        // GUARDAR CITA
        // =================================================

        Cita citaGuardada =
                citaRepository
                        .saveAndFlush(
                                cita
                        );


        // =================================================
        // LIBERAR RESERVA TEMPORAL
        // =================================================
        //
        // La reserva temporal ya no necesita bloquear
        // el horario porque ahora existe una Cita real.
        // =================================================

        reservaTemporalCitaService
                .liberar(
                        wizard.getTokenReserva()
                );


        return citaGuardada;
    }


    // =====================================================
    // VALIDAR ENTRADA
    // =====================================================

    private void validarEntrada(
            Usuario paciente,
            CitaWizardDTO wizard
    ) {

        // =================================================
        // PACIENTE
        // =================================================

        if (paciente == null
                || paciente.getId() == null) {

            throw new ReservaCitaInvalidaException(
                    "No existe un paciente autenticado."
            );
        }


        // =================================================
        // WIZARD
        // =================================================

        if (wizard == null) {

            throw new ReservaCitaInvalidaException(
                    "Los datos de la cita no están disponibles."
            );
        }


        // =================================================
        // TOKEN DE RESERVA
        // =================================================

        if (wizard.getTokenReserva() == null
                || wizard
                .getTokenReserva()
                .isBlank()) {

            throw new ReservaCitaInvalidaException(
                    "No existe una reserva temporal para confirmar."
            );
        }


        // =================================================
        // MOTIVO
        // =================================================

        if (wizard.getMotivoConsulta() == null
                || wizard
                .getMotivoConsulta()
                .isBlank()) {

            throw new ReservaCitaInvalidaException(
                    "El motivo de consulta es obligatorio."
            );
        }
    }
}