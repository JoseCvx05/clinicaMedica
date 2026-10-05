package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.ConsultaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: PANEL MÉDICO
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Construye las tres secciones exigidas por el caso de uso:
 *
 * 1. En Espera de Consulta.
 * 2. En Consulta Médica.
 * 3. Evaluados — Pendiente de cierre.
 *
 * Solo muestra citas pertenecientes al Médico autenticado.
 *
 * =========================================================
 */
@Service
public class PanelMedicoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";

    private static final String ESTADO_EN_ESPERA =
            "En Espera";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final UsuarioActualService
            usuarioActualService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PanelMedicoService(

            CitaRepository citaRepository,

            ConsultaMedicaRepository consultaMedicaRepository,

            UsuarioActualService usuarioActualService
    ) {

        this.citaRepository =
                citaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.usuarioActualService =
                usuarioActualService;
    }


    // =====================================================
    // OBTENER PANEL
    // =====================================================

    @Transactional(readOnly = true)
    public PanelMedico obtenerPanel() {

        Usuario medico =
                validarMedicoActual();


        // =================================================
        // EN ESPERA
        // =================================================

        List<PacientePanelMedico> enEspera =
                citaRepository
                        .buscarParaPanelMedicoEnEspera(
                                medico.getId(),
                                ESTADO_EN_ESPERA
                        )
                        .stream()
                        .map(
                                this::convertirCita
                        )
                        .toList();


        // =================================================
        // EN CONSULTA
        // =================================================

        List<PacientePanelMedico> enConsulta =
                consultaMedicaRepository
                        .buscarParaPanelMedicoPorEstado(
                                medico.getId(),
                                EstadoConsultaMedica.EN_CURSO
                        )
                        .stream()
                        .map(
                                this::convertirConsulta
                        )
                        .toList();


        // =================================================
        // EVALUADOS / PENDIENTES DE CIERRE
        // =================================================

        List<PacientePanelMedico> evaluados =
                consultaMedicaRepository
                        .buscarParaPanelMedicoPorEstado(
                                medico.getId(),
                                EstadoConsultaMedica.FINALIZADA
                        )
                        .stream()
                        .map(
                                this::convertirConsulta
                        )
                        .toList();


        return new PanelMedico(
                enEspera,
                enConsulta,
                evaluados
        );
    }


    // =====================================================
    // CONVERTIR CITA
    // =====================================================

    private PacientePanelMedico convertirCita(
            Cita cita
    ) {

        return new PacientePanelMedico(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getFechaHoraCita(),

                cita.getEstadoCita()
                        .getNombre(),

                cita.getPrioridad(),

                null
        );
    }


    // =====================================================
    // CONVERTIR CONSULTA
    // =====================================================

    private PacientePanelMedico convertirConsulta(
            ConsultaMedica consulta
    ) {

        Cita cita =
                consulta.getCita();


        return new PacientePanelMedico(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getFechaHoraCita(),

                cita.getEstadoCita()
                        .getNombre(),

                cita.getPrioridad(),

                consulta.getEstadoConsulta()
        );
    }


    // =====================================================
    // VALIDAR MÉDICO AUTENTICADO
    // =====================================================

    private Usuario validarMedicoActual() {

        Usuario usuario =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (usuario == null

                || !Boolean.TRUE.equals(
                usuario.getActivo()
        )

                || usuario.getRol() == null

                || usuario.getRol()
                .getNombre() == null

                || !ROL_MEDICO
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para acceder al módulo Médico."
            );
        }


        return usuario;
    }


    // =====================================================
    // PANEL
    // =====================================================

    public record PanelMedico(

            List<PacientePanelMedico> enEspera,

            List<PacientePanelMedico> enConsulta,

            List<PacientePanelMedico> evaluados

    ) {
    }


    // =====================================================
    // PACIENTE DEL PANEL
    // =====================================================

    public record PacientePanelMedico(

            Integer numeroCita,

            String nombrePaciente,

            String especialidad,

            OffsetDateTime fechaHoraCita,

            String estadoCita,

            String prioridad,

            EstadoConsultaMedica estadoConsulta

    ) {


        public boolean esEmergencia() {

            return prioridad != null
                    && "Emergencia"
                    .equalsIgnoreCase(
                            prioridad.trim()
                    );
        }
    }
}