
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.SolicitudDespachoFarmaciaDTO;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * =========================================================
 * CU-11 - VALIDADOR DE SOLICITUD DE DESPACHO
 * =========================================================
 *
 * Comprueba que la solicitud tenga una estructura válida
 * antes de procesar el despacho en Farmacia.
 *
 * IMPORTANTE:
 *
 * No consulta PostgreSQL.
 * No registra despachos.
 * No modifica inventario.
 *
 * Las validaciones clínicas y de negocio se realizarán
 * posteriormente en el servicio transaccional.
 *
 * =========================================================
 */
@Component
public class FarmaciaSolicitudDespachoValidador {


    // =====================================================
    // VALIDAR SOLICITUD
    // =====================================================

    public void validar(
            SolicitudDespachoFarmaciaDTO solicitud
    ) {

        // -------------------------------------------------
        // SOLICITUD OBLIGATORIA
        // -------------------------------------------------

        if (solicitud == null) {

            throw new IllegalArgumentException(
                    "La solicitud de despacho es obligatoria."
            );
        }


        // -------------------------------------------------
        // RECETA MÉDICA
        // -------------------------------------------------

        if (solicitud.getIdReceta() == null
                || solicitud.getIdReceta() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una receta médica válida."
            );
        }


        // -------------------------------------------------
        // CLAVE DE OPERACIÓN
        // -------------------------------------------------

        UUID claveOperacion =
                solicitud.getClaveOperacion();

        if (claveOperacion == null) {

            throw new IllegalArgumentException(
                    "La clave de operación es obligatoria."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTOS OBLIGATORIOS
        // -------------------------------------------------

        if (solicitud.getMedicamentos() == null
                || solicitud.getMedicamentos().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un medicamento."
            );
        }


        // -------------------------------------------------
        // OBSERVACIONES
        // -------------------------------------------------

        if (solicitud.getObservaciones() != null
                && solicitud.getObservaciones()
                .trim()
                .length() > 1000) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar "
                            + "los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // DETECTAR DETALLES DUPLICADOS
        // -------------------------------------------------

        Set<Integer> detallesProcesados =
                new HashSet<>();


        // -------------------------------------------------
        // VALIDAR CADA MEDICAMENTO
        // -------------------------------------------------

        for (SolicitudDespachoFarmaciaDTO.LineaDespachoDTO linea
                : solicitud.getMedicamentos()) {

            if (linea == null) {

                throw new IllegalArgumentException(
                        "La solicitud contiene un medicamento inválido."
                );
            }


            // ---------------------------------------------
            // DETALLE DE RECETA
            // ---------------------------------------------

            Integer idDetalle =
                    linea.getIdDetalleReceta();

            if (idDetalle == null || idDetalle <= 0) {

                throw new IllegalArgumentException(
                        "El detalle de receta no es válido."
                );
            }


            // ---------------------------------------------
            // EVITAR DETALLES REPETIDOS
            // ---------------------------------------------

            if (!detallesProcesados.add(idDetalle)) {

                throw new IllegalArgumentException(
                        "No puede incluir dos veces el mismo "
                                + "detalle de receta en un despacho."
                );
            }


            // ---------------------------------------------
            // MEDICAMENTO ENTREGADO
            // ---------------------------------------------

            if (linea.getIdMedicamentoEntregado() == null
                    || linea.getIdMedicamentoEntregado() <= 0) {

                throw new IllegalArgumentException(
                        "Debe seleccionar un medicamento válido."
                );
            }


            // ---------------------------------------------
            // CANTIDAD
            // ---------------------------------------------

            if (linea.getCantidadEntregada() == null
                    || linea.getCantidadEntregada() <= 0) {

                throw new IllegalArgumentException(
                        "La cantidad a despachar debe ser "
                                + "mayor a cero."
                );
            }


            // ---------------------------------------------
            // JUSTIFICACIÓN DE SUSTITUCIÓN
            // ---------------------------------------------

            String razon =
                    linea.getRazonSustitucion();

            if (razon != null
                    && razon.trim().length() > 1000) {

                throw new IllegalArgumentException(
                        "La razón de sustitución no puede "
                                + "superar los 1000 caracteres."
                );
            }

            // ---------------------------------------------
            // CU-11 FA02 - VALIDAR SOLICITUD DE SUSTITUCIÓN
            // ---------------------------------------------
            //
            // Estas validaciones se realizan en el backend.
            //
            // JavaScript no determina si una sustitución
            // puede registrarse.
            //
            // ---------------------------------------------

            boolean sustitucionSolicitada =
                    linea.isSustitucionSolicitada();

            String razonNormalizada =
                    razon == null
                            ? ""
                            : razon.trim();


            // ---------------------------------------------
            // SUSTITUCIÓN CON JUSTIFICACIÓN OBLIGATORIA
            // ---------------------------------------------

            if (sustitucionSolicitada
                    && razonNormalizada.isEmpty()) {

                throw new IllegalArgumentException(
                        "Debe ingresar la razón de sustitución "
                                + "cuando solicita entregar "
                                + "un medicamento alternativo."
                );
            }


            // ---------------------------------------------
            // NO PERMITIR JUSTIFICACIÓN SIN SUSTITUCIÓN
            // ---------------------------------------------

            if (!sustitucionSolicitada
                    && !razonNormalizada.isEmpty()) {

                throw new IllegalArgumentException(
                        "No puede ingresar una razón de sustitución "
                                + "si no ha solicitado sustituir "
                                + "el medicamento."
                );
            }
        }
    }
}
