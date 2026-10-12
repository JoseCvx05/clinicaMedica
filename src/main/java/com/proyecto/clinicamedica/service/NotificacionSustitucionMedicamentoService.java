
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleDespacho;
import com.proyecto.clinicamedica.entity.NotificacionCorreo;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DespachoRepository;
import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;
import com.proyecto.clinicamedica.repository.NotificacionCorreoRepository;

import com.proyecto.clinicamedica.service.correo.PlantillaCorreoSustitucionMedicamento;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * CU-11 - FA02
 * NOTIFICACIÓN DE SUSTITUCIÓN DE MEDICAMENTOS
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Consultar un despacho confirmado.
 * - Identificar sustituciones reales.
 * - Obtener al médico tratante desde PostgreSQL.
 * - Evitar una notificación duplicada por despacho.
 * - Registrar la notificación.
 * - Enviar un correo HTML.
 * - Registrar el resultado del envío.
 *
 * IMPORTANTE:
 *
 * Este servicio debe invocarse únicamente después
 * de confirmar la transacción del despacho.
 *
 * No modifica recetas ni inventarios.
 *
 * =========================================================
 */
@Service
public class NotificacionSustitucionMedicamentoService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    public static final String TIPO_NOTIFICACION =
            "SUSTITUCION_MEDICAMENTO";

    public static final String TABLA_REFERENCIA =
            "despacho";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final JavaMailSender mailSender;

    private final DespachoRepository despachoRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;

    private final NotificacionCorreoRepository
            notificacionCorreoRepository;

    private final PlantillaCorreoSustitucionMedicamento
            plantillaCorreo;

    private final String correoRemitente;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionSustitucionMedicamentoService(
            JavaMailSender mailSender,
            DespachoRepository despachoRepository,
            DetalleDespachoRepository detalleDespachoRepository,
            NotificacionCorreoRepository notificacionCorreoRepository,
            PlantillaCorreoSustitucionMedicamento plantillaCorreo,
            @Value("${spring.mail.username}")
            String correoRemitente
    ) {

        this.mailSender = mailSender;

        this.despachoRepository = despachoRepository;

        this.detalleDespachoRepository =
                detalleDespachoRepository;

        this.notificacionCorreoRepository =
                notificacionCorreoRepository;

        this.plantillaCorreo = plantillaCorreo;

        this.correoRemitente = correoRemitente;
    }


    // =====================================================
    // NOTIFICAR SUSTITUCIÓN
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void enviarNotificacion(
            Integer idDespacho
    ) {

        // -------------------------------------------------
        // 1. VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idDespacho == null || idDespacho <= 0) {
            return;
        }


        // -------------------------------------------------
        // 2. CONSULTAR DESPACHO
        // -------------------------------------------------

        Despacho despacho =
                despachoRepository.findById(
                        idDespacho
                ).orElse(null);

        if (despacho == null) {
            return;
        }


        // -------------------------------------------------
        // 3. DEBE ESTAR CONFIRMADO
        // -------------------------------------------------

        if (!despacho.estaConfirmado()) {
            return;
        }


        // -------------------------------------------------
        // 4. CONSULTAR DETALLES DEL DESPACHO
        // -------------------------------------------------

        Integer idSucursal =
                despacho.getSucursal().getId();

        List<DetalleDespacho> detalles =
                detalleDespachoRepository
                        .findByDespacho_IdAndDespacho_Sucursal_IdOrderByIdAsc(
                                idDespacho,
                                idSucursal
                        );


        // -------------------------------------------------
        // 5. IDENTIFICAR SUSTITUCIONES
        // -------------------------------------------------

        List<PlantillaCorreoSustitucionMedicamento.Sustitucion>
                sustituciones = new ArrayList<>();

        for (DetalleDespacho detalle : detalles) {

            Integer idOriginal =
                    detalle.getDetalleRecetaMedica()
                            .getMedicamento()
                            .getId();

            Integer idEntregado =
                    detalle.getMedicamentoEntregado()
                            .getId();

            boolean fueSustituido =
                    !Objects.equals(
                            idOriginal,
                            idEntregado
                    );

            if (!fueSustituido) {
                continue;
            }

            String razon =
                    detalle.getRazonSustitucion();

            if (razon == null || razon.isBlank()) {

                throw new IllegalStateException(
                        "El despacho contiene una sustitución "
                                + "sin justificación registrada."
                );
            }

            sustituciones.add(
                    new PlantillaCorreoSustitucionMedicamento.Sustitucion(
                            detalle.getDetalleRecetaMedica()
                                    .getMedicamento()
                                    .getNombre(),

                            detalle.getMedicamentoEntregado()
                                    .getNombre(),

                            detalle.getCantidadEntregada(),

                            razon
                    )
            );
        }


        // -------------------------------------------------
        // 6. SIN SUSTITUCIONES, NO ENVIAR
        // -------------------------------------------------

        if (sustituciones.isEmpty()) {
            return;
        }


        // -------------------------------------------------
        // 7. EVITAR NOTIFICACIONES DUPLICADAS
        // -------------------------------------------------

        boolean yaRegistrada =
                notificacionCorreoRepository
                        .existsByTipoNotificacionAndTablaReferenciaAndIdReferencia(
                                TIPO_NOTIFICACION,
                                TABLA_REFERENCIA,
                                idDespacho
                        );

        if (yaRegistrada) {
            return;
        }


        // -------------------------------------------------
        // 8. OBTENER MÉDICO Y PACIENTE
        // -------------------------------------------------

        RecetaMedica receta =
                despacho.getRecetaMedica();

        Usuario medico =
                receta.getMedico();

        if (medico == null
                || medico.getCorreoElectronico() == null
                || medico.getCorreoElectronico().isBlank()) {

            throw new IllegalStateException(
                    "El médico tratante no tiene "
                            + "un correo electrónico válido."
            );
        }

        if (receta.getCita() == null
                || receta.getCita().getPaciente() == null) {

            throw new IllegalStateException(
                    "No se pudo identificar al paciente "
                            + "de la receta."
            );
        }

        Usuario paciente =
                receta.getCita().getPaciente();


        // -------------------------------------------------
        // 9. REGISTRAR NOTIFICACIÓN
        // -------------------------------------------------

        NotificacionCorreo notificacion =
                new NotificacionCorreo();

        notificacion.setTipoNotificacion(
                TIPO_NOTIFICACION
        );

        notificacion.setTablaReferencia(
                TABLA_REFERENCIA
        );

        notificacion.setIdReferencia(
                idDespacho
        );

        notificacion.setDestinatarioCorreo(
                medico.getCorreoElectronico().trim()
        );

        notificacion.setAsunto(
                plantillaCorreo.construirAsunto()
        );

        notificacion.setEstadoEnvio(
                NotificacionCorreo.ESTADO_PENDIENTE
        );

        notificacion.setIntentosEnvio(
                (short) 0
        );

        notificacionCorreoRepository.saveAndFlush(
                notificacion
        );


        // -------------------------------------------------
        // 10. INTENTAR ENVÍO HTML
        // -------------------------------------------------

        try {

            MimeMessage mensaje =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(
                    correoRemitente
            );

            helper.setTo(
                    notificacion.getDestinatarioCorreo()
            );

            helper.setSubject(
                    notificacion.getAsunto()
            );

            String cuerpoHtml =
                    plantillaCorreo.construirCuerpo(
                            medico.getNombreCompleto(),
                            paciente.getNombreCompleto(),
                            receta.getId(),
                            despacho.getId(),
                            despacho.getSucursal().getNombre(),
                            sustituciones
                    );

            helper.setText(
                    cuerpoHtml,
                    true
            );

            mailSender.send(
                    mensaje
            );

            // Envío exitoso.

            notificacion.setEstadoEnvio(
                    NotificacionCorreo.ESTADO_ENVIADO
            );

            notificacion.setFechaHoraEnvio(
                    OffsetDateTime.now()
            );

            notificacion.setUltimoError(
                    null
            );

        } catch (
                MessagingException | MailException ex
        ) {

            // El error queda registrado.
            // No se modifica el despacho ya confirmado.

            notificacion.setEstadoEnvio(
                    NotificacionCorreo.ESTADO_FALLIDO
            );

            notificacion.setFechaHoraEnvio(
                    null
            );

            notificacion.setUltimoError(
                    limitarError(ex.getMessage())
            );
        }


        // -------------------------------------------------
        // 11. REGISTRAR INTENTO
        // -------------------------------------------------

        notificacion.setIntentosEnvio(
                (short) 1
        );

        notificacionCorreoRepository.save(
                notificacion
        );
    }


    // =====================================================
    // LIMITAR MENSAJE DE ERROR
    // =====================================================

    private String limitarError(
            String mensaje
    ) {

        if (mensaje == null || mensaje.isBlank()) {

            return "Error desconocido durante "
                    + "el envío de la notificación.";
        }

        String limpio =
                mensaje.trim();

        return limpio.length() <= 500
                ? limpio
                : limpio.substring(0, 500);
    }
}
