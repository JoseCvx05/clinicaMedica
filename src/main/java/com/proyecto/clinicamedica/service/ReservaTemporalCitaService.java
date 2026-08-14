package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.ReservaTemporalCita;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.exception.HorarioNoDisponibleException;
import com.proyecto.clinicamedica.exception.ReservaCitaInvalidaException;

import com.proyecto.clinicamedica.model.cita.EstadoReservaTemporal;

import com.proyecto.clinicamedica.repository.EspecialidadRepository;
import com.proyecto.clinicamedica.repository.ReservaTemporalCitaRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * =========================================================
 * SERVICIO: RESERVA TEMPORAL DE CITA
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Responsabilidades:
 *
 * - Validar la selección.
 * - Volver a comprobar disponibilidad.
 * - Bloquear al médico durante la reserva.
 * - Liberar reservas anteriores del paciente.
 * - Crear una reserva temporal.
 * - Controlar expiración.
 * - Consultar estado.
 * - Liberar reservas.
 * - Liberar reservas vencidas.
 *
 * =========================================================
 */
@Service
public class ReservaTemporalCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String MENSAJE_HORARIO_OCUPADO =
            "El horario seleccionado ya no está disponible. "
                    + "Por favor, seleccione otro horario.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ReservaTemporalCitaRepository
            reservaTemporalCitaRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final SucursalRepository
            sucursalRepository;

    private final EspecialidadRepository
            especialidadRepository;

    private final DisponibilidadCitaService
            disponibilidadCitaService;

    private final CatalogoCitaService
            catalogoCitaService;

    private final int duracionReservaMinutos;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ReservaTemporalCitaService(
            ReservaTemporalCitaRepository reservaTemporalCitaRepository,
            UsuarioRepository usuarioRepository,
            SucursalRepository sucursalRepository,
            EspecialidadRepository especialidadRepository,
            DisponibilidadCitaService disponibilidadCitaService,
            CatalogoCitaService catalogoCitaService,

            @Value("${cita.reserva.duracion-minutos:5}")
            int duracionReservaMinutos,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.reservaTemporalCitaRepository =
                reservaTemporalCitaRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.sucursalRepository =
                sucursalRepository;

        this.especialidadRepository =
                especialidadRepository;

        this.disponibilidadCitaService =
                disponibilidadCitaService;

        this.catalogoCitaService =
                catalogoCitaService;

        this.duracionReservaMinutos =
                duracionReservaMinutos;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // CREAR RESERVA
    // =====================================================

    @Transactional
    public ReservaTemporalCita reservar(
            Integer idPaciente,
            Integer idMedico,
            Integer idSucursal,
            Integer idEspecialidad,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        validarParametros(
                idPaciente,
                idMedico,
                idSucursal,
                idEspecialidad,
                inicio,
                fin
        );


        // =================================================
        // PACIENTE
        // =================================================

        Usuario paciente =
                usuarioRepository
                        .findById(
                                idPaciente
                        )
                        .orElseThrow(
                                () ->
                                        new ReservaCitaInvalidaException(
                                                "El paciente no existe."
                                        )
                        );


        if (!Boolean.TRUE.equals(
                paciente.getActivo()
        )) {

            throw new ReservaCitaInvalidaException(
                    "El paciente no está activo."
            );
        }


        // =================================================
        // VALIDAR SUCURSAL
        // =================================================

        if (!catalogoCitaService
                .existeSucursalActiva(
                        idSucursal
                )) {

            throw new ReservaCitaInvalidaException(
                    "La sucursal seleccionada no está disponible."
            );
        }


        // =================================================
        // VALIDAR ESPECIALIDAD
        // =================================================

        if (!catalogoCitaService
                .especialidadDisponibleEnSucursal(
                        idSucursal,
                        idEspecialidad
                )) {

            throw new ReservaCitaInvalidaException(
                    "La especialidad seleccionada no está "
                            + "disponible en la sucursal."
            );
        }


        // =================================================
        // VALIDAR MÉDICO
        // =================================================

        if (!catalogoCitaService
                .medicoDisponibleParaSeleccion(
                        idMedico,
                        idSucursal,
                        idEspecialidad
                )) {

            throw new ReservaCitaInvalidaException(
                    "El médico seleccionado no corresponde "
                            + "con la sucursal y especialidad."
            );
        }


        // =================================================
        // BLOQUEAR MÉDICO
        // =================================================
        //
        // Esta consulta utiliza PESSIMISTIC_WRITE.
        //
        // De esta forma, dos pacientes que intenten
        // reservar al mismo médico simultáneamente
        // no podrán completar esta sección al mismo tiempo.
        // =================================================

        Usuario medico =
                usuarioRepository
                        .findByIdForUpdate(
                                idMedico
                        )
                        .orElseThrow(
                                () ->
                                        new ReservaCitaInvalidaException(
                                                "El médico no existe."
                                        )
                        );


        if (!Boolean.TRUE.equals(
                medico.getActivo()
        )) {

            throw new ReservaCitaInvalidaException(
                    "El médico seleccionado no está activo."
            );
        }


        // =================================================
        // RECOMPROBAR DISPONIBILIDAD
        // =================================================
        //
        // Esta comprobación debe realizarse DESPUÉS
        // de obtener el bloqueo del médico.
        // =================================================

        if (!disponibilidadCitaService
                .estaDisponible(
                        idMedico,
                        inicio,
                        fin
                )) {

            throw new HorarioNoDisponibleException(
                    MENSAJE_HORARIO_OCUPADO
            );
        }


        // =================================================
        // LIBERAR RESERVAS ANTERIORES DEL PACIENTE
        // =================================================

        liberarDelPacienteInterno(
                idPaciente
        );


        // =================================================
        // OBTENER SUCURSAL
        // =================================================

        Sucursal sucursal =
                sucursalRepository
                        .findById(
                                idSucursal
                        )
                        .orElseThrow(
                                () ->
                                        new ReservaCitaInvalidaException(
                                                "La sucursal no existe."
                                        )
                        );


        // =================================================
        // OBTENER ESPECIALIDAD
        // =================================================

        Especialidad especialidad =
                especialidadRepository
                        .findById(
                                idEspecialidad
                        )
                        .orElseThrow(
                                () ->
                                        new ReservaCitaInvalidaException(
                                                "La especialidad no existe."
                                        )
                        );


        // =================================================
        // CREAR RESERVA
        // =================================================

        ReservaTemporalCita reserva =
                new ReservaTemporalCita();


        reserva.setPaciente(
                paciente
        );


        reserva.setMedico(
                medico
        );


        reserva.setSucursal(
                sucursal
        );


        reserva.setEspecialidad(
                especialidad
        );


        reserva.setFechaHoraInicio(
                inicio
        );


        reserva.setFechaHoraFin(
                fin
        );


        reserva.setTokenReserva(
                UUID.randomUUID()
                        .toString()
        );


        reserva.setFechaExpiracion(
                OffsetDateTime
                        .now(
                                zonaHoraria
                        )
                        .plusMinutes(
                                duracionReservaMinutos
                        )
        );


        reserva.setActiva(
                true
        );


        // =================================================
        // GUARDAR RESERVA
        // =================================================

        try {

            /*
             * saveAndFlush() es intencional.
             *
             * Fuerza a PostgreSQL a validar inmediatamente
             * las restricciones e índices de la reserva.
             */
            return reservaTemporalCitaRepository
                    .saveAndFlush(
                            reserva
                    );

        } catch (DataIntegrityViolationException ex) {

            /*
             * Si otra transacción consiguió ocupar el
             * horario antes, la reserva actual falla y
             * se revierte.
             */
            throw new HorarioNoDisponibleException(
                    MENSAJE_HORARIO_OCUPADO
            );
        }
    }


    // =====================================================
    // BUSCAR RESERVA VIGENTE
    // =====================================================

    @Transactional
    public Optional<ReservaTemporalCita> buscarVigente(
            String tokenReserva
    ) {

        if (tokenReserva == null
                || tokenReserva.isBlank()) {

            return Optional.empty();
        }


        Optional<ReservaTemporalCita> encontrada =
                reservaTemporalCitaRepository
                        .findByTokenReservaAndActivaTrue(
                                tokenReserva
                        );


        if (encontrada.isEmpty()) {

            return Optional.empty();
        }


        ReservaTemporalCita reserva =
                encontrada.get();


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // COMPROBAR EXPIRACIÓN
        // =================================================

        if (!reserva
                .getFechaExpiracion()
                .isAfter(
                        ahora
                )) {

            reserva.liberar();

            return Optional.empty();
        }


        return Optional.of(
                reserva
        );
    }


    // =====================================================
    // OBTENER ESTADO
    // =====================================================

    @Transactional
    public EstadoReservaTemporal obtenerEstado(
            String tokenReserva
    ) {

        if (tokenReserva == null
                || tokenReserva.isBlank()) {

            return EstadoReservaTemporal.NO_ENCONTRADA;
        }


        Optional<ReservaTemporalCita> encontrada =
                reservaTemporalCitaRepository
                        .findByTokenReserva(
                                tokenReserva
                        );


        if (encontrada.isEmpty()) {

            return EstadoReservaTemporal.NO_ENCONTRADA;
        }


        ReservaTemporalCita reserva =
                encontrada.get();


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // EXPIRADA
        // =================================================

        if (!reserva
                .getFechaExpiracion()
                .isAfter(
                        ahora
                )) {

            /*
             * Puede permanecer activa unos segundos
             * hasta que el scheduler la procese.
             *
             * La liberamos inmediatamente cuando
             * detectamos la expiración.
             */
            if (Boolean.TRUE.equals(
                    reserva.getActiva()
            )) {

                reserva.liberar();
            }


            return EstadoReservaTemporal.EXPIRADA;
        }


        // =================================================
        // LIBERADA MANUALMENTE
        // =================================================

        if (!Boolean.TRUE.equals(
                reserva.getActiva()
        )) {

            return EstadoReservaTemporal.NO_ENCONTRADA;
        }


        return EstadoReservaTemporal.VIGENTE;
    }


    // =====================================================
    // LIBERAR POR TOKEN
    // =====================================================

    @Transactional
    public void liberar(
            String tokenReserva
    ) {

        if (tokenReserva == null
                || tokenReserva.isBlank()) {

            return;
        }


        reservaTemporalCitaRepository
                .findByTokenReservaAndActivaTrue(
                        tokenReserva
                )
                .ifPresent(
                        ReservaTemporalCita::liberar
                );
    }


    // =====================================================
    // LIBERAR RESERVAS DEL PACIENTE
    // =====================================================

    @Transactional
    public void liberarDelPaciente(
            Integer idPaciente
    ) {

        liberarDelPacienteInterno(
                idPaciente
        );
    }


    // =====================================================
    // LIBERACIÓN INTERNA DEL PACIENTE
    // =====================================================

    private void liberarDelPacienteInterno(
            Integer idPaciente
    ) {

        if (idPaciente == null) {

            return;
        }


        List<ReservaTemporalCita> reservas =
                reservaTemporalCitaRepository
                        .findByPaciente_IdAndActivaTrue(
                                idPaciente
                        );


        for (ReservaTemporalCita reserva :
                reservas) {

            reserva.liberar();
        }
    }


    // =====================================================
    // LIBERAR RESERVAS EXPIRADAS
    // =====================================================

    @Transactional
    public void liberarExpiradas() {

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        List<ReservaTemporalCita> expiradas =
                reservaTemporalCitaRepository
                        .findTop100ByActivaTrueAndFechaExpiracionLessThanEqualOrderByFechaExpiracionAsc(
                                ahora
                        );


        for (ReservaTemporalCita reserva :
                expiradas) {

            reserva.liberar();
        }
    }


    // =====================================================
    // VALIDACIÓN BÁSICA
    // =====================================================

    private void validarParametros(
            Integer idPaciente,
            Integer idMedico,
            Integer idSucursal,
            Integer idEspecialidad,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        if (idPaciente == null
                || idMedico == null
                || idSucursal == null
                || idEspecialidad == null
                || inicio == null
                || fin == null) {

            throw new ReservaCitaInvalidaException(
                    "Los datos de la reserva están incompletos."
            );
        }


        if (!fin.isAfter(
                inicio
        )) {

            throw new ReservaCitaInvalidaException(
                    "El intervalo seleccionado no es válido."
            );
        }


        if (duracionReservaMinutos <= 0) {

            throw new IllegalStateException(
                    "La duración de la reserva temporal "
                            + "debe ser mayor que cero."
            );
        }
    }
}