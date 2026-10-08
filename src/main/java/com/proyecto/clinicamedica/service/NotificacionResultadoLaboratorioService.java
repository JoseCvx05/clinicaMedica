package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.ResultadoLaboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.repository.ResultadoLaboratorioRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoResultadoLaboratorio;

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
 * SERVICIO: NOTIFICACIÓN DE RESULTADO DE LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * RNF-007:
 *
 * Cuando un resultado de laboratorio sea publicado,
 * el Médico solicitante debe ser notificado.
 *
 * Responsabilidades:
 *
 * - Recuperar el resultado desde PostgreSQL.
 * - Verificar que el resultado esté publicado.
 * - Identificar la orden correspondiente.
 * - Identificar al Médico solicitante.
 * - Obtener el correo del Médico desde la BD.
 * - Evitar notificaciones duplicadas.
 * - Registrar la notificación.
 * - Enviar correo HTML.
 * - Registrar estado Enviado/Fallido.
 *
 * SEGURIDAD:
 *
 * El destinatario NO se recibe desde el navegador.
 *
 * Se obtiene mediante:
 *
 * ResultadoLaboratorio
 *      -> DetalleOrdenLaboratorio
 *      -> OrdenLaboratorio
 *      -> Médico solicitante
 *      -> correoElectronico
 *
 * IMPORTANTE:
 *
 * Un fallo en el correo NO debe modificar ni eliminar
 * el resultado de laboratorio publicado.
 *
 * =========================================================
 */
@Service
public class NotificacionResultadoLaboratorioService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    public static final String TIPO_NOTIFICACION =
            "RESULTADO_LABORATORIO_PUBLICADO";


    public static final String TABLA_REFERENCIA =
            "resultado_laboratorio";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender
            mailSender;


    private final NotificacionCorreoRepository
            notificacionCorreoRepository;


    private final ResultadoLaboratorioRepository
            resultadoLaboratorioRepository;


    private final PlantillaCorreoResultadoLaboratorio
            plantillaCorreo;


    private final String
            correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionResultadoLaboratorioService(

            JavaMailSender mailSender,

            NotificacionCorreoRepository
                    notificacionCorreoRepository,

            ResultadoLaboratorioRepository
                    resultadoLaboratorioRepository,

            PlantillaCorreoResultadoLaboratorio
                    plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente

    ) {

        this.mailSender =
                mailSender;


        this.notificacionCorreoRepository =
                notificacionCorreoRepository;


        this.resultadoLaboratorioRepository =
                resultadoLaboratorioRepository;


        this.plantillaCorreo =
                plantillaCorreo;


        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // CU-09
    // NOTIFICAR RESULTADO PUBLICADO
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarNotificacion(
            Integer idResultadoLaboratorio
    ) {


        // =================================================
        // VALIDAR ID
        // =================================================

        if (idResultadoLaboratorio == null
                || idResultadoLaboratorio <= 0) {

            return;
        }

        // =================================================
        // OBTENER RESULTADO
        // =================================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findById(
                                idResultadoLaboratorio
                        )
                        .orElse(
                                null
                        );


        if (resultado == null) {

            return;
        }


        // =================================================
        // SOLO RESULTADOS PUBLICADOS
        // =================================================
        //
        // Guardado solamente:
        // NO notificar.
        //
        // Validado solamente:
        // NO notificar.
        //
        // Publicado:
        // SÍ notificar.
        //
        // =================================================

        if (!resultado.estaPublicado()) {

            return;
        }

        // =================================================
// EVITAR DUPLICADOS POR PUBLICACIÓN
// =================================================
//
// RNF-024:
//
// Un mismo resultado puede tener varias publicaciones:
//
// 1. Publicación original.
// 2. Corrección autorizada.
// 3. Nueva validación.
// 4. Republicación.
//
// Cada publicación real debe generar su propio correo.
//
// Pero volver a ejecutar accidentalmente la misma
// publicación NO debe mandar otro correo.
//
// Para distinguirlas comparamos:
//
// fechaHoraPublicacion actual
//
// contra
//
// fechaCreacion de la última notificación.
//
// =================================================

        OffsetDateTime fechaPublicacionActual =
                resultado.getFechaHoraPublicacion();


        if (fechaPublicacionActual == null) {

            return;
        }


        NotificacionCorreo ultimaNotificacion =
                notificacionCorreoRepository
                        .findTopByTipoNotificacionAndTablaReferenciaAndIdReferenciaOrderByFechaCreacionDesc(

                                TIPO_NOTIFICACION,

                                TABLA_REFERENCIA,

                                idResultadoLaboratorio
                        )
                        .orElse(
                                null
                        );


        if (ultimaNotificacion != null
                && ultimaNotificacion.getFechaCreacion() != null) {

            OffsetDateTime fechaUltimaNotificacion =
                    ultimaNotificacion
                            .getFechaCreacion();


            /*
             * Si la última notificación fue creada al mismo
             * tiempo o después de la publicación actual,
             * significa que esta publicación ya fue notificada.
             */
            if (!fechaUltimaNotificacion
                    .isBefore(
                            fechaPublicacionActual
                    )) {

                return;
            }
        }


        // =================================================
        // DETALLE DE LA ORDEN
        // =================================================

        DetalleOrdenLaboratorio detalle =
                resultado
                        .getDetalleOrdenLaboratorio();


        if (detalle == null
                || detalle.getId() == null) {

            return;
        }


        // =================================================
        // ORDEN
        // =================================================

        OrdenLaboratorio orden =
                detalle
                        .getOrdenLaboratorio();


        if (orden == null
                || orden.getId() == null) {

            return;
        }


        // =================================================
        // MÉDICO SOLICITANTE
        // =================================================

        Usuario medico =
                orden.getMedico();


        if (medico == null
                || medico.getId() == null) {

            return;
        }


        String correoMedico =
                medico.getCorreoElectronico();


        if (correoMedico == null
                || correoMedico.isBlank()) {

            return;
        }


        // =================================================
        // CITA
        // =================================================

        Cita cita =
                orden.getCita();


        if (cita == null
                || cita.getId() == null) {

            return;
        }


        // =================================================
        // PACIENTE
        // =================================================

        Usuario paciente =
                cita.getPaciente();


        if (paciente == null
                || paciente.getId() == null) {

            return;
        }


        // =================================================
        // INFORMACIÓN MÍNIMA
        // =================================================

        if (orden.getNumeroOrden() == null
                || orden.getNumeroOrden().isBlank()

                || detalle.getNombreExamen() == null
                || detalle.getNombreExamen().isBlank()

                || resultado.getValorResultado() == null
                || resultado.getValorResultado().isBlank()

                || resultado.getFechaResultado() == null

                || resultado.getFechaHoraPublicacion() == null) {

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
                correoMedico.trim()
        );


        notificacion.setAsunto(
                plantillaCorreo
                        .construirAsunto()
        );


        notificacion.setTablaReferencia(
                TABLA_REFERENCIA
        );


        notificacion.setIdReferencia(
                idResultadoLaboratorio
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

            MimeMessage mensaje =
                    construirMensaje(

                            notificacion,

                            resultado,

                            detalle,

                            orden,

                            medico,

                            paciente
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
        // GUARDAR RESULTADO DEL ENVÍO
        // =================================================

        notificacionCorreoRepository
                .save(
                        notificacion
                );
    }


    // =====================================================
    // CONSTRUIR MENSAJE
    // =====================================================

    private MimeMessage construirMensaje(

            NotificacionCorreo notificacion,

            ResultadoLaboratorio resultado,

            DetalleOrdenLaboratorio detalle,

            OrdenLaboratorio orden,

            Usuario medico,

            Usuario paciente

    ) throws MessagingException {


        // =================================================
        // MIME MESSAGE
        // =================================================

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
        // ASUNTO
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

                                medico.getNombreCompleto(),

                                paciente.getNombreCompleto(),

                                orden.getNumeroOrden(),

                                detalle.getNombreExamen(),

                                resultado.getValorResultado(),

                                resultado.getUnidadResultado(),

                                detalle.getRangoReferencia(),

                                resultado.estaFueraDeRango(),

                                resultado.getNotasResultado(),

                                resultado.getFechaResultado(),

                                resultado.getFechaHoraPublicacion()
                        );


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

                        : notificacion
                        .getIntentosEnvio();


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