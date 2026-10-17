
package com.proyecto.clinicamedica.service.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * =========================================================
 * PLANTILLA DE CORREO: COMPROBANTE DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Construye:
 * - Asunto institucional.
 * - Comprobante en texto plano.
 * - Comprobante HTML con diseño SANTOS.
 * - Información completa del pago.
 * - Información de la cita médica.
 * - Pie automático RN-GLOBAL-006.
 *
 * No procesa ni modifica pagos.
 * No modifica citas ni transacciones.
 * No guarda información en PostgreSQL.
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoPago {

    // =====================================================
    // FORMATO DE FECHAS
    // =====================================================

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =====================================================
    // INFORMACIÓN INSTITUCIONAL
    // =====================================================

    private final String nombreHospital;
    private final String telefonoHospital;

    public PlantillaCorreoPago(
            @Value("${hospital.nombre}") String nombreHospital,
            @Value("${hospital.telefono}") String telefonoHospital
    ) {
        this.nombreHospital = nombreHospital;
        this.telefonoHospital = telefonoHospital;
    }

    // =====================================================
    // ASUNTO RN-CU04-05
    // =====================================================

    public String construirAsunto() {
        return "Comprobante de Pago - Consulta Médica | "
                + nombreHospital;
    }

    // =====================================================
    // CUERPO EN TEXTO PLANO - COMPATIBILIDAD
    // =====================================================

    public String construirCuerpo(
            String nombrePaciente,
            String numeroTransaccion,
            String medico,
            String especialidad,
            String sucursal,
            OffsetDateTime fechaHoraCita,
            BigDecimal monto,
            String formaPago,
            OffsetDateTime fechaHoraPago
    ) {

        return """
                SANTOS CLÍNICA MÉDICA
                CONFIRMACIÓN DE PAGO DE CONSULTA MÉDICA

                Estimado(a) %s:

                Nos complace informarle que su pago ha sido
                procesado satisfactoriamente.

                COMPROBANTE DE PAGO

                Estado: Pago realizado correctamente
                Número de transacción: %s
                Paciente: %s
                Monto pagado: %s
                Forma de pago: %s
                Fecha y hora del pago: %s

                INFORMACIÓN DE LA CITA

                Médico: %s
                Especialidad: %s
                Sucursal: %s
                Fecha y hora de la cita: %s
                Servicio: Consulta médica - %s

                RECOMENDACIONES

                Preséntese con anticipación a su cita.
                Lleve su documento de identificación.
                Conserve este comprobante como respaldo.

                ATENCIÓN AL PACIENTE

                Para consultas, comuníquese al teléfono %s.

                Agradecemos su confianza.

                Atentamente,
                Equipo de Atención al Paciente
                SANTOS Clínica Médica

                Este correo se generó automáticamente.
                No responda directamente a este mensaje.
                Si no reconoce el pago, contacte a la clínica.
                """
                .formatted(
                        nombrePaciente,
                        numeroTransaccion,
                        nombrePaciente,
                        formatearMonto(monto),
                        formaPago,
                        formatearFecha(fechaHoraPago),
                        medico,
                        especialidad,
                        sucursal,
                        formatearFecha(fechaHoraCita),
                        especialidad,
                        telefonoHospital
                );
    }

    // =====================================================
    // CU-04 - COMPROBANTE HTML
    // =====================================================

    public String construirCuerpoHtml(
            String nombrePaciente,
            String numeroTransaccion,
            String medico,
            String especialidad,
            String sucursal,
            OffsetDateTime fechaHoraCita,
            BigDecimal monto,
            String formaPago,
            OffsetDateTime fechaHoraPago
    ) {

        StringBuilder html = new StringBuilder();

        // =================================================
        // ENCABEZADO INSTITUCIONAL
        // =================================================

        html.append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                    <title>Comprobante de Pago - SANTOS</title>
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background-color:#f2f6fa;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#243247;">

                <table role="presentation"
                       cellpadding="0"
                       cellspacing="0"
                       border="0"
                       width="100%"
                       style="background-color:#f2f6fa;">

                    <tr>
                        <td align="center" style="padding:28px 12px;">

                            <table role="presentation"
                                   cellpadding="0"
                                   cellspacing="0"
                                   border="0"
                                   width="100%"
                                   style="
                                       max-width:650px;
                                       background-color:#ffffff;
                                       border:1px solid #dce6f0;
                                       border-radius:12px;">

                                <tr>
                                    <td align="center"
                                        style="
                                            background-color:#123c72;
                                            padding:32px 24px;">

                                        <h1 style="
                                            color:#ffffff;
                                            font-size:26px;
                                            margin:0 0 8px;">
                                            SANTOS Clínica Médica
                                        </h1>

                                        <p style="
                                            color:#dbeafe;
                                            font-size:14px;
                                            margin:0;">
                                            Atención y servicios médicos
                                        </p>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="padding:30px 26px;">

                                        <p style="
                                            color:#15803d;
                                            font-size:14px;
                                            font-weight:bold;">
                                            &#10003; PAGO CONFIRMADO
                                        </p>

                                        <h2 style="
                                            color:#123c72;
                                            font-size:23px;">
                                            Comprobante de Pago
                                        </h2>

                                        <p style="
                                            font-size:15px;
                                            line-height:1.7;">
                                            Estimado(a)
                                            <strong>
                """);

        html.append(escaparHtml(nombrePaciente));

        html.append("""
                                            </strong>:
                                        </p>

                                        <p style="
                                            font-size:15px;
                                            line-height:1.7;">
                                            Le informamos que el pago
                                            correspondiente a su consulta
                                            médica ha sido procesado
                                            satisfactoriamente.
                                        </p>

                                        <p style="
                                            font-size:15px;
                                            line-height:1.7;">
                                            A continuación encontrará
                                            los detalles de su transacción
                                            y de su cita médica.
                                        </p>

                                        <!-- DATOS DEL PAGO -->

                                        <h3 style="
                                            color:#123c72;
                                            font-size:18px;
                                            margin-top:26px;">
                                            Información del Pago
                                        </h3>

                                        <table role="presentation"
                                               width="100%"
                                               cellpadding="0"
                                               cellspacing="0"
                                               border="0"
                                               style="
                                                   background-color:#eff6ff;
                                                   border:1px solid #bfdbfe;
                                                   border-radius:8px;">
                                            <tbody>
                """);

        agregarFila(
                html,
                "Estado",
                "Pago confirmado"
        );

        agregarFila(
                html,
                "Número de transacción",
                numeroTransaccion
        );

        agregarFila(
                html,
                "Paciente",
                nombrePaciente
        );

        agregarFila(
                html,
                "Monto pagado",
                formatearMonto(monto)
        );

        agregarFila(
                html,
                "Forma de pago",
                formaPago
        );

        agregarFila(
                html,
                "Fecha y hora del pago",
                formatearFecha(fechaHoraPago)
        );

        html.append("""
                                            </tbody>
                                        </table>

                                        <!-- DATOS DE LA CITA -->

                                        <h3 style="
                                            color:#123c72;
                                            font-size:18px;
                                            margin-top:28px;">
                                            Información de la Cita
                                        </h3>

                                        <table role="presentation"
                                               width="100%"
                                               cellpadding="0"
                                               cellspacing="0"
                                               border="0"
                                               style="
                                                   background-color:#f8fafc;
                                                   border:1px solid #e2e8f0;
                                                   border-radius:8px;">
                                            <tbody>
                """);

        agregarFila(html, "Médico asignado", medico);

        agregarFila(html, "Especialidad", especialidad);

        agregarFila(html, "Sucursal", sucursal);

        agregarFila(
                html,
                "Fecha y hora de la cita",
                formatearFecha(fechaHoraCita)
        );

        agregarFila(
                html,
                "Servicio",
                "Consulta médica - " + especialidad
        );

        html.append("""
                                            </tbody>
                                        </table>

                                        <!-- RECOMENDACIONES -->

                                        <div style="
                                            background-color:#ecfdf5;
                                            border-left:4px solid #15803d;
                                            padding:18px;
                                            margin-top:25px;">

                                            <h3 style="
                                                color:#166534;
                                                font-size:16px;
                                                margin:0 0 12px;">
                                                Recomendaciones para su cita
                                            </h3>

                                            <p style="
                                                font-size:14px;
                                                line-height:1.8;
                                                margin:0;">
                                                Le recomendamos presentarse
                                                con anticipación a la hora
                                                programada, llevar su
                                                documento de identificación
                                                y conservar este comprobante
                                                como respaldo de su pago.
                                            </p>

                                        </div>

                                        <!-- DESPEDIDA -->

                                        <p style="
                                            font-size:15px;
                                            line-height:1.8;
                                            margin-top:25px;">
                                            Agradecemos la confianza
                                            depositada en nuestra institución.
                                        </p>

                                        <p style="
                                            font-size:15px;
                                            line-height:1.8;">
                                            Atentamente,<br>
                                            <strong>
                                                Equipo de Atención al Paciente
                                            </strong><br>
                                            SANTOS Clínica Médica
                                        </p>

                                    </td>
                                </tr>

                                <!-- ATENCIÓN AL PACIENTE -->

                                <tr>
                                    <td style="
                                        background-color:#f8fafc;
                                        padding:22px 26px;
                                        border-top:1px solid #e2e8f0;">

                                        <h3 style="
                                            color:#123c72;
                                            font-size:15px;
                                            margin:0 0 10px;">
                                            ¿Necesita asistencia?
                                        </h3>

                                        <p style="
                                            font-size:14px;
                                            line-height:1.7;
                                            margin:0;">
                                            Para consultas relacionadas
                                            con su cita o transacción,
                                            comuníquese al teléfono:
                                            <strong>
                """);

        html.append(escaparHtml(telefonoHospital));

        html.append("""
                                            </strong>
                                        </p>

                                    </td>
                                </tr>

                                <!-- AVISO AUTOMÁTICO -->

                                <tr>
                                    <td align="center"
                                        style="
                                            background-color:#edf2f7;
                                            padding:22px 26px;">

                                        <p style="
                                            font-size:12px;
                                            color:#64748b;
                                            line-height:1.7;
                                            margin:0 0 10px;">
                                            Este correo fue generado
                                            automáticamente por SANTOS
                                            Clínica Médica.
                                            Por favor, no responda
                                            directamente a este mensaje.
                                        </p>

                                        <p style="
                                            font-size:12px;
                                            color:#64748b;
                                            line-height:1.7;
                                            margin:0;">
                                            Si no reconoce esta transacción,
                                            comuníquese con nuestra institución.
                                        </p>

                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>

                </body>
                </html>
                """);

        return html.toString();
    }

    // =====================================================
    // FILAS DEL COMPROBANTE HTML
    // =====================================================

    private void agregarFila(
            StringBuilder html,
            String etiqueta,
            String valor
    ) {

        html.append("""
                <tr>
                    <td style="
                        padding:12px 15px;
                        border-bottom:1px solid #e2e8f0;
                        font-size:14px;
                        width:42%;
                        color:#475569;">
                """);

        html.append(escaparHtml(etiqueta));

        html.append("""
                    </td>
                    <td style="
                        padding:12px 15px;
                        border-bottom:1px solid #e2e8f0;
                        font-size:14px;
                        font-weight:bold;
                        color:#123c72;
                        overflow-wrap:anywhere;">
                """);

        html.append(escaparHtml(valor));

        html.append("""
                    </td>
                </tr>
                """);
    }

    // =====================================================
    // FORMATO DE FECHA
    // =====================================================

    private String formatearFecha(
            OffsetDateTime fecha
    ) {

        return fecha == null
                ? "No disponible"
                : fecha.format(FORMATO_FECHA);
    }

    // =====================================================
    // FORMATO DE MONTO
    // =====================================================

    private String formatearMonto(
            BigDecimal monto
    ) {

        return monto == null
                ? "No disponible"
                : "Q" + monto.setScale(2).toPlainString();
    }

    // =====================================================
    // PROTEGER DATOS EN HTML
    // =====================================================

    private String escaparHtml(
            String texto
    ) {

        if (texto == null || texto.isBlank()) {
            return "No disponible";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
