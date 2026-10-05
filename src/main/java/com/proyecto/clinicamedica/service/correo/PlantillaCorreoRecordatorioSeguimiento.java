package com.proyecto.clinicamedica.service.correo;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * =========================================================
 * PLANTILLA: RECORDATORIO DE CITA DE SEGUIMIENTO
 * =========================================================
 *
 * RN-CU11-05.
 *
 * Construye:
 *
 * - Asunto exacto del recordatorio.
 * - Cuerpo HTML.
 * - Fecha y hora en zona Guatemala.
 * - Tipo de seguimiento.
 * - Médico.
 * - Sucursal.
 *
 * No realiza envíos.
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoRecordatorioSeguimiento {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of(
                    "America/Guatemala"
            );


    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
            );


    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern(
                    "HH:mm"
            );


    /*
     * Mantener el mismo teléfono/configuración utilizada
     * en el resto de plantillas.
     *
     * Cuando tengamos el número oficial puede moverse
     * a application.properties.
     */
    private static final String TELEFONO_CONTACTO =
            "[número]";


    // =====================================================
    // ASUNTO
    // RN-CU11-05
    // =====================================================

    public String construirAsunto() {

        return "Recordatorio: Su Cita de Seguimiento Mañana";
    }


    // =====================================================
    // CUERPO HTML
    // =====================================================

    public String construirCuerpo(

            String nombrePaciente,

            String nombreMedico,

            String sucursal,

            OffsetDateTime fechaHora,

            String tipoSeguimiento

    ) {

        // =================================================
        // DATOS SEGUROS
        // =================================================

        String pacienteSeguro =
                escapar(
                        limpiar(
                                nombrePaciente,
                                "Paciente"
                        )
                );


        String medicoSeguro =
                escapar(
                        limpiar(
                                nombreMedico,
                                "Médico"
                        )
                );


        String sucursalSegura =
                escapar(
                        limpiar(
                                sucursal,
                                "No especificada"
                        )
                );


        String tipoSeguro =
                escapar(
                        obtenerNombreTipo(
                                tipoSeguimiento
                        )
                );


        // =================================================
        // ZONA HORARIA GUATEMALA
        // =================================================

        ZonedDateTime fechaHoraGuatemala =
                fechaHora != null
                        ? fechaHora.atZoneSameInstant(
                        ZONA_GUATEMALA
                )
                        : null;


        String fecha =
                fechaHoraGuatemala != null
                        ? escapar(
                        fechaHoraGuatemala.format(
                                FORMATO_FECHA
                        )
                )
                        : "No especificada";


        String hora =
                fechaHoraGuatemala != null
                        ? escapar(
                        fechaHoraGuatemala.format(
                                FORMATO_HORA
                        )
                )
                        : "No especificada";


        String telefonoSeguro =
                escapar(
                        TELEFONO_CONTACTO
                );


        // =================================================
        // HTML
        // =================================================

        return """
                <!DOCTYPE html>
                <html lang="es">

                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>
                        Recordatorio de Cita de Seguimiento
                    </title>
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background-color:#f3f6f9;
                    font-family:Arial, Helvetica, sans-serif;
                    color:#263238;
                ">

                    <table width="100%%"
                           cellpadding="0"
                           cellspacing="0"
                           border="0"
                           style="
                               width:100%%;
                               background-color:#f3f6f9;
                               padding:30px 15px;
                           ">

                        <tr>
                            <td align="center">

                                <table width="620"
                                       cellpadding="0"
                                       cellspacing="0"
                                       border="0"
                                       style="
                                           width:100%%;
                                           max-width:620px;
                                           background:#ffffff;
                                           border-radius:14px;
                                           overflow:hidden;
                                           box-shadow:
                                               0 4px 18px rgba(0,0,0,0.08);
                                       ">

                                    <!-- =============================
                                         ENCABEZADO
                                         ============================= -->

                                    <tr>
                                        <td style="
                                            background:#17324d;
                                            color:#ffffff;
                                            padding:28px 30px;
                                            text-align:center;
                                        ">

                                            <div style="
                                                font-size:34px;
                                                margin-bottom:8px;
                                            ">
                                                ✚
                                            </div>

                                            <div style="
                                                font-size:22px;
                                                font-weight:bold;
                                            ">
                                                Clínica Médica
                                            </div>

                                            <div style="
                                                font-size:14px;
                                                margin-top:6px;
                                                opacity:0.9;
                                            ">
                                                Recordatorio de Cita
                                            </div>

                                        </td>
                                    </tr>


                                    <!-- =============================
                                         MENSAJE
                                         ============================= -->

                                    <tr>
                                        <td style="
                                            padding:32px 32px 18px 32px;
                                        ">

                                            <p style="
                                                margin:0 0 15px 0;
                                                font-size:16px;
                                                line-height:1.6;
                                            ">
                                                Hola
                                                <strong>%s</strong>:
                                            </p>

                                            <h2 style="
                                                margin:0 0 15px 0;
                                                color:#17324d;
                                                font-size:22px;
                                            ">
                                                Su cita de seguimiento
                                                es mañana
                                            </h2>

                                            <p style="
                                                margin:0;
                                                color:#546e7a;
                                                font-size:15px;
                                                line-height:1.7;
                                            ">
                                                Le recordamos que tiene
                                                programada una cita de
                                                seguimiento médico.
                                            </p>

                                        </td>
                                    </tr>


                                    <!-- =============================
                                         DATOS
                                         ============================= -->

                                    <tr>
                                        <td style="
                                            padding:10px 32px 25px 32px;
                                        ">

                                            <div style="
                                                background:#f7fafc;
                                                border:1px solid #e2e8ee;
                                                border-radius:10px;
                                                padding:22px;
                                            ">

                                                <div style="
                                                    font-size:17px;
                                                    font-weight:bold;
                                                    color:#17324d;
                                                    margin-bottom:16px;
                                                ">
                                                    Detalles de su cita
                                                </div>


                                                <table width="100%%"
                                                       cellpadding="0"
                                                       cellspacing="0"
                                                       border="0">

                                                    <tr>
                                                        <td style="
                                                            padding:8px 0;
                                                            font-weight:bold;
                                                            color:#607d8b;
                                                            width:42%%;
                                                        ">
                                                            Fecha
                                                        </td>

                                                        <td style="
                                                            padding:8px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>


                                                    <tr>
                                                        <td style="
                                                            padding:8px 0;
                                                            font-weight:bold;
                                                            color:#607d8b;
                                                        ">
                                                            Hora
                                                        </td>

                                                        <td style="
                                                            padding:8px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>


                                                    <tr>
                                                        <td style="
                                                            padding:8px 0;
                                                            font-weight:bold;
                                                            color:#607d8b;
                                                        ">
                                                            Tipo de seguimiento
                                                        </td>

                                                        <td style="
                                                            padding:8px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>


                                                    <tr>
                                                        <td style="
                                                            padding:8px 0;
                                                            font-weight:bold;
                                                            color:#607d8b;
                                                        ">
                                                            Médico
                                                        </td>

                                                        <td style="
                                                            padding:8px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>


                                                    <tr>
                                                        <td style="
                                                            padding:8px 0;
                                                            font-weight:bold;
                                                            color:#607d8b;
                                                        ">
                                                            Sucursal
                                                        </td>

                                                        <td style="
                                                            padding:8px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>

                                                </table>

                                            </div>

                                        </td>
                                    </tr>


                                    <!-- =============================
                                         INDICACIÓN
                                         ============================= -->

                                    <tr>
                                        <td style="
                                            padding:0 32px 30px 32px;
                                        ">

                                            <div style="
                                                background:#eef6fb;
                                                border-left:4px solid #1f6b9b;
                                                padding:16px;
                                                border-radius:6px;
                                                color:#455a64;
                                                font-size:14px;
                                                line-height:1.6;
                                            ">

                                                Le recomendamos presentarse
                                                con anticipación para continuar
                                                con su atención médica.

                                            </div>

                                        </td>
                                    </tr>


                                    <!-- =============================
                                         PIE
                                         ============================= -->

                                    <tr>
                                        <td style="
                                            background:#eef2f5;
                                            padding:22px 28px;
                                            text-align:center;
                                            border-top:1px solid #dde5ea;
                                        ">

                                            <p style="
                                                margin:0;
                                                font-size:12px;
                                                line-height:1.7;
                                                color:#687b8d;
                                            ">
                                                Este es un correo automático
                                                del Sistema Informático
                                                Hospitalario.
                                                No responda a este mensaje.
                                                Para consultas, comuníquese
                                                al teléfono %s.
                                            </p>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>

                    </table>

                </body>
                </html>
                """.formatted(

                pacienteSeguro,

                fecha,

                hora,

                tipoSeguro,

                medicoSeguro,

                sucursalSegura,

                telefonoSeguro
        );
    }


    // =====================================================
    // TIPO VISIBLE
    // =====================================================

    private String obtenerNombreTipo(
            String tipoSeguimiento
    ) {

        if (tipoSeguimiento == null
                || tipoSeguimiento.isBlank()) {

            return "Seguimiento médico";
        }


        return switch (
                tipoSeguimiento.trim()
                ) {

            case "MONITOREO_TRATAMIENTO" ->
                    "Monitoreo de Tratamiento";

            case "REVISION_RESULTADOS_LABORATORIO" ->
                    "Revisión de Resultados de Laboratorio";

            default ->
                    "Seguimiento médico";
        };
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor,
            String valorDefecto
    ) {

        if (valor == null
                || valor.isBlank()) {

            return valorDefecto;
        }


        return valor.trim();
    }


    // =====================================================
    // ESCAPAR HTML
    // =====================================================

    private String escapar(
            String valor
    ) {

        return HtmlUtils.htmlEscape(
                valor != null
                        ? valor
                        : ""
        );
    }
}