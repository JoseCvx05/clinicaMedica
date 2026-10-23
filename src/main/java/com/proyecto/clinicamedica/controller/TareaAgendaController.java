
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.TareaAgendaFormularioDTO;

import com.proyecto.clinicamedica.service.TareaAgendaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * =========================================================
 * CU-16 - CONTROLLER: TAREAS DE AGENDA MÉDICA
 * =========================================================
 *
 * FA06 - TASKPANEL.
 *
 * FUNCIONALIDADES:
 *
 * - Consultar tareas del día seleccionado.
 * - Filtrar Pendientes, Completadas y Todas.
 * - Crear nuevas tareas.
 * - Completar y descompletar tareas.
 * - Eliminar tareas.
 *
 * SEGURIDAD:
 *
 * - Rutas dentro de /interno/medico/**
 * - El Service exige el rol Médico.
 * - No se recibe el ID del médico del navegador.
 * - El Service verifica la propiedad de cada tarea.
 * - Las operaciones POST estarán sujetas a CSRF.
 *
 * IMPORTANTE:
 *
 * - No contiene lógica de negocio.
 * - No accede directamente a Repository.
 * - No modifica citas ni eventos.
 * - No altera la disponibilidad del CU-03.
 *
 * =========================================================
 */
@RestController
@RequestMapping("/interno/medico/agenda/tareas")
public class TareaAgendaController {

    // =====================================================
    // ZONA HORARIA DE GUATEMALA
    // =====================================================

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final TareaAgendaService tareaAgendaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public TareaAgendaController(
            TareaAgendaService tareaAgendaService
    ) {

        this.tareaAgendaService = tareaAgendaService;
    }

    // =====================================================
    // FA06 - CONSULTAR TAREAS
    // =====================================================
    //
    // GET /interno/medico/agenda/tareas
    //
    // Parámetros:
    //
    // fecha=2026-10-21
    //
    // filtro=todas
    // filtro=pendientes
    // filtro=completadas
    //
    // Si no se envía fecha, utiliza el día actual
    // en horario de Guatemala.
    //
    // =====================================================

    @GetMapping
    public TareaAgendaService.PanelTareas consultarTareas(

            @RequestParam(
                    name = "fecha",
                    required = false
            )
            String fecha,

            @RequestParam(
                    name = "filtro",
                    defaultValue = "todas"
            )
            String filtro
    ) {

        // =============================================
        // 1. DETERMINAR FECHA
        // =============================================

        LocalDate fechaSeleccionada =
                convertirFecha(fecha);

        // =============================================
        // 2. DETERMINAR FILTRO
        // =============================================

        Boolean completadas =
                convertirFiltro(filtro);

        // =============================================
        // 3. CONSULTAR SERVICE
        // =============================================

        return tareaAgendaService.obtenerPanel(
                fechaSeleccionada,
                completadas
        );
    }

    // =====================================================
    // FA06 - CREAR NUEVA TAREA
    // =====================================================
    //
    // POST /interno/medico/agenda/tareas
    //
    // Recibe JSON.
    //
    // El Médico se obtiene desde el Service.
    // Las tareas nuevas comienzan pendientes.
    //
    // =====================================================

    @PostMapping
    public ResponseEntity<TareaAgendaService.ResultadoOperacion>
    crearTarea(

            @Valid
            @RequestBody
            TareaAgendaFormularioDTO formulario
    ) {

        TareaAgendaService.ResultadoOperacion resultado =
                tareaAgendaService.crearTarea(
                        formulario
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resultado);
    }

    // =====================================================
    // FA06 - COMPLETAR / DESCOMPLETAR
    // =====================================================
    //
    // POST /interno/medico/agenda/tareas/{id}/estado
    //
    // completar=true  -> Completada.
    // completar=false -> Pendiente.
    //
    // El Service verifica que la tarea pertenezca
    // al Médico autenticado.
    //
    // =====================================================

    @PostMapping("/{idTarea}/estado")
    public TareaAgendaService.ResultadoOperacion
    cambiarEstado(

            @PathVariable
            Integer idTarea,

            @RequestParam(
                    name = "completar",
                    required = false
            )
            String completar
    ) {

        // =============================================
        // VALIDAR PARÁMETRO
        // =============================================

        if (completar == null
                || (!completar.equalsIgnoreCase("true")
                && !completar.equalsIgnoreCase("false"))) {

            throw new IllegalArgumentException(
                    "Debe indicar un estado válido "
                            + "para la tarea."
            );
        }

        // =============================================
        // DELEGAR AL SERVICE
        // =============================================

        return tareaAgendaService.cambiarEstado(
                idTarea,
                Boolean.parseBoolean(completar)
        );
    }

    // =====================================================
    // FA06 - ELIMINAR TAREA
    // =====================================================
    //
    // POST /interno/medico/agenda/tareas/{id}/eliminar
    //
    // El Service comprueba la propiedad de la tarea.
    //
    // =====================================================

    @PostMapping("/{idTarea}/eliminar")
    public ResultadoEliminacion eliminarTarea(

            @PathVariable
            Integer idTarea
    ) {

        String mensaje =
                tareaAgendaService.eliminarTarea(
                        idTarea
                );

        return new ResultadoEliminacion(
                mensaje
        );
    }

    // =====================================================
    // RESULTADO DE ELIMINACIÓN
    // =====================================================

    public record ResultadoEliminacion(
            String mensaje
    ) {
    }

    // =====================================================
    // CONVERTIR FECHA
    // =====================================================
    //
    // Formato ISO: yyyy-MM-dd.
    //
    // Si no se indica fecha, utiliza el día
    // actual en horario de Guatemala.
    //
    // =====================================================

    private LocalDate convertirFecha(
            String fecha
    ) {

        if (fecha == null || fecha.isBlank()) {

            return LocalDate.now(
                    ZONA_GUATEMALA
            );
        }

        try {

            return LocalDate.parse(
                    fecha.trim()
            );

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "La fecha seleccionada no es válida. "
                            + "Utilice el formato yyyy-MM-dd."
            );
        }
    }

    // =====================================================
    // CONVERTIR FILTRO
    // =====================================================
    //
    // Todas       -> null.
    // Pendientes  -> false.
    // Completadas -> true.
    //
    // =====================================================

    private Boolean convertirFiltro(
            String filtro
    ) {

        if (filtro == null || filtro.isBlank()) {

            return null;
        }

        String normalizado =
                filtro.trim()
                        .toLowerCase(Locale.ROOT);

        return switch (normalizado) {

            case "todas" -> null;

            case "pendientes" -> false;

            case "completadas" -> true;

            default -> throw new IllegalArgumentException(
                    "El filtro de tareas seleccionado "
                            + "no es válido."
            );
        };
    }
}
