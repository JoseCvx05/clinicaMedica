package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * =========================================================
 * SERVICIO: REASIGNACIÓN DE MÉDICO
 * =========================================================
 *
 * CU-05 - FA07.
 *
 * Responsabilidades:
 *
 * - Validar que la cita permita reasignación.
 * - Buscar médicos de la misma sede y especialidad.
 * - Excluir al médico actual.
 * - Validar disponibilidad real.
 * - Revalidar disponibilidad antes de guardar.
 * - Cambiar el médico asignado.
 * - Registrar fecha y usuario modificador.
 * - Registrar la reasignación en bitácora.
 * - Registrar el motivo opcional de reasignación.
 *
 * =========================================================
 */
@Service
public class ReasignacionMedicoService {


    // =====================================================
    // ESTADOS
    // =====================================================

    private static final String ESTADO_CONFIRMADA =
            "Confirmada";

    private static final String ESTADO_PRESENTE =
            "Paciente Presente";


    // =====================================================
    // MENSAJES
    // =====================================================

    private static final String MENSAJE_EXITOSO =
            "Médico reasignado correctamente";

    private static final String MENSAJE_NO_PERMITIDO =
            "Operación no permitida";

    private static final String MENSAJE_MEDICO_INVALIDO =
            "El médico seleccionado no está disponible "
                    + "para la sucursal y especialidad de la cita.";

    private static final String MENSAJE_MEDICO_OCUPADO =
            "El médico seleccionado ya no está disponible "
                    + "para el horario de la cita.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final UsuarioRepository usuarioRepository;

    private final CatalogoCitaService catalogoCitaService;

    private final DisponibilidadCitaService
            disponibilidadCitaService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ReasignacionMedicoService(

            CitaRepository citaRepository,

            UsuarioRepository usuarioRepository,

            CatalogoCitaService catalogoCitaService,

            DisponibilidadCitaService disponibilidadCitaService,

            UsuarioActualService usuarioActualService,

            AuditoriaService auditoriaService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.catalogoCitaService =
                catalogoCitaService;

        this.disponibilidadCitaService =
                disponibilidadCitaService;

        this.usuarioActualService =
                usuarioActualService;

        this.auditoriaService =
                auditoriaService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // CONSULTAR DATOS DE REASIGNACIÓN
    // =====================================================

    @Transactional(readOnly = true)
    public DetalleReasignacion obtenerDatos(
            Integer idCita
    ) {

        Cita cita =
                obtenerCita(
                        idCita
                );


        validarEstado(
                cita
        );


        if (cita.getMedico() == null
                || cita.getSucursal() == null
                || cita.getEspecialidad() == null
                || cita.getPaciente() == null
                || cita.getFechaHoraCita() == null
                || cita.getFechaHoraFin() == null) {

            throw new IllegalArgumentException(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // MÉDICOS DISPONIBLES
        // =================================================

        List<MedicoReasignacion> medicosDisponibles =
                catalogoCitaService
                        .listarMedicos(

                                cita.getSucursal()
                                        .getId(),

                                cita.getEspecialidad()
                                        .getId()
                        )
                        .stream()


                        // =================================
                        // EXCLUIR MÉDICO ACTUAL
                        // =================================

                        .filter(
                                medico ->
                                        !medico.id()
                                                .equals(
                                                        cita.getMedico()
                                                                .getId()
                                                )
                        )


                        // =================================
                        // DISPONIBILIDAD REAL
                        // =================================

                        .filter(
                                medico ->
                                        disponibilidadCitaService
                                                .estaDisponibleParaReasignacion(

                                                        medico.id(),

                                                        cita.getFechaHoraCita(),

                                                        cita.getFechaHoraFin()
                                                )
                        )


                        .map(
                                medico ->
                                        new MedicoReasignacion(

                                                medico.id(),

                                                medico.nombre()
                                        )
                        )


                        .toList();


        return new DetalleReasignacion(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getFechaHoraCita(),

                cita.getFechaHoraFin(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getSucursal()
                        .getNombre(),

                cita.getMedico()
                        .getId(),

                cita.getMedico()
                        .getNombreCompleto(),

                medicosDisponibles
        );
    }


    // =====================================================
    // CONFIRMAR REASIGNACIÓN
    // =====================================================

    @Transactional
    public ResultadoReasignacion reasignar(

            Integer idCita,

            Integer idNuevoMedico,

            String motivo,

            String direccionIp
    ) {

        // =================================================
        // VALIDACIÓN BÁSICA
        // =================================================

        if (idCita == null
                || idCita <= 0
                || idNuevoMedico == null
                || idNuevoMedico <= 0) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // USUARIO QUE REALIZA LA OPERACIÓN
        // =================================================

        Usuario recepcionista =
                usuarioActualService
                        .obtenerUsuarioActual();


        // =================================================
        // BLOQUEO PESIMISTA
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaRegistrarLlegadaConBloqueo(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // VALIDAR ESTADO
        // =================================================

        try {

            validarEstado(
                    cita
            );

        } catch (IllegalArgumentException ex) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // DATOS OBLIGATORIOS
        // =================================================

        if (cita.getMedico() == null
                || cita.getSucursal() == null
                || cita.getEspecialidad() == null
                || cita.getFechaHoraCita() == null
                || cita.getFechaHoraFin() == null) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // NO PERMITIR EL MISMO MÉDICO
        // =================================================

        if (idNuevoMedico.equals(
                cita.getMedico()
                        .getId()
        )) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // VALIDAR SUCURSAL / ESPECIALIDAD / ACTIVO
        // =================================================

        boolean medicoValido =
                catalogoCitaService
                        .medicoDisponibleParaSeleccion(

                                idNuevoMedico,

                                cita.getSucursal()
                                        .getId(),

                                cita.getEspecialidad()
                                        .getId()
                        );


        if (!medicoValido) {

            return ResultadoReasignacion.error(
                    MENSAJE_MEDICO_INVALIDO
            );
        }


        // =================================================
        // REVALIDAR DISPONIBILIDAD
        // =================================================

        boolean disponible =
                disponibilidadCitaService
                        .estaDisponibleParaReasignacion(

                                idNuevoMedico,

                                cita.getFechaHoraCita(),

                                cita.getFechaHoraFin()
                        );


        if (!disponible) {

            return ResultadoReasignacion.error(
                    MENSAJE_MEDICO_OCUPADO
            );
        }


        // =================================================
        // OBTENER NUEVO MÉDICO
        // =================================================

        Usuario nuevoMedico =
                usuarioRepository
                        .findById(
                                idNuevoMedico
                        )
                        .orElse(
                                null
                        );


        if (nuevoMedico == null) {

            return ResultadoReasignacion.error(
                    MENSAJE_NO_PERMITIDO
            );
        }


        // =================================================
        // CONSERVAR MÉDICO ANTERIOR PARA AUDITORÍA
        // =================================================

        Usuario medicoAnterior =
                cita.getMedico();


        Map<String, Object> valoresAnteriores =
                crearSnapshotMedico(
                        medicoAnterior
                );


        // =================================================
        // ACTUALIZAR CITA
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        cita.setMedico(
                nuevoMedico
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                recepcionista
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        // =================================================
        // SNAPSHOT NUEVO
        // =================================================

        Map<String, Object> valoresNuevos =
                crearSnapshotMedico(
                        nuevoMedico
                );


        // =================================================
        // MOTIVO OPCIONAL
        // =================================================

        String motivoLimpio =
                limpiarOpcional(
                        motivo
                );


        if (motivoLimpio != null) {

            valoresNuevos.put(
                    "motivoReasignacion",
                    motivoLimpio
            );
        }


        // =================================================
        // AUDITORÍA INMUTABLE
        // =================================================

        auditoriaService.registrar(

                new RegistroAuditoria(

                        "cita",

                        String.valueOf(
                                cita.getId()
                        ),

                        AccionAuditoria.REASIGNACION,

                        recepcionista.getId(),

                        recepcionista.getNombreUsuario(),

                        valoresAnteriores,

                        valoresNuevos,

                        limpiarOpcional(
                                direccionIp
                        )
                )
        );


        return ResultadoReasignacion.exito(
                MENSAJE_EXITOSO
        );
    }


    // =====================================================
    // OBTENER CITA
    // =====================================================

    private Cita obtenerCita(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            throw new IllegalArgumentException(
                    MENSAJE_NO_PERMITIDO
            );
        }


        return citaRepository
                .buscarParaRecepcionPorNumero(
                        idCita
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        MENSAJE_NO_PERMITIDO
                                )
                );
    }


    // =====================================================
    // VALIDAR ESTADO
    // =====================================================

    private void validarEstado(
            Cita cita
    ) {

        if (cita == null
                || cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null) {

            throw new IllegalArgumentException(
                    MENSAJE_NO_PERMITIDO
            );
        }


        String estado =
                cita.getEstadoCita()
                        .getNombre()
                        .trim();


        boolean permitido =
                ESTADO_CONFIRMADA
                        .equalsIgnoreCase(
                                estado
                        )

                        || ESTADO_PRESENTE
                        .equalsIgnoreCase(
                                estado
                        );


        if (!permitido) {

            throw new IllegalArgumentException(
                    MENSAJE_NO_PERMITIDO
            );
        }
    }


    // =====================================================
    // SNAPSHOT DEL MÉDICO
    // =====================================================

    private Map<String, Object> crearSnapshotMedico(
            Usuario medico
    ) {

        Map<String, Object> datos =
                new LinkedHashMap<>();


        datos.put(
                "idMedico",
                medico.getId()
        );


        datos.put(
                "medico",
                medico.getNombreCompleto()
        );


        return datos;
    }


    // =====================================================
    // LIMPIAR CAMPO OPCIONAL
    // =====================================================

    private String limpiarOpcional(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isBlank()
                ? null
                : limpio;
    }


    // =====================================================
    // MÉDICO DISPONIBLE
    // =====================================================

    public record MedicoReasignacion(

            Integer id,

            String nombre

    ) {
    }


    // =====================================================
    // DATOS DE PANTALLA
    // =====================================================

    public record DetalleReasignacion(

            Integer idCita,

            String paciente,

            OffsetDateTime fechaHoraInicio,

            OffsetDateTime fechaHoraFin,

            String especialidad,

            String sucursal,

            Integer idMedicoActual,

            String medicoActual,

            List<MedicoReasignacion> medicosDisponibles

    ) {
    }


    // =====================================================
    // RESULTADO
    // =====================================================

    public record ResultadoReasignacion(

            boolean exitoso,

            String mensaje

    ) {


        public static ResultadoReasignacion exito(
                String mensaje
        ) {

            return new ResultadoReasignacion(
                    true,
                    mensaje
            );
        }


        public static ResultadoReasignacion error(
                String mensaje
        ) {

            return new ResultadoReasignacion(
                    false,
                    mensaje
            );
        }
    }
}