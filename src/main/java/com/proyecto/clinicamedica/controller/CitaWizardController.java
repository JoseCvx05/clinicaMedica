package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ReservaTemporalCita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.exception.DocumentoCitaInvalidoException;
import com.proyecto.clinicamedica.exception.HorarioNoDisponibleException;
import com.proyecto.clinicamedica.exception.ReservaCitaInvalidaException;
import com.proyecto.clinicamedica.exception.ReservaExpiradaException;

import com.proyecto.clinicamedica.model.cita.EstadoReservaTemporal;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import com.proyecto.clinicamedica.service.CatalogoCitaService;
import com.proyecto.clinicamedica.service.CitaWizardNavegacionService;
import com.proyecto.clinicamedica.service.FinalizacionCitaService;
import com.proyecto.clinicamedica.service.RecepcionCitaService;
import com.proyecto.clinicamedica.service.ReservaTemporalCitaService;
import com.proyecto.clinicamedica.service.UsuarioActualService;
import com.proyecto.clinicamedica.service.ValidacionCitaWizardService;
import com.proyecto.clinicamedica.service.SeguimientoCitaService;
import com.proyecto.clinicamedica.service.NotificacionSeguimientoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import org.springframework.web.bind.support.SessionStatus;

import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * =========================================================
 * CONTROLADOR: WIZARD PARA AGENDAR CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Reutilizado también por:
 *
 * CU-05 FA04 - Nueva Cita Walk-in desde recepción.
 *
 * Flujo:
 *
 * 1. Sucursal
 * 2. Especialidad
 * 3. Médico
 * 4. Fecha y hora
 * 5. Confirmación
 *
 * =========================================================
 */
@Controller
@SessionAttributes("citaWizard")
public class CitaWizardController {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String RUTA_PACIENTE =
            "/paciente/citas";

    private static final String RUTA_RECEPCION =
            "/interno/recepcion/citas";

    private static final String RUTA_MEDICO =
            "/interno/medico/citas";

    private static final String SESION_PACIENTE_WALKIN =
            "recepcionPacienteWalkInId";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CatalogoCitaService
            catalogoCitaService;

    private final ValidacionCitaWizardService
            validacionCitaWizardService;

    private final CitaWizardNavegacionService
            navegacionService;

    private final ReservaTemporalCitaService
            reservaTemporalCitaService;

    private final UsuarioActualService
            usuarioActualService;

    private final FinalizacionCitaService
            finalizacionCitaService;

    private final RecepcionCitaService
            recepcionCitaService;

    private final SeguimientoCitaService
            seguimientoCitaService;

    private final NotificacionSeguimientoService
            notificacionSeguimientoService;
    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CitaWizardController(

            CatalogoCitaService catalogoCitaService,

            ValidacionCitaWizardService
                    validacionCitaWizardService,

            CitaWizardNavegacionService
                    navegacionService,

            ReservaTemporalCitaService
                    reservaTemporalCitaService,

            UsuarioActualService
                    usuarioActualService,

            FinalizacionCitaService
                    finalizacionCitaService,

            RecepcionCitaService
                    recepcionCitaService,

            SeguimientoCitaService
                    seguimientoCitaService,

            NotificacionSeguimientoService
                    notificacionSeguimientoService
    ) {

        this.catalogoCitaService =
                catalogoCitaService;

        this.validacionCitaWizardService =
                validacionCitaWizardService;

        this.navegacionService =
                navegacionService;

        this.reservaTemporalCitaService =
                reservaTemporalCitaService;

        this.usuarioActualService =
                usuarioActualService;

        this.finalizacionCitaService =
                finalizacionCitaService;

        this.recepcionCitaService =
                recepcionCitaService;

        this.seguimientoCitaService =
                seguimientoCitaService;

        this.notificacionSeguimientoService =
                notificacionSeguimientoService;
    }


    // =====================================================
    // OBJETO DEL WIZARD
    // =====================================================

    @ModelAttribute("citaWizard")
    public CitaWizardDTO crearWizard() {

        CitaWizardDTO wizard =
                new CitaWizardDTO();


        wizard.setPasoActual(
                PasoCita.SUCURSAL
        );


        return wizard;
    }


    // =====================================================
    // RUTA BASE PARA LA VISTA
    // =====================================================

    @ModelAttribute("rutaBaseCitas")
    public String rutaBaseCitas(
            HttpServletRequest request
    ) {

        if (esFlujoRecepcion(request)) {

            return RUTA_RECEPCION;
        }


        if (esFlujoMedico(request)) {

            return RUTA_MEDICO;
        }


        return RUTA_PACIENTE;
    }


    // =====================================================
    // IDENTIFICAR MODO WALK-IN
    // =====================================================

    @ModelAttribute("modoWalkIn")
    public boolean modoWalkIn(
            HttpServletRequest request
    ) {

        return esFlujoRecepcion(
                request
        );
    }


    // =====================================================
    // INICIAR CU-03 NORMAL
    // =====================================================

    @GetMapping("/paciente/citas/agendar")
    public String iniciar(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpSession session,

            Model model
    ) {

        /*
         * Si antes existió un flujo Walk-in,
         * eliminamos cualquier contexto residual.
         */
        session.removeAttribute(
                SESION_PACIENTE_WALKIN
        );


        reiniciarWizard(
                wizard
        );


        cargarPaso1(
                model
        );


        return "citas/agendar";
    }


    // =====================================================
    // CU-05 FA04 - INICIAR WALK-IN
    // =====================================================

    @GetMapping(
            "/interno/recepcion/citas/agendar/walk-in/{idPaciente}"
    )
    public String iniciarWalkIn(

            @PathVariable("idPaciente")
            Integer idPaciente,

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpSession session,

            Model model
    ) {

        /*
         * Validamos en backend que el ID realmente
         * corresponda a un paciente.
         */
        recepcionCitaService
                .obtenerPacienteParaAgendamiento(
                        idPaciente
                );


        /*
         * Guardamos solamente el ID en sesión.
         * No guardamos una entidad JPA completa.
         */
        session.setAttribute(
                SESION_PACIENTE_WALKIN,
                idPaciente
        );


        reiniciarWizard(
                wizard
        );


        cargarPaso1(
                model
        );


        return "citas/agendar";
    }

    // =====================================================
// CU-08 FA02 - INICIAR CITA DE SEGUIMIENTO
// =====================================================

    @GetMapping(
            "/interno/medico/citas/agendar/seguimiento/{idConsulta}"
    )
    public String iniciarSeguimiento(

            @PathVariable("idConsulta")
            Integer idConsulta,

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpSession session,

            RedirectAttributes redirectAttributes
    ) {

        try {

            /*
             * Eliminamos cualquier contexto Walk-in
             * que pudiera permanecer en la sesión.
             */
            session.removeAttribute(
                    SESION_PACIENTE_WALKIN
            );


            /*
             * El Service recupera desde PostgreSQL:
             *
             * - paciente;
             * - médico;
             * - sucursal;
             * - especialidad;
             * - consulta de origen.
             *
             * Y deja el wizard directamente en FECHA_HORA.
             */
            seguimientoCitaService
                    .prepararWizardSeguimiento(
                            idConsulta,
                            wizard
                    );


            return "redirect:/interno/medico/citas/agendar/paso";


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "redirect:/interno/medico";
        }
    }

    // =====================================================
// MOSTRAR PASO ACTUAL DEL WIZARD
// =====================================================
//
// PRG:
//
// POST
//   ↓
// REDIRECT
//   ↓
// GET
//
// Evita el reenvío de formularios al:
// - actualizar;
// - regresar con el navegador;
// - navegar entre pasos.
// =====================================================

    @GetMapping({
            "/paciente/citas/agendar/paso",
            "/interno/recepcion/citas/agendar/paso",
            "/interno/medico/citas/agendar/paso"
    })
    public String mostrarPasoActual(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request,

            HttpSession session,

            Model model
    ) {

        // =================================================
        // VALIDAR CONTEXTO WALK-IN
        // =================================================

        if (esFlujoRecepcion(request)) {

            Object pacienteWalkIn =
                    session.getAttribute(
                            SESION_PACIENTE_WALKIN
                    );


            if (!(pacienteWalkIn instanceof Integer)) {

                return "redirect:/interno/recepcion";
            }
        }

        // =================================================
// VALIDAR CONTEXTO DE SEGUIMIENTO
// =================================================

        if (esFlujoMedico(request)
                && !Boolean.TRUE.equals(
                wizard.getEsSeguimiento()
        )) {

            return "redirect:/interno/medico";
        }


        cargarModeloDelPaso(
                wizard,
                model
        );


        return "citas/agendar";
    }


    // =====================================================
    // PASO 1 → PASO 2
    // =====================================================

    @PostMapping({
            "/paciente/citas/agendar/sucursal",
            "/interno/recepcion/citas/agendar/sucursal"
    })
    public String seleccionarSucursal(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.SUCURSAL,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            wizard.setPasoActual(
                    PasoCita.SUCURSAL
            );


            redirectAttributes.addFlashAttribute(
                    "errores",
                    resultado
            );


            return
                    redirigirAlPasoActual(
                    request
            );
        }


        wizard.setIdEspecialidad(null);

        wizard.setIdMedico(null);

        wizard.setFechaHoraInicio(null);

        wizard.setFechaHoraFin(null);

        wizard.setTokenReserva(null);

        wizard.setFechaExpiracionReserva(null);

        wizard.setMotivoConsulta(null);


        wizard.setPasoActual(
                PasoCita.ESPECIALIDAD
        );


        return redirigirAlPasoActual(
                request
        );
    }


    // =====================================================
    // PASO 2 → PASO 3
    // =====================================================

    @PostMapping({
            "/paciente/citas/agendar/especialidad",
            "/interno/recepcion/citas/agendar/especialidad"
    })
    public String seleccionarEspecialidad(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDAR SUCURSAL
        // =================================================

        if (!catalogoCitaService
                .existeSucursalActiva(
                        wizard.getIdSucursal()
                )) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.SUCURSAL
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "La sucursal seleccionada ya no está disponible."
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        // =================================================
        // VALIDAR ESPECIALIDAD
        // =================================================

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.ESPECIALIDAD,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            wizard.setPasoActual(
                    PasoCita.ESPECIALIDAD
            );


            redirectAttributes.addFlashAttribute(
                    "errores",
                    resultado
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        wizard.setIdMedico(null);

        wizard.setFechaHoraInicio(null);

        wizard.setFechaHoraFin(null);

        wizard.setTokenReserva(null);

        wizard.setFechaExpiracionReserva(null);

        wizard.setMotivoConsulta(null);


        wizard.setPasoActual(
                PasoCita.MEDICO
        );


        return redirigirAlPasoActual(
                request
        );
    }


    // =====================================================
    // PASO 3 → PASO 4
    // =====================================================

    @PostMapping({
            "/paciente/citas/agendar/medico",
            "/interno/recepcion/citas/agendar/medico"
    })
    public String seleccionarMedico(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // VALIDAR SUCURSAL
        // =================================================

        if (!catalogoCitaService
                .existeSucursalActiva(
                        wizard.getIdSucursal()
                )) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.SUCURSAL
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "La sucursal seleccionada ya no está disponible."
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        // =================================================
        // VALIDAR ESPECIALIDAD
        // =================================================

        if (!catalogoCitaService
                .especialidadDisponibleEnSucursal(
                        wizard.getIdSucursal(),
                        wizard.getIdEspecialidad()
                )) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.ESPECIALIDAD
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "La especialidad seleccionada ya no está disponible."
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        // =================================================
        // VALIDAR MÉDICO
        // =================================================

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.MEDICO,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            wizard.setPasoActual(
                    PasoCita.MEDICO
            );


            redirectAttributes.addFlashAttribute(
                    "errores",
                    resultado
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        wizard.setFechaHoraInicio(null);

        wizard.setFechaHoraFin(null);

        wizard.setTokenReserva(null);

        wizard.setFechaExpiracionReserva(null);

        wizard.setMotivoConsulta(null);


        wizard.setPasoActual(
                PasoCita.FECHA_HORA
        );


        return redirigirAlPasoActual(
                request
        );
    }

    // =====================================================
    // VOLVER
    // =====================================================

    @PostMapping({
            "/paciente/citas/agendar/volver",
            "/interno/recepcion/citas/agendar/volver",
            "/interno/medico/citas/agendar/volver"
    })
    public String volver(

            @RequestParam("paso")
            Integer numeroPaso,

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request
    ) {

        PasoCita destino =
                obtenerPaso(
                        numeroPaso
                );

        if (esFlujoMedico(request)
                && destino.getNumero()
                < PasoCita.FECHA_HORA.getNumero()) {

            destino =
                    PasoCita.FECHA_HORA;
        }


        navegacionService.regresarA(
                wizard,
                destino
        );


        return redirigirAlPasoActual(
                request
        );
    }


    // =====================================================
    // PASO 4 → PASO 5
    // =====================================================

    @PostMapping({
            "/paciente/citas/agendar/horario",
            "/interno/recepcion/citas/agendar/horario",
            "/interno/medico/citas/agendar/horario"
    })
    public String seleccionarHorario(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            HttpServletRequest request,

            HttpSession session,

            RedirectAttributes redirectAttributes
    ) {

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.FECHA_HORA,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            wizard.setPasoActual(
                    PasoCita.FECHA_HORA
            );


            redirectAttributes.addFlashAttribute(
                    "errores",
                    resultado
            );


            return redirigirAlPasoActual(
                    request
            );
        }


        try {

            // =============================================
            // CU-08 FA02
            // VALIDAR TIPO ANTES DE RESERVAR
            // =============================================

            if (esFlujoMedico(
                    request
            )) {

                seguimientoCitaService
                        .validarTipoParaReserva(
                                wizard
                        );
            }


            // =============================================
            // OBTENER PACIENTE OBJETIVO
            // =============================================

            Usuario paciente =
                    obtenerPacienteObjetivo(
                            request,
                            session
                    );


            // =============================================
            // CREAR RESERVA TEMPORAL
            // =============================================

            ReservaTemporalCita reserva =
                    reservaTemporalCitaService
                            .reservar(
                                    paciente.getId(),
                                    wizard.getIdMedico(),
                                    wizard.getIdSucursal(),
                                    wizard.getIdEspecialidad(),
                                    wizard.getFechaHoraInicio(),
                                    wizard.getFechaHoraFin()
                            );


            wizard.setTokenReserva(
                    reserva.getTokenReserva()
            );


            wizard.setFechaExpiracionReserva(
                    reserva.getFechaExpiracion()
            );


            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            return redirigirAlPasoActual(
                    request
            );


        } catch (
                HorarioNoDisponibleException
                | ReservaCitaInvalidaException ex
        ) {

            wizard.setPasoActual(
                    PasoCita.FECHA_HORA
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return redirigirAlPasoActual(
                    request
            );


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            wizard.setPasoActual(
                    PasoCita.FECHA_HORA
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return redirigirAlPasoActual(
                    request
            );
        }
    }


    // =====================================================
    // FA03 CU-03 - RESERVA TEMPORAL EXPIRADA
    // =====================================================

    @GetMapping({
            "/paciente/citas/agendar/reserva-expirada",
            "/interno/recepcion/citas/agendar/reserva-expirada",
            "/interno/medico/citas/agendar/reserva-expirada"
    })
    public String procesarReservaExpirada(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            Model model
    ) {

        EstadoReservaTemporal estado =
                reservaTemporalCitaService
                        .obtenerEstado(
                                wizard.getTokenReserva()
                        );


        // =================================================
        // TODAVÍA ESTÁ VIGENTE
        // =================================================

        if (estado == EstadoReservaTemporal.VIGENTE) {

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            cargarPaso5(
                    wizard,
                    model
            );


            return "citas/agendar";
        }


        // =================================================
        // VOLVER AL HORARIO
        // =================================================

        navegacionService.regresarA(
                wizard,
                PasoCita.FECHA_HORA
        );


        if (estado == EstadoReservaTemporal.EXPIRADA) {

            model.addAttribute(
                    "mensajeError",

                    "El tiempo para confirmar su cita ha expirado. "
                            + "El horario seleccionado ha sido liberado. "
                            + "Por favor, seleccione un nuevo horario."
            );

        } else {

            model.addAttribute(
                    "mensajeError",

                    "La reserva temporal ya no está disponible. "
                            + "Seleccione un nuevo horario."
            );
        }


        return "citas/agendar";
    }


    // =====================================================
    // PASO 5 - CONFIRMAR CITA
    // =====================================================
    @PostMapping({
            "/paciente/citas/agendar/confirmar",
            "/interno/recepcion/citas/agendar/confirmar",
            "/interno/medico/citas/agendar/confirmar"
    })
    public String confirmarCita(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            @RequestParam(
                    value = "documento",
                    required = false
            )
            MultipartFile documento,

            HttpServletRequest request,

            HttpSession session,

            RedirectAttributes redirectAttributes,

            SessionStatus sessionStatus
    ) {

        // =================================================
        // VALIDAR MOTIVO
        // =================================================

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.CONFIRMACION,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            redirectAttributes.addFlashAttribute(
                    "errores",
                    resultado
            );


            return redirigirAlPasoActual(
                    request
            );
        }

        // =================================================
        // PACIENTE OBJETIVO
        // =================================================
        //
        // CU-08 FA02:
        // También realiza validaciones defensivas sobre
        // Médico, sucursal y especialidad del seguimiento.
        //
        // Debe estar dentro del try para que cualquier
        // manipulación se maneje de forma controlada.
        // ===============================================
        try {

            Usuario paciente =
                    obtenerPacienteObjetivo(
                            request,
                            session
                    );


            Cita cita;


            // =============================================
            // CU-05 WALK-IN
            // =============================================

            if (esFlujoRecepcion(
                    request
            )) {

                // =============================================
                // CU-05 WALK-IN
                // =============================================

                Usuario recepcionista =
                        usuarioActualService
                                .obtenerUsuarioActual();


                cita =
                        finalizacionCitaService
                                .finalizar(
                                        paciente,
                                        recepcionista,
                                        wizard,
                                        documento,
                                        "Presencial",
                                        "Normal"
                                );


            } else if (esFlujoMedico(
                    request
            )) {

                // =============================================
                // CU-08 FA02
                // CITA DE SEGUIMIENTO
                // =============================================

                Usuario medico =
                        usuarioActualService
                                .obtenerUsuarioActual();


                cita =
                        finalizacionCitaService
                                .finalizar(
                                        paciente,
                                        medico,
                                        wizard,
                                        documento,

                                        /*
                                         * Conservamos el flujo normal
                                         * PENDIENTE DE PAGO de CU-03.
                                         */
                                        "Portal Web",

                                        /*
                                         * La prioridad ya fue validada
                                         * en SeguimientoCitaService.
                                         */
                                        wizard.getPrioridad()
                                );
                // =============================================
// CU-08 FA02
// NOTIFICAR AL PACIENTE
// =============================================

                notificacionSeguimientoService
                        .enviarConfirmacion(
                                cita.getId()
                        );


            } else {

                // =============================================
                // CU-03 NORMAL
                // =============================================

                cita =
                        finalizacionCitaService
                                .finalizar(
                                        paciente,
                                        wizard,
                                        documento
                                );
            }

            // =================================================
            // FINALIZAR WIZARD
            // =================================================

            sessionStatus.setComplete();

            // =================================================
// CU-08 FA02
// SEGUIMIENTO CREADO
// =================================================

            if (esFlujoMedico(
                    request
            )) {

                java.time.ZonedDateTime fechaHoraGuatemala =
                        cita.getFechaHoraCita()
                                .atZoneSameInstant(
                                        java.time.ZoneId.of(
                                                "America/Guatemala"
                                        )
                                );


                String fecha =
                        fechaHoraGuatemala
                                .format(
                                        java.time.format.DateTimeFormatter
                                                .ofPattern(
                                                        "dd/MM/yyyy"
                                                )
                                );


                String hora =
                        fechaHoraGuatemala
                                .format(
                                        java.time.format.DateTimeFormatter
                                                .ofPattern(
                                                        "HH:mm"
                                                )
                                );


                redirectAttributes.addFlashAttribute(
                        "mensajeExito",

                        "Cita de seguimiento agendada para el "
                                + fecha
                                + " a las "
                                + hora
                                + ". Se enviará notificación al paciente."
                );


                return "redirect:/interno/medico";
            }


            // =================================================
            // CU-05 - REGRESAR A RECEPCIÓN
            // =================================================

            if (esFlujoRecepcion(
                    request
            )) {

                session.removeAttribute(
                        SESION_PACIENTE_WALKIN
                );


                return "redirect:/interno/recepcion/cita/"
                        + cita.getId();
            }


            // =================================================
            // CU-03 - CITA CREADA
            // =================================================

            return "redirect:/paciente/citas/agendar/exito?idCita="
                    + cita.getId();


        } catch (
                ReservaExpiradaException ex
        ) {

            // =================================================
            // RESERVA EXPIRADA
            // =================================================

            navegacionService.regresarA(
                    wizard,
                    PasoCita.FECHA_HORA
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return redirigirAlPasoActual(
                    request
            );


        } catch (
                DocumentoCitaInvalidoException
                | ReservaCitaInvalidaException ex
        ) {

            // =================================================
            // ERROR EN CONFIRMACIÓN
            // =================================================

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return redirigirAlPasoActual(
                    request
            );
        }
        catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            // =================================================
            // CU-08 FA02
            // ERROR DE VALIDACIÓN DE SEGUIMIENTO
            // =================================================

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return redirigirAlPasoActual(
                    request
            );
        }
    }


    // =====================================================
    // RESULTADO EXITOSO CU-03
    // =====================================================

    @GetMapping("/paciente/citas/agendar/exito")
    public String citaRegistrada(

            @RequestParam("idCita")
            Integer idCita,

            Model model
    ) {

        model.addAttribute(
                "idCita",
                idCita
        );


        model.addAttribute(
                "mensajeExito",

                "Su cita ha sido registrada exitosamente. "
                        + "Será redirigido al proceso de pago "
                        + "para confirmar la reserva."
        );


        return "citas/agendar-exito";
    }


    // =====================================================
    // DETERMINAR FLUJO
    // =====================================================

    private boolean esFlujoRecepcion(
            HttpServletRequest request
    ) {

        return request != null
                && request
                .getRequestURI()
                .startsWith(
                        "/interno/recepcion/"
                );
    }

    private boolean esFlujoMedico(
            HttpServletRequest request
    ) {

        return request != null
                && request
                .getRequestURI()
                .startsWith(
                        "/interno/medico/"
                );
    }


    // =====================================================
    // OBTENER PACIENTE OBJETIVO
    // =====================================================

    private Usuario obtenerPacienteObjetivo(

            HttpServletRequest request,

            HttpSession session
    ) {

        // =================================================
// CU-08 FA02
// PACIENTE DE LA CONSULTA ORIGINAL
// =================================================

        if (esFlujoMedico(request)) {

            return seguimientoCitaService
                    .obtenerPacienteParaReserva(
                            (CitaWizardDTO)
                                    session.getAttribute(
                                            "citaWizard"
                                    )
                    );
        }

        // =================================================
        // CU-03 NORMAL
        // =================================================

        if (!esFlujoRecepcion(
                request
        )) {

            return usuarioActualService
                    .obtenerUsuarioActual();
        }


        // =================================================
        // CU-05 WALK-IN
        // =================================================

        Object valor =
                session.getAttribute(
                        SESION_PACIENTE_WALKIN
                );


        if (!(valor instanceof Integer idPaciente)) {

            throw new ReservaCitaInvalidaException(
                    "No existe un paciente seleccionado "
                            + "para el agendamiento Walk-in."
            );
        }


        return recepcionCitaService
                .obtenerPacienteParaAgendamiento(
                        idPaciente
                );
    }


    // =====================================================
    // REINICIAR WIZARD
    // =====================================================

    private void reiniciarWizard(
            CitaWizardDTO wizard
    ) {

        wizard.setPasoActual(
                PasoCita.SUCURSAL
        );

        wizard.setIdSucursal(
                null
        );

        wizard.setIdEspecialidad(
                null
        );

        wizard.setIdMedico(
                null
        );

        wizard.setFechaHoraInicio(
                null
        );

        wizard.setFechaHoraFin(
                null
        );

        wizard.setTokenReserva(
                null
        );

        wizard.setFechaExpiracionReserva(
                null
        );

        wizard.setMotivoConsulta(
                null
        );

        wizard.setEsSeguimiento(
                false
        );

        wizard.setIdConsultaOrigen(
                null
        );

        wizard.setTipoSeguimiento(
                null
        );

        wizard.setPrioridad(
                null
        );
    }


    // =====================================================
    // CARGAR PASO 1
    // =====================================================

    private void cargarPaso1(
            Model model
    ) {

        model.addAttribute(
                "sucursales",

                catalogoCitaService
                        .listarSucursales()
        );
    }


    // =====================================================
    // CARGAR PASO 2
    // =====================================================

    private void cargarPaso2(

            CitaWizardDTO wizard,

            Model model
    ) {

        model.addAttribute(
                "especialidades",

                catalogoCitaService
                        .listarEspecialidades(
                                wizard.getIdSucursal()
                        )
        );
    }


    // =====================================================
    // CARGAR PASO 3
    // =====================================================

    private void cargarPaso3(

            CitaWizardDTO wizard,

            Model model
    ) {

        model.addAttribute(
                "medicos",

                catalogoCitaService
                        .listarMedicos(
                                wizard.getIdSucursal(),
                                wizard.getIdEspecialidad()
                        )
        );
    }


    // =====================================================
    // CARGAR PASO CORRESPONDIENTE
    // =====================================================

    private void cargarModeloDelPaso(

            CitaWizardDTO wizard,

            Model model
    ) {

        switch (wizard.getPasoActual()) {

            case SUCURSAL ->
                    cargarPaso1(
                            model
                    );


            case ESPECIALIDAD ->
                    cargarPaso2(
                            wizard,
                            model
                    );


            case MEDICO ->
                    cargarPaso3(
                            wizard,
                            model
                    );


            case FECHA_HORA -> {

                /*
                 * La disponibilidad se obtiene
                 * mediante AJAX.
                 */
            }


            case CONFIRMACION ->
                    cargarPaso5(
                            wizard,
                            model
                    );
        }
    }


    // =====================================================
    // CARGAR PASO 5
    // =====================================================

    private void cargarPaso5(

            CitaWizardDTO wizard,

            Model model
    ) {

        String nombreSucursal =
                catalogoCitaService
                        .listarSucursales()
                        .stream()
                        .filter(
                                opcion ->
                                        opcion
                                                .id()
                                                .equals(
                                                        wizard.getIdSucursal()
                                                )
                        )
                        .map(
                                opcion ->
                                        opcion.nombre()
                        )
                        .findFirst()
                        .orElse(
                                "Sucursal"
                        );


        String nombreEspecialidad =
                catalogoCitaService
                        .listarEspecialidades(
                                wizard.getIdSucursal()
                        )
                        .stream()
                        .filter(
                                opcion ->
                                        opcion
                                                .id()
                                                .equals(
                                                        wizard.getIdEspecialidad()
                                                )
                        )
                        .map(
                                opcion ->
                                        opcion.nombre()
                        )
                        .findFirst()
                        .orElse(
                                "Especialidad"
                        );


        String nombreMedico =
                catalogoCitaService
                        .listarMedicos(
                                wizard.getIdSucursal(),
                                wizard.getIdEspecialidad()
                        )
                        .stream()
                        .filter(
                                opcion ->
                                        opcion
                                                .id()
                                                .equals(
                                                        wizard.getIdMedico()
                                                )
                        )
                        .map(
                                opcion ->
                                        opcion.nombre()
                        )
                        .findFirst()
                        .orElse(
                                "Médico"
                        );


        model.addAttribute(
                "nombreSucursal",
                nombreSucursal
        );


        model.addAttribute(
                "nombreEspecialidad",
                nombreEspecialidad
        );


        model.addAttribute(
                "nombreMedico",
                nombreMedico
        );
    }

    // =====================================================
// REDIRECCIÓN AL PASO ACTUAL
// =====================================================
//
// Permite aplicar el patrón:
//
// POST -> REDIRECT -> GET
//
// tanto para:
// - paciente;
// - Walk-in desde recepción.
// =====================================================

    private String redirigirAlPasoActual(
            HttpServletRequest request
    ) {

        if (esFlujoRecepcion(request)) {

            return "redirect:/interno/recepcion/citas/agendar/paso";
        }


        if (esFlujoMedico(request)) {

            return "redirect:/interno/medico/citas/agendar/paso";
        }


        return "redirect:/paciente/citas/agendar/paso";
    }


    // =====================================================
    // CONVERTIR NÚMERO → PASO
    // =====================================================

    private PasoCita obtenerPaso(
            Integer numero
    ) {

        if (numero == null) {

            return PasoCita.SUCURSAL;
        }


        for (PasoCita paso :
                PasoCita.values()) {

            if (Objects.equals(
                    paso.getNumero(),
                    numero
            )) {

                return paso;
            }
        }


        return PasoCita.SUCURSAL;
    }
}