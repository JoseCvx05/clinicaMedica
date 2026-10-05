package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.ConsultaMedicaHistorial;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.ConsultaMedicaHistorialRepository;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * SERVICIO: VERSIONAMIENTO DE CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * RNF-026:
 * El historial de consultas debe mantener versionamiento
 * para auditoría.
 *
 * Antes de modificar una ConsultaMedica:
 *
 * 1. Guarda una fotografía completa de la versión actual.
 * 2. Incrementa la versión vigente.
 *
 * La transacción principal será controlada por
 * ConsultaMedicaService.
 *
 * =========================================================
 */
@Service
public class ConsultaMedicaVersionService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final ConsultaMedicaHistorialRepository
            historialRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ConsultaMedicaVersionService(
            ConsultaMedicaHistorialRepository historialRepository
    ) {

        this.historialRepository =
                historialRepository;
    }


    // =====================================================
    // VERSIONAR ANTES DE MODIFICAR
    // =====================================================

    public void versionarAntesDeModificar(

            ConsultaMedica consulta,

            Usuario usuarioModificador,

            OffsetDateTime fechaModificacion
    ) {

        // =================================================
        // 1. VALIDAR CONSULTA
        // =================================================

        if (consulta == null
                || consulta.getId() == null) {

            throw new IllegalArgumentException(
                    "La consulta médica no es válida "
                            + "para versionamiento."
            );
        }


        // =================================================
        // 2. VALIDAR VERSIÓN ACTUAL
        // =================================================

        Integer versionActual =
                consulta.getVersion();


        if (versionActual == null
                || versionActual < 1) {

            throw new IllegalStateException(
                    "La consulta médica posee "
                            + "una versión inválida."
            );
        }


        // =================================================
        // 3. EVITAR DUPLICIDAD
        // =================================================

        if (historialRepository
                .existsByConsulta_IdAndVersion(
                        consulta.getId(),
                        versionActual
                )) {

            throw new IllegalStateException(
                    "La versión "
                            + versionActual
                            + " de la consulta médica "
                            + "ya existe en el historial."
            );
        }


        // =================================================
        // 4. CREAR SNAPSHOT HISTÓRICO
        // =================================================

        ConsultaMedicaHistorial historial =
                new ConsultaMedicaHistorial();


        historial.setConsulta(
                consulta
        );


        historial.setVersion(
                versionActual
        );


        // =================================================
        // DATOS CLÍNICOS
        // =================================================

        historial.setMotivoVisita(
                consulta.getMotivoVisita()
        );


        historial.setHallazgosClinicos(
                consulta.getHallazgosClinicos()
        );


        historial.setCie10(
                consulta.getCie10()
        );


        historial.setDiagnostico(
                consulta.getDiagnostico()
        );


        historial.setPlanTratamiento(
                consulta.getPlanTratamiento()
        );


        historial.setNotasAdicionales(
                consulta.getNotasAdicionales()
        );


        // =================================================
        // ESTADO DE LA CONSULTA
        // =================================================

        historial.setEstadoConsulta(
                consulta.getEstadoConsulta()
        );


        // =================================================
        // FIRMA DIGITAL
        // =================================================

        historial.setFirmaDigitalHash(
                consulta.getFirmaDigitalHash()
        );


        // =================================================
        // FECHA DE FINALIZACIÓN
        // =================================================

        historial.setFechaHoraFin(
                consulta.getFechaHoraFin()
        );


        // =================================================
        // AUDITORÍA DE LA VERSIÓN
        // =================================================

        historial.setModificadoPor(
                usuarioModificador
        );


        historial.setFechaModificacion(

                fechaModificacion != null
                        ? fechaModificacion
                        : OffsetDateTime.now()
        );


        // =================================================
        // 5. GUARDAR VERSIÓN ANTERIOR
        // =================================================

        historialRepository
                .save(
                        historial
                );


        // =================================================
        // 6. INCREMENTAR VERSIÓN ACTUAL
        // =================================================

        consulta.setVersion(
                versionActual + 1
        );
    }
}