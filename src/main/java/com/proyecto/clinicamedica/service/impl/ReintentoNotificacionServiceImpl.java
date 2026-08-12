package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.service.ReintentoNotificacionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.clinicamedica.service.correo.PlantillaCorreoRegistroExterno;
import java.time.OffsetDateTime;
import java.util.List;


/**
 * =========================================================
 * IMPLEMENTACIÓN: REINTENTO DE NOTIFICACIONES
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Recupera notificaciones pendientes/fallidas y vuelve
 * a intentar su envío.
 *
 * =========================================================
 */
@Service
public class ReintentoNotificacionServiceImpl
        implements ReintentoNotificacionService {


    private static final short MAXIMO_INTENTOS = 3;

    private static final String TIPO_REGISTRO_EXTERNO =
            "REGISTRO_USUARIO_EXTERNO";


    private final NotificacionCorreoRepository
            notificacionCorreoRepository;

    private final UsuarioRepository usuarioRepository;

    private final JavaMailSender mailSender;

    private final String correoRemitente;

    private final PlantillaCorreoRegistroExterno
            plantillaCorreo;

    public ReintentoNotificacionServiceImpl(
            NotificacionCorreoRepository notificacionCorreoRepository,
            UsuarioRepository usuarioRepository,
            JavaMailSender mailSender,
            PlantillaCorreoRegistroExterno plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente
    ) {

        this.notificacionCorreoRepository =
                notificacionCorreoRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.mailSender =
                mailSender;

        this.plantillaCorreo =
                plantillaCorreo;

        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // PROCESAR PENDIENTES
    // =====================================================

    @Override
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


        for (NotificacionCorreo notificacion : notificaciones) {

            procesarNotificacion(
                    notificacion
            );
        }
    }


    // =====================================================
    // PROCESAR NOTIFICACIÓN
    // =====================================================

    private void procesarNotificacion(
            NotificacionCorreo notificacion
    ) {

        if (notificacion == null) {
            return;
        }


        /*
         * Este servicio solamente procesa por ahora
         * correos pertenecientes a CU-02.
         */
        if (!TIPO_REGISTRO_EXTERNO.equals(
                notificacion.getTipoNotificacion()
        )) {
            return;
        }


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
                        .findById(idUsuario)
                        .orElse(null);


        if (usuario == null) {

            marcarFallido(
                    notificacion,
                    "No se encontró el usuario asociado a la notificación."
            );

            return;
        }


        intentarEnvio(
                notificacion,
                usuario
        );
    }


    // =====================================================
    // INTENTAR ENVÍO
    // =====================================================

    private void intentarEnvio(
            NotificacionCorreo notificacion,
            Usuario usuario
    ) {

        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_REINTENTANDO
        );


        notificacionCorreoRepository.saveAndFlush(
                notificacion
        );


        try {

            SimpleMailMessage mensaje =
                    construirMensaje(
                            notificacion,
                            usuario
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
    // CONSTRUIR MENSAJE
    // =====================================================

    private SimpleMailMessage construirMensaje(
            NotificacionCorreo notificacion,
            Usuario usuario
    ) {

        SimpleMailMessage mensaje =
                new SimpleMailMessage();


        mensaje.setFrom(
                correoRemitente
        );


        mensaje.setTo(
                notificacion.getDestinatarioCorreo()
        );


        mensaje.setSubject(
                plantillaCorreo.construirAsunto()
        );

        mensaje.setText(
                plantillaCorreo.construirCuerpo(
                        usuario.getNombreCompleto()
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


        notificacionCorreoRepository.save(
                notificacion
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
                limitarError(error)
        );


        notificacionCorreoRepository.save(
                notificacion
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
    // LIMITAR MENSAJE
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