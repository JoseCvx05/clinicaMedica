
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.TareaAgendaFormularioDTO;

import com.proyecto.clinicamedica.entity.TareaAgenda;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.TareaAgendaRepository;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * =========================================================
 * CU-16 - SERVICE: GESTIÓN DE TAREAS DE AGENDA MÉDICA
 * =========================================================
 *
 * FA06 - TASKPANEL.
 *
 * FUNCIONALIDADES:
 *
 * - Consultar tareas por fecha.
 * - Filtrar tareas pendientes.
 * - Filtrar tareas completadas.
 * - Consultar todas las tareas.
 * - Contar tareas del día.
 * - Crear tareas.
 * - Completar tareas.
 * - Descompletar tareas.
 * - Eliminar tareas.
 *
 * REGLAS DE NEGOCIO:
 *
 * - Título obligatorio: 5 a 200 caracteres.
 * - Descripción opcional: máximo 1000 caracteres.
 * - Prioridad obligatoria:
 *      0 = Baja
 *      1 = Normal
 *      2 = Alta
 * - Fecha límite obligatoria.
 * - Estado inicial: Pendiente.
 *
 * SEGURIDAD:
 *
 * - Solo usuarios con rol Médico.
 * - Cada tarea pertenece al Médico autenticado.
 * - No se acepta el ID del Médico desde el navegador.
 * - No permite modificar tareas ajenas.
 *
 * CONCURRENCIA:
 *
 * - La entidad utiliza @Version.
 * - Las modificaciones son transaccionales.
 *
 * COMPATIBILIDAD:
 *
 * - No modifica citas.
 * - No modifica eventos.
 * - No altera disponibilidad del CU-03.
 * - No modifica funcionalidades del CU-08.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class TareaAgendaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final TareaAgendaRepository tareaRepository;

    private final AgendaMedicaSeguridadService seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public TareaAgendaService(
            TareaAgendaRepository tareaRepository,
            AgendaMedicaSeguridadService seguridadService
    ) {

        this.tareaRepository = tareaRepository;

        this.seguridadService = seguridadService;
    }

    // =====================================================
    // DTO DE RESPUESTA - TAREA
    // =====================================================

    public record TareaResumen(

            Integer id,

            String titulo,

            String descripcion,

            Integer prioridad,

            String nombrePrioridad,

            LocalDate fechaLimite,

            Boolean completada

    ) {
    }

    // =====================================================
    // DTO DE RESPUESTA - TASKPANEL
    // =====================================================

    public record PanelTareas(

            LocalDate fechaSeleccionada,

            long totalTareas,

            long totalPendientes,

            long totalCompletadas,

            List<TareaResumen> tareas

    ) {
    }

    // =====================================================
    // DTO DE RESPUESTA - OPERACIONES
    // =====================================================

    public record ResultadoOperacion(

            String mensaje,

            TareaResumen tarea

    ) {
    }

    // =====================================================
    // FA06 - CONSULTAR TASKPANEL
    // =====================================================
    //
    // Filtro:
    //
    // null  = Todas.
    // false = Pendientes.
    // true  = Completadas.
    //
    // El contador corresponde a todas las tareas
    // del día seleccionado.
    //
    // =====================================================

    public PanelTareas obtenerPanel(
            LocalDate fecha,
            Boolean completadas
    ) {

        // =============================================
        // 1. VALIDAR MÉDICO AUTENTICADO
        // =============================================

        Integer idMedico =
                seguridadService.obtenerIdMedicoActual();

        // =============================================
        // 2. VALIDAR FECHA
        // =============================================

        if (fecha == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una fecha válida."
            );
        }

        // =============================================
        // 3. CONSULTAR TODAS LAS TAREAS DEL DÍA
        // =============================================

        List<TareaAgenda> todas =
                tareaRepository.listarTareasDelDia(
                        idMedico,
                        fecha
                );

        // =============================================
        // 4. CONTADORES
        // =============================================

        long total = todas.size();

        long completadasTotal =
                todas.stream()
                        .filter(
                                tarea -> Boolean.TRUE.equals(
                                        tarea.getIsCompleted()
                                )
                        )
                        .count();

        long pendientesTotal =
                total - completadasTotal;

        // =============================================
        // 5. APLICAR FILTRO
        // =============================================

        List<TareaResumen> tareas =
                todas.stream()

                        .filter(
                                tarea -> completadas == null
                                        || completadas.equals(
                                        tarea.getIsCompleted()
                                )
                        )

                        .map(this::convertirResumen)

                        .toList();

        // =============================================
        // 6. DEVOLVER PANEL
        // =============================================

        return new PanelTareas(
                fecha,
                total,
                pendientesTotal,
                completadasTotal,
                tareas
        );
    }

    // =====================================================
    // FA06 - CREAR NUEVA TAREA
    // =====================================================

    @Transactional
    public ResultadoOperacion crearTarea(
            TareaAgendaFormularioDTO formulario
    ) {

        // =============================================
        // 1. VALIDAR MÉDICO AUTENTICADO
        // =============================================

        Usuario medico =
                seguridadService.obtenerMedicoActual();

        // =============================================
        // 2. VALIDAR FORMULARIO
        // =============================================

        if (formulario == null) {

            throw new IllegalArgumentException(
                    "Debe ingresar los datos de la tarea."
            );
        }

        String titulo =
                validarTitulo(
                        formulario.getTitulo()
                );

        String descripcion =
                validarDescripcion(
                        formulario.getDescripcion()
                );

        Integer prioridad =
                validarPrioridad(
                        formulario.getPrioridad()
                );

        LocalDate fechaLimite =
                validarFecha(
                        formulario.getFechaLimite()
                );

        // =============================================
        // 3. CREAR ENTIDAD
        // =============================================

        TareaAgenda tarea =
                new TareaAgenda();

        // Médico obtenido del servidor,
        // nunca del formulario.

        tarea.setMedico(medico);

        tarea.setTitulo(titulo);

        tarea.setDescripcion(descripcion);

        tarea.setPrioridad(prioridad);

        tarea.setFechaLimite(fechaLimite);

        // =============================================
        // 4. ESTADO INICIAL
        // =============================================

        tarea.setIsCompleted(false);

        // =============================================
        // 5. GUARDAR EN POSTGRESQL
        // =============================================

        TareaAgenda guardada =
                tareaRepository.saveAndFlush(
                        tarea
                );

        // =============================================
        // 6. DEVOLVER RESULTADO
        // =============================================

        return new ResultadoOperacion(
                "Tarea creada exitosamente.",
                convertirResumen(guardada)
        );
    }

    // =====================================================
    // FA06 - COMPLETAR / DESCOMPLETAR TAREA
    // =====================================================
    //
    // true  = Completada.
    // false = Pendiente.
    //
    // La operación utiliza un estado explícito.
    // No alterna automáticamente el estado.
    //
    // =====================================================

    @Transactional
    public ResultadoOperacion cambiarEstado(
            Integer idTarea,
            boolean completar
    ) {

        // =============================================
        // 1. VALIDAR MÉDICO AUTENTICADO
        // =============================================

        Integer idMedico =
                seguridadService.obtenerIdMedicoActual();

        // =============================================
        // 2. VALIDAR ID
        // =============================================

        validarIdTarea(idTarea);

        // =============================================
        // 3. BUSCAR ÚNICAMENTE TAREA PROPIA
        // =============================================

        TareaAgenda tarea =
                buscarTareaPropia(
                        idTarea,
                        idMedico
                );

        // =============================================
        // 4. EVITAR CAMBIOS REPETIDOS
        // =============================================

        if (Boolean.valueOf(completar).equals(
                tarea.getIsCompleted()
        )) {

            String mensaje = completar
                    ? "La tarea ya se encuentra completada."
                    : "La tarea ya se encuentra pendiente.";

            return new ResultadoOperacion(
                    mensaje,
                    convertirResumen(tarea)
            );
        }

        // =============================================
        // 5. CAMBIAR ESTADO
        // =============================================

        tarea.setIsCompleted(completar);

        // =============================================
        // 6. GUARDAR
        // =============================================

        TareaAgenda actualizada =
                tareaRepository.saveAndFlush(
                        tarea
                );

        // =============================================
        // 7. CONFIRMACIÓN
        // =============================================

        String mensaje = completar
                ? "Tarea completada correctamente."
                : "Tarea marcada como pendiente correctamente.";

        return new ResultadoOperacion(
                mensaje,
                convertirResumen(actualizada)
        );
    }

    // =====================================================
    // FA06 - ELIMINAR TAREA
    // =====================================================
    //
    // Solo el Médico propietario puede eliminarla.
    //
    // No afecta las citas ni los eventos.
    //
    // =====================================================

    @Transactional
    public String eliminarTarea(
            Integer idTarea
    ) {

        // =============================================
        // 1. VALIDAR MÉDICO AUTENTICADO
        // =============================================

        Integer idMedico =
                seguridadService.obtenerIdMedicoActual();

        // =============================================
        // 2. VALIDAR ID
        // =============================================

        validarIdTarea(idTarea);

        // =============================================
        // 3. VERIFICAR PROPIETARIO
        // =============================================

        TareaAgenda tarea =
                buscarTareaPropia(
                        idTarea,
                        idMedico
                );

        // =============================================
        // 4. ELIMINAR
        // =============================================

        tareaRepository.delete(tarea);

        tareaRepository.flush();

        // =============================================
        // 5. MENSAJE
        // =============================================

        return "Tarea eliminada correctamente.";
    }

    // =====================================================
    // VALIDAR IDENTIFICADOR DE TAREA
    // =====================================================

    private void validarIdTarea(
            Integer idTarea
    ) {

        if (idTarea == null || idTarea <= 0) {

            throw new IllegalArgumentException(
                    "La tarea seleccionada no es válida."
            );
        }
    }

    // =====================================================
    // BUSCAR TAREA PROPIA
    // =====================================================
    //
    // No permite consultar ni modificar tareas
    // pertenecientes a otro Médico.
    //
    // Se utiliza el mismo mensaje para una tarea
    // inexistente y para una tarea ajena, evitando
    // revelar información de otros usuarios.
    //
    // =====================================================

    private TareaAgenda buscarTareaPropia(
            Integer idTarea,
            Integer idMedico
    ) {

        return tareaRepository.buscarTareaPropia(
                        idTarea,
                        idMedico
                )
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "No tiene permisos para acceder "
                                        + "a esta tarea o la tarea no existe."
                        )
                );
    }

    // =====================================================
    // VALIDAR TÍTULO
    // =====================================================

    private String validarTitulo(
            String titulo
    ) {

        if (titulo == null || titulo.isBlank()) {

            throw new IllegalArgumentException(
                    "El título de la tarea es obligatorio."
            );
        }

        String normalizado =
                titulo.trim();

        if (normalizado.length() < 5
                || normalizado.length() > 200) {

            throw new IllegalArgumentException(
                    "El título debe contener entre 5 y 200 caracteres."
            );
        }

        return normalizado;
    }

    // =====================================================
    // VALIDAR DESCRIPCIÓN
    // =====================================================

    private String validarDescripcion(
            String descripcion
    ) {

        if (descripcion == null
                || descripcion.isBlank()) {

            return null;
        }

        String normalizada =
                descripcion.trim();

        if (normalizada.length() > 1000) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder "
                            + "los 1000 caracteres."
            );
        }

        return normalizada;
    }

    // =====================================================
    // VALIDAR PRIORIDAD
    // =====================================================

    private Integer validarPrioridad(
            Integer prioridad
    ) {

        if (prioridad == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una prioridad."
            );
        }

        if (prioridad < 0 || prioridad > 2) {

            throw new IllegalArgumentException(
                    "La prioridad seleccionada no es válida."
            );
        }

        return prioridad;
    }

    // =====================================================
    // VALIDAR FECHA LÍMITE
    // =====================================================

    private LocalDate validarFecha(
            LocalDate fecha
    ) {

        if (fecha == null) {

            throw new IllegalArgumentException(
                    "La fecha límite es obligatoria."
            );
        }

        return fecha;
    }

    // =====================================================
    // CONVERTIR ENTIDAD A DTO DE RESPUESTA
    // =====================================================
    //
    // Evita devolver directamente entidades JPA
    // al Controller y al navegador.
    //
    // =====================================================

    private TareaResumen convertirResumen(
            TareaAgenda tarea
    ) {

        return new TareaResumen(

                tarea.getId(),

                tarea.getTitulo(),

                tarea.getDescripcion(),

                tarea.getPrioridad(),

                obtenerNombrePrioridad(
                        tarea.getPrioridad()
                ),

                tarea.getFechaLimite(),

                tarea.getIsCompleted()
        );
    }

    // =====================================================
    // NOMBRE DE PRIORIDAD
    // =====================================================

    private String obtenerNombrePrioridad(
            Integer prioridad
    ) {

        if (prioridad == null) {
            return "Sin prioridad";
        }

        return switch (prioridad) {

            case 0 -> "Baja";

            case 1 -> "Normal";

            case 2 -> "Alta";

            default -> "Desconocida";
        };
    }
}
