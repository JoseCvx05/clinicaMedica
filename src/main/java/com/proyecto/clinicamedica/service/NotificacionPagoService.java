package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoPago;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * SERVICIO: NOTIFICACIÓN DE COMPROBANTE DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Responsabilidades:
 *
 * - Crear la notificación del comprobante.
 * - Intentar el primer envío.
 * - Registrar Enviado/Fallido.
 *
 * Si falla:
 *
 * - El pago NO se revierte.
 * - La cita continúa Pagada.
 * - La notificación queda Fallida.
 * - El scheduler realizará los reintentos.
 *
 * =========================================================
 */
@Service
public class NotificacionPagoService {


    public static final String TIPO_NOTIFICACION =
            "COMPROBANTE_PAGO";


    public static final String TABLA_REFERENCIA =
            "pago";


    private final JavaMailSender mailSender;

    private final NotificacionCorreoRepository
            notificacionCorreoRepository;

    private final PagoRepository pagoRepository;

    private final PlantillaCorreoPago plantillaCorreo;

    private final String correoRemitente;


    public NotificacionPagoService(

            JavaMailSender mailSender,

            NotificacionCorreoRepository
                    notificacionCorreoRepository,

            PagoRepository pagoRepository,

            PlantillaCorreoPago plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente
    ) {

        this.mailSender =
                mailSender;

        this.notificacionCorreoRepository =
                notificacionCorreoRepository;

        this.pagoRepository =
                pagoRepository;

        this.plantillaCorreo =
                plantillaCorreo;

        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // ENVIAR COMPROBANTE
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarComprobante(
            Integer idPago
    ) {

        if (idPago == null) {

            return;
        }


        // =================================================
        // EVITAR DUPLICADOS
        // =================================================

        if (notificacionCorreoRepository
                .existsByTipoNotificacionAndTablaReferenciaAndIdReferencia(
                        TIPO_NOTIFICACION,
                        TABLA_REFERENCIA,
                        idPago
                )) {

            return;
        }


        // =================================================
        // OBTENER PAGO APROBADO
        // =================================================

        Pago pago =
                pagoRepository
                        .buscarParaNotificacion(
                                idPago
                        )
                        .orElse(
                                null
                        );


        if (pago == null) {

            return;
        }


        Cita cita =
                pago.getCita();


        Usuario paciente =
                cita.getPaciente();


        /*
         * IMPORTANTE:
         *
         * Si en Usuario tu getter se llama distinto,
         * por ejemplo getCorreoElectronico(), cambia
         * únicamente esta línea.
         */
        String correoPaciente =
                paciente.getCorreoElectronico();


        if (correoPaciente == null
                || correoPaciente.isBlank()) {

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
                correoPaciente
        );


        notificacion.setAsunto(
                plantillaCorreo
                        .construirAsunto()
        );


        notificacion.setTablaReferencia(
                TABLA_REFERENCIA
        );


        notificacion.setIdReferencia(
                idPago
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
        // PRIMER INTENTO
        // =================================================

        try {

            SimpleMailMessage mensaje =
                    construirMensaje(
                            notificacion,
                            pago
                    );


            mailSender.send(
                    mensaje
            );


            marcarEnviado(
                    notificacion
            );

        } catch (MailException ex) {

            marcarFallido(
                    notificacion,
                    ex.getMessage()
            );
        }


        notificacionCorreoRepository.save(
                notificacion
        );
    }


    // =====================================================
    // CONSTRUIR MENSAJE
    // =====================================================

    private SimpleMailMessage construirMensaje(
            NotificacionCorreo notificacion,
            Pago pago
    ) {

        Cita cita =
                pago.getCita();


        SimpleMailMessage mensaje =
                new SimpleMailMessage();


        mensaje.setFrom(
                correoRemitente
        );


        mensaje.setTo(
                notificacion.getDestinatarioCorreo()
        );


        mensaje.setSubject(
                notificacion.getAsunto()
        );


        mensaje.setText(
                plantillaCorreo
                        .construirCuerpo(

                                cita.getPaciente()
                                        .getNombreCompleto(),

                                pago.getNumeroTransaccion(),

                                cita.getMedico()
                                        .getNombreCompleto(),

                                cita.getEspecialidad()
                                        .getNombre(),

                                cita.getSucursal()
                                        .getNombre(),

                                cita.getFechaHoraCita(),

                                pago.getMonto(),

                                pago.getFormaPago()
                                        .getNombre(),

                                pago.getFechaHoraPago()
                        )
        );


        return mensaje;
    }


    // =====================================================
    // ENVIADO
    // =====================================================

    private void marcarEnviado(
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
    // FALLIDO
    // =====================================================

    private void marcarFallido(
            NotificacionCorreo notificacion,
            String error
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
                        error
                )
        );
    }


    // =====================================================
    // INTENTOS
    // =====================================================

    private void incrementarIntentos(
            NotificacionCorreo notificacion
    ) {

        short intentos =
                notificacion.getIntentosEnvio() == null
                        ? 0
                        : notificacion.getIntentosEnvio();


        notificacion.setIntentosEnvio(
                (short) (intentos + 1)
        );
    }


    // =====================================================
    // LIMITAR ERROR
    // =====================================================

    private String limitarError(
            String error
    ) {

        if (error == null
                || error.isBlank()) {

            return "Error desconocido durante el envío del correo.";
        }


        String limpio =
                error.trim();


        return limpio.length() <= 500
                ? limpio
                : limpio.substring(
                0,
                500
        );
    }
}