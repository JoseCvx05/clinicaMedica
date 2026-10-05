package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;
import com.proyecto.clinicamedica.model.cita.PasoCita;

import com.proyecto.clinicamedica.repository.ConsultaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.clinicamedica.entity.ReservaTemporalCita;

import java.util.Set;

/**
 * =========================================================
 * SERVICIO: CITA DE SEGUIMIENTO
 * =========================================================
 *
 * CU-08 FA02 - Agendar Seguimiento.
 *
 * Responsabilidades iniciales:
 *
 * - Validar al Médico autenticado.
 * - Recuperar la Consulta Médica de origen.
 * - Evitar acceso a consultas de otro Médico.
 * - Validar que la consulta esté finalizada.
 * - Recuperar paciente, Médico, sucursal y especialidad
 *   exclusivamente desde PostgreSQL.
 * - Preparar el CitaWizardDTO para reutilizar CU-03.
 *
 * IMPORTANTE:
 *
 * Este método todavía NO crea una nueva cita.
 * Tampoco reserva ningún horario.
 *
 * =========================================================
 */
@Service
public class SeguimientoCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ESTADO_CITA_CONSULTA_MEDICA =
            "Consulta Médica";

    private static final Set<String> TIPOS_SEGUIMIENTO_PERMITIDOS =
            Set.of(
                    "MONITOREO_TRATAMIENTO",
                    "REVISION_RESULTADOS_LABORATORIO"
            );
    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final UsuarioActualService
            usuarioActualService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SeguimientoCitaService(
            ConsultaMedicaRepository consultaMedicaRepository,
            UsuarioActualService usuarioActualService
    ) {

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.usuarioActualService =
                usuarioActualService;
    }


    // =====================================================
    // CU-08 FA02
    // PREPARAR WIZARD DE SEGUIMIENTO
    // =====================================================

    @Transactional(readOnly = true)
    public ConsultaMedica prepararWizardSeguimiento(
            Integer idConsulta,
            CitaWizardDTO wizard
    ) {

        // =================================================
        // VALIDAR ID
        // =================================================

        if (idConsulta == null
                || idConsulta <= 0) {

            throw new IllegalArgumentException(
                    "La consulta médica de origen es obligatoria."
            );
        }


        // =================================================
        // VALIDAR WIZARD
        // =================================================

        if (wizard == null) {

            throw new IllegalArgumentException(
                    "No fue posible preparar el agendamiento de seguimiento."
            );
        }


        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (medico == null
                || medico.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Médico autenticado."
            );
        }


        // =================================================
        // CONSULTA DE ORIGEN
        // =================================================

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findById(
                                idConsulta
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La consulta médica indicada no existe."
                                        )
                        );


        // =================================================
        // VALIDAR MÉDICO PROPIETARIO
        // =================================================

        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null
                || !consulta.getMedico()
                .getId()
                .equals(
                        medico.getId()
                )) {

            throw new IllegalStateException(
                    "La consulta médica no pertenece al Médico autenticado."
            );
        }


        // =================================================
        // CONSULTA FINALIZADA
        //
        // FA02 nace desde:
        //
        // Evaluados — Pendiente de cierre.
        // =================================================

        if (!EstadoConsultaMedica.FINALIZADA.equals(
                consulta.getEstadoConsulta()
        )) {

            throw new IllegalStateException(
                    "Solo una consulta finalizada puede generar "
                            + "una cita de seguimiento."
            );
        }


        // =================================================
        // CITA DE ORIGEN
        // =================================================

        Cita citaOrigen =
                consulta.getCita();


        if (citaOrigen == null
                || citaOrigen.getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee una cita de origen válida."
            );
        }


        // =================================================
        // VALIDAR ESTADO DE CITA
        //
        // Debe seguir en:
        //
        // Consulta Médica
        //
        // porque todavía está pendiente de cierre.
        // =================================================

        if (citaOrigen.getEstadoCita() == null
                || citaOrigen.getEstadoCita().getNombre() == null
                || !ESTADO_CITA_CONSULTA_MEDICA
                .equalsIgnoreCase(
                        citaOrigen
                                .getEstadoCita()
                                .getNombre()
                )) {

            throw new IllegalStateException(
                    "Solo una cita evaluada y pendiente de cierre "
                            + "puede generar seguimiento."
            );
        }


        // =================================================
        // VALIDAR RELACIONES NECESARIAS
        // =================================================

        if (citaOrigen.getPaciente() == null
                || citaOrigen.getPaciente().getId() == null) {

            throw new IllegalStateException(
                    "La cita de origen no posee un paciente válido."
            );
        }


        if (citaOrigen.getMedico() == null
                || citaOrigen.getMedico().getId() == null) {

            throw new IllegalStateException(
                    "La cita de origen no posee un Médico válido."
            );
        }


        if (citaOrigen.getSucursal() == null
                || citaOrigen.getSucursal().getId() == null) {

            throw new IllegalStateException(
                    "La cita de origen no posee una sucursal válida."
            );
        }


        if (citaOrigen.getEspecialidad() == null
                || citaOrigen.getEspecialidad().getId() == null) {

            throw new IllegalStateException(
                    "La cita de origen no posee una especialidad válida."
            );
        }


        // =================================================
        // DEFENSA ADICIONAL
        // MÉDICO DE CONSULTA = MÉDICO DE CITA
        // =================================================

        if (!citaOrigen
                .getMedico()
                .getId()
                .equals(
                        medico.getId()
                )) {

            throw new IllegalStateException(
                    "La cita de origen no pertenece al Médico autenticado."
            );
        }


        // =================================================
        // PREPARAR WIZARD
        //
        // Estos valores NO vienen del navegador.
        // Se obtienen directamente de PostgreSQL.
        // =================================================

        wizard.setEsSeguimiento(
                true
        );


        wizard.setIdConsultaOrigen(
                consulta.getId()
        );


        wizard.setIdSucursal(
                citaOrigen
                        .getSucursal()
                        .getId()
        );


        wizard.setIdEspecialidad(
                citaOrigen
                        .getEspecialidad()
                        .getId()
        );


        wizard.setIdMedico(
                citaOrigen
                        .getMedico()
                        .getId()
        );


        // =================================================
        // DATOS QUE EL MÉDICO DEBE SELECCIONAR
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

        wizard.setTipoSeguimiento(
                null
        );

        wizard.setPrioridad(
                "Normal"
        );


        // =================================================
        // SALTAR DIRECTAMENTE A FECHA / HORA
        //
        // Sucursal, especialidad y Médico ya vienen
        // precargados desde la consulta original.
        // =================================================

        wizard.setPasoActual(
                PasoCita.FECHA_HORA
        );


        return consulta;
    }

    // =====================================================
// CU-08 FA02
// VALIDAR TIPO ANTES DE RESERVAR HORARIO
// =====================================================

    @Transactional(readOnly = true)
    public void validarTipoParaReserva(
            CitaWizardDTO wizard
    ) {

        // =================================================
        // CONTEXTO DE SEGUIMIENTO
        // =================================================

        if (wizard == null
                || !Boolean.TRUE.equals(
                wizard.getEsSeguimiento()
        )) {

            throw new IllegalArgumentException(
                    "El agendamiento no corresponde a una cita de seguimiento."
            );
        }


        // =================================================
        // CONSULTA DE ORIGEN
        // =================================================

        if (wizard.getIdConsultaOrigen() == null
                || wizard.getIdConsultaOrigen() <= 0) {

            throw new IllegalArgumentException(
                    "La consulta médica de origen es obligatoria."
            );
        }


        // =================================================
        // TIPO DE SEGUIMIENTO
        // =================================================

        String tipo =
                limpiar(
                        wizard.getTipoSeguimiento()
                );


        if (tipo == null
                || !TIPOS_SEGUIMIENTO_PERMITIDOS.contains(
                tipo
        )) {

            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de seguimiento."
            );
        }


        // =================================================
        // NORMALIZAR
        // =================================================

        wizard.setTipoSeguimiento(
                tipo
        );
    }

    // =====================================================
// CU-08 FA02
// VALIDAR SEGUIMIENTO ANTES DE CREAR LA CITA
// =====================================================

    @Transactional(readOnly = true)
    public ConsultaMedica validarParaConfirmacion(
            CitaWizardDTO wizard,
            Usuario paciente,
            ReservaTemporalCita reserva
    ) {

        // =================================================
        // CONTEXTO
        // =================================================

        if (wizard == null
                || !Boolean.TRUE.equals(
                wizard.getEsSeguimiento()
        )) {

            throw new IllegalArgumentException(
                    "El agendamiento no corresponde a una cita de seguimiento."
            );
        }


        if (wizard.getIdConsultaOrigen() == null
                || wizard.getIdConsultaOrigen() <= 0) {

            throw new IllegalArgumentException(
                    "La consulta médica de origen es obligatoria."
            );
        }

        // =================================================
// TIPO DE SEGUIMIENTO
// =================================================

        String tipoSeguimiento =
                limpiar(
                        wizard.getTipoSeguimiento()
                );


        if (tipoSeguimiento == null
                || !TIPOS_SEGUIMIENTO_PERMITIDOS
                .contains(
                        tipoSeguimiento
                )) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un tipo de seguimiento válido."
            );
        }


// =================================================
// PRIORIDAD
// =================================================

        String prioridad =
                limpiar(
                        wizard.getPrioridad()
                );


        if (prioridad == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar la prioridad del seguimiento."
            );
        }


        if (!"Normal".equalsIgnoreCase(prioridad)
                && !"Emergencia".equalsIgnoreCase(prioridad)) {

            throw new IllegalArgumentException(
                    "La prioridad seleccionada no es válida."
            );
        }


// =================================================
// NORMALIZAR PRIORIDAD
// =================================================

        if ("Emergencia".equalsIgnoreCase(
                prioridad
        )) {

            wizard.setPrioridad(
                    "Emergencia"
            );

        } else {

            wizard.setPrioridad(
                    "Normal"
            );
        }

        // =================================================
// MOTIVO / OBSERVACIONES DEL SEGUIMIENTO
// RN-CU11-03
// =================================================

        String motivoSeguimiento =
                limpiar(
                        wizard.getMotivoConsulta()
                );


        if (motivoSeguimiento == null
                || motivoSeguimiento.length() < 10
                || motivoSeguimiento.length() > 2000) {

            throw new IllegalArgumentException(
                    "Las observaciones son obligatorias. "
                            + "Deben contener entre 10 y 2000 caracteres."
            );
        }


// =================================================
// NORMALIZAR MOTIVO
// =================================================

        wizard.setMotivoConsulta(
                motivoSeguimiento
        );


// =================================================
// MÉDICO AUTENTICADO
// =================================================

        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (medico == null
                || medico.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Médico autenticado."
            );
        }


        // =================================================
        // CONSULTA DESDE POSTGRESQL
        // =================================================

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findById(
                                wizard.getIdConsultaOrigen()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La consulta médica indicada no existe."
                                        )
                        );


        // =================================================
        // CONSULTA FINALIZADA
        // =================================================

        if (!EstadoConsultaMedica.FINALIZADA.equals(
                consulta.getEstadoConsulta()
        )) {

            throw new IllegalStateException(
                    "Solo una consulta finalizada puede generar "
                            + "una cita de seguimiento."
            );
        }


        // =================================================
        // MÉDICO PROPIETARIO
        // =================================================

        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null
                || !medico.getId().equals(
                consulta.getMedico().getId()
        )) {

            throw new IllegalStateException(
                    "La consulta médica no pertenece al Médico autenticado."
            );
        }


        // =================================================
        // CITA ORIGINAL
        // =================================================

        Cita citaOrigen =
                consulta.getCita();


        if (citaOrigen == null
                || citaOrigen.getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee una cita de origen válida."
            );
        }


        // =================================================
        // ESTADO
        // =================================================

        if (citaOrigen.getEstadoCita() == null
                || citaOrigen.getEstadoCita().getNombre() == null
                || !ESTADO_CITA_CONSULTA_MEDICA
                .equalsIgnoreCase(
                        citaOrigen
                                .getEstadoCita()
                                .getNombre()
                )) {

            throw new IllegalStateException(
                    "Solo una cita evaluada y pendiente de cierre "
                            + "puede generar seguimiento."
            );
        }


        // =================================================
        // PACIENTE
        // =================================================

        if (paciente == null
                || paciente.getId() == null
                || citaOrigen.getPaciente() == null
                || citaOrigen.getPaciente().getId() == null
                || !paciente.getId().equals(
                citaOrigen.getPaciente().getId()
        )) {

            throw new IllegalStateException(
                    "El paciente del seguimiento no corresponde "
                            + "al paciente de la consulta original."
            );
        }


        // =================================================
        // RESERVA
        // =================================================

        if (reserva == null) {

            throw new IllegalStateException(
                    "No existe una reserva válida para el seguimiento."
            );
        }


        if (reserva.getPaciente() == null
                || reserva.getPaciente().getId() == null
                || !paciente.getId().equals(
                reserva.getPaciente().getId()
        )) {

            throw new IllegalStateException(
                    "La reserva no corresponde al paciente "
                            + "de la consulta original."
            );
        }


        // =================================================
        // MÉDICO DE RESERVA
        // =================================================

        if (reserva.getMedico() == null
                || reserva.getMedico().getId() == null
                || citaOrigen.getMedico() == null
                || citaOrigen.getMedico().getId() == null
                || !citaOrigen
                .getMedico()
                .getId()
                .equals(
                        reserva.getMedico().getId()
                )) {

            throw new IllegalStateException(
                    "El horario reservado pertenece a otro Médico."
            );
        }


        // =================================================
        // SUCURSAL DE RESERVA
        // =================================================

        if (reserva.getSucursal() == null
                || reserva.getSucursal().getId() == null
                || citaOrigen.getSucursal() == null
                || citaOrigen.getSucursal().getId() == null
                || !citaOrigen
                .getSucursal()
                .getId()
                .equals(
                        reserva.getSucursal().getId()
                )) {

            throw new IllegalStateException(
                    "El horario reservado pertenece a otra sucursal."
            );
        }


        // =================================================
        // ESPECIALIDAD DE RESERVA
        // =================================================

        if (reserva.getEspecialidad() == null
                || reserva.getEspecialidad().getId() == null
                || citaOrigen.getEspecialidad() == null
                || citaOrigen.getEspecialidad().getId() == null
                || !citaOrigen
                .getEspecialidad()
                .getId()
                .equals(
                        reserva.getEspecialidad().getId()
                )) {

            throw new IllegalStateException(
                    "El horario reservado pertenece a otra especialidad."
            );
        }


        // =================================================
        // NORMALIZAR VALOR QUE FINALMENTE SE GUARDARÁ
        // =================================================

        wizard.setTipoSeguimiento(
                tipoSeguimiento
        );


        return consulta;
    }

    // =====================================================
// CU-08 FA02
// OBTENER PACIENTE PARA LA RESERVA DE SEGUIMIENTO
// =====================================================
//
// Se utiliza cuando el Médico selecciona fecha/hora.
//
// Nunca tomamos el paciente desde el navegador.
// El paciente se obtiene nuevamente desde la
// Consulta Médica de origen.
// =====================================================

    @Transactional(readOnly = true)
    public Usuario obtenerPacienteParaReserva(
            CitaWizardDTO wizard
    ) {

        // =================================================
        // VALIDAR CONTEXTO
        // =================================================

        if (wizard == null
                || !Boolean.TRUE.equals(
                wizard.getEsSeguimiento()
        )) {

            throw new IllegalArgumentException(
                    "No existe un seguimiento médico válido."
            );
        }


        if (wizard.getIdConsultaOrigen() == null
                || wizard.getIdConsultaOrigen() <= 0) {

            throw new IllegalArgumentException(
                    "La consulta médica de origen es obligatoria."
            );
        }


        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (medico == null
                || medico.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Médico autenticado."
            );
        }


        // =================================================
        // CONSULTA REAL DESDE POSTGRESQL
        // =================================================

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findById(
                                wizard.getIdConsultaOrigen()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La consulta médica indicada no existe."
                                        )
                        );


        // =================================================
        // CONSULTA FINALIZADA
        // =================================================

        if (!EstadoConsultaMedica.FINALIZADA.equals(
                consulta.getEstadoConsulta()
        )) {

            throw new IllegalStateException(
                    "Solo una consulta finalizada puede generar "
                            + "una cita de seguimiento."
            );
        }


        // =================================================
        // PROPIEDAD DEL MÉDICO
        // =================================================

        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null
                || !medico.getId().equals(
                consulta.getMedico().getId()
        )) {

            throw new IllegalStateException(
                    "La consulta médica no pertenece al Médico autenticado."
            );
        }


        // =================================================
        // CITA ORIGINAL
        // =================================================

        Cita citaOrigen =
                consulta.getCita();


        if (citaOrigen == null
                || citaOrigen.getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee una cita de origen válida."
            );
        }


        // =================================================
        // ESTADO ORIGINAL
        // =================================================

        if (citaOrigen.getEstadoCita() == null
                || citaOrigen.getEstadoCita().getNombre() == null
                || !ESTADO_CITA_CONSULTA_MEDICA
                .equalsIgnoreCase(
                        citaOrigen
                                .getEstadoCita()
                                .getNombre()
                )) {

            throw new IllegalStateException(
                    "Solo una cita evaluada y pendiente de cierre "
                            + "puede generar seguimiento."
            );
        }


        // =================================================
        // PROTEGER LOS DATOS PRECARGADOS
        //
        // Si alguien altera el wizard entre peticiones,
        // se rechaza antes de reservar un horario.
        // =================================================

        if (citaOrigen.getMedico() == null
                || citaOrigen.getMedico().getId() == null
                || !citaOrigen
                .getMedico()
                .getId()
                .equals(
                        wizard.getIdMedico()
                )) {

            throw new IllegalStateException(
                    "El Médico del seguimiento fue modificado."
            );
        }


        if (citaOrigen.getSucursal() == null
                || citaOrigen.getSucursal().getId() == null
                || !citaOrigen
                .getSucursal()
                .getId()
                .equals(
                        wizard.getIdSucursal()
                )) {

            throw new IllegalStateException(
                    "La sucursal del seguimiento fue modificada."
            );
        }


        if (citaOrigen.getEspecialidad() == null
                || citaOrigen.getEspecialidad().getId() == null
                || !citaOrigen
                .getEspecialidad()
                .getId()
                .equals(
                        wizard.getIdEspecialidad()
                )) {

            throw new IllegalStateException(
                    "La especialidad del seguimiento fue modificada."
            );
        }


        // =================================================
        // PACIENTE ORIGINAL
        // =================================================

        if (citaOrigen.getPaciente() == null
                || citaOrigen.getPaciente().getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee un paciente válido."
            );
        }


        return citaOrigen.getPaciente();
    }

// =====================================================
// LIMPIAR TEXTO
// =====================================================

    private String limpiar(
            String valor
    ) {

        if (valor == null) {
            return null;
        }

        String limpio =
                valor.trim();


        return limpio.isEmpty()
                ? null
                : limpio;
    }
}