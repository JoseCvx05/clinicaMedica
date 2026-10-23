
package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.EventoAgendaFormularioDTO;

import com.proyecto.clinicamedica.service.AgendaMedicaSeguridadService;
import com.proyecto.clinicamedica.service.AgendaMedicaCitaService;
import com.proyecto.clinicamedica.service.EventoAgendaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

/**
 * =========================================================
 * CU-16 - CONTROLLER: GESTIÓN DE AGENDA MÉDICA
 * =========================================================
 *
 * FLUJO NORMAL:
 *
 * - Mostrar calendario.
 * - Consultar citas médicas.
 * - Consultar eventos.
 * - Crear eventos.
 *
 * FLUJOS ALTERNOS:
 *
 * FA01 - Consultar detalle.
 * FA03 - Validación de formularios.
 * FA04 - Eliminar eventos.
 * FA05 - Navegación del calendario.
 * FA07 - Editar eventos.
 *
 * FA06 - TaskPanel:
 *
 * Se mantiene en TareaAgendaController.
 *
 * SEGURIDAD:
 *
 * - Acceso exclusivo para Médico.
 * - Médico obtenido de la autenticación.
 * - No se recibe ID del Médico desde el navegador.
 * - Propiedad verificada por el Service.
 * - Solicitudes POST protegidas con CSRF.
 *
 * COMPATIBILIDAD:
 *
 * - No modifica citas existentes.
 * - No altera los estados médicos.
 * - No modifica disponibilidad del CU-03.
 * - Conserva el TaskPanel del CU-16.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/interno/medico/agenda")
public class AgendaMedicaController {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final AgendaMedicaSeguridadService
            seguridadService;

    private final AgendaMedicaCitaService
            citaService;

    private final EventoAgendaService
            eventoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AgendaMedicaController(

            AgendaMedicaSeguridadService seguridadService,

            AgendaMedicaCitaService citaService,

            EventoAgendaService eventoService

    ) {

        this.seguridadService = seguridadService;

        this.citaService = citaService;

        this.eventoService = eventoService;
    }

    // =====================================================
    // CU-16 - MOSTRAR AGENDA MÉDICA
    // =====================================================
    //
    // GET /interno/medico/agenda
    //
    // Conserva el funcionamiento existente.
    //
    // =====================================================

    @GetMapping
    public String mostrarAgenda() {

        seguridadService.obtenerIdMedicoActual();

        return "medico/agenda";
    }

    // =====================================================
    // CU-16 - CONSULTAR CITAS MÉDICAS
    // =====================================================
    //
    // GET /interno/medico/agenda/citas
    //
    // Esta funcionalidad ya funciona y se conserva.
    //
    // =====================================================

    @GetMapping("/citas")
    @ResponseBody
    public List<AgendaMedicaCitaService.CitaCalendario>
    consultarCitas(

            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta

    ) {

        return citaService.listarCitas(
                desde,
                hasta
        );
    }

    // =====================================================
    // CU-16 - CONSULTAR EVENTOS MÉDICOS
    // =====================================================
    //
    // GET /interno/medico/agenda/eventos
    //
    // Ejemplo:
    //
    // ?desde=2026-10-01&hasta=2026-10-31
    //
    // Solo devuelve eventos del Médico autenticado.
    //
    // =====================================================

    @GetMapping("/eventos")
    @ResponseBody
    public List<EventoAgendaService.EventoCalendario>
    consultarEventos(

            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta

    ) {

        return eventoService.listarEventos(
                desde,
                hasta
        );
    }

    // =====================================================
    // FA01 - CONSULTAR DETALLE DE EVENTO
    // =====================================================
    //
    // GET /interno/medico/agenda/eventos/{idEvento}
    //
    // No permite consultar eventos de otro Médico.
    //
    // =====================================================

    @GetMapping("/eventos/{idEvento}")
    @ResponseBody
    public EventoAgendaService.EventoCalendario
    consultarDetalleEvento(

            @PathVariable("idEvento")
            Integer idEvento

    ) {

        return eventoService.obtenerEvento(
                idEvento
        );
    }

    // =====================================================
    // FLUJO NORMAL - CREAR EVENTO
    // =====================================================
    //
    // POST /interno/medico/agenda/eventos
    //
    // Recibe JSON con:
    //
    // - titulo
    // - descripcion
    // - fechaInicio
    // - fechaFin
    // - tipoEvento
    // - todoElDia
    //
    // No recibe ID del Médico ni color.
    //
    // =====================================================

    @PostMapping("/eventos")
    @ResponseBody
    public ResponseEntity<EventoAgendaService.ResultadoEvento>
    crearEvento(

            @Valid
            @RequestBody
            EventoAgendaFormularioDTO formulario

    ) {

        EventoAgendaService.ResultadoEvento resultado =
                eventoService.crearEvento(
                        formulario
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resultado);
    }

    // =====================================================
    // FA07 - ACTUALIZAR EVENTO
    // =====================================================
    //
    // POST /interno/medico/agenda/eventos/{id}/actualizar
    //
    // El Service valida:
    //
    // - Médico autenticado.
    // - Propiedad del evento.
    // - Campos obligatorios.
    // - Fechas consistentes.
    // - Conflictos de disponibilidad.
    //
    // =====================================================

    @PostMapping("/eventos/{idEvento}/actualizar")
    @ResponseBody
    public EventoAgendaService.ResultadoEvento
    actualizarEvento(

            @PathVariable("idEvento")
            Integer idEvento,

            @Valid
            @RequestBody
            EventoAgendaFormularioDTO formulario

    ) {

        return eventoService.actualizarEvento(
                idEvento,
                formulario
        );
    }

    // =====================================================
    // FA04 - ELIMINAR EVENTO
    // =====================================================
    //
    // POST /interno/medico/agenda/eventos/{id}/eliminar
    //
    // La confirmación del usuario se mostrará
    // posteriormente desde agenda.html.
    //
    // Solo permite eliminar eventos propios.
    //
    // =====================================================

    @PostMapping("/eventos/{idEvento}/eliminar")
    @ResponseBody
    public ResultadoEliminacionEvento eliminarEvento(

            @PathVariable("idEvento")
            Integer idEvento

    ) {

        String mensaje =
                eventoService.eliminarEvento(
                        idEvento
                );

        return new ResultadoEliminacionEvento(
                mensaje
        );
    }

    // =====================================================
    // RESPUESTA DE ELIMINACIÓN
    // =====================================================

    public record ResultadoEliminacionEvento(
            String mensaje
    ) {
    }
}
