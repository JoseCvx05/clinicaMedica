package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.HorarioDisponibleDTO;
import com.proyecto.clinicamedica.service.CatalogoCitaService;
import com.proyecto.clinicamedica.service.DisponibilidadCitaService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


/**
 * =========================================================
 * CONTROLADOR: DISPONIBILIDAD DE CITAS
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Endpoint utilizado por el calendario del Paso 4.
 *
 * La disponibilidad NO se almacena en caché.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/paciente/citas")
public class CitaDisponibilidadController {


    private final DisponibilidadCitaService
            disponibilidadCitaService;

    private final CatalogoCitaService
            catalogoCitaService;


    public CitaDisponibilidadController(
            DisponibilidadCitaService disponibilidadCitaService,
            CatalogoCitaService catalogoCitaService
    ) {

        this.disponibilidadCitaService =
                disponibilidadCitaService;

        this.catalogoCitaService =
                catalogoCitaService;
    }


    // =====================================================
    // HORARIOS DISPONIBLES DE UN DÍA
    // =====================================================

    @GetMapping("/disponibilidad")
    @ResponseBody
    public ResponseEntity<List<HorarioDisponibleDTO>>
    obtenerDisponibilidad(

            @SessionAttribute("citaWizard")
            CitaWizardDTO wizard,

            @RequestParam("fecha")
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fecha
    ) {

        // =================================================
        // SESIÓN DEL WIZARD INCOMPLETA
        // =================================================

        if (wizard == null
                || wizard.getIdSucursal() == null
                || wizard.getIdEspecialidad() == null
                || wizard.getIdMedico() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            List.of()
                    );
        }


        // =================================================
        // MÉDICO DEBE SEGUIR SIENDO VÁLIDO
        // =================================================

        boolean medicoValido =
                catalogoCitaService
                        .medicoDisponibleParaSeleccion(
                                wizard.getIdMedico(),
                                wizard.getIdSucursal(),
                                wizard.getIdEspecialidad()
                        );


        if (!medicoValido) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            List.of()
                    );
        }


        // =================================================
        // CONSULTA EN TIEMPO REAL
        // =================================================

        List<HorarioDisponibleDTO> horarios =
                disponibilidadCitaService
                        .obtenerHorarios(
                                wizard.getIdMedico(),
                                fecha
                        );


        return ResponseEntity.ok(
                horarios
        );
    }
}