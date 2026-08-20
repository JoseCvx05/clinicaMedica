package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.AtencionEmergencia;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.AtencionEmergenciaRepository;
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
 * Registra el ingreso de una persona por emergencia.
 *
 * Puede:
 * - Reutilizar un paciente existente.
 * - Registrar una atención sin crear una cuenta falsa.
 * - Proteger el DPI.
 * - Asociar la sede del recepcionista.
 * - Dejar la atención pendiente de signos vitales.
 *
 * =========================================================
 */
@Service
public class RegistroEmergenciaService {


    private static final String PRIORIDAD_EMERGENCIA =
            "Emergencia";

    private static final String ESTADO_PENDIENTE_SIGNOS =
            "Pendiente de signos vitales";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final AtencionEmergenciaRepository
            atencionEmergenciaRepository;

    private final HashService hashService;

    private final CifradoService cifradoService;

    private final UsuarioActualService usuarioActualService;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RegistroEmergenciaService(

            UsuarioRepository usuarioRepository,

            AtencionEmergenciaRepository atencionEmergenciaRepository,

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
    // REGISTRAR EMERGENCIA
    // =====================================================

    @Transactional
    public ResultadoEmergencia registrar(
            String nombreCompleto,
            String dpi
    ) {

        String nombre =
                limpiar(
                        nombreCompleto
                );


        String dpiLimpio =
                limpiar(
                        dpi
                );


        // =================================================
        // NOMBRE
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
        // DPI 13 DÍGITOS
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
        // SI EXISTE, DEBE SER PACIENTE
        // =================================================

        if (paciente != null) {

            if (paciente.getRol() == null
                    || paciente.getRol().getNombre() == null
                    || !"Paciente".equalsIgnoreCase(
                    paciente.getRol()
                            .getNombre()
                            .trim()
            )) {

                return ResultadoEmergencia.error(
                        "El DPI ingresado pertenece a un usuario "
                                + "que no está registrado como paciente."
                );
            }


            /*
             * Utilizamos el nombre ya registrado.
             */
            nombre =
                    paciente.getNombreCompleto();
        }


        // =================================================
        // FECHA/HORA
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // CREAR ATENCIÓN
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