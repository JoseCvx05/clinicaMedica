
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.EventoAgendaFormularioDTO;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.EventoAgenda;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.ReservaTemporalCitaRepository;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EventoAgendaRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * CU-16 - SERVICE: EVENTOS DE AGENDA MÉDICA
 * =========================================================
 *
 * FLUJO NORMAL:
 * - Crear eventos médicos.
 *
 * FLUJOS ALTERNOS:
 * - FA01: Consultar eventos.
 * - FA03: Validaciones.
 * - FA04: Eliminar eventos.
 * - FA07: Editar eventos.
 *
 * SEGURIDAD:
 * - Solo Médico autenticado.
 * - Solo eventos propios.
 * - No recibe ID del Médico desde el navegador.
 *
 * DISPONIBILIDAD:
 * - Evita solapamientos con citas ocupadas.
 * - Evita solapamientos con otros eventos.
 * - No modifica la lógica del CU-03.
 *
 * CONCURRENCIA:
 * - Bloqueo pesimista para editar y eliminar
 *   un mismo evento.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class EventoAgendaService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private static final String COLOR_EVENTO =
            "#8B5CF6";

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EventoAgendaRepository eventoRepository;

    private final CitaRepository citaRepository;

    private final ReservaTemporalCitaRepository
            reservaTemporalCitaRepository;

    private final AgendaMedicaSeguridadService seguridadService;

    private final AuditoriaService auditoriaService;

    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================



    public EventoAgendaService(
            EventoAgendaRepository eventoRepository,
            CitaRepository citaRepository,
            ReservaTemporalCitaRepository reservaTemporalCitaRepository,
            UsuarioRepository usuarioRepository,
            AgendaMedicaSeguridadService seguridadService,
            AuditoriaService auditoriaService
    ) {

        this.eventoRepository = eventoRepository;

        this.citaRepository = citaRepository;

        this.reservaTemporalCitaRepository =
                reservaTemporalCitaRepository;

        this.usuarioRepository = usuarioRepository;

        this.seguridadService = seguridadService;

        this.auditoriaService = auditoriaService;
    }



    // =====================================================
    // DTO DE RESPUESTA
    // =====================================================

    public record EventoCalendario(
            Integer id,
            String tipo,
            String titulo,
            String descripcion,
            String tipoEvento,
            Integer codigoTipoEvento,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            Boolean todoElDia,
            String color
    ) {
    }

    public record ResultadoEvento(
            String mensaje,
            EventoCalendario evento
    ) {
    }

    // =====================================================
    // DATOS VALIDADOS
    // =====================================================

    private record DatosEvento(
            String titulo,
            String descripcion,
            String tipoEvento,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            boolean todoElDia
    ) {
    }

    // =====================================================
    // CU-16 - CONSULTAR EVENTOS DEL MÉDICO
    // =====================================================

    public List<EventoCalendario> listarEventos(
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {

        Integer idMedico =
                seguridadService.obtenerIdMedicoActual();

        if (fechaDesde == null || fechaHasta == null) {

            throw new IllegalArgumentException(
                    "Debe indicar las fechas del calendario."
            );
        }

        if (fechaHasta.isBefore(fechaDesde)) {

            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior "
                            + "a la fecha inicial."
            );
        }

        if (ChronoUnit.DAYS.between(
                fechaDesde,
                fechaHasta
        ) > 62) {

            throw new IllegalArgumentException(
                    "El período solicitado no puede superar 63 días."
            );
        }

        OffsetDateTime desde =
                fechaDesde
                        .atStartOfDay(ZONA_GUATEMALA)
                        .toOffsetDateTime();

        OffsetDateTime hasta =
                fechaHasta
                        .plusDays(1)
                        .atStartOfDay(ZONA_GUATEMALA)
                        .toOffsetDateTime();

        return eventoRepository
                .buscarEventosParaAgendaMedica(
                        idMedico,
                        desde,
                        hasta
                )
                .stream()
                .map(this::convertirEvento)
                .toList();
    }

    // =====================================================
    // FA01 - CONSULTAR DETALLE DEL EVENTO
    // =====================================================

    public EventoCalendario obtenerEvento(Integer idEvento) {

        Usuario medico =
                seguridadService.obtenerMedicoActual();

        EventoAgenda evento =
                buscarEventoPropio(
                        idEvento,
                        medico.getId(),
                        false
                );

        return convertirEvento(evento);
    }

    // =====================================================
    // FLUJO NORMAL - CREAR EVENTO
    // =====================================================

    @Transactional
    public ResultadoEvento crearEvento(
            EventoAgendaFormularioDTO formulario
    ) {

        Usuario medico =
                seguridadService.obtenerMedicoActual();

        bloquearMedicoParaAgenda(medico.getId());


        DatosEvento datos =
                validarFormulario(formulario);

        // Evitar superposición con citas y eventos.

        validarConflictos(
                medico.getId(),
                datos.inicio(),
                datos.fin(),
                null
        );

        EventoAgenda evento =
                new EventoAgenda();

        evento.setMedico(medico);
        evento.setCreadoPor(medico);

        aplicarDatos(evento, datos);

        EventoAgenda guardado =
                eventoRepository.saveAndFlush(evento);

        registrarAuditoria(
                guardado,
                medico,
                AccionAuditoria.CREAR,
                null,
                crearSnapshot(guardado)
        );

        String mensaje =
                "Evento creado exitosamente. "
                        + datos.titulo()
                        + " — "
                        + fechaVisibleInicio(datos)
                        .format(FORMATO_FECHA)
                        + " a "
                        + fechaVisibleFin(datos)
                        .format(FORMATO_FECHA)
                        + ".";

        return new ResultadoEvento(
                mensaje,
                convertirEvento(guardado)
        );
    }

    // =====================================================
    // FA07 - ACTUALIZAR EVENTO
    // =====================================================

    @Transactional
    public ResultadoEvento actualizarEvento(
            Integer idEvento,
            EventoAgendaFormularioDTO formulario
    ) {

        Usuario medico =
                seguridadService.obtenerMedicoActual();

        EventoAgenda evento =
                buscarEventoPropio(
                        idEvento,
                        medico.getId(),
                        true
                );

        DatosEvento datos =
                validarFormulario(formulario);

        Map<String, Object> anterior =
                crearSnapshot(evento);

        // Se excluye el evento que estamos editando
        // para no detectar conflicto consigo mismo.

        validarConflictos(
                medico.getId(),
                datos.inicio(),
                datos.fin(),
                evento.getId()
        );

        aplicarDatos(evento, datos);

        evento.setModificadoPor(medico);

        evento.setFechaModificacion(
                OffsetDateTime.now(ZONA_GUATEMALA)
        );

        EventoAgenda actualizado =
                eventoRepository.saveAndFlush(evento);

        registrarAuditoria(
                actualizado,
                medico,
                AccionAuditoria.ACTUALIZAR,
                anterior,
                crearSnapshot(actualizado)
        );

        return new ResultadoEvento(
                "Evento actualizado exitosamente.",
                convertirEvento(actualizado)
        );
    }

    // =====================================================
    // FA04 - ELIMINAR EVENTO
    // =====================================================

    @Transactional
    public String eliminarEvento(Integer idEvento) {

        Usuario medico =
                seguridadService.obtenerMedicoActual();

        EventoAgenda evento =
                buscarEventoPropio(
                        idEvento,
                        medico.getId(),
                        true
                );

        Map<String, Object> anterior =
                crearSnapshot(evento);

        registrarAuditoria(
                evento,
                medico,
                AccionAuditoria.ELIMINAR,
                anterior,
                null
        );

        eventoRepository.delete(evento);
        eventoRepository.flush();

        return "Evento eliminado exitosamente.";
    }

    // =====================================================
    // VALIDAR FORMULARIO COMPLETO
    // =====================================================

    private DatosEvento validarFormulario(
            EventoAgendaFormularioDTO formulario
    ) {

        if (formulario == null) {

            throw new IllegalArgumentException(
                    "Debe ingresar los datos del evento."
            );
        }

        String titulo = formulario.getTitulo();

        if (titulo == null || titulo.isBlank()) {

            throw new IllegalArgumentException(
                    "El título del evento es obligatorio."
            );
        }

        titulo = titulo.trim();

        if (titulo.length() < 5 || titulo.length() > 200) {

            throw new IllegalArgumentException(
                    "El título debe contener entre 5 y 200 caracteres."
            );
        }

        String descripcion =
                formulario.getDescripcion();

        if (descripcion != null) {

            descripcion = descripcion.trim();

            if (descripcion.isEmpty()) {
                descripcion = null;
            }
        }

        if (descripcion != null
                && descripcion.length() > 500) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder los 500 caracteres."
            );
        }

        Integer codigo =
                formulario.getTipoEvento();

        if (codigo == null || codigo < 0 || codigo > 4) {

            throw new IllegalArgumentException(
                    "El tipo de evento seleccionado no es válido."
            );
        }

        if (formulario.getTodoElDia() == null) {

            throw new IllegalArgumentException(
                    "Debe indicar si el evento es de todo el día."
            );
        }

        boolean todoElDia =
                formulario.getTodoElDia();

        if (formulario.getFechaInicio() == null
                || formulario.getFechaInicio().isBlank()) {

            throw new IllegalArgumentException(
                    "La fecha de inicio es obligatoria."
            );
        }

        if (formulario.getFechaFin() == null
                || formulario.getFechaFin().isBlank()) {

            throw new IllegalArgumentException(
                    "La fecha de fin es obligatoria."
            );
        }

        OffsetDateTime inicio;
        OffsetDateTime fin;

        try {

            if (todoElDia) {

                // Eventos de todo el día:
                //
                // La fecha final seleccionada se considera
                // inclusiva para el usuario.
                //
                // En PostgreSQL guardamos el final exclusivo.

                LocalDate inicioDia =
                        LocalDate.parse(
                                formulario.getFechaInicio().trim()
                        );

                LocalDate finDia =
                        LocalDate.parse(
                                formulario.getFechaFin().trim()
                        );

                inicio =
                        inicioDia
                                .atStartOfDay(ZONA_GUATEMALA)
                                .toOffsetDateTime();

                fin =
                        finDia
                                .plusDays(1)
                                .atStartOfDay(ZONA_GUATEMALA)
                                .toOffsetDateTime();

            } else {

                LocalDateTime inicioLocal =
                        LocalDateTime.parse(
                                formulario.getFechaInicio().trim()
                        );

                LocalDateTime finLocal =
                        LocalDateTime.parse(
                                formulario.getFechaFin().trim()
                        );

                inicio =
                        inicioLocal
                                .atZone(ZONA_GUATEMALA)
                                .toOffsetDateTime();

                fin =
                        finLocal
                                .atZone(ZONA_GUATEMALA)
                                .toOffsetDateTime();
            }

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "Las fechas del evento no tienen un formato válido."
            );
        }

        if (!fin.isAfter(inicio)) {

            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior "
                            + "a la fecha de inicio."
            );
        }

        return new DatosEvento(
                titulo,
                descripcion,
                obtenerNombreTipo(codigo),
                inicio,
                fin,
                todoElDia
        );
    }


// =====================================================
// CU-16 / CU-03
// BLOQUEO PESIMISTA DEL MÉDICO
// =====================================================
//
// Coordina la creación y modificación de eventos
// con las reservas temporales del CU-03.
//
// =====================================================

    private void bloquearMedicoParaAgenda(
            Integer idMedico
    ) {

        Usuario medico =
                usuarioRepository
                        .findByIdForUpdate(idMedico)
                        .orElseThrow(
                                () -> new AccessDeniedException(
                                        "El médico autenticado no existe."
                                )
                        );

        if (!Boolean.TRUE.equals(medico.getActivo())) {

            throw new AccessDeniedException(
                    "El médico no se encuentra activo."
            );
        }
    }


    // =====================================================
    // VALIDAR CONFLICTOS CON CITAS Y EVENTOS
    // =====================================================

    private void validarConflictos(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            Integer idEventoExcluido
    ) {


// =====================================================
// CU-16 / CU-03
// VALIDAR RESERVAS TEMPORALES VIGENTES
// =====================================================
//
// Impide crear o modificar un evento cuando
// un paciente ya reservó temporalmente el horario.
//
// Solo considera reservas:
// - Activas.
// - No expiradas.
// - Del mismo médico.
// - Que se superponen con el evento.
//
// =====================================================

        boolean conflictoReserva =
                reservaTemporalCitaRepository
                        .existeReservaActivaSolapada(
                                idMedico,
                                inicio,
                                fin,
                                OffsetDateTime.now(ZONA_GUATEMALA)
                        );

        if (conflictoReserva) {

            throw new IllegalArgumentException(
                    "El horario seleccionado coincide "
                            + "con una reserva temporal "
                            + "de cita médica vigente."
            );
        }


        // =============================================
        // 1. CITAS MÉDICAS OCUPADAS
        // =============================================
        //
        // Reutiliza la consulta del CU-03.
        //
        // No modificamos sus reglas de disponibilidad.
        //
        // =============================================

        boolean conflictoCita =
                citaRepository.existeCitaSolapada(
                        idMedico,
                        inicio,
                        fin,
                        OffsetDateTime.now(ZONA_GUATEMALA)
                );

        if (conflictoCita) {

            throw new IllegalArgumentException(
                    "El horario del evento coincide "
                            + "con una cita médica existente."
            );
        }

        // =============================================
        // 2. EVENTOS DEL MISMO MÉDICO
        // =============================================

        boolean conflictoEvento =
                eventoRepository
                        .buscarEventosParaAgendaMedica(
                                idMedico,
                                inicio,
                                fin
                        )
                        .stream()
                        .anyMatch(evento ->
                                idEventoExcluido == null
                                        || !idEventoExcluido.equals(
                                        evento.getId()
                                )
                        );

        if (conflictoEvento) {

            throw new IllegalArgumentException(
                    "El horario seleccionado coincide "
                            + "con otro evento de la agenda."
            );
        }
    }

    // =====================================================
    // BUSCAR EVENTO PROPIO
    // =====================================================

    private EventoAgenda buscarEventoPropio(
            Integer idEvento,
            Integer idMedico,
            boolean bloquear
    ) {

        if (idEvento == null || idEvento <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un evento válido."
            );
        }

        EventoAgenda evento;

        if (bloquear) {

            // Protege actualizaciones y eliminaciones
            // simultáneas del mismo registro.

            evento = entityManager.find(
                    EventoAgenda.class,
                    idEvento,
                    LockModeType.PESSIMISTIC_WRITE
            );

        } else {

            evento =
                    eventoRepository
                            .findById(idEvento)
                            .orElse(null);
        }

        if (evento == null
                || evento.getMedico() == null
                || !idMedico.equals(
                evento.getMedico().getId()
        )) {

            throw new AccessDeniedException(
                    "No tiene permisos para acceder "
                            + "a este evento o el evento no existe."
            );
        }

        return evento;
    }

    // =====================================================
    // ASIGNAR DATOS AL EVENTO
    // =====================================================

    private void aplicarDatos(
            EventoAgenda evento,
            DatosEvento datos
    ) {

        evento.setTitulo(datos.titulo());

        evento.setDescripcion(datos.descripcion());

        evento.setFechaInicio(datos.inicio());

        evento.setFechaFin(datos.fin());

        evento.setTipoEvento(datos.tipoEvento());

        evento.setTodoElDia(datos.todoElDia());

        // Color fijo exigido por CU-16.

        evento.setColor(COLOR_EVENTO);
    }

    // =====================================================
    // CONVERTIR A DTO SEGURO
    // =====================================================

    private EventoCalendario convertirEvento(
            EventoAgenda evento
    ) {

        return new EventoCalendario(
                evento.getId(),
                "EVENTO",
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getTipoEvento(),
                obtenerCodigoTipo(evento.getTipoEvento()),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getTodoElDia(),
                COLOR_EVENTO
        );
    }

    // =====================================================
    // TIPOS DE EVENTO
    // =====================================================

    private String obtenerNombreTipo(Integer codigo) {

        return switch (codigo) {

            case 0 -> "Reunión";
            case 1 -> "Descanso";
            case 2 -> "Capacitación";
            case 3 -> "Personal";
            case 4 -> "Otro";

            default -> throw new IllegalArgumentException(
                    "Tipo de evento no válido."
            );
        };
    }

    private Integer obtenerCodigoTipo(String nombre) {

        if (nombre == null) {
            return 4;
        }

        return switch (
                nombre.trim().toLowerCase(java.util.Locale.ROOT)
                ) {

            case "0", "reunión", "reunion" -> 0;
            case "1", "descanso" -> 1;
            case "2", "capacitación", "capacitacion" -> 2;
            case "3", "personal" -> 3;

            default -> 4;
        };
    }

    // =====================================================
    // FECHAS VISIBLES AL USUARIO
    // =====================================================

    private LocalDate fechaVisibleInicio(
            DatosEvento datos
    ) {

        return datos.inicio()
                .atZoneSameInstant(ZONA_GUATEMALA)
                .toLocalDate();
    }

    private LocalDate fechaVisibleFin(
            DatosEvento datos
    ) {

        LocalDate fecha =
                datos.fin()
                        .atZoneSameInstant(ZONA_GUATEMALA)
                        .toLocalDate();

        if (datos.todoElDia()) {

            return fecha.minusDays(1);
        }

        return fecha;
    }

    // =====================================================
    // AUDITORÍA
    // =====================================================

    private void registrarAuditoria(
            EventoAgenda evento,
            Usuario medico,
            AccionAuditoria accion,
            Map<String, Object> anterior,
            Map<String, Object> nuevo
    ) {

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "evento_agenda",
                        String.valueOf(evento.getId()),
                        accion,
                        medico.getId(),
                        medico.getNombreUsuario(),
                        anterior,
                        nuevo,
                        null
                )
        );
    }

    // =====================================================
    // SNAPSHOT DE AUDITORÍA
    // =====================================================
    //
    // Evitamos registrar la descripción libre
    // para reducir exposición de datos sensibles.
    //
    // =====================================================

    private Map<String, Object> crearSnapshot(
            EventoAgenda evento
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "titulo",
                evento.getTitulo()
        );

        snapshot.put(
                "fechaInicio",
                String.valueOf(evento.getFechaInicio())
        );

        snapshot.put(
                "fechaFin",
                String.valueOf(evento.getFechaFin())
        );

        snapshot.put(
                "tipoEvento",
                evento.getTipoEvento()
        );

        snapshot.put(
                "todoElDia",
                evento.getTodoElDia()
        );

        snapshot.put(
                "color",
                evento.getColor()
        );

        return snapshot;
    }
}
