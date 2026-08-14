package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoPago;
import com.proyecto.clinicamedica.service.correo.PlantillaCorreoRegistroExterno;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: REINTENTO DE NOTIFICACIONES
 * =========================================================
 *
 * Procesa notificaciones pendientes o fallidas.
 *
 * Actualmente soporta:
 *
 * - CU-02 Registro de Usuarios Externos.
 * - CU-04 Comprobante de Pago.
 *
 * Se utiliza un único mecanismo de reintentos para evitar
 * duplicar schedulers e infraestructura de correo.
 *
 * =========================================================
 */
@Service
public class ReintentoNotificacionService {


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    /**
     * Número máximo TOTAL de intentos.
     *
     * El primer envío también cuenta.
     */
    private static final short MAXIMO_INTENTOS =
            3;


    // =====================================================
    // TIPOS DE NOTIFICACIÓN
    // =====================================================

    private static final String TIPO_REGISTRO_EXTERNO =
            "REGISTRO_USUARIO_EXTERNO";


    private static final String TIPO_COMPROBANTE_PAGO =
            "COMPROBANTE_PAGO";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final NotificacionCorreoRepository
            notificacionCorreoRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final PagoRepository
            pagoRepository;

    private final JavaMailSender
            mailSender;

    private final PlantillaCorreoRegistroExterno
            plantillaRegistro;

    private final PlantillaCorreoPago
            plantillaPago;


    // =====================================================
    // CONFIGURACIÓN DE CORREO
    // =====================================================

    private final String correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ReintentoNotificacionService(

            NotificacionCorreoRepository
                    notificacionCorreoRepository,

            UsuarioRepository
                    usuarioRepository,

            PagoRepository
                    pagoRepository,

            JavaMailSender
                    mailSender,

            PlantillaCorreoRegistroExterno
                    plantillaRegistro,

            PlantillaCorreoPago
                    plantillaPago,

            @Value("${spring.mail.username}")
            String correoRemitente
    ) {

        this.notificacionCorreoRepository =
                notificacionCorreoRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.pagoRepository =
                pagoRepository;

        this.mailSender =
                mailSender;

        this.plantillaRegistro =
                plantillaRegistro;

        this.plantillaPago =
                plantillaPago;

        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // PROCESAR PENDIENTES
    // =====================================================

    @Transactional
    public void procesarPendientes() {

        List<NotificacionCorreo> notificaciones =
                notificacionCorreoRepository
                        .findTop20ByEstadoEnvioInAndIntentosEnvioLessThanOrderByFechaCreacionAsc(
                                List.of(
                                        NotificacionCorreo.ESTADO_PENDIENTE,
                                        NotificacionCorreo.ESTADO_FALLIDO
                                ),
                                MAXIMO_INTENTOS
                        );


        for (NotificacionCorreo notificacion :
                notificaciones) {

            procesarNotificacion(
                    notificacion
            );
        }
    }


    // =====================================================
    // PROCESAR UNA NOTIFICACIÓN
    // =====================================================

    private void procesarNotificacion(
            NotificacionCorreo notificacion
    ) {

        if (notificacion == null
                || notificacion.getTipoNotificacion() == null) {

            return;
        }


        switch (notificacion.getTipoNotificacion()) {


            // =================================================
            // CU-02
            // =================================================

            case TIPO_REGISTRO_EXTERNO ->

                    procesarRegistroExterno(
                            notificacion
                    );


            // =================================================
            // CU-04
            // =================================================

            case TIPO_COMPROBANTE_PAGO ->

                    procesarComprobantePago(
                            notificacion
                    );


            // =================================================
            // TIPO NO SOPORTADO
            // =================================================

            default -> {

                /*
                 * No modificamos notificaciones desconocidas.
                 *
                 * Esto evita que este servicio afecte
                 * futuros casos de uso.
                 */
            }
        }
    }


    // =====================================================
    // CU-02 - REGISTRO EXTERNO
    // =====================================================

    private void procesarRegistroExterno(
            NotificacionCorreo notificacion
    ) {

        Integer idUsuario =
                notificacion.getIdReferencia();


        if (idUsuario == null) {

            marcarFallido(
                    notificacion,
                    "La notificación no posee un usuario asociado."
            );

            return;
        }


        Usuario usuario =
                usuarioRepository
                        .findById(
                                idUsuario
                        )
                        .orElse(
                                null
                        );


        if (usuario == null) {

            marcarFallido(
                    notificacion,
                    "No se encontró el usuario asociado a la notificación."
            );

            return;
        }


        intentarEnvioRegistro(
                notificacion,
                usuario
        );
    }


    // =====================================================
    // REINTENTAR REGISTRO
    // =====================================================

    private void intentarEnvioRegistro(
            NotificacionCorreo notificacion,
            Usuario usuario
    ) {

        marcarReintentando(
                notificacion
        );


        try {

            SimpleMailMessage mensaje =
                    new SimpleMailMessage();


            mensaje.setFrom(
                    correoRemitente
            );


            mensaje.setTo(
                    notificacion.getDestinatarioCorreo()
            );


            /*
             * Utilizamos el asunto guardado originalmente.
             */
            mensaje.setSubject(
                    notificacion.getAsunto()
            );


            mensaje.setText(
                    plantillaRegistro
                            .construirCuerpo(
                                    usuario.getNombreCompleto()
                            )
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
    }


    // =====================================================
    // CU-04 - COMPROBANTE DE PAGO
    // =====================================================

    private void procesarComprobantePago(
            NotificacionCorreo notificacion
    ) {

        Integer idPago =
                notificacion.getIdReferencia();


        if (idPago == null) {

            marcarFallido(
                    notificacion,
                    "La notificación no posee un pago asociado."
            );

            return;
        }


        Pago pago =
                pagoRepository
                        .buscarParaNotificacion(
                                idPago
                        )
                        .orElse(
                                null
                        );


        if (pago == null) {

            marcarFallido(
                    notificacion,
                    "No se encontró el pago aprobado asociado a la notificación."
            );

            return;
        }


        intentarEnvioPago(
                notificacion,
                pago
        );
    }


    // =====================================================
    // REINTENTAR COMPROBANTE
    // =====================================================

    private void intentarEnvioPago(
            NotificacionCorreo notificacion,
            Pago pago
    ) {

        marcarReintentando(
                notificacion
        );


        try {

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
                    plantillaPago
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
    }


    // =====================================================
    // MARCAR REINTENTANDO
    // =====================================================

    private void marcarReintentando(
            NotificacionCorreo notificacion
    ) {

        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_REINTENTANDO
        );


        notificacionCorreoRepository
                .saveAndFlush(
                        notificacion
                );
    }


    // =====================================================
    // MARCAR ENVIADO
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


        notificacionCorreoRepository
                .save(
                        notificacion
                );
    }


    // =====================================================
    // MARCAR FALLIDO
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


        notificacionCorreoRepository
                .save(
                        notificacion
                );
    }


    // =====================================================
    // INCREMENTAR INTENTOS
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