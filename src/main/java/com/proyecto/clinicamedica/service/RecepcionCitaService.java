package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.recepcion.RecepcionBusquedaDTO;
import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: RECEPCIÓN Y VERIFICACIÓN DE CITA
 * =========================================================
 *
 * CU-05 Recepción y Verificación de Cita.
 *
 * Responsabilidades:
 *
 * - Buscar cita por número.
 * - Buscar paciente por DPI.
 * - Interpretar estados de la cita.
 * - Resolver FA03, FA04, FA05 y FA06.
 * - Registrar llegada.
 * - Controlar llegada de emergencia.
 * - Manejar concurrencia del registro de llegada.
 * - Obtener paciente objetivo para agendamiento Walk-in.
 *
 * FA07 - Reasignación de médico se maneja en:
 * ReasignacionMedicoService.
 *
 * =========================================================
 */
@Service
public class RecepcionCitaService {


    // =====================================================
    // ESTADOS
    // =====================================================

    private static final String ESTADO_PAGADA =
            "Pagada";

    private static final String ESTADO_CONFIRMADA =
            "Confirmada";

    private static final String ESTADO_PENDIENTE_PAGO =
            "Pendiente de pago";

    private static final String ESTADO_CANCELADA =
            "Cancelada";

    private static final String ESTADO_PRESENTE =
            "Paciente Presente";


    // =====================================================
    // PRIORIDAD
    // =====================================================

    private static final String PRIORIDAD_EMERGENCIA =
            "Emergencia";


    // =====================================================
    // MENSAJES
    // =====================================================

    public static final String MENSAJE_BUSQUEDA_OBLIGATORIA =
            "Debe ingresar un número de cita o DPI para buscar.";

    public static final String MENSAJE_SIN_RESULTADOS =
            "No se encontró una cita asociada a los parámetros ingresados. "
                    + "Verifique los datos e intente nuevamente.";

    public static final String MENSAJE_PACIENTE_NO_EXISTE =
            "No se encontró ningún paciente con ese DPI.";

    public static final String SUBTEXTO_PACIENTE_NO_EXISTE =
            "Es necesario registrar al paciente antes de continuar.";

    public static final String MENSAJE_PENDIENTE_PAGO =
            "La cita del paciente tiene estado 'Pendiente de pago'. "
                    + "Debe realizar el pago en caja antes de ser atendido.";

    public static final String MENSAJE_CANCELADA =
            "La cita fue cancelada. El paciente debe agendar una nueva cita.";

    private static final String MENSAJE_OPERACION_NO_PERMITIDA =
            "Operación no permitida";

    private static final String MENSAJE_ERROR_LLEGADA =
            "Error al registrar la llegada";

    private static final String INDICADOR_ESPERA =
            "Llegada registrada — esperando llamado de enfermería";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final UsuarioRepository usuarioRepository;

    private final HashService hashService;

    private final EstadoCitaRepository estadoCitaRepository;

    private final UsuarioActualService usuarioActualService;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RecepcionCitaService(
            CitaRepository citaRepository,
            UsuarioRepository usuarioRepository,
            HashService hashService,
            EstadoCitaRepository estadoCitaRepository,
            UsuarioActualService usuarioActualService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.hashService =
                hashService;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // BUSCAR
    // =====================================================

    @Transactional(readOnly = true)
    public ResultadoBusqueda buscar(
            RecepcionBusquedaDTO formulario
    ) {

        if (formulario == null) {

            return ResultadoBusqueda.error(
                    MENSAJE_BUSQUEDA_OBLIGATORIA
            );
        }


        String tipo =
                normalizar(
                        formulario.getTipoBusqueda()
                );


        String valor =
                normalizar(
                        formulario.getValorBusqueda()
                );


        // =================================================
        // DPI VACÍO
        // =================================================

        if (RecepcionBusquedaDTO.TIPO_DPI
                .equalsIgnoreCase(tipo)
                && valor.isBlank()) {

            return ResultadoBusqueda.error(
                    "El campo DPI es obligatorio. "
                            + "Por favor, ingrese su número de DPI."
            );
        }


        // =================================================
        // CRITERIO VACÍO
        // =================================================

        if (valor.isBlank()) {

            return ResultadoBusqueda.error(
                    MENSAJE_BUSQUEDA_OBLIGATORIA
            );
        }


        // =================================================
        // BÚSQUEDA POR DPI
        // =================================================

        if (RecepcionBusquedaDTO.TIPO_DPI
                .equalsIgnoreCase(tipo)) {

            return buscarPorDpi(
                    valor
            );
        }


        // =================================================
        // BÚSQUEDA POR NÚMERO DE CITA
        // =================================================

        if (RecepcionBusquedaDTO.TIPO_CITA
                .equalsIgnoreCase(tipo)) {

            return buscarPorNumeroCita(
                    valor
            );
        }


        return ResultadoBusqueda.error(
                MENSAJE_BUSQUEDA_OBLIGATORIA
        );
    }


    // =====================================================
    // BUSCAR POR NÚMERO
    // =====================================================

    private ResultadoBusqueda buscarPorNumeroCita(
            String valor
    ) {

        Integer idCita;


        try {

            idCita =
                    Integer.valueOf(
                            valor
                    );

        } catch (NumberFormatException ex) {

            return ResultadoBusqueda.sinResultados(
                    MENSAJE_SIN_RESULTADOS
            );
        }


        if (idCita <= 0) {

            return ResultadoBusqueda.sinResultados(
                    MENSAJE_SIN_RESULTADOS
            );
        }


        Cita cita =
                citaRepository
                        .buscarParaRecepcionPorNumero(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoBusqueda.sinResultados(
                    MENSAJE_SIN_RESULTADOS
            );
        }


        return resultadoCita(
                cita
        );
    }


    // =====================================================
    // BUSCAR POR DPI
    // =====================================================

    private ResultadoBusqueda buscarPorDpi(
            String dpi
    ) {

        // =================================================
        // SOLO NÚMEROS
        // =================================================

        if (!dpi.matches("\\d+")) {

            return ResultadoBusqueda.error(
                    "El DPI debe contener únicamente números. "
                            + "No se permiten letras ni caracteres especiales."
            );
        }


        // =================================================
        // EXACTAMENTE 13 DÍGITOS
        // =================================================

        if (dpi.length() != 13) {

            return ResultadoBusqueda.error(
                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpi.length()
                            + " dígitos."
            );
        }


        // =================================================
        // HASH HMAC
        // =================================================

        String dpiHash =
                hashService
                        .generarHash(
                                dpi
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
        // FA03 - PACIENTE NO REGISTRADO
        // =================================================

        if (paciente == null) {

            return ResultadoBusqueda.pacienteNoExiste(
                    MENSAJE_PACIENTE_NO_EXISTE,
                    SUBTEXTO_PACIENTE_NO_EXISTE
            );
        }


        // =================================================
        // CITAS ACTIVAS
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        List<Cita> citas =
                citaRepository
                        .buscarCitasActivasParaRecepcion(
                                paciente.getId(),
                                ahora
                        );


        // =================================================
        // FA04 - PACIENTE SIN CITAS
        // =================================================

        if (citas.isEmpty()) {

            return ResultadoBusqueda.pacienteSinCitas(

                    "El paciente "
                            + paciente.getNombreCompleto()
                            + " está registrado pero no tiene citas activas.",

                    "Puede crear una nueva cita para este paciente.",

                    paciente.getId(),

                    paciente.getNombreCompleto()
            );
        }


        /*
         * El repositorio devuelve las citas en orden de
         * prioridad para recepción:
         *
         * 1. Paciente Presente.
         * 2. Próxima cita futura.
         * 3. Cita pasada activa más reciente.
         *
         * Por lo tanto, la posición 0 representa la cita
         * más relevante para el proceso de recepción.
         */
        return resultadoCita(
                citas.get(0)
        );
    }


    // =====================================================
    // INTERPRETAR ESTADO
    // =====================================================

    private ResultadoBusqueda resultadoCita(
            Cita cita
    ) {

        DetalleCita detalle =
                construirDetalle(
                        cita
                );


        String estado =
                normalizar(
                        cita.getEstadoCita()
                                .getNombre()
                );


        // =================================================
        // FA05 - PENDIENTE DE PAGO
        // =================================================

        if (ESTADO_PENDIENTE_PAGO
                .equalsIgnoreCase(estado)) {

            return ResultadoBusqueda.encontrada(
                    detalle,
                    MENSAJE_PENDIENTE_PAGO,
                    false,
                    false,
                    false
            );
        }


        // =================================================
        // FA06 - CANCELADA
        // =================================================

        if (ESTADO_CANCELADA
                .equalsIgnoreCase(estado)) {

            return ResultadoBusqueda.encontrada(
                    detalle,
                    MENSAJE_CANCELADA,
                    false,
                    true,
                    false
            );
        }


        // =================================================
        // PACIENTE PRESENTE
        // =================================================

        if (ESTADO_PRESENTE
                .equalsIgnoreCase(estado)) {

            return ResultadoBusqueda.encontrada(
                    detalle,
                    INDICADOR_ESPERA,
                    false,
                    false,
                    esEmergencia(cita)
            );
        }


        // =================================================
        // PAGADA / CONFIRMADA
        // =================================================

        if (ESTADO_PAGADA
                .equalsIgnoreCase(estado)

                || ESTADO_CONFIRMADA
                .equalsIgnoreCase(estado)) {

            return ResultadoBusqueda.encontrada(
                    detalle,
                    null,
                    true,
                    false,
                    esEmergencia(cita)
            );
        }


        return ResultadoBusqueda.encontrada(
                detalle,
                "La cita no se encuentra disponible para registrar llegada.",
                false,
                false,
                false
        );
    }


    // =====================================================
    // CONSULTAR DIRECTAMENTE POR NÚMERO
    // =====================================================

    @Transactional(readOnly = true)
    public ResultadoBusqueda consultarPorNumero(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            return ResultadoBusqueda.sinResultados(
                    MENSAJE_SIN_RESULTADOS
            );
        }


        Cita cita =
                citaRepository
                        .buscarParaRecepcionPorNumero(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoBusqueda.sinResultados(
                    MENSAJE_SIN_RESULTADOS
            );
        }


        return resultadoCita(
                cita
        );
    }


    // =====================================================
    // CU-05 - REGISTRAR LLEGADA
    // =====================================================

    @Transactional
    public ResultadoLlegada registrarLlegada(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            return ResultadoLlegada.error(
                    MENSAJE_OPERACION_NO_PERMITIDA
            );
        }


        Usuario recepcionista =
                usuarioActualService
                        .obtenerUsuarioActual();


        /*
         * Bloqueo pesimista para evitar que dos
         * recepcionistas registren la misma llegada.
         */
        Cita cita =
                citaRepository
                        .buscarParaRegistrarLlegadaConBloqueo(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null
                || cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null) {

            return ResultadoLlegada.error(
                    MENSAJE_ERROR_LLEGADA
            );
        }


        String estadoActual =
                normalizar(
                        cita.getEstadoCita()
                                .getNombre()
                );


        // =================================================
        // YA PRESENTE
        // =================================================

        if (ESTADO_PRESENTE
                .equalsIgnoreCase(estadoActual)) {

            return ResultadoLlegada.error(
                    MENSAJE_OPERACION_NO_PERMITIDA
            );
        }


        // =================================================
        // PENDIENTE DE PAGO
        // =================================================

        if (ESTADO_PENDIENTE_PAGO
                .equalsIgnoreCase(estadoActual)) {

            return ResultadoLlegada.error(
                    MENSAJE_PENDIENTE_PAGO
            );
        }


        // =================================================
        // CANCELADA
        // =================================================

        if (ESTADO_CANCELADA
                .equalsIgnoreCase(estadoActual)) {

            return ResultadoLlegada.error(
                    MENSAJE_CANCELADA
            );
        }


        boolean estadoPermitido =
                ESTADO_PAGADA
                        .equalsIgnoreCase(estadoActual)

                        || ESTADO_CONFIRMADA
                        .equalsIgnoreCase(estadoActual);


        if (!estadoPermitido) {

            return ResultadoLlegada.error(
                    MENSAJE_OPERACION_NO_PERMITIDA
            );
        }


        EstadoCita estadoPacientePresente =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_PRESENTE
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita "
                                                        + "'Paciente Presente'."
                                        )
                        );


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        cita.setEstadoCita(
                estadoPacientePresente
        );

        cita.setHoraLlegada(
                ahora
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
        // FA08 - EMERGENCIA
        // =================================================

        if (esEmergencia(cita)) {

            return ResultadoLlegada.exito(

                    "Paciente "
                            + cita.getPaciente()
                            .getNombreCompleto()
                            + " registrado con prioridad de EMERGENCIA. "
                            + "El paciente debe pasar directamente "
                            + "a toma de signos vitales.",

                    INDICADOR_ESPERA,

                    true,

                    ahora
            );
        }


        // =================================================
        // FLUJO NORMAL
        // =================================================

        return ResultadoLlegada.exito(

                "La llegada del paciente "
                        + cita.getPaciente()
                        .getNombreCompleto()
                        + " ha sido registrada exitosamente. "
                        + "El paciente debe pasar a la sala de espera.",

                INDICADOR_ESPERA,

                false,

                ahora
        );
    }


    // =====================================================
    // PACIENTE PARA WALK-IN
    // =====================================================

    @Transactional(readOnly = true)
    public Usuario obtenerPacienteParaAgendamiento(
            Integer idPaciente
    ) {

        if (idPaciente == null
                || idPaciente <= 0) {

            throw new IllegalArgumentException(
                    "No se indicó el paciente."
            );
        }


        Usuario paciente =
                usuarioRepository
                        .findById(
                                idPaciente
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El paciente seleccionado ya no existe."
                                        )
                        );


        if (paciente.getRol() == null
                || paciente.getRol().getNombre() == null
                || !"Paciente".equalsIgnoreCase(
                paciente.getRol().getNombre()
        )) {

            throw new IllegalArgumentException(
                    "El usuario seleccionado no corresponde a un paciente."
            );
        }


        return paciente;
    }


    // =====================================================
    // CONSTRUIR DETALLE
    // =====================================================

    private DetalleCita construirDetalle(
            Cita cita
    ) {

        return new DetalleCita(

                cita.getId(),

                cita.getPaciente()
                        .getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getEstadoCita()
                        .getNombre(),

                cita.getPrioridad(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getSucursal()
                        .getNombre(),

                cita.getMedico()
                        .getNombreCompleto(),


                // =============================================
                // HORA LOCAL DE GUATEMALA
                // =============================================

                convertirAZonaHoraria(
                        cita.getFechaHoraCita()
                ),


                cita.getMotivoConsulta(),


                // =============================================
                // HORA DE LLEGADA EN GUATEMALA
                // =============================================

                convertirAZonaHoraria(
                        cita.getHoraLlegada()
                ),


                esEmergencia(
                        cita
                )
        );
    }


    private boolean esEmergencia(
            Cita cita
    ) {

        return cita.getPrioridad() != null

                && PRIORIDAD_EMERGENCIA
                .equalsIgnoreCase(
                        cita.getPrioridad()
                                .trim()
                );
    }


    // =====================================================
// CONVERTIR A ZONA HORARIA DE LA CLÍNICA
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

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // RESULTADOS
    // =====================================================

    public enum TipoResultado {

        ENCONTRADA,

        SIN_RESULTADOS,

        PACIENTE_NO_EXISTE,

        PACIENTE_SIN_CITAS,

        ERROR_VALIDACION
    }


    public record ResultadoBusqueda(

            TipoResultado tipo,

            String mensaje,

            String subtexto,

            DetalleCita cita,

            Integer idPaciente,

            String nombrePaciente,

            boolean puedeRegistrarLlegada,

            boolean mostrarNuevaCita,

            boolean mostrarAccionUrgente

    ) {


        public static ResultadoBusqueda error(
                String mensaje
        ) {

            return new ResultadoBusqueda(
                    TipoResultado.ERROR_VALIDACION,
                    mensaje,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    false
            );
        }


        public static ResultadoBusqueda sinResultados(
                String mensaje
        ) {

            return new ResultadoBusqueda(
                    TipoResultado.SIN_RESULTADOS,
                    mensaje,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    false
            );
        }


        public static ResultadoBusqueda pacienteNoExiste(
                String mensaje,
                String subtexto
        ) {

            return new ResultadoBusqueda(
                    TipoResultado.PACIENTE_NO_EXISTE,
                    mensaje,
                    subtexto,
                    null,
                    null,
                    null,
                    false,
                    false,
                    false
            );
        }


        public static ResultadoBusqueda pacienteSinCitas(
                String mensaje,
                String subtexto,
                Integer idPaciente,
                String nombrePaciente
        ) {

            return new ResultadoBusqueda(
                    TipoResultado.PACIENTE_SIN_CITAS,
                    mensaje,
                    subtexto,
                    null,
                    idPaciente,
                    nombrePaciente,
                    false,
                    true,
                    false
            );
        }


        public static ResultadoBusqueda encontrada(
                DetalleCita cita,
                String mensaje,
                boolean puedeRegistrarLlegada,
                boolean mostrarNuevaCita,
                boolean mostrarAccionUrgente
        ) {

            return new ResultadoBusqueda(
                    TipoResultado.ENCONTRADA,
                    mensaje,
                    null,
                    cita,
                    cita != null
                            ? cita.idPaciente()
                            : null,
                    cita != null
                            ? cita.nombrePaciente()
                            : null,
                    puedeRegistrarLlegada,
                    mostrarNuevaCita,
                    mostrarAccionUrgente
            );
        }
    }


    public record DetalleCita(

            Integer numeroCita,

            Integer idPaciente,

            String nombrePaciente,

            String estado,

            String prioridad,

            String especialidad,

            String sucursal,

            String medico,

            OffsetDateTime fechaHoraCita,

            String motivoConsulta,

            OffsetDateTime horaLlegada,

            boolean emergencia

    ) {
    }


    public record ResultadoLlegada(

            boolean exitoso,

            String mensaje,

            String indicador,

            boolean emergencia,

            OffsetDateTime horaLlegada

    ) {


        public static ResultadoLlegada exito(
                String mensaje,
                String indicador,
                boolean emergencia,
                OffsetDateTime horaLlegada
        ) {

            return new ResultadoLlegada(
                    true,
                    mensaje,
                    indicador,
                    emergencia,
                    horaLlegada
            );
        }


        public static ResultadoLlegada error(
                String mensaje
        ) {

            return new ResultadoLlegada(
                    false,
                    mensaje,
                    null,
                    false,
                    null
            );
        }
    }
}