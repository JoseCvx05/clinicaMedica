package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.service.CatalogoCitaService;
import com.proyecto.clinicamedica.service.CitaWizardNavegacionService;
import com.proyecto.clinicamedica.service.ValidacionCitaWizardService;
import com.proyecto.clinicamedica.entity.ReservaTemporalCita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.exception.HorarioNoDisponibleException;
import com.proyecto.clinicamedica.exception.ReservaCitaInvalidaException;

import com.proyecto.clinicamedica.service.ReservaTemporalCitaService;
import com.proyecto.clinicamedica.service.UsuarioActualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.proyecto.clinicamedica.model.cita.EstadoReservaTemporal;
import java.util.Objects;
import com.proyecto.clinicamedica.entity.Cita;

import com.proyecto.clinicamedica.exception.DocumentoCitaInvalidoException;
import com.proyecto.clinicamedica.exception.ReservaExpiradaException;

import com.proyecto.clinicamedica.service.FinalizacionCitaService;

import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;


/**
 * =========================================================
 * CONTROLADOR: WIZARD PARA AGENDAR CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
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
@RequestMapping("/paciente/citas")
@SessionAttributes("citaWizard")
public class CitaWizardController {


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

    public CitaWizardController(
            CatalogoCitaService catalogoCitaService,
            ValidacionCitaWizardService validacionCitaWizardService,
            CitaWizardNavegacionService navegacionService,
            ReservaTemporalCitaService reservaTemporalCitaService,
            UsuarioActualService usuarioActualService,
            FinalizacionCitaService finalizacionCitaService
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
    // INICIAR CU-03
    // =====================================================

    @GetMapping("/agendar")
    public String iniciar(
            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,
            Model model
    ) {

        /*
         * Cada vez que el usuario inicia expresamente
         * un nuevo agendamiento, comenzamos desde Paso 1.
         */
        navegacionService.regresarA(
                wizard,
                PasoCita.SUCURSAL
        );


        cargarPaso1(
                model
        );


        return "citas/agendar";
    }


    // =====================================================
    // PASO 1 → PASO 2
    // =====================================================

    @PostMapping("/agendar/sucursal")
    public String seleccionarSucursal(
            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,
            Model model
    ) {

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.SUCURSAL,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            cargarPaso1(
                    model
            );

            model.addAttribute(
                    "errores",
                    resultado
            );


            return "citas/agendar";
        }


        /*
         * Si cambió la sucursal, cualquier selección
         * posterior deja de ser válida.
         */
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


        wizard.setPasoActual(
                PasoCita.ESPECIALIDAD
        );


        cargarPaso2(
                wizard,
                model
        );


        return "citas/agendar";
    }


    // =====================================================
    // PASO 2 → PASO 3
    // =====================================================

    @PostMapping("/agendar/especialidad")
    public String seleccionarEspecialidad(
            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,
            Model model
    ) {

        /*
         * Primero comprobamos que todavía exista
         * una sucursal válida.
         */
        if (!catalogoCitaService
                .existeSucursalActiva(
                        wizard.getIdSucursal()
                )) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.SUCURSAL
            );


            cargarPaso1(
                    model
            );


            model.addAttribute(
                    "mensajeError",
                    "La sucursal seleccionada ya no está disponible."
            );


            return "citas/agendar";
        }


        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.ESPECIALIDAD,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            cargarPaso2(
                    wizard,
                    model
            );

            model.addAttribute(
                    "errores",
                    resultado
            );


            return "citas/agendar";
        }


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


        wizard.setPasoActual(
                PasoCita.MEDICO
        );


        cargarPaso3(
                wizard,
                model
        );


        return "citas/agendar";
    }

    // =====================================================
// PASO 3 → PASO 4
// =====================================================

    @PostMapping("/agendar/medico")
    public String seleccionarMedico(
            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,
            Model model
    ) {

        // =================================================
        // VALIDAR QUE LA SUCURSAL SIGA DISPONIBLE
        // =================================================

        if (!catalogoCitaService
                .existeSucursalActiva(
                        wizard.getIdSucursal()
                )) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.SUCURSAL
            );

            cargarPaso1(
                    model
            );

            model.addAttribute(
                    "mensajeError",
                    "La sucursal seleccionada ya no está disponible."
            );

            return "citas/agendar";
        }


        // =================================================
        // VALIDAR ESPECIALIDAD EN LA SUCURSAL
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

            cargarPaso2(
                    wizard,
                    model
            );

            model.addAttribute(
                    "mensajeError",
                    "La especialidad seleccionada ya no está disponible."
            );

            return "citas/agendar";
        }


        // =================================================
        // VALIDACIÓN POLIMÓRFICA DEL MÉDICO
        // =================================================

        ResultadoValidacionCita resultado =
                validacionCitaWizardService
                        .validar(
                                PasoCita.MEDICO,
                                wizard
                        );


        if (resultado.tieneErrores()) {

            cargarPaso3(
                    wizard,
                    model
            );

            model.addAttribute(
                    "errores",
                    resultado
            );

            return "citas/agendar";
        }


        // =================================================
        // LIMPIAR HORARIO ANTERIOR
        // =================================================

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


        // =================================================
        // PASAR AL PASO 4
        // =================================================

        wizard.setPasoActual(
                PasoCita.FECHA_HORA
        );


        return "citas/agendar";
    }


    // =====================================================
    // VOLVER
    // =====================================================

    @PostMapping("/agendar/volver")
    public String volver(
            @RequestParam("paso")
            Integer numeroPaso,

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            Model model
    ) {

        PasoCita destino =
                obtenerPaso(
                        numeroPaso
                );


        navegacionService.regresarA(
                wizard,
                destino
        );


        cargarModeloDelPaso(
                wizard,
                model
        );


        return "citas/agendar";
    }


    // =====================================================
    // CARGA DE MODELOS
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
                 * Los horarios se consultan mediante
                 * el endpoint AJAX.
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
    // CONVERTIR NÚMERO → PASO
    // =====================================================

    private PasoCita obtenerPaso(
            Integer numero
    ) {

        if (numero == null) {

            return PasoCita.SUCURSAL;
        }


        for (PasoCita paso : PasoCita.values()) {

            if (Objects.equals(
                    paso.getNumero(),
                    numero
            )) {

                return paso;
            }
        }


        return PasoCita.SUCURSAL;
    }

    // =====================================================
// PASO 4 → PASO 5
// =====================================================

    @PostMapping("/agendar/horario")
    public String seleccionarHorario(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            Model model
    ) {

        // =================================================
        // VALIDAR FECHA/HORA
        // =================================================

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


            model.addAttribute(
                    "errores",
                    resultado
            );


            return "citas/agendar";
        }


        // =================================================
        // OBTENER PACIENTE AUTENTICADO
        // =================================================

        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        try {

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


            // =============================================
            // GUARDAR TOKEN EN EL WIZARD
            // =============================================

            wizard.setTokenReserva(
                    reserva.getTokenReserva()
            );


            wizard.setFechaExpiracionReserva(
                    reserva.getFechaExpiracion()
            );


            // =============================================
            // PASO 5
            // =============================================

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            cargarPaso5(
                    wizard,
                    model
            );


            return "citas/agendar";


        } catch (
                HorarioNoDisponibleException
                | ReservaCitaInvalidaException ex
        ) {

            wizard.setPasoActual(
                    PasoCita.FECHA_HORA
            );


            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "citas/agendar";
        }
    }

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
                                        opcion.id().equals(
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
                                        opcion.id().equals(
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
                                        opcion.id().equals(
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
// FA03 - RESERVA TEMPORAL EXPIRADA
// =====================================================

    @GetMapping("/agendar/reserva-expirada")
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
        // EL BACKEND DICE QUE TODAVÍA ESTÁ VIGENTE
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
        // EXPIRADA O YA NO DISPONIBLE
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

    @PostMapping("/agendar/confirmar")
    public String confirmarCita(

            @ModelAttribute("citaWizard")
            CitaWizardDTO wizard,

            @RequestParam(
                    value = "documento",
                    required = false
            )
            MultipartFile documento,

            Model model,

            SessionStatus sessionStatus
    ) {

        // =================================================
        // VALIDAR MOTIVO 10 - 2000
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


            cargarPaso5(
                    wizard,
                    model
            );


            model.addAttribute(
                    "errores",
                    resultado
            );


            return "citas/agendar";
        }


        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        try {

            Cita cita =
                    finalizacionCitaService
                            .finalizar(
                                    paciente,
                                    wizard,
                                    documento
                            );


            /*
             * El wizard ya terminó.
             */
            sessionStatus.setComplete();


            return "redirect:/paciente/citas/agendar/exito?idCita="
                    + cita.getId();


        } catch (
                ReservaExpiradaException ex
        ) {

            navegacionService.regresarA(
                    wizard,
                    PasoCita.FECHA_HORA
            );


            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "citas/agendar";


        } catch (
                DocumentoCitaInvalidoException
                | ReservaCitaInvalidaException ex
        ) {

            wizard.setPasoActual(
                    PasoCita.CONFIRMACION
            );


            cargarPaso5(
                    wizard,
                    model
            );


            model.addAttribute(
                    "mensajeError",
                    ex.getMessage()
            );


            return "citas/agendar";
        }
    }
    // =====================================================
// RESULTADO EXITOSO
// =====================================================

    @GetMapping("/agendar/exito")
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
}