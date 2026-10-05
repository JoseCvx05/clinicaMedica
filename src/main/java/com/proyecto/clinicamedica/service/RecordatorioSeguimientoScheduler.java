package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.repository.CitaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;


/**
 * =========================================================
 * SCHEDULER: RECORDATORIOS DE CITAS DE SEGUIMIENTO
 * =========================================================
 *
 * CU-08 FA02
 * RN-CU11-05
 *
 * Responsabilidades:
 *
 * - Ejecutarse automáticamente.
 * - Buscar citas de seguimiento programadas para mañana.
 * - Trabajar con hora de Guatemala.
 * - Delegar el envío al servicio de notificación.
 *
 * El scheduler NO construye correos.
 * El scheduler NO modifica las citas.
 *
 * =========================================================
 */
@Component
public class RecordatorioSeguimientoScheduler {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository
            citaRepository;


    private final NotificacionRecordatorioSeguimientoService
            notificacionRecordatorioSeguimientoService;


    private final ZoneId
            zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RecordatorioSeguimientoScheduler(

            CitaRepository citaRepository,

            NotificacionRecordatorioSeguimientoService
                    notificacionRecordatorioSeguimientoService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria

    ) {

        this.citaRepository =
                citaRepository;


        this.notificacionRecordatorioSeguimientoService =
                notificacionRecordatorioSeguimientoService;


        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // PROCESAR RECORDATORIOS
    // =====================================================
    //
    // Se ejecuta cada hora.
    //
    // ¿Por qué cada hora?
    //
    // Si el servidor estuvo apagado a una hora específica,
    // todavía podrá enviar el recordatorio al iniciar
    // posteriormente durante el día anterior a la cita.
    //
    // La protección contra duplicados evita enviar
    // múltiples recordatorios a la misma cita.
    // =====================================================

    @Scheduled(
            cron = "0 0 * * * *",
            zone = "America/Guatemala"
    )
    public void procesarRecordatorios() {

        // =================================================
        // FECHA ACTUAL EN GUATEMALA
        // =================================================

        ZonedDateTime ahoraGuatemala =
                ZonedDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // INICIO DE MAÑANA
        // =================================================

        ZonedDateTime inicioManana =
                ahoraGuatemala
                        .toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(
                                zonaHoraria
                        );


        // =================================================
        // FIN DEL RANGO
        //
        // Exclusivo:
        //
        // [mañana 00:00, pasado mañana 00:00)
        // =================================================

        ZonedDateTime finManana =
                inicioManana
                        .plusDays(1);


        OffsetDateTime inicio =
                inicioManana
                        .toOffsetDateTime();


        OffsetDateTime fin =
                finManana
                        .toOffsetDateTime();


        // =================================================
        // BUSCAR SEGUIMIENTOS DE MAÑANA
        // =================================================

        List<Cita> citas =
                citaRepository
                        .buscarSeguimientosParaRecordatorio(
                                inicio,
                                fin
                        );


        // =================================================
        // PROCESAR
        // =================================================

        for (Cita cita : citas) {

            if (cita == null
                    || cita.getId() == null) {

                continue;
            }


            try {

                notificacionRecordatorioSeguimientoService
                        .enviarRecordatorio(
                                cita.getId()
                        );


            } catch (Exception ex) {

                /*
                 * Una cita con error no debe impedir que
                 * se procesen los demás recordatorios.
                 */
                System.err.println(
                        "No fue posible procesar el recordatorio "
                                + "de la cita #"
                                + cita.getId()
                                + ": "
                                + ex.getMessage()
                );
            }
        }
    }
}