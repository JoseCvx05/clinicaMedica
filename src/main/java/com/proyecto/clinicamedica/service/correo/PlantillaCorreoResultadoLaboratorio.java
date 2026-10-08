package com.proyecto.clinicamedica.service.correo;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * =========================================================
 * PLANTILLA: RESULTADO DE LABORATORIO PUBLICADO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Responsabilidades:
 *
 * - Construir el asunto del correo.
 * - Construir el cuerpo HTML.
 * - Escapar datos dinámicos.
 * - Informar al Médico sobre un resultado publicado.
 * - Mostrar alerta cuando el resultado está fuera de rango.
 *
 * IMPORTANTE:
 *
 * Esta clase NO envía correos.
 *
 * El envío será responsabilidad de:
 *
 * NotificacionResultadoLaboratorioService
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoResultadoLaboratorio {


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final String NOMBRE_HOSPITAL =
            "Clínica Médica";


    private static final String TELEFONO_CONTACTO =
            "[número]";


    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
            );


    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern(
                    "HH:mm"
            );


    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of(
                    "America/Guatemala"
            );


    // =====================================================
    // ASUNTO
    // =====================================================

    public String construirAsunto() {

        return "Resultado de Laboratorio Publicado - "
                + NOMBRE_HOSPITAL;
    }


    // =====================================================
    // CUERPO HTML
    // =====================================================

    public String construirCuerpo(

            String nombreMedico,

            String nombrePaciente,

            String numeroOrden,

            String nombreExamen,

            String valorResultado,

            String unidadResultado,

            String rangoReferencia,

            boolean fueraRango,

            String notasResultado,

            OffsetDateTime fechaResultado,

            OffsetDateTime fechaHoraPublicacion

    ) {


        // =================================================
        // DATOS SEGUROS
        // =================================================

        String medicoSeguro =
                escapar(
                        limpiar(
                                nombreMedico,
                                "Médico"
                        )
                );


        String pacienteSeguro =
                escapar(
                        limpiar(
                                nombrePaciente,
                                "Paciente"
                        )
                );


        String numeroOrdenSeguro =
                escapar(
                        limpiar(
                                numeroOrden,
                                "No especificada"
                        )
                );


        String examenSeguro =
                escapar(
                        limpiar(
                                nombreExamen,
                                "Examen de laboratorio"
                        )
                );


        String valorSeguro =
                escapar(
                        limpiar(
                                valorResultado,
                                "No especificado"
                        )
                );


        String unidadSegura =
                escapar(
                        limpiar(
                                unidadResultado,
                                ""
                        )
                );


        String rangoSeguro =
                escapar(
                        limpiar(
                                rangoReferencia,
                                "No especificado"
                        )
                );


        String notasSeguras =
                escapar(
                        limpiar(
                                notasResultado,
                                "Sin notas adicionales."
                        )
                );


        String telefonoSeguro =
                escapar(
                        TELEFONO_CONTACTO
                );


        // =================================================
        // FECHA DEL RESULTADO
        // =================================================

        ZonedDateTime fechaResultadoGuatemala =
                fechaResultado != null

                        ? fechaResultado
                        .atZoneSameInstant(
                                ZONA_GUATEMALA
                        )

                        : null;


        String fechaResultadoTexto =
                fechaResultadoGuatemala != null

                        ? escapar(
                        fechaResultadoGuatemala
                                .format(
                                        FORMATO_FECHA
                                )
                )

                        : "No registrada";


        String horaResultadoTexto =
                fechaResultadoGuatemala != null

                        ? escapar(
                        fechaResultadoGuatemala
                                .format(
                                        FORMATO_HORA
                                )
                )

                        : "No registrada";


        // =================================================
        // FECHA DE PUBLICACIÓN
        // =================================================

        ZonedDateTime publicacionGuatemala =
                fechaHoraPublicacion != null

                        ? fechaHoraPublicacion
                        .atZoneSameInstant(
                                ZONA_GUATEMALA
                        )

                        : null;


        String fechaPublicacion =
                publicacionGuatemala != null

                        ? escapar(
                        publicacionGuatemala
                                .format(
                                        FORMATO_FECHA
                                )
                )

                        : "No registrada";


        String horaPublicacion =
                publicacionGuatemala != null

                        ? escapar(
                        publicacionGuatemala
                                .format(
                                        FORMATO_HORA
                                )
                )

                        : "No registrada";


        // =================================================
        // ALERTA DE RANGO
        // =================================================

        String tituloEstado =
                fueraRango

                        ? "⚠ Resultado fuera de rango"

                        : "✓ Resultado dentro del control registrado";


        String colorEstado =
                fueraRango

                        ? "#b91c1c"

                        : "#166534";


        String fondoEstado =
                fueraRango

                        ? "#fff1f2"

                        : "#f0fdf4";


        String bordeEstado =
                fueraRango

                        ? "#fecdd3"

                        : "#bbf7d0";


        String mensajeEstado =
                fueraRango

                        ? "El resultado fue marcado por Laboratorio "
                        + "como fuera del rango de referencia. "
                        + "Se recomienda revisar el resultado clínicamente."

                        : "El resultado no fue marcado por Laboratorio "
                        + "como fuera del rango de referencia.";


        // =================================================
        // HTML
        // =================================================

        return """
                <!DOCTYPE html>
                <html lang="es">

                <head>

                    <meta charset="UTF-8">

                    <meta
                            name="viewport"
                            content="width=device-width, initial-scale=1.0">

                    <title>
                        Resultado de Laboratorio Publicado
                    </title>

                </head>


                <body style="
                    margin:0;
                    padding:0;
                    background-color:#f3f6f9;
                    font-family:Arial, Helvetica, sans-serif;
                    color:#263238;
                ">


                    <table
                            width="100%%"
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


                                <table
                                        width="620"
                                        cellpadding="0"
                                        cellspacing="0"
                                        border="0"
                                        style="
                                            width:100%%;
                                            max-width:620px;
                                            background:#ffffff;
                                            border-radius:14px;
                                            overflow:hidden;
                                            box-shadow:0 4px 18px rgba(0,0,0,0.08);
                                        ">


                                    <!-- =====================================
                                         ENCABEZADO
                                         ===================================== -->

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
                                                margin-bottom:5px;
                                            ">
                                                Clínica Médica
                                            </div>


                                            <div style="
                                                font-size:14px;
                                                opacity:0.9;
                                            ">
                                                Resultado de Laboratorio Publicado
                                            </div>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         SALUDO
                                         ===================================== -->

                                    <tr>

                                        <td style="
                                            padding:30px 32px 15px 32px;
                                        ">

                                            <p style="
                                                margin:0 0 16px 0;
                                                font-size:16px;
                                                line-height:1.6;
                                            ">

                                                Doctor(a)
                                                <strong>%s</strong>:

                                            </p>


                                            <p style="
                                                margin:0;
                                                font-size:15px;
                                                line-height:1.7;
                                                color:#455a64;
                                            ">

                                                Se ha publicado un nuevo
                                                resultado de laboratorio
                                                correspondiente al paciente

                                                <strong>
                                                    %s
                                                </strong>.

                                            </p>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         IDENTIFICACIÓN
                                         ===================================== -->

                                    <tr>

                                        <td style="
                                            padding:15px 32px;
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
                                                    margin-bottom:18px;
                                                ">
                                                    Información del resultado
                                                </div>


                                                <table
                                                        width="100%%"
                                                        cellpadding="0"
                                                        cellspacing="0"
                                                        border="0">


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                            width:42%%;
                                                        ">
                                                            Orden
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#263238;
                                                        ">
                                                            %s
                                                        </td>

                                                    </tr>


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                        ">
                                                            Examen
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#263238;
                                                            font-weight:bold;
                                                        ">
                                                            %s
                                                        </td>

                                                    </tr>


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                        ">
                                                            Resultado
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#0f766e;
                                                            font-size:17px;
                                                            font-weight:bold;
                                                        ">

                                                            %s %s

                                                        </td>

                                                    </tr>


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                        ">
                                                            Rango de referencia
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#263238;
                                                        ">
                                                            %s
                                                        </td>

                                                    </tr>


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                        ">
                                                            Fecha del resultado
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#263238;
                                                        ">
                                                            %s %s
                                                        </td>

                                                    </tr>


                                                    <tr>

                                                        <td style="
                                                            padding:7px 0;
                                                            font-weight:bold;
                                                            color:#546e7a;
                                                        ">
                                                            Publicado
                                                        </td>


                                                        <td style="
                                                            padding:7px 0;
                                                            color:#263238;
                                                        ">
                                                            %s %s
                                                        </td>

                                                    </tr>


                                                </table>

                                            </div>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         ESTADO DEL RESULTADO
                                         ===================================== -->

                                    <tr>

                                        <td style="
                                            padding:10px 32px 15px 32px;
                                        ">


                                            <div style="
                                                border:1px solid %s;
                                                background:%s;
                                                padding:17px 18px;
                                                border-radius:9px;
                                            ">


                                                <div style="
                                                    font-weight:bold;
                                                    color:%s;
                                                    margin-bottom:7px;
                                                ">
                                                    %s
                                                </div>


                                                <div style="
                                                    font-size:14px;
                                                    line-height:1.6;
                                                    color:#455a64;
                                                ">
                                                    %s
                                                </div>


                                            </div>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         NOTAS
                                         ===================================== -->

                                    <tr>

                                        <td style="
                                            padding:10px 32px 15px 32px;
                                        ">


                                            <div style="
                                                border-left:4px solid #1f6b9b;
                                                background:#eef6fb;
                                                padding:16px 18px;
                                                border-radius:6px;
                                            ">


                                                <div style="
                                                    font-weight:bold;
                                                    color:#17324d;
                                                    margin-bottom:7px;
                                                ">
                                                    Notas del Laboratorio
                                                </div>


                                                <div style="
                                                    font-size:14px;
                                                    line-height:1.6;
                                                    color:#455a64;
                                                ">
                                                    %s
                                                </div>


                                            </div>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         INDICACIÓN
                                         ===================================== -->

                                    <tr>

                                        <td style="
                                            padding:15px 32px 30px 32px;
                                        ">


                                            <p style="
                                                margin:0;
                                                font-size:14px;
                                                line-height:1.7;
                                                color:#546e7a;
                                            ">

                                                El resultado ya se encuentra
                                                disponible para consulta dentro
                                                del expediente médico del paciente.

                                            </p>

                                        </td>

                                    </tr>


                                    <!-- =====================================
                                         PIE
                                         ===================================== -->

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

                                                Este es un correo automático del
                                                Sistema Informático Hospitalario.
                                                No responda a este mensaje.
                                                Para consultas, comuníquese al
                                                teléfono %s.

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

                medicoSeguro,

                pacienteSeguro,

                numeroOrdenSeguro,

                examenSeguro,

                valorSeguro,

                unidadSegura,

                rangoSeguro,

                fechaResultadoTexto,

                horaResultadoTexto,

                fechaPublicacion,

                horaPublicacion,

                bordeEstado,

                fondoEstado,

                colorEstado,

                escapar(
                        tituloEstado
                ),

                escapar(
                        mensajeEstado
                ),

                notasSeguras,

                telefonoSeguro
        );
    }


    // =====================================================
    // LIMPIAR TEXTO
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