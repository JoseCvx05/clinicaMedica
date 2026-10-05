package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoRecordatorioSeguimiento;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * SERVICIO: RECORDATORIO DE CITA DE SEGUIMIENTO
 * =========================================================
 *
 * RN-CU11-05.
 *
 * Responsabilidades:
 *
 * - Recuperar nuevamente la cita desde PostgreSQL.
 * - Verificar que sea una cita de seguimiento.
 * - No enviar si la cita está Cancelada.
 * - Evitar recordatorios duplicados.
 * - Registrar el intento en notificacion_correo.
 * - Enviar correo HTML.
 * - Registrar Enviado/Fallido.
 *
 * =========================================================
 */
@Service
public class NotificacionRecordatorioSeguimientoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    public static final String TIPO_NOTIFICACION =
            "RECORDATORIO_CITA_SEGUIMIENTO";


    public static final String TABLA_REFERENCIA =
            "cita";


    private static final String ESTADO_CANCELADA =
            "Cancelada";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender
            mailSender;


    private final CitaRepository
            citaRepository;


    private final NotificacionCorreoRepository
            notificacionCorreoRepository;


    private final PlantillaCorreoRecordatorioSeguimiento
            plantillaCorreo;


    private final String
            correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionRecordatorioSeguimientoService(

            JavaMailSender mailSender,

            CitaRepository citaRepository,

            NotificacionCorreoRepository
                    notificacionCorreoRepository,

            PlantillaCorreoRecordatorioSeguimiento
                    plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente

    ) {

        this.mailSender =
                mailSender;


        this.citaRepository =
                citaRepository;


        this.notificacionCorreoRepository =
                notificacionCorreoRepository;


        this.plantillaCorreo =
                plantillaCorreo;


        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // ENVIAR RECORDATORIO
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarRecordatorio(
            Integer idCita
    ) {

        // =================================================
        // VALIDAR ID
        // =================================================

        if (idCita == null
                || idCita <= 0) {

            return;
        }


        // =================================================
        // EVITAR DUPLICADOS
        // =================================================
        //
        // El scheduler podrá ejecutarse varias veces.
        //
        // Una cita solamente debe recibir un recordatorio.
        // =================================================

        if (notificacionCorreoRepository
                .existsByTipoNotificacionAndTablaReferenciaAndIdReferencia(

                        TIPO_NOTIFICACION,

                        TABLA_REFERENCIA,

                        idCita
                )) {

            return;
        }


        // =================================================
        // RECUPERAR CITA NUEVAMENTE
        // =================================================
        //
        // No confiamos solamente en la cita encontrada por
        // el scheduler porque pudo cambiar de estado antes
        // de llegar a este punto.
        // =================================================

        Cita cita =
                citaRepository
                        .findById(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return;
        }


        // =================================================
        // DEBE SER SEGUIMIENTO
        // =================================================

        if (!Boolean.TRUE.equals(
                cita.getEsSeguimiento()
        )) {

            return;
        }


        // =================================================
        // RN-CU11-05
        // NO ENVIAR SI ESTÁ CANCELADA
        // =================================================

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita()
                .getNombre() == null) {

            return;
        }


        if (ESTADO_CANCELADA
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            return;
        }


        // =================================================
        // PACIENTE
        // =================================================

        Usuario paciente =
                cita.getPaciente();


        if (paciente == null) {

            return;
        }


        String correoPaciente =
                paciente.getCorreoElectronico();


        if (correoPaciente == null
                || correoPaciente.isBlank()) {

            return;
        }


        // =================================================
        // DATOS OBLIGATORIOS DEL RECORDATORIO
        // =================================================

        if (cita.getMedico() == null
                || cita.getSucursal() == null
                || cita.getFechaHoraCita() == null
                || cita.getTipoSeguimiento() == null
                || cita.getTipoSeguimiento().isBlank()) {

            return;
        }


        // =================================================
        // CREAR REGISTRO DE NOTIFICACIÓN
        // =================================================

        NotificacionCorreo notificacion =
                new NotificacionCorreo();


        notificacion.setTipoNotificacion(
                TIPO_NOTIFICACION
        );


        notificacion.setDestinatarioCorreo(
                correoPaciente.trim()
        );


        notificacion.setAsunto(
                plantillaCorreo
                        .construirAsunto()
        );


        notificacion.setTablaReferencia(
                TABLA_REFERENCIA
        );


        notificacion.setIdReferencia(
                idCita
        );


        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_PENDIENTE
        );


        notificacion.setIntentosEnvio(
                (short) 0
        );


        notificacionCorreoRepository
                .saveAndFlush(
                        notificacion
                );


        // =================================================
        // INTENTO DE ENVÍO
        // =================================================

        try {

            MimeMessage mensaje =
                    construirMensaje(
                            notificacion,
                            cita
                    );


            mailSender.send(
                    mensaje
            );


            marcarComoEnviado(
                    notificacion
            );


        } catch (
                MessagingException
                | MailException ex
        ) {

            marcarComoFallido(
                    notificacion,
                    ex
            );
        }


        // =================================================
        // GUARDAR RESULTADO
        // =================================================

        notificacionCorreoRepository
                .save(
                        notificacion
                );
    }


    // =====================================================
    // CONSTRUIR CORREO HTML
    // =====================================================

    private MimeMessage construirMensaje(

            NotificacionCorreo notificacion,

            Cita cita

    ) throws MessagingException {

        MimeMessage mensaje =
                mailSender
                        .createMimeMessage();


        MimeMessageHelper helper =
                new MimeMessageHelper(
                        mensaje,
                        false,
                        "UTF-8"
                );


        // =================================================
        // REMITENTE
        // =================================================

        helper.setFrom(
                correoRemitente
        );


        // =================================================
        // DESTINATARIO
        // =================================================

        helper.setTo(
                notificacion
                        .getDestinatarioCorreo()
        );


        // =================================================
        // ASUNTO EXACTO RN-CU11-05
        // =================================================

        helper.setSubject(
                notificacion
                        .getAsunto()
        );


        // =================================================
        // HTML
        // =================================================

        String cuerpoHtml =
                plantillaCorreo
                        .construirCuerpo(

                                cita.getPaciente()
                                        .getNombreCompleto(),

                                cita.getMedico()
                                        .getNombreCompleto(),

                                cita.getSucursal()
                                        .getNombre(),

                                cita.getFechaHoraCita(),

                                cita.getTipoSeguimiento()
                        );


        helper.setText(
                cuerpoHtml,
                true
        );


        return mensaje;
    }


    // =====================================================
    // MARCAR ENVIADO
    // =====================================================

    private void marcarComoEnviado(
            NotificacionCorreo notificacion
    ) {

        incrementarIntentos(
                notificacion
        );


        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_ENVIADO
        );


        notificacion.setFechaHoraEnvio(
                OffsetDateTime.now()
        );


        notificacion.setUltimoError(
                null
        );
    }


    // =====================================================
    // MARCAR FALLIDO
    // =====================================================

    private void marcarComoFallido(

            NotificacionCorreo notificacion,

            Exception excepcion

    ) {

        incrementarIntentos(
                notificacion
        );


        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_FALLIDO
        );


        notificacion.setFechaHoraEnvio(
                null
        );


        notificacion.setUltimoError(
                limitarError(
                        excepcion.getMessage()
                )
        );
    }


    // =====================================================
    // INCREMENTAR INTENTOS
    // =====================================================

    private void incrementarIntentos(
            NotificacionCorreo notificacion
    ) {

        short actuales =
                notificacion.getIntentosEnvio() == null
                        ? 0
                        : notificacion.getIntentosEnvio();


        notificacion.setIntentosEnvio(
                (short) (
                        actuales + 1
                )
        );
    }


    // =====================================================
    // LIMITAR MENSAJE DE ERROR
    // =====================================================

    private String limitarError(
            String mensaje
    ) {

        if (mensaje == null
                || mensaje.isBlank()) {

            return "Error desconocido durante el envío del recordatorio.";
        }


        String limpio =
                mensaje.trim();


        return limpio.length() <= 500
                ? limpio
                : limpio.substring(
                0,
                500
        );
    }
}