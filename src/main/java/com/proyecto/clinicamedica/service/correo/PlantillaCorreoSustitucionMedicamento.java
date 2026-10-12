
package com.proyecto.clinicamedica.service.correo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * PLANTILLA DE CORREO: SUSTITUCIÓN DE MEDICAMENTOS
 * =========================================================
 *
 * CU-11 - FA02.
 *
 * Construye el correo HTML que recibirá el médico
 * tratante cuando se confirme un despacho que
 * incluya sustituciones de medicamentos.
 *
 * Esta clase:
 *
 * - No envía correos.
 * - No modifica inventario.
 * - No guarda registros en PostgreSQL.
 * - Solo construye el asunto y el contenido HTML.
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoSustitucionMedicamento {

    // =====================================================
    // DATOS DE UNA SUSTITUCIÓN
    // =====================================================

    public record Sustitucion(
            String medicamentoOriginal,
            String medicamentoEntregado,
            Integer cantidad,
            String razon
    ) {
    }


    // =====================================================
    // ASUNTO DEL CORREO
    // =====================================================

    public String construirAsunto() {

        return "Aviso de sustitución de medicamentos - SANTOS";
    }


    // =====================================================
    // CONSTRUIR CORREO HTML
    // =====================================================

    public String construirCuerpo(
            String nombreMedico,
            String nombrePaciente,
            Integer idReceta,
            Integer idDespacho,
            String nombreSucursal,
            List<Sustitucion> sustituciones
    ) {

        // Debe existir al menos una sustitución.

        if (sustituciones == null
                || sustituciones.isEmpty()) {

            throw new IllegalArgumentException(
                    "El correo requiere al menos una sustitución."
            );
        }

        Objects.requireNonNull(
                idReceta,
                "El identificador de receta es obligatorio."
        );

        Objects.requireNonNull(
                idDespacho,
                "El identificador de despacho es obligatorio."
        );


        StringBuilder html =
                new StringBuilder();


        // =================================================
        // ENCABEZADO
        // =================================================

        html.append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>

                <body style="
                    margin: 0;
                    padding: 25px;
                    background: #f2f6f8;
                    font-family: Arial, sans-serif;
                    color: #243746;
                ">

                <div style="
                    max-width: 680px;
                    margin: auto;
                    background: #ffffff;
                    border-radius: 14px;
                    overflow: hidden;
                    border: 1px solid #dce5ea;
                ">

                <div style="
                    background: #103846;
                    color: white;
                    padding: 25px;
                ">

                    <h1 style="
                        margin: 0;
                        font-size: 25px;
                    ">
                        SANTOS +
                    </h1>

                    <p style="
                        margin-bottom: 0;
                        color: #c9dce2;
                    ">
                        Sistema de Gestión Clínica
                        | Notificación de Farmacia
                    </p>

                </div>

                <div style="padding: 25px;">

                    <h2 style="color: #103846;">
                        Sustitución de medicamentos
                    </h2>
                """);


        // =================================================
        // INFORMACIÓN DEL MÉDICO
        // =================================================

        html.append("<p>Estimado(a) médico(a) <strong>")
                .append(escaparHtml(nombreMedico))
                .append("</strong>:</p>");


        html.append("""
                <p>
                    Le informamos que el personal de
                    Farmacia registró un despacho
                    con sustitución de medicamentos
                    correspondiente a una receta médica.
                </p>
                """);


        // =================================================
        // DATOS DEL DESPACHO
        // =================================================

        html.append("<p><strong>Paciente:</strong> ")
                .append(escaparHtml(nombrePaciente))
                .append("</p>");

        html.append("<p><strong>Receta médica:</strong> #")
                .append(idReceta)
                .append("</p>");

        html.append("<p><strong>Despacho:</strong> #")
                .append(idDespacho)
                .append("</p>");

        html.append("<p><strong>Sucursal:</strong> ")
                .append(escaparHtml(nombreSucursal))
                .append("</p>");


        // =================================================
        // TABLA DE SUSTITUCIONES
        // =================================================

        html.append("""
                <h3 style="color: #103846;">
                    Medicamentos sustituidos
                </h3>

                <table style="
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 13px;
                ">

                <thead>
                    <tr style="
                        background: #e8f4f1;
                        color: #103846;
                    ">
                        <th style="padding: 10px;">
                            Recetado
                        </th>

                        <th style="padding: 10px;">
                            Entregado
                        </th>

                        <th style="padding: 10px;">
                            Cantidad
                        </th>

                        <th style="padding: 10px;">
                            Justificación
                        </th>
                    </tr>
                </thead>

                <tbody>
                """);


        for (Sustitucion sustitucion : sustituciones) {

            Objects.requireNonNull(
                    sustitucion,
                    "No se permiten sustituciones nulas."
            );

            html.append("<tr>");

            html.append("<td style='padding:10px;border-bottom:1px solid #ddd;'>")
                    .append(escaparHtml(
                            sustitucion.medicamentoOriginal()
                    ))
                    .append("</td>");

            html.append("<td style='padding:10px;border-bottom:1px solid #ddd;'>")
                    .append(escaparHtml(
                            sustitucion.medicamentoEntregado()
                    ))
                    .append("</td>");

            html.append("<td style='padding:10px;border-bottom:1px solid #ddd;'>")
                    .append(sustitucion.cantidad())
                    .append("</td>");

            html.append("<td style='padding:10px;border-bottom:1px solid #ddd;'>")
                    .append(escaparHtml(
                            sustitucion.razon()
                    ))
                    .append("</td>");

            html.append("</tr>");
        }


        // =================================================
        // PIE DEL CORREO
        // =================================================

        html.append("""
                </tbody>
                </table>

                <p style="
                    margin-top: 25px;
                    line-height: 1.6;
                ">
                    Este aviso permite al médico tratante
                    conocer los medicamentos entregados
                    como sustitución y la justificación
                    registrada por Farmacia.
                </p>

                <div style="
                    background: #edf7f5;
                    border-left: 4px solid #0c9484;
                    padding: 14px;
                    margin-top: 20px;
                    font-size: 13px;
                ">
                    Esta es una notificación automática
                    del sistema Clínica Médica SANTOS.
                </div>

                </div>
                </div>
                </body>
                </html>
                """);


        return html.toString();
    }


    // =====================================================
    // ESCAPAR TEXTO PARA HTML
    // =====================================================

    private String escaparHtml(
            String texto
    ) {

        if (texto == null) {
            return "";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
