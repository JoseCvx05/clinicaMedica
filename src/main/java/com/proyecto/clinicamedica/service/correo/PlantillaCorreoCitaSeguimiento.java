package com.proyecto.clinicamedica.service.correo;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
/**
 * =========================================================
 * PLANTILLA: CITA DE SEGUIMIENTO
 * =========================================================
 *
 * CU-08 FA02
 * CU-11 - Agendamiento de Cita de Seguimiento
 *
 * Responsabilidades:
 *
 * - Construir el asunto del correo.
 * - Construir el cuerpo HTML.
 * - Escapar datos dinámicos antes de insertarlos en HTML.
 * - Cumplir RN-CU11-04.
 * - Cumplir RN-GLOBAL-006.
 *
 * NO realiza el envío del correo.
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoCitaSeguimiento {


    // =====================================================
    // CONFIGURACIÓN DE LA PLANTILLA
    // =====================================================

    private static final String NOMBRE_HOSPITAL =
            "Clínica Médica";


    /*
     * RN-GLOBAL-006 exige mostrar un teléfono.
     *
     * Actualmente dejamos el marcador definido por la
     * regla de negocio hasta que configuremos el número
     * oficial de la clínica.
     */
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
            ZoneId.of("America/Guatemala");

    // =====================================================
    // ASUNTO
    // RN-CU11-04
    // =====================================================

    public String construirAsunto() {

        return "Cita de Seguimiento Agendada - Hospital "
                + NOMBRE_HOSPITAL;
    }


    public String construirCuerpo(
            String nombrePaciente,
            String nombreMedico,
            String especialidad,
            String sucursal,
            OffsetDateTime fechaHora,
            String tipoSeguimiento,
            String motivo,
            String prioridad
    ) {
        return construirCuerpo(
                null,
                nombrePaciente,
                nombreMedico,
                especialidad,
                sucursal,
                fechaHora,
                tipoSeguimiento,
                motivo,
                prioridad
        );
    }



    // =====================================================
    // CUERPO HTML
    // RN-CU11-04
    // RN-GLOBAL-006
    // =====================================================


    public String construirCuerpo(
            Integer numeroCita,
            String nombrePaciente,
            String nombreMedico,
            String especialidad,
            String sucursal,
            OffsetDateTime fechaHora,
            String tipoSeguimiento,
            String motivo,
            String prioridad
    ) {


        // =================================================
        // DATOS SEGUROS PARA HTML
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


        String especialidadSegura =
                escapar(
                        limpiar(
                                especialidad,
                                "No especificada"
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


        String motivoSeguro =
                escapar(
                        limpiar(
                                motivo,
                                "No especificado"
                        )
                );


        String prioridadSegura =
                escapar(
                        limpiar(
                                prioridad,
                                "Normal"
                        )
                );


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


        String filaNumeroCita =
                numeroCita != null && numeroCita > 0
                        ? """
                    <tr>
                        <td style="
                            padding:7px 0;
                            font-weight:bold;
                            color:#546e7a;
                        ">
                            Número de cita
                        </td>

                        <td style="
                            padding:7px 0;
                            color:#263238;
                        ">
                            <strong>No. %d</strong>
                        </td>
                    </tr>
                    """.formatted(numeroCita)
                        : "";



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
                    <title>Cita de Seguimiento</title>
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
                                                Cita de Seguimiento
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
                                                Hola
                                                <strong>%s</strong>:
                                            </p>

                                            <p style="
                                                margin:0;
                                                font-size:15px;
                                                line-height:1.7;
                                                color:#455a64;
                                            ">
                                                Se ha agendado exitosamente
                                                una cita de seguimiento para
                                                continuar con su atención médica.
                                            </p>

                                        </td>
                                    </tr>


                                    <!-- =====================================
                                         DATOS DE LA CITA
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
                                                    Detalles de la cita
                                                </div>

                                                             <table width="100%%"
                                                                    cellpadding="0"
                                                                    cellspacing="0"
                                                                    border="0">
                
                                                                 <!-- CU-12: NÚMERO DE CITA -->
                                                                 %s
                
                                                                 <tr>
                                                                     <td style="
                                                                         padding:7px 0;
                                                                         font-weight:bold;
                                                                         color:#546e7a;
                                                                         width:42%%;
                                                                     ">
                                                                         Tipo de seguimiento
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
                                                            Médico
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
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
                                                            Especialidad
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
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
                                                            Sucursal
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
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
                                                            Fecha
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
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
                                                            Hora
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
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
                                                            Prioridad
                                                        </td>

                                                        <td style="
                                                            padding:7px 0;
                                                        ">
                                                            %s
                                                        </td>
                                                    </tr>

                                                </table>

                                            </div>

                                        </td>
                                    </tr>


                                    <!-- =====================================
                                         OBSERVACIONES
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
                                                    Observaciones del seguimiento
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
                                                Por favor, preséntese con
                                                anticipación para continuar
                                                con su atención médica.
                                            </p>

                                        </td>
                                    </tr>


                                    <!-- =====================================
                                         PIE RN-GLOBAL-006
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

                pacienteSeguro,

                filaNumeroCita,

                tipoSeguro,

                medicoSeguro,

                especialidadSegura,

                sucursalSegura,

                fecha,

                hora,

                prioridadSegura,

                motivoSeguro,

                telefonoSeguro
        );
    }


    // =====================================================
    // NOMBRE VISIBLE DEL TIPO DE SEGUIMIENTO
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