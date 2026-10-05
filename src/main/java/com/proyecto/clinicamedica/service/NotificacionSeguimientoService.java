package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoCitaSeguimiento;

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
 * SERVICIO: NOTIFICACIÓN DE CITA DE SEGUIMIENTO
 * =========================================================
 *
 * CU-08 FA02 - Cita de seguimiento.
 * CU-11 - Notificación de seguimiento.
 *
 * Responsabilidades:
 *
 * - Recuperar la cita creada desde PostgreSQL.
 * - Validar que realmente sea de seguimiento.
 * - Evitar notificaciones duplicadas.
 * - Registrar la notificación.
 * - Enviar correo HTML al paciente.
 * - Registrar Enviado/Fallido.
 *
 * IMPORTANTE:
 *
 * Un fallo del correo NO debe eliminar ni revertir
 * la cita de seguimiento ya creada.
 *
 * =========================================================
 */
@Service
public class NotificacionSeguimientoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    public static final String TIPO_NOTIFICACION =
            "CITA_SEGUIMIENTO";


    public static final String TABLA_REFERENCIA =
            "cita";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender
            mailSender;


    private final NotificacionCorreoRepository
            notificacionCorreoRepository;


    private final CitaRepository
            citaRepository;


    private final PlantillaCorreoCitaSeguimiento
            plantillaCorreo;


    private final String
            correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionSeguimientoService(

            JavaMailSender mailSender,

            NotificacionCorreoRepository
                    notificacionCorreoRepository,

            CitaRepository citaRepository,

            PlantillaCorreoCitaSeguimiento
                    plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente

    ) {

        this.mailSender =
                mailSender;


        this.notificacionCorreoRepository =
                notificacionCorreoRepository;


        this.citaRepository =
                citaRepository;


        this.plantillaCorreo =
                plantillaCorreo;


        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // ENVIAR CONFIRMACIÓN DEL SEGUIMIENTO
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarConfirmacion(
            Integer idCita
    ) {

        // =================================================
        // ID
        // =================================================

        if (idCita == null
                || idCita <= 0) {

            return;
        }


        // =================================================
        // EVITAR DUPLICADOS
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
        // OBTENER CITA
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
        // DEBE SER CITA DE SEGUIMIENTO
        // =================================================

        if (!Boolean.TRUE.equals(
                cita.getEsSeguimiento()
        )) {

            return;
        }


        // =================================================
        // CONSULTA DE ORIGEN
        // =================================================

        if (cita.getIdConsultaOrigen() == null
                || cita.getIdConsultaOrigen() <= 0) {

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
        // DATOS NECESARIOS
        // =================================================

        if (cita.getMedico() == null
                || cita.getEspecialidad() == null
                || cita.getSucursal() == null
                || cita.getFechaHoraCita() == null
                || cita.getTipoSeguimiento() == null
                || cita.getMotivoConsulta() == null) {

            return;
        }


        // =================================================
        // CREAR NOTIFICACIÓN
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
        // PRIMER INTENTO DE ENVÍO
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
    // CONSTRUIR MENSAJE HTML
    // =====================================================

    private MimeMessage construirMensaje(

            NotificacionCorreo notificacion,

            Cita cita

    ) throws MessagingException {

        // =================================================
        // CREAR MIME MESSAGE
        // =================================================

        MimeMessage mensaje =
                mailSender
                        .createMimeMessage();


        // =================================================
        // HELPER UTF-8
        // =================================================

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
        // ASUNTO
        // RN-CU11-04
        // =================================================

        helper.setSubject(
                notificacion
                        .getAsunto()
        );


        // =================================================
        // CUERPO HTML
        // =================================================

        String cuerpoHtml =
                plantillaCorreo
                        .construirCuerpo(

                                cita.getPaciente()
                                        .getNombreCompleto(),

                                cita.getMedico()
                                        .getNombreCompleto(),

                                cita.getEspecialidad()
                                        .getNombre(),

                                cita.getSucursal()
                                        .getNombre(),

                                cita.getFechaHoraCita(),

                                cita.getTipoSeguimiento(),

                                cita.getMotivoConsulta(),

                                cita.getPrioridad()
                        );


        /*
         * Segundo parámetro = true:
         *
         * indica a JavaMail que el contenido enviado
         * debe interpretarse como HTML.
         */
        helper.setText(
                cuerpoHtml,
                true
        );


        return mensaje;
    }


    // =====================================================
    // MARCAR COMO ENVIADO
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
    // MARCAR COMO FALLIDO
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

        short intentosActuales =
                notificacion.getIntentosEnvio() == null
                        ? 0
                        : notificacion.getIntentosEnvio();


        notificacion.setIntentosEnvio(
                (short) (
                        intentosActuales + 1
                )
        );
    }


    // =====================================================
    // LIMITAR ERROR
    // =====================================================

    private String limitarError(
            String mensaje
    ) {

        if (mensaje == null
                || mensaje.isBlank()) {

            return "Error desconocido durante el envío del correo.";
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