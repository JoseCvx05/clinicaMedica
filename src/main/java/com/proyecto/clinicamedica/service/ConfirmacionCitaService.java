package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;
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

    // =====================================================
// CU-03 / CU-13 - PROTECCIÓN DE SEDE Y ESPECIALIDAD
// =====================================================

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final ReservaTemporalCitaService
            reservaTemporalCitaService;

    private final SeguimientoCitaService
            seguimientoCitaService;

    private final int duracionPagoMinutos;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================


    public ConfirmacionCitaService(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            SeguimientoCitaService seguimientoCitaService,
            ReservaTemporalCitaService reservaTemporalCitaService,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,

            @Value("${cita.pago.duracion-minutos:10}")
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

        this.seguimientoCitaService =
                seguimientoCitaService;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.duracionPagoMinutos =
                duracionPagoMinutos;

        this.zonaHoraria =
                ZoneId.of(zonaHoraria);
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
// SEGURIDAD
//
// El flujo normal del paciente NO puede crear
// citas de seguimiento.
// =================================================

        if (Boolean.TRUE.equals(
                wizard.getEsSeguimiento()
        )) {

            throw new ReservaCitaInvalidaException(
                    "Las citas de seguimiento solamente pueden "
                            + "ser agendadas por el Médico responsable."
            );
        }


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
        // CU-03 / CU-13 - VALIDAR ASIGNACIÓN ACTIVA
        // =================================================
        //
        // Impide confirmar una cita cuando su sede
        // y especialidad fueron desactivadas.
        // =================================================

        validarAsignacionActivaParaConfirmacion(
                reserva
        );



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
        // ESTADO / CAMBIA DE PENDIENTE DE PAGO
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
//
// Cita creada directamente por el paciente
// desde el Portal Web.
//
// CU-06 - FA03:
// Tiene 10 minutos para realizar el pago.
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

        if (wizard == null) { // no se puede continuar sin datos de la cita y lanza un mensaje

            throw new ReservaCitaInvalidaException(
                    "Los datos de la cita no están disponibles."
            );
        }


        // =================================================
        // TOKEN DE RESERVA / VALIDAR QUE EXISTA UNA RESERVA TEMPORAL
        // =================================================

        if (wizard.getTokenReserva() == null
                || wizard
                .getTokenReserva()
                .isBlank()) {  // significa que no hay una reserva temporal para confirmar

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
    // =====================================================
// CONFIRMAR CITA CON CONTEXTO DE CREACIÓN
// =====================================================

    @Transactional
    public Cita confirmar(
            Usuario paciente,
            Usuario creadoPor,
            CitaWizardDTO wizard,
            String canalOrigen,
            String prioridad
    ) {

        // =================================================
        // VALIDAR ENTRADA
        // =================================================

        validarEntrada(
                paciente,
                wizard
        );


        if (creadoPor == null
                || creadoPor.getId() == null) {

            throw new ReservaCitaInvalidaException(
                    "No existe un usuario responsable del agendamiento."
            );
        }


        // =================================================
        // NORMALIZAR CONTEXTO
        // =================================================

        String canalSeguro =
                canalOrigen == null
                        || canalOrigen.isBlank()
                        ? "Portal Web"
                        : canalOrigen.trim();


        String prioridadSegura =
                prioridad == null
                        || prioridad.isBlank()
                        ? "Normal"
                        : prioridad.trim();


        // =================================================
        // VERIFICAR ESTADO DE RESERVA
        // =================================================

        EstadoReservaTemporal estadoReserva =
                reservaTemporalCitaService
                        .obtenerEstado(
                                wizard.getTokenReserva()
                        );


        if (estadoReserva
                == EstadoReservaTemporal.EXPIRADA) {

            throw new ReservaExpiradaException(
                    MENSAJE_RESERVA_EXPIRADA
            );
        }


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
                        .buscarVigente( //busca un reserva temporal activa
                                wizard.getTokenReserva()
                        )
                        .orElseThrow( // sino se encotra lanza una excepción
                                () ->
                                        new ReservaExpiradaException(
                                                MENSAJE_RESERVA_EXPIRADA
                                        )
                        );


        // =================================================
        // VALIDAR PACIENTE DE LA RESERVA / EVITA QUE OTRO PACIENTE CONFIRME LA CITA DE OTRO USUARIO
        // =================================================

        if (reserva.getPaciente() == null
                || reserva.getPaciente().getId() == null
                || !paciente.getId().equals(
                reserva.getPaciente().getId()
        )) {

            throw new ReservaCitaInvalidaException(
                    "La reserva no pertenece al paciente indicado."
            );
        }

        // =================================================
// CU-08 FA02
// VALIDAR CITA DE SEGUIMIENTO
// =================================================

        boolean esSeguimiento =
                Boolean.TRUE.equals(
                        wizard.getEsSeguimiento()
                );

        if (esSeguimiento) {

            seguimientoCitaService
                    .validarParaConfirmacion(
                            wizard,
                            paciente,
                            reserva
                    );
        }



        // =================================================
        // CU-03 / CU-13 - VALIDAR ASIGNACIÓN ACTIVA
        // =================================================
        //
        // También protege las citas creadas
        // por Recepción y las de seguimiento.
        // =================================================

        validarAsignacionActivaParaConfirmacion(
                reserva
        );


        // =================================================
        // ESTADO PENDIENTE DE PAGO
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
         * Médico, sede, especialidad y horario siempre salen
         * de la reserva validada.
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


        cita.setEstadoCita(
                estadoPendientePago
        );


        cita.setMotivoConsulta(
                wizard
                        .getMotivoConsulta()
                        .trim()
        );


        // =================================================
        // CONTEXTO
        // =================================================

        cita.setPrioridad(
                prioridadSegura
        );


        cita.setCanalOrigen(
                canalSeguro
        );


        // =================================================
// CU-08 FA02
// DATOS DE SEGUIMIENTO
// =================================================

        if (esSeguimiento) {

            cita.setEsSeguimiento(
                    true
            );

            cita.setTipoSeguimiento(
                    wizard.getTipoSeguimiento()
            );

            cita.setIdConsultaOrigen(
                    wizard.getIdConsultaOrigen()
            );

        } else {

            /*
             * CU-03 / CU-05 mantienen exactamente
             * su comportamiento anterior.
             */
            cita.setEsSeguimiento(
                    false
            );

            cita.setTipoSeguimiento(
                    null
            );

            cita.setIdConsultaOrigen(
                    null
            );
        }


        // =================================================
        // AUDITORÍA
        // =================================================

        cita.setCreadoPor(
                creadoPor
        );


        // =================================================
// EXPIRACIÓN DEL PAGO
// =================================================
//
// CU-06 - FA03:
//
// Portal Web:
//     Tiene 10 minutos para realizar el pago.
//
// Presencial:
//     No se cancela automáticamente por falta
//     de pago.
// =================================================

        if ("Portal Web".equalsIgnoreCase(
                canalSeguro
        )) {

            cita.setFechaExpiracionPago(

                    OffsetDateTime
                            .now(
                                    zonaHoraria
                            )
                            .plusMinutes(
                                    duracionPagoMinutos
                            )
            );

        } else {

            cita.setFechaExpiracionPago(
                    null
            );
        }

        // =================================================
        // GUARDAR
        // =================================================

        Cita citaGuardada =
                citaRepository //aceso a la tabla cita
                        .saveAndFlush(  //guarda y sincroniza con la bd
                                cita
                        );


        // =================================================
        // LIBERAR RESERVA TEMPORAL
        // =================================================

        reservaTemporalCitaService
                .liberar(
                        wizard.getTokenReserva()
                );


        return citaGuardada;
    }


    // =====================================================
    // CU-03 / CU-13
    // VALIDAR SEDE Y ESPECIALIDAD ANTES DE CONFIRMAR
    // =====================================================
    //
    // Utiliza un bloqueo de PostgreSQL para coordinar
    // la creación de citas con la desactivación
    // de asignaciones realizada por Administración.
    //
    // La transacción debe continuar abierta hasta
    // guardar la cita.
    // =====================================================

    private void validarAsignacionActivaParaConfirmacion(
            ReservaTemporalCita reserva
    ) {

        // ================================================
        // VALIDAR DATOS DE LA RESERVA
        // ================================================

        if (reserva == null
                || reserva.getSucursal() == null
                || reserva.getSucursal().getId() == null
                || reserva.getEspecialidad() == null
                || reserva.getEspecialidad().getId() == null) {

            throw new ReservaCitaInvalidaException(
                    "La reserva no tiene una sede "
                            + "o especialidad válida."
            );
        }

        // ================================================
        // CONSULTAR ASIGNACIÓN ACTIVA CON BLOQUEO
        // ================================================

        Integer idSucursal =
                reserva.getSucursal().getId();

        Integer idEspecialidad =
                reserva.getEspecialidad().getId();

        sucursalEspecialidadRepository
                .buscarActivaParaConfirmarConBloqueo(
                        idSucursal,
                        idEspecialidad
                )
                .orElseThrow(
                        () -> new ReservaCitaInvalidaException(
                                "La especialidad seleccionada "
                                        + "ya no está disponible "
                                        + "en esta sede. "
                                        + "Seleccione otra especialidad "
                                        + "o sede para agendar su cita."
                        )
                );
    }

}