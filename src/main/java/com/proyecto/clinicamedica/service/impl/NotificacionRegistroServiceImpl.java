package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;
import com.proyecto.clinicamedica.service.NotificacionRegistroService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.OffsetDateTime;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoRegistroExterno;

/**
 * =========================================================
 * IMPLEMENTACIÓN: NOTIFICACIÓN DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad:
 *
 * - Crear la notificación de bienvenida.
 * - Intentar enviar el correo.
 * - Registrar el resultado del intento.
 *
 * Si el correo falla:
 *
 * - el paciente NO se elimina;
 * - la notificación queda como Fallido;
 * - se conserva el error;
 * - podrá ser procesada posteriormente por el
 *   mecanismo automático de reintentos.
 *
 * =========================================================
 */
@Service
public class NotificacionRegistroServiceImpl
        implements NotificacionRegistroService {


    private static final String TIPO_NOTIFICACION =
            "REGISTRO_USUARIO_EXTERNO";

    private static final String TABLA_REFERENCIA =
            "usuario";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender mailSender;

    private final NotificacionCorreoRepository
            notificacionCorreoRepository;


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private final PlantillaCorreoRegistroExterno
            plantillaCorreo;
    private final String correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionRegistroServiceImpl(
            JavaMailSender mailSender,
            NotificacionCorreoRepository notificacionCorreoRepository,
            PlantillaCorreoRegistroExterno plantillaCorreo,

            @Value("${spring.mail.username}")
            String correoRemitente
    ) {

        this.mailSender =
                mailSender;

        this.notificacionCorreoRepository =
                notificacionCorreoRepository;

        this.plantillaCorreo =
                plantillaCorreo;

        this.correoRemitente =
                correoRemitente;
    }


    // =====================================================
    // ENVIAR BIENVENIDA
    // =====================================================

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarBienvenida(
            Integer idUsuario,
            String nombreCompleto,
            String correoElectronico
    ) {

        if (idUsuario == null
                || nombreCompleto == null
                || correoElectronico == null) {

            return;
        }


        // =================================================
        // 1. CREAR REGISTRO DE NOTIFICACIÓN
        // =================================================

        NotificacionCorreo notificacion =
                construirNotificacion(
                        idUsuario,
                        correoElectronico
                );


        notificacionCorreoRepository.saveAndFlush(
                notificacion
        );


        // =================================================
        // 2. INTENTAR ENVÍO
        // =================================================

        try {

            enviarCorreo(
                    nombreCompleto,
                    correoElectronico,
                    notificacion.getAsunto()
            );


            marcarComoEnviado(
                    notificacion
            );

        } catch (MailException ex) {

            marcarComoFallido(
                    notificacion,
                    ex
            );
        }


        // =================================================
        // 3. GUARDAR RESULTADO
        // =================================================

        notificacionCorreoRepository.save(
                notificacion
        );
    }


    // =====================================================
    // CONSTRUIR NOTIFICACIÓN
    // =====================================================

    private NotificacionCorreo construirNotificacion(
            Integer idUsuario,
            String correoElectronico
    ) {

        NotificacionCorreo notificacion =
                new NotificacionCorreo();


        notificacion.setTipoNotificacion(
                TIPO_NOTIFICACION
        );


        notificacion.setDestinatarioCorreo(
                correoElectronico
        );


        notificacion.setAsunto(
                plantillaCorreo.construirAsunto()
        );


        notificacion.setTablaReferencia(
                TABLA_REFERENCIA
        );


        notificacion.setIdReferencia(
                idUsuario
        );


        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_PENDIENTE
        );


        notificacion.setIntentosEnvio(
                (short) 0
        );


        return notificacion;
    }


    // =====================================================
    // ENVIAR CORREO
    // =====================================================

    private void enviarCorreo(
            String nombreCompleto,
            String correoElectronico,
            String asunto
    ) {

        SimpleMailMessage mensaje =
                new SimpleMailMessage();


        mensaje.setFrom(
                correoRemitente
        );


        mensaje.setTo(
                correoElectronico
        );


        mensaje.setSubject(
                asunto
        );


        mensaje.setText(
                plantillaCorreo.construirCuerpo(
                        nombreCompleto
                )
        );


        mailSender.send(
                mensaje
        );
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

        short intentosActuales =
                notificacion.getIntentosEnvio() == null
                        ? 0
                        : notificacion.getIntentosEnvio();


        notificacion.setIntentosEnvio(
                (short) (intentosActuales + 1)
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