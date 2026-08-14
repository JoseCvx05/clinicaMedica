package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.EstadoCita;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;


/**
 * =========================================================
 * SERVICIO: EXPIRACIÓN DE PAGO DE CITAS
 * =========================================================
 *
 * Cancela citas cuyo tiempo disponible para completar
 * el pago ya finalizó.
 *
 * Las citas vencidas pasan al estado:
 *
 * "Cancelada"
 *
 * =========================================================
 */
@Service
public class ExpiracionPagoCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ESTADO_CANCELADA =
            "Cancelada";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final EstadoCitaRepository estadoCitaRepository;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ExpiracionPagoCitaService(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // CANCELAR CITAS EXPIRADAS
    // =====================================================

    @Transactional
    public int cancelarCitasExpiradas() {

        // =================================================
        // OBTENER ESTADO CANCELADA
        // =================================================

        EstadoCita cancelada =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_CANCELADA
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita 'Cancelada'."
                                        )
                        );


        // =================================================
        // FECHA/HORA ACTUAL
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // CANCELAR CITAS VENCIDAS
        // =================================================

        return citaRepository
                .cancelarPendientesDePagoExpiradas(
                        cancelada,
                        ahora
                );
    }
}