package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.AtencionEmergencia;
import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.AtencionEmergenciaRepository;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;


/**
 * =========================================================
 * SERVICIO: REGISTRO DE EMERGENCIA
 * =========================================================
 *
 * CU-05 - FA01.
 *
 * Permite registrar:
 *
 * 1. Emergencia independiente, sin cita.
 * 2. Emergencia asociada a una cita existente.
 *
 * Cuando existe una cita:
 *
 * - Debe estar en "Paciente Presente".
 * - El DPI debe pertenecer al paciente de esa cita.
 * - La prioridad de la cita cambia a "Emergencia".
 *
 * De esta forma CU-07 puede reconocer automáticamente
 * la emergencia al recibir al paciente en Enfermería.
 *
 * =========================================================
 */
@Service
public class RegistroEmergenciaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String PRIORIDAD_EMERGENCIA =
            "Emergencia";


    private static final String ESTADO_PENDIENTE_SIGNOS =
            "Pendiente de signos vitales";


    private static final String ESTADO_PACIENTE_PRESENTE =
            "Paciente Presente";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository
            usuarioRepository;


    private final AtencionEmergenciaRepository
            atencionEmergenciaRepository;


    private final CitaRepository
            citaRepository;


    private final HashService
            hashService;


    private final CifradoService
            cifradoService;


    private final UsuarioActualService
            usuarioActualService;


    private final ZoneId
            zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RegistroEmergenciaService(

            UsuarioRepository usuarioRepository,

            AtencionEmergenciaRepository
                    atencionEmergenciaRepository,

            CitaRepository citaRepository,

            HashService hashService,

            CifradoService cifradoService,

            UsuarioActualService usuarioActualService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.usuarioRepository =
                usuarioRepository;


        this.atencionEmergenciaRepository =
                atencionEmergenciaRepository;


        this.citaRepository =
                citaRepository;


        this.hashService =
                hashService;


        this.cifradoService =
                cifradoService;


        this.usuarioActualService =
                usuarioActualService;


        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // COMPATIBILIDAD - EMERGENCIA SIN CITA
    // =====================================================

    /**
     * Mantiene disponible el flujo original de CU-05:
     *
     * Registrar una emergencia sin seleccionar previamente
     * una cita.
     */
    @Transactional
    public ResultadoEmergencia registrar(

            String nombreCompleto,

            String dpi
    ) {

        return registrar(
                nombreCompleto,
                dpi,
                null
        );
    }


    // =====================================================
    // REGISTRAR EMERGENCIA
    // =====================================================

    @Transactional
    public ResultadoEmergencia registrar(

            String nombreCompleto,

            String dpi,

            Integer idCita
    ) {

        // =================================================
        // LIMPIAR ENTRADAS
        // =================================================

        String nombre =
                limpiar(
                        nombreCompleto
                );


        String dpiLimpio =
                limpiar(
                        dpi
                );


        // =================================================
        // VALIDAR NOMBRE
        // =================================================

        if (nombre.isBlank()) {

            return ResultadoEmergencia.error(
                    "El nombre del paciente es obligatorio."
            );
        }


        if (nombre.length() > 100) {

            return ResultadoEmergencia.error(
                    "El nombre del paciente no puede exceder los 100 caracteres."
            );
        }


        // =================================================
        // DPI OBLIGATORIO
        // =================================================

        if (dpiLimpio.isBlank()) {

            return ResultadoEmergencia.error(
                    "El campo DPI es obligatorio. "
                            + "Por favor, ingrese su número de DPI."
            );
        }


        // =================================================
        // DPI SOLO NÚMEROS
        // =================================================

        if (!dpiLimpio.matches("\\d+")) {

            return ResultadoEmergencia.error(
                    "El DPI debe contener únicamente números. "
                            + "No se permiten letras ni caracteres especiales."
            );
        }


        // =================================================
        // DPI EXACTAMENTE 13 DÍGITOS
        // =================================================

        if (dpiLimpio.length() != 13) {

            return ResultadoEmergencia.error(
                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpiLimpio.length()
                            + " dígitos."
            );
        }


        // =================================================
        // USUARIO INTERNO
        // =================================================

        Usuario recepcionista =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (recepcionista == null
                || recepcionista.getSucursal() == null) {

            return ResultadoEmergencia.error(
                    "No fue posible determinar la sucursal de recepción."
            );
        }


        // =================================================
        // PROTEGER DPI
        // =================================================

        String dpiHash =
                hashService
                        .generarHash(
                                dpiLimpio
                        );


        String dpiCifrado =
                cifradoService
                        .cifrar(
                                dpiLimpio
                        );


        // =================================================
        // BUSCAR PACIENTE EXISTENTE
        // =================================================

        Usuario paciente =
                usuarioRepository
                        .findByDpiHash(
                                dpiHash
                        )
                        .orElse(
                                null
                        );


        // =================================================
        // SI EXISTE DEBE SER PACIENTE
        // =================================================

        if (paciente != null) {

            if (paciente.getRol() == null

                    || paciente.getRol()
                    .getNombre() == null

                    || !"Paciente"
                    .equalsIgnoreCase(
                            paciente
                                    .getRol()
                                    .getNombre()
                                    .trim()
                    )) {

                return ResultadoEmergencia.error(
                        "El DPI ingresado pertenece a un usuario "
                                + "que no está registrado como paciente."
                );
            }


            /*
             * Si el paciente existe utilizamos el nombre
             * oficial almacenado en el sistema.
             */
            nombre =
                    paciente.getNombreCompleto();
        }


        // =================================================
        // FECHA / HORA
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // EMERGENCIA ASOCIADA A UNA CITA
        // =================================================
        //
        // Si idCita es null, este bloque se omite y
        // conservamos el flujo original de emergencia
        // independiente.
        // =================================================

        if (idCita != null) {

            ResultadoEmergencia errorCita =
                    asociarEmergenciaACita(

                            idCita,

                            paciente,

                            recepcionista,

                            ahora
                    );


            if (errorCita != null) {

                return errorCita;
            }
        }


        // =================================================
        // CREAR ATENCIÓN DE EMERGENCIA
        // =================================================

        AtencionEmergencia emergencia =
                new AtencionEmergencia();


        emergencia.setPaciente(
                paciente
        );


        emergencia.setNombrePaciente(
                nombre
        );


        emergencia.setDpiHash(
                dpiHash
        );


        emergencia.setDpiCifrado(
                dpiCifrado
        );


        emergencia.setSucursal(
                recepcionista.getSucursal()
        );


        emergencia.setPrioridad(
                PRIORIDAD_EMERGENCIA
        );


        emergencia.setEstado(
                ESTADO_PENDIENTE_SIGNOS
        );


        emergencia.setFechaHoraLlegada(
                ahora
        );


        emergencia.setCreadoPor(
                recepcionista
        );


        // =================================================
        // GUARDAR
        // =================================================

        AtencionEmergencia guardada =
                atencionEmergenciaRepository
                        .saveAndFlush(
                                emergencia
                        );


        // =================================================
        // MENSAJE FA01
        // =================================================

        String mensaje =
                "Paciente "
                        + nombre
                        + " registrado con prioridad de EMERGENCIA. "
                        + "El paciente debe pasar directamente "
                        + "a toma de signos vitales.";


        return ResultadoEmergencia.exito(

                guardada.getId(),

                paciente == null
                        ? null
                        : paciente.getId(),

                nombre,

                mensaje
        );
    }


    // =====================================================
    // ASOCIAR EMERGENCIA A CITA
    // =====================================================

    /**
     * Cuando Recepción está trabajando sobre una cita
     * específica, la marca como Emergencia.
     *
     * Esto permite la integración:
     *
     * CU-05 -> CU-07
     */
    private ResultadoEmergencia asociarEmergenciaACita(

            Integer idCita,

            Usuario paciente,

            Usuario recepcionista,

            OffsetDateTime ahora
    ) {

        // =================================================
        // ID CITA
        // =================================================

        if (idCita <= 0) {

            return ResultadoEmergencia.error(
                    "La cita seleccionada no es válida."
            );
        }


        // =================================================
        // PARA UNA CITA DEBE EXISTIR PACIENTE
        // =================================================

        if (paciente == null
                || paciente.getId() == null) {

            return ResultadoEmergencia.error(
                    "El DPI ingresado no corresponde "
                            + "al paciente de la cita seleccionada."
            );
        }


        // =================================================
        // BUSCAR CITA
        // =================================================

        Cita cita =
                citaRepository
                        .findById(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoEmergencia.error(
                    "La cita seleccionada no existe."
            );
        }


        // =================================================
        // VALIDAR PACIENTE DE LA CITA
        // =================================================

        if (cita.getPaciente() == null

                || cita.getPaciente()
                .getId() == null

                || !paciente.getId()
                .equals(
                        cita.getPaciente()
                                .getId()
                )) {

            return ResultadoEmergencia.error(
                    "El DPI ingresado no corresponde "
                            + "al paciente de la cita seleccionada."
            );
        }


        // =================================================
        // VALIDAR ESTADO
        // =================================================

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita()
                .getNombre() == null

                || !ESTADO_PACIENTE_PRESENTE
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            return ResultadoEmergencia.error(
                    "La cita debe encontrarse en estado "
                            + "'Paciente Presente' antes de marcarla "
                            + "como emergencia."
            );
        }


        // =================================================
        // EVITAR DOBLE MARCADO
        // =================================================

        if (PRIORIDAD_EMERGENCIA
                .equalsIgnoreCase(
                        limpiar(
                                cita.getPrioridad()
                        )
                )) {

            return ResultadoEmergencia.error(
                    "La cita ya se encuentra marcada "
                            + "con prioridad de EMERGENCIA."
            );
        }


        // =================================================
        // CAMBIAR PRIORIDAD
        // =================================================

        cita.setPrioridad(
                PRIORIDAD_EMERGENCIA
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


        return null;
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // RESULTADO
    // =====================================================

    public record ResultadoEmergencia(

            boolean exitoso,

            Integer idAtencionEmergencia,

            Integer idPaciente,

            String nombrePaciente,

            String mensaje

    ) {


        public static ResultadoEmergencia exito(

                Integer idAtencionEmergencia,

                Integer idPaciente,

                String nombrePaciente,

                String mensaje
        ) {

            return new ResultadoEmergencia(

                    true,

                    idAtencionEmergencia,

                    idPaciente,

                    nombrePaciente,

                    mensaje
            );
        }


        public static ResultadoEmergencia error(
                String mensaje
        ) {

            return new ResultadoEmergencia(

                    false,

                    null,

                    null,

                    null,

                    mensaje
            );
        }
    }
}