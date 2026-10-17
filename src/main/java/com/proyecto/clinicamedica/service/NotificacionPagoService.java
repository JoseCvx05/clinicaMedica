
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoPago;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

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
 * - Enviar comprobante institucional HTML.
 * - Mantener alternativa de texto plano.
 * - Registrar Enviado/Fallido.
 *
 * Si falla:
 *
 * - El pago NO se revierte.
 * - La cita continúa Pagada.
 * - La notificación queda Fallida.
 * - El scheduler realizará los reintentos.
 *
 * SEGURIDAD:
 *
 * - No modifica montos.
 * - No genera nuevas transacciones.
 * - No modifica el estado de la cita.
 * - Mantiene el control contra duplicados.
 * - Conserva la trazabilidad de notificaciones.
 *
 * =========================================================
 */
@Service
public class NotificacionPagoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    public static final String TIPO_NOTIFICACION =
            "COMPROBANTE_PAGO";


    public static final String TABLA_REFERENCIA =
            "pago";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender mailSender;

    private final NotificacionCorreoRepository
            notificacionCorreoRepository;

    private final PagoRepository pagoRepository;

    private final PlantillaCorreoPago plantillaCorreo;

    private final String correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

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
                        .orElse(null);


        if (pago == null) {
            return;
        }


        Cita cita =
                pago.getCita();


        Usuario paciente =
                cita.getPaciente();


        // =================================================
        // OBTENER CORREO DEL PACIENTE
        // =================================================

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
                plantillaCorreo.construirAsunto()
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
        // PRIMER INTENTO DE ENVÍO
        // =================================================

        try {

            // =============================================
            // CONSTRUIR MENSAJE HTML
            // =============================================

            MimeMessage mensaje =
                    construirMensaje(
                            notificacion,
                            pago
                    );


            // =============================================
            // ENVIAR CORREO
            // =============================================

            mailSender.send(
                    mensaje
            );


            // =============================================
            // REGISTRAR ENVÍO EXITOSO
            // =============================================

            marcarEnviado(
                    notificacion
            );

        } catch (MailException ex) {

            // =============================================
            // REGISTRAR FALLO DE ENVÍO
            // =============================================

            marcarFallido(
                    notificacion,
                    ex.getMessage()
            );
        }


        // =================================================
        // GUARDAR RESULTADO
        // =================================================

        notificacionCorreoRepository.save(
                notificacion
        );
    }


    // =====================================================
    // CONSTRUIR MENSAJE HTML
    // =====================================================
    //
    // CU-04:
    //
    // Se reemplaza SimpleMailMessage por MimeMessage.
    //
    // El correo incluye:
    //
    // - Versión de texto plano.
    // - Versión HTML institucional.
    //
    // Gmail y otros clientes compatibles mostrarán
    // preferentemente la versión HTML.
    //
    // =====================================================

    private MimeMessage construirMensaje(
            NotificacionCorreo notificacion,
            Pago pago
    ) {

        Cita cita =
                pago.getCita();


        try {

            // =============================================
            // CREAR MENSAJE MIME
            // =============================================

            MimeMessage mensaje =
                    mailSender.createMimeMessage();


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );


            // =============================================
            // REMITENTE
            // =============================================

            helper.setFrom(
                    correoRemitente
            );


            // =============================================
            // DESTINATARIO
            // =============================================

            helper.setTo(
                    notificacion.getDestinatarioCorreo()
            );


            // =============================================
            // ASUNTO
            // =============================================

            helper.setSubject(
                    notificacion.getAsunto()
            );


            // =============================================
            // CONSTRUIR VERSIÓN EN TEXTO
            // =============================================

            String cuerpoTexto =
                    plantillaCorreo.construirCuerpo(

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
                    );


            // =============================================
            // CONSTRUIR VERSIÓN HTML
            // =============================================

            String cuerpoHtml =
                    plantillaCorreo.construirCuerpoHtml(

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
                    );


            // =============================================
            // CONFIGURAR TEXTO + HTML
            // =============================================

            helper.setText(
                    cuerpoTexto,
                    cuerpoHtml
            );


            return mensaje;

        } catch (MessagingException ex) {

            // =============================================
            // CONSERVAR MANEJO DE ERRORES DE CORREO
            // =============================================

            throw new MailPreparationException(
                    "No fue posible preparar el comprobante HTML.",
                    ex
            );
        }
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
