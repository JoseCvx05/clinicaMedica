
package com.proyecto.clinicamedica.service.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * PLANTILLA DE CORREO: REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Construye el asunto y las versiones de texto y HTML
 * del correo de bienvenida de SANTOS Clínica Médica.
 *
 * Tanto el primer envío como los reintentos pueden utilizar
 * esta misma plantilla, sin modificar los datos del paciente.
 *
 * Esta clase no envía correos ni modifica PostgreSQL.
 * =========================================================
 */
@Component
public class PlantillaCorreoRegistroExterno {

    // =====================================================
    // CONFIGURACIÓN INSTITUCIONAL
    // =====================================================

    private final String nombreHospital;
    private final String telefonoHospital;

    public PlantillaCorreoRegistroExterno(
            @Value("${hospital.nombre}") String nombreHospital,
            @Value("${hospital.telefono}") String telefonoHospital
    ) {
        this.nombreHospital = nombreHospital;
        this.telefonoHospital = telefonoHospital;
    }

    // =====================================================
    // ASUNTO DEL CORREO
    // =====================================================

    public String construirAsunto() {
        return "Confirmación de registro | SANTOS Clínica Médica";
    }

    // =====================================================
    // CUERPO EN TEXTO PLANO (COMPATIBILIDAD)
    // =====================================================

    public String construirCuerpo(String nombreCompleto) {
        return """
                SANTOS CLÍNICA MÉDICA
                CONFIRMACIÓN DE REGISTRO DE PACIENTE

                Estimado(a) %s:

                Nos complace informarle que su registro en nuestro
                portal de pacientes se completó satisfactoriamente.

                Ya puede consultar la disponibilidad y programar
                sus citas médicas a través del portal.

                INFORMACIÓN DE SU REGISTRO
                Paciente: %s
                Servicio: Registro de usuario externo
                Estado: Confirmado

                Para consultas, comuníquese al teléfono: %s.

                Atentamente,
                Equipo de Atención al Paciente
                %s

                Este mensaje se generó automáticamente.
                Por favor, no responda a este correo electrónico.
                Si no reconoce este registro, comuníquese con la clínica.
                """.formatted(
                nombreCompleto,
                nombreCompleto,
                telefonoHospital,
                nombreHospital
        );
    }

    // =====================================================
    // CUERPO HTML - DISEÑO SANTOS
    // =====================================================

    public String construirCuerpoHtml(String nombreCompleto) {

        String pacienteSeguro = escaparHtml(nombreCompleto);
        String telefonoSeguro = escaparHtml(telefonoHospital);

        StringBuilder html = new StringBuilder();

        // =================================================
        // ENCABEZADO INSTITUCIONAL
        // =================================================

        html.append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Confirmación de registro - SANTOS</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f2f6fa;font-family:Arial,Helvetica,sans-serif;color:#243247;">
                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%"
                           style="background-color:#f2f6fa;">
                        <tr>
                            <td align="center" style="padding:28px 12px;">
                                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%"
                                       style="max-width:620px;background-color:#ffffff;border:1px solid #dce6f0;border-radius:12px;">
                                    <tr>
                                        <td align="center" style="background-color:#123c72;padding:32px 24px;">
                                            <h1 style="color:#ffffff;font-size:26px;margin:0 0 8px;">SANTOS Clínica Médica</h1>
                                            <p style="color:#dbeafe;font-size:14px;margin:0;">Atención y servicios médicos</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px 28px;">
                                            <p style="font-size:13px;font-weight:bold;color:#15803d;margin:0 0 15px;">
                                                &#10003; REGISTRO CONFIRMADO
                                            </p>
                                            <h2 style="font-size:23px;color:#123c72;margin:0 0 20px;">
                                                ¡Bienvenido(a) a SANTOS!
                                            </h2>
                                            <p style="font-size:15px;line-height:1.7;">Estimado(a) <strong>
                """);

        html.append(pacienteSeguro);

        html.append("""
                                            </strong>:</p>
                                            <p style="font-size:15px;line-height:1.7;">
                                                Nos complace informarle que su registro en nuestro
                                                portal de pacientes se ha completado satisfactoriamente.
                                            </p>
                                            <p style="font-size:15px;line-height:1.7;">
                                                A partir de este momento, podrá consultar la disponibilidad
                                                y programar sus citas médicas desde el portal.
                                            </p>
                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%"
                                                   style="background-color:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;margin:24px 0;">
                                                <tr>
                                                    <td style="padding:20px;">
                                                        <h3 style="font-size:17px;color:#123c72;margin:0 0 14px;">
                                                            Información del registro
                                                        </h3>
                                                        <p style="font-size:14px;margin:0 0 10px;"><strong>Paciente:</strong>
                """);

        html.append(pacienteSeguro);

        html.append("""
                                                        </p>
                                                        <p style="font-size:14px;margin:0 0 10px;">
                                                            <strong>Servicio:</strong> Registro de usuario externo
                                                        </p>
                                                        <p style="font-size:14px;margin:0;color:#15803d;">
                                                            <strong>Estado:</strong> Confirmado
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="font-size:15px;line-height:1.7;">
                                                Agradecemos la confianza depositada en nuestra institución.
                                                Será un gusto acompañarle en el cuidado de su salud.
                                            </p>
                                            <p style="font-size:15px;line-height:1.7;margin-top:24px;">
                                                Atentamente,<br>
                                                <strong>Equipo de Atención al Paciente</strong><br>
                                                SANTOS Clínica Médica
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f8fafc;padding:22px 28px;border-top:1px solid #e2e8f0;">
                                            <p style="font-size:14px;color:#123c72;font-weight:bold;margin:0 0 8px;">
                                                ¿Necesita asistencia?
                                            </p>
                                            <p style="font-size:14px;line-height:1.7;margin:0;">
                                                Comuníquese con nuestra clínica al teléfono: <strong>
                """);

        html.append(telefonoSeguro);

        html.append("""
                                                </strong>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="background-color:#edf2f7;padding:22px 28px;">
                                            <p style="font-size:12px;color:#64748b;line-height:1.7;margin:0 0 8px;">
                                                Este mensaje se generó automáticamente. Por favor, no responda a este correo.
                                            </p>
                                            <p style="font-size:12px;color:#64748b;line-height:1.7;margin:0;">
                                                Si no reconoce este registro, comuníquese con nuestra institución.
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
    // PROTEGER VALORES INSERTADOS EN HTML
    // =====================================================

    private String escaparHtml(String texto) {
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
