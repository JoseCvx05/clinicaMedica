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
import com.proyecto.clinicamedica.entity.SucursalEspecialidad;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;
import java.math.BigDecimal;
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
    // CU-14 - PRECIO RESERVADO POR SUCURSAL Y ESPECIALIDAD
    // =====================================================

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;


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
            SucursalEspecialidadRepository sucursalEspecialidadRepository,

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

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

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
                        .orElseThrow(  //si no encuentra el paciente, lanza una excepción
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
                        idSucursal // Verifica que la sucursal exista y esté activa.
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

        Usuario medico = // Bloquea al médico para evitar que otros pacientes lo reserven al mismo tiempo.
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

        // verifica el que medico este activo para poder reservar una cita con el, si no esta activo lanza una excepción.z
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
        // Se liberan las reservas anteriores del paciente para evitar que tenga múltiples reservas activas al mismo tiempo.
        liberarDelPacienteInterno(
                idPaciente
        );


        // =================================================
        // OBTENER SUCURSAL desde la base de datos para asegurarse de que existe y está activa.
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
        // OBTENER ESPECIALIDAD DESDE LA BASE DE DATOS PARA ASEGURARSE DE QUE EXISTE Y ESTÁ ACTIVA.
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


        // =====================================================
        // CU-14 - CATÁLOGO 9
        // OBTENER PRECIO REAL DE LA CONSULTA
        // =====================================================
        //
        // Precio obtenido directamente de PostgreSQL.
        // No se acepta un precio enviado por el navegador.
        //
        // =====================================================

        SucursalEspecialidad configuracion =
                sucursalEspecialidadRepository
                        .buscarActivaParaConfirmarConBloqueo(
                                idSucursal,
                                idEspecialidad
                        )
                        .orElseThrow(
                                () -> new ReservaCitaInvalidaException(
                                        "La especialidad ya no está "
                                                + "disponible en esta sucursal."
                                )
                        );

        // =====================================================
        // VALIDAR PRECIO CONFIGURADO
        // =====================================================

        BigDecimal precioReservado =
                configuracion.getPrecioConsulta();

        if (precioReservado == null
                || precioReservado.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ReservaCitaInvalidaException(
                    "La tarifa de consulta no está "
                            + "configurada correctamente."
            );
        }

        // =====================================================
        // CREAR RESERVA TEMPORAL
        // =====================================================


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


        // =====================================================
        // CU-14 - CONSERVAR PRECIO ACORDADO
        // =====================================================
        //
        // Este precio procede de la configuración
        // validada y bloqueada en PostgreSQL.
        //
        // Se guardará junto con la reserva temporal.
        //
        // =====================================================

        reserva.setPrecioConsultaReservado(
                precioReservado
        );



        reserva.setFechaHoraInicio(
                inicio
        );


        reserva.setFechaHoraFin(
                fin
        );


        reserva.setTokenReserva( // Genera un token único para la reserva temporal.
                UUID.randomUUID() // sirve para identificar la reserva de manera única y segura.
                        .toString() //lo convierte a cadena de texto para almacenarlo en la base de datos
        );

        // =================================================
        // ESTABLECER FECHA DE EXPIRACIÓN
        // =================================================
        reserva.setFechaExpiracion(
                OffsetDateTime
                        .now(
                                zonaHoraria
                        )
                        .plusMinutes(
                                duracionReservaMinutos
                        )
        );

        // GUARDAR RESERVA TEMPORAL ACTIVA LUEGO DE CREARLA
        reserva.setActiva(
                true
        );


        // =================================================
        // GUARDAR RESERVA // segunda barrera para corroborar que el horario haya sido ocupado
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

            //maneja el caso de que otro paciente haya ocupado el horario justo al momento de guardar la reserva.
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
                .getFechaExpiracion() //obtiene la fecha de expiración de la reserva temporal
                .isAfter( //verica si la fecha de expiración es después de la fecha actual
                        ahora
                )) {

            reserva.liberar(); //si la reserva ha expirado, se libera inmediatamente para que no quede activa en la base de datos

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

        // =================================================
        // OBTENER RESERVAS EXPIRADAS
        // =================================================
        //
        List<ReservaTemporalCita> expiradas =
                reservaTemporalCitaRepository
                        .findTop100ByActivaTrueAndFechaExpiracionLessThanEqualOrderByFechaExpiracionAsc(
                                //LessThanEqual(ahora) fecha menor o igual a ahora, o sea, ya venció.
                                //Top100 limita a 100 resultados para evitar sobrecargar la memoria si hay muchas reservas vencidas.
                                //for recorre la lista de reservas vencidas y las libera una por una.
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

            throw new ReservaCitaInvalidaException(    //throw new hace que se lance una excepción
                    "Los datos de la reserva están incompletos."
            );
        }



        if (!fin.isAfter( //verifica que la fecha de fin sea después de la fecha de inicio
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