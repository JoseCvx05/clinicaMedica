package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;
import com.proyecto.clinicamedica.repository.SignosVitalesRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import java.util.Map;
/**
 * =========================================================
 * SERVICIO: PANEL DE ENFERMERÍA
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Responsabilidades:
 *
 * - Mostrar pacientes en estado Paciente Presente.
 * - Mostrar pacientes que ya están en Signos Vitales.
 * - Llamar al paciente.
 * - Cambiar la cita:
 *
 *      Paciente Presente -> Signos Vitales
 *
 * - Preparar el mensaje que será reproducido por TTS.
 *
 * =========================================================
 */
@Service
public class PanelEnfermeriaService {


    // =====================================================
    // ESTADOS
    // =====================================================

    private static final String ESTADO_PACIENTE_PRESENTE =
            "Paciente Presente";

    private static final String ESTADO_SIGNOS_VITALES =
            "Signos Vitales";


    // =====================================================
    // ROL
    // =====================================================

    private static final String ROL_ENFERMERO =
            "Enfermero";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final EstadoCitaRepository estadoCitaRepository;

    private final SignosVitalesRepository
            signosVitalesRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final ZoneId zonaHoraria;

    private final UsuarioRepository usuarioRepository;

    private final HashService hashService;

    private final CatalogoEstadoCitaService
            catalogoEstadoCitaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PanelEnfermeriaService(

            CitaRepository citaRepository,

            EstadoCitaRepository estadoCitaRepository,

            SignosVitalesRepository signosVitalesRepository,

            UsuarioActualService usuarioActualService,

            UsuarioRepository usuarioRepository,
            HashService hashService,

            CatalogoEstadoCitaService catalogoEstadoCitaService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.signosVitalesRepository =
                signosVitalesRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );

        this.usuarioRepository =
                usuarioRepository;

        this.hashService =
                hashService;

        this.catalogoEstadoCitaService =
                catalogoEstadoCitaService;
    }


    // =====================================================
// OBTENER PANEL COMPLETO
// =====================================================

    @Transactional(readOnly = true)
    public PanelEnfermeria obtenerPanel() {

        validarEnfermeroActual();

        return construirPanel(
                null
        );
    }


// =====================================================
// CU-07 / RN-GLOBAL-001
// BUSCAR POR DPI
// =====================================================

    @Transactional(readOnly = true)
    public ResultadoBusquedaDpi buscarPorDpi(
            String dpi
    ) {

        validarEnfermeroActual();


        String dpiLimpio =
                dpi == null
                        ? ""
                        : dpi.trim();


        // =================================================
        // DPI OBLIGATORIO
        // =================================================

        if (dpiLimpio.isBlank()) {

            return ResultadoBusquedaDpi.error(
                    "El campo DPI es obligatorio. "
                            + "Por favor, ingrese su número de DPI."
            );
        }


        // =================================================
        // SOLO NÚMEROS
        // =================================================

        if (!dpiLimpio.matches("\\d+")) {

            return ResultadoBusquedaDpi.error(
                    "El DPI debe contener únicamente números. "
                            + "No se permiten letras ni caracteres especiales."
            );
        }


        // =================================================
        // EXACTAMENTE 13 DÍGITOS
        // =================================================

        if (dpiLimpio.length() != 13) {

            return ResultadoBusquedaDpi.error(
                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpiLimpio.length()
                            + " dígitos."
            );
        }


        // =================================================
        // HASH PARA BÚSQUEDA
        // =================================================

        String dpiHash =
                hashService
                        .generarHash(
                                dpiLimpio
                        );


        Usuario paciente =
                usuarioRepository
                        .findByDpiHash(
                                dpiHash
                        )
                        .orElse(
                                null
                        );


        // =================================================
        // PACIENTE NO ENCONTRADO
        // =================================================

        if (paciente == null
                || paciente.getId() == null) {

            return ResultadoBusquedaDpi.exito(
                    new PanelEnfermeria(
                            List.of(),
                            List.of()
                    ),
                    "No se encontraron pacientes pendientes "
                            + "de atención con el DPI indicado."
            );
        }


        PanelEnfermeria panel =
                construirPanel(
                        paciente.getId()
                );


        String mensaje = null;


        if (panel.pacientesPresentes().isEmpty()
                && panel.pacientesEnSignos().isEmpty()) {

            mensaje =
                    "El paciente no tiene citas pendientes "
                            + "de atención en Enfermería.";
        }


        return ResultadoBusquedaDpi.exito(
                panel,
                mensaje
        );
    }


// =====================================================
// CONSTRUIR PANEL
// =====================================================

    private PanelEnfermeria construirPanel(
            Integer idPacienteFiltro
    ) {

        List<PacientePanelEnfermeria> presentes =
                citaRepository
                        .buscarParaPanelEnfermeriaPorEstado(
                                ESTADO_PACIENTE_PRESENTE
                        )
                        .stream()

                        .filter(
                                cita ->
                                        idPacienteFiltro == null
                                                ||
                                                (
                                                        cita.getPaciente() != null
                                                                && idPacienteFiltro.equals(
                                                                cita.getPaciente()
                                                                        .getId()
                                                        )
                                                )
                        )

                        .map(
                                this::convertirPaciente
                        )

                        .toList();


        List<PacientePanelEnfermeria> enSignos =
                citaRepository
                        .buscarParaPanelEnfermeriaPorEstado(
                                ESTADO_SIGNOS_VITALES
                        )
                        .stream()

                        .filter(
                                cita ->
                                        idPacienteFiltro == null
                                                ||
                                                (
                                                        cita.getPaciente() != null
                                                                && idPacienteFiltro.equals(
                                                                cita.getPaciente()
                                                                        .getId()
                                                        )
                                                )
                        )

                        .map(
                                this::convertirPaciente
                        )

                        .toList();


        return new PanelEnfermeria(
                presentes,
                enSignos
        );
    }


    // =====================================================
    // LLAMAR Y TOMAR SIGNOS
    // =====================================================

    @Transactional
    public LlamadoEnfermeria llamarYTomarSignos(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            throw new IllegalArgumentException(
                    "La cita seleccionada no es válida."
            );
        }


        Usuario enfermero =
                validarEnfermeroActual();


        // =================================================
        // BLOQUEAR CITA
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaLlamarEnfermeriaConBloqueo(
                                idCita
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La cita seleccionada no existe."
                                        )
                        );


        // =================================================
        // ESTADO ACTUAL
        // =================================================

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null
                || !ESTADO_PACIENTE_PRESENTE
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "La cita ya no se encuentra en estado Paciente Presente."
            );
        }


        // =================================================
        // DEFENSA CONTRA REGISTRO PREVIO
        // =================================================

        if (signosVitalesRepository
                .existsByCita_Id(
                        idCita
                )) {

            throw new IllegalStateException(
                    "La cita ya posee un registro de signos vitales."
            );
        }


        // =================================================
// CATÁLOGO EN CACHÉ
// =================================================

        Map<String, Integer> catalogoEstados =
                catalogoEstadoCitaService
                        .listarEstadosActivos();


        Integer idEstadoSignosVitales =
                catalogoEstadoCitaService
                        .obtenerIdEstadoActivo(

                                ESTADO_SIGNOS_VITALES,

                                catalogoEstados
                        );


        if (idEstadoSignosVitales == null) {

            throw new IllegalStateException(
                    "No existe el estado de cita "
                            + "'Signos Vitales'."
            );
        }


        /*
         * getReferenceById no necesita realizar una búsqueda
         * del catálogo por nombre.
         *
         * El ID fue obtenido desde Caffeine.
         */
        EstadoCita estadoSignosVitales =
                estadoCitaRepository
                        .getReferenceById(
                                idEstadoSignosVitales
                        );


        // =================================================
        // TRANSICIÓN
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        cita.setEstadoCita(
                estadoSignosVitales
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                enfermero
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        // =================================================
        // MENSAJE TTS
        // =================================================

        String mensajeTts =
                "Turno número "
                        + cita.getId()
                        + ". Paciente "
                        + cita.getPaciente()
                        .getNombreCompleto()
                        + ", favor pasar a toma de signos vitales.";


        return new LlamadoEnfermeria(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                mensajeTts
        );
    }


    // =====================================================
    // CONVERTIR PARA PANEL
    // =====================================================

    private PacientePanelEnfermeria convertirPaciente(
            Cita cita
    ) {

        return new PacientePanelEnfermeria(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getSucursal()
                        .getNombre(),

                convertirAZonaHoraria(
                        cita.getFechaHoraCita()
                ),

                cita.getPrioridad(),

                cita.getEstadoCita()
                        .getNombre()
        );
    }

    // =====================================================
// ZONA HORARIA
// =====================================================

    private OffsetDateTime convertirAZonaHoraria(
            OffsetDateTime fechaHora
    ) {

        if (fechaHora == null) {

            return null;
        }


        return fechaHora
                .atZoneSameInstant(
                        zonaHoraria
                )
                .toOffsetDateTime();
    }

    // =====================================================
    // VALIDAR ENFERMERO
    // =====================================================

    private Usuario validarEnfermeroActual() {

        Usuario usuario =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (usuario == null

                || !Boolean.TRUE.equals(
                usuario.getActivo()
        )

                || usuario.getRol() == null

                || usuario.getRol().getNombre() == null

                || !ROL_ENFERMERO
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para acceder al módulo de Enfermería."
            );
        }


        return usuario;
    }


    // =====================================================
    // PANEL
    // =====================================================

    public record PanelEnfermeria(

            List<PacientePanelEnfermeria> pacientesPresentes,

            List<PacientePanelEnfermeria> pacientesEnSignos

    ) {
    }


    // =====================================================
    // PACIENTE DEL PANEL
    // =====================================================

    public record PacientePanelEnfermeria(

            Integer numeroCita,

            String nombrePaciente,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            String prioridad,

            String estado

    ) {


        public boolean esEmergencia() {

            return prioridad != null
                    && "Emergencia"
                    .equalsIgnoreCase(
                            prioridad.trim()
                    );
        }
    }


    // =====================================================
    // RESULTADO DEL LLAMADO
    // =====================================================

    public record LlamadoEnfermeria(

            Integer numeroCita,

            String nombrePaciente,

            String mensajeTts

    ) {
    }
    // =====================================================
// RESULTADO DE BÚSQUEDA POR DPI
// =====================================================

    public record ResultadoBusquedaDpi(

            boolean exitoso,

            PanelEnfermeria panel,

            String mensaje

    ) {


        public static ResultadoBusquedaDpi exito(

                PanelEnfermeria panel,

                String mensaje
        ) {

            return new ResultadoBusquedaDpi(
                    true,
                    panel,
                    mensaje
            );
        }


        public static ResultadoBusquedaDpi error(
                String mensaje
        ) {

            return new ResultadoBusquedaDpi(
                    false,
                    null,
                    mensaje
            );
        }
    }
}