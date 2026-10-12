
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.AutorizacionMedicamentoControlado;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;

import com.proyecto.clinicamedica.repository.AutorizacionMedicamentoControladoRepository;

import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * =========================================================
 * SERVICE: VALIDACION DE MEDICAMENTOS CONTROLADOS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * RNF-017:
 *
 * Los medicamentos controlados requieren una
 * autorizacion adicional del medico responsable.
 *
 * Este servicio:
 *
 * - Identifica medicamentos controlados.
 * - Verifica la autorizacion medica.
 * - Comprueba el medico responsable.
 * - Valida la cantidad autorizada.
 * - Impide sustituciones de controlados.
 *
 * IMPORTANTE:
 *
 * No registra autorizaciones.
 * No registra despachos.
 * No modifica inventario.
 *
 * Debe utilizarse desde el servicio transaccional
 * de despacho, antes de entregar medicamentos.
 *
 * =========================================================
 */
@Service
public class FarmaciaMedicamentoControladoValidacionService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final AutorizacionMedicamentoControladoRepository
            autorizacionRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaMedicamentoControladoValidacionService(
            AutorizacionMedicamentoControladoRepository
                    autorizacionRepository
    ) {

        this.autorizacionRepository =
                autorizacionRepository;
    }


    // =====================================================
    // VALIDAR MEDICAMENTO ANTES DEL DESPACHO
    // =====================================================

    public void validarParaDespacho(
            DetalleRecetaMedica detalleReceta,
            Medicamento medicamentoEntregado,
            Integer cantidadEntregada,
            long cantidadEntregadaAnteriormente
    ) {


        // -------------------------------------------------
        // DETALLE DE RECETA VALIDO
        // -------------------------------------------------

        if (detalleReceta == null
                || detalleReceta.getId() == null
                || detalleReceta.getRecetaMedica() == null
                || detalleReceta.getRecetaMedica().getId() == null) {

            throw new IllegalStateException(
                    "El detalle de receta no es válido."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ORIGINAL
        // -------------------------------------------------

        Medicamento medicamentoOriginal =
                detalleReceta.getMedicamento();

        if (medicamentoOriginal == null
                || medicamentoOriginal.getId() == null) {

            throw new IllegalStateException(
                    "El medicamento original de la receta no es válido."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ENTREGADO
        // -------------------------------------------------

        if (medicamentoEntregado == null
                || medicamentoEntregado.getId() == null) {

            throw new IllegalStateException(
                    "Debe seleccionar un medicamento válido."
            );
        }


        // -------------------------------------------------
        // CANTIDAD SOLICITADA
        // -------------------------------------------------

        if (cantidadEntregada == null
                || cantidadEntregada <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad a entregar debe ser mayor a cero."
            );
        }

        if (cantidadEntregadaAnteriormente < 0) {

            throw new IllegalStateException(
                    "La cantidad entregada anteriormente no puede ser negativa."
            );
        }


        // -------------------------------------------------
        // IDENTIFICAR MEDICAMENTOS CONTROLADOS
        // -------------------------------------------------

        boolean originalControlado =
                medicamentoOriginal.esControlado();

        boolean entregadoControlado =
                medicamentoEntregado.esControlado();


        // -------------------------------------------------
        // IDENTIFICAR SUSTITUCION
        // -------------------------------------------------

        boolean esSustitucion =
                !Objects.equals(
                        medicamentoOriginal.getId(),
                        medicamentoEntregado.getId()
                );


        // -------------------------------------------------
        // BLOQUEAR SUSTITUCION DE CONTROLADOS
        // -------------------------------------------------
        //
        // No se permite sustituir un medicamento
        // controlado por otro producto.
        //
        // Tampoco se permite entregar un medicamento
        // controlado como sustituto de uno normal.
        //
        // Una justificacion escrita no reemplaza
        // la autorizacion del medico.
        //
        // -------------------------------------------------

        if (esSustitucion
                && (originalControlado || entregadoControlado)) {

            throw new IllegalStateException(
                    "No se permite sustituir medicamentos "
                            + "controlados durante el despacho. "
                            + "Se requiere una prescripcion "
                            + "o autorizacion clinica especifica."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO NO CONTROLADO
        // -------------------------------------------------
        //
        // No necesita autorizacion especial.
        //
        // Las validaciones normales de sustitucion,
        // receta e inventario corresponden al
        // servicio principal de despacho.
        //
        // -------------------------------------------------

        if (!originalControlado) {

            return;
        }


        // -------------------------------------------------
        // BUSCAR AUTORIZACION MEDICA
        // -------------------------------------------------

        AutorizacionMedicamentoControlado autorizacion =
                autorizacionRepository
                        .findByDetalleRecetaMedica_Id(
                                detalleReceta.getId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El medicamento controlado "
                                                + "requiere autorizacion "
                                                + "del medico responsable."
                                )
                        );


        // -------------------------------------------------
        // AUTORIZACION DEL DETALLE CORRECTO
        // -------------------------------------------------

        if (autorizacion.getDetalleRecetaMedica() == null
                || !Objects.equals(
                autorizacion
                        .getDetalleRecetaMedica()
                        .getId(),
                detalleReceta.getId()
        )) {

            throw new IllegalStateException(
                    "La autorizacion no corresponde "
                            + "al medicamento de la receta."
            );
        }


        // -------------------------------------------------
        // RECETA MEDICA
        // -------------------------------------------------

        RecetaMedica receta =
                detalleReceta.getRecetaMedica();


        // -------------------------------------------------
        // MEDICO RESPONSABLE
        // -------------------------------------------------

        if (receta.getMedico() == null
                || receta.getMedico().getId() == null) {

            throw new IllegalStateException(
                    "La receta no tiene un medico responsable válido."
            );
        }


        // -------------------------------------------------
        // MEDICO AUTORIZADOR
        // -------------------------------------------------

        if (autorizacion.getMedicoAutorizador() == null
                || autorizacion
                .getMedicoAutorizador()
                .getId() == null) {

            throw new IllegalStateException(
                    "La autorizacion no tiene un medico válido."
            );
        }


        // -------------------------------------------------
        // MISMO MEDICO QUE EMITIO LA RECETA
        // -------------------------------------------------

        if (!Objects.equals(
                receta.getMedico().getId(),
                autorizacion.getMedicoAutorizador().getId()
        )) {

            throw new IllegalStateException(
                    "El medicamento controlado no fue "
                            + "autorizado por el medico "
                            + "responsable de la receta."
            );
        }


        // -------------------------------------------------
        // CANTIDAD AUTORIZADA
        // -------------------------------------------------

        Integer cantidadAutorizada =
                autorizacion.getCantidadAutorizada();


        if (cantidadAutorizada == null
                || cantidadAutorizada <= 0) {

            throw new IllegalStateException(
                    "El medicamento controlado no tiene "
                            + "una cantidad autorizada válida."
            );
        }


        // -------------------------------------------------
        // NO SUPERAR CANTIDAD AUTORIZADA
        // -------------------------------------------------

        long totalDespachado =
                cantidadEntregadaAnteriormente
                        + cantidadEntregada.longValue();

        if (totalDespachado > cantidadAutorizada.longValue()) {

            throw new IllegalStateException(
                    "La cantidad solicitada supera "
                            + "la autorizacion medica."
            );
        }


        // -------------------------------------------------
        // NO SUPERAR CANTIDAD PRESCRITA
        // -------------------------------------------------

        if (detalleReceta.getCantidad() == null
                || totalDespachado
                > detalleReceta.getCantidad()) {

            throw new IllegalStateException(
                    "La cantidad solicitada supera "
                            + "la cantidad prescrita."
            );
        }


        // -------------------------------------------------
        // VALIDACION EXITOSA
        // -------------------------------------------------
        //
        // La autorizacion es compatible con esta
        // linea del despacho.
        //
        // El servicio principal todavía deberá
        // comprobar:
        //
        // - Cantidad previamente entregada.
        // - Vigencia de receta.
        // - Firma digital de receta.
        // - Inventario disponible.
        // - Farmaceutico autorizado.
        // - Sucursal correcta.
        //
        // -------------------------------------------------
    }
}
