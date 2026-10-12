
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * =========================================================
 * SERVICE: VALIDACION DE RECETAS PARA FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Responsabilidades:
 *
 * - Verificar acceso autorizado a la receta.
 * - Comprobar que la receta esté activa.
 * - Validar fecha de emisión.
 * - Aplicar vigencia máxima de 7 días.
 * - Verificar que la consulta esté finalizada.
 * - Impedir despachar recetas vencidas.
 *
 * IMPORTANTE:
 *
 * Todas estas validaciones se hacen en Java.
 *
 * El HTML únicamente mostrará los resultados.
 *
 * Las validaciones de inventario y medicamentos
 * se implementarán en el Service de Despacho.
 *
 * =========================================================
 */
@Service
public class FarmaciaRecetaValidacionService {

    // =====================================================
    // VIGENCIA MAXIMA
    // =====================================================

    private static final long DIAS_VIGENCIA = 7;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FarmaciaSeguridadService
            farmaciaSeguridadService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaRecetaValidacionService(
            FarmaciaSeguridadService farmaciaSeguridadService
    ) {

        this.farmaciaSeguridadService =
                farmaciaSeguridadService;
    }


    // =====================================================
    // RESULTADO DE VIGENCIA
    // =====================================================

    public record VigenciaReceta(
            boolean vigente,
            long diasTranscurridos
    ) {
    }


    // =====================================================
    // CONSULTAR VIGENCIA
    // =====================================================
    //
    // Permite mostrar recetas activas vencidas
    // dentro de la búsqueda de Farmacia.
    //
    // NO autoriza el despacho.
    //
    // =====================================================

    public VigenciaReceta consultarVigencia(
            RecetaMedica receta
    ) {

        // -------------------------------------------------
        // SEGURIDAD POR USUARIO Y SUCURSAL
        // -------------------------------------------------

        farmaciaSeguridadService
                .validarAccesoReceta(receta);


        // -------------------------------------------------
        // RECETA ACTIVA
        // -------------------------------------------------

        if (!receta.estaActiva()) {

            throw new IllegalStateException(
                    "La receta médica no se encuentra activa."
            );
        }


        // -------------------------------------------------
        // FECHA DE EMISION
        // -------------------------------------------------

        if (receta.getFechaEmision() == null) {

            throw new IllegalStateException(
                    "La receta médica no tiene una fecha de emisión válida."
            );
        }


        Instant emision =
                receta.getFechaEmision().toInstant();

        Instant ahora =
                Instant.now();


        // -------------------------------------------------
        // IMPEDIR FECHAS FUTURAS
        // -------------------------------------------------

        if (emision.isAfter(ahora)) {

            throw new IllegalStateException(
                    "La fecha de emisión de la receta no puede ser futura."
            );
        }


        // -------------------------------------------------
        // CALCULAR DIAS TRANSCURRIDOS
        // -------------------------------------------------

        long diasTranscurridos =
                Duration.between(
                        emision,
                        ahora
                ).toDays();


        // -------------------------------------------------
        // VIGENCIA MAXIMA: 7 DIAS
        // -------------------------------------------------
        //
        // La receta vence al superar exactamente
        // siete días desde su emisión.
        //
        // =================================================

        Instant fechaVencimiento =
                emision.plus(
                        DIAS_VIGENCIA,
                        ChronoUnit.DAYS
                );

        boolean vigente =
                !ahora.isAfter(fechaVencimiento);


        return new VigenciaReceta(
                vigente,
                diasTranscurridos
        );
    }


    // =====================================================
    // VALIDAR RECETA ANTES DEL DESPACHO
    // =====================================================
    //
    // Este método será obligatorio en el Service
    // transaccional de Despacho.
    //
    // =====================================================

    public void validarParaDespacho(
            RecetaMedica receta
    ) {

        // -------------------------------------------------
        // VALIDAR SEGURIDAD, ESTADO Y VIGENCIA
        // -------------------------------------------------

        VigenciaReceta vigencia =
                consultarVigencia(receta);


        // -------------------------------------------------
        // RECETA VENCIDA
        // -------------------------------------------------

        if (!vigencia.vigente()) {

            throw new IllegalStateException(
                    "Receta Vencida. La receta #"
                            + receta.getId()
                            + " fue emitida hace "
                            + vigencia.diasTranscurridos()
                            + " días y ya no es válida para despacho."
            );
        }


        // -------------------------------------------------
        // CONSULTA MEDICA VALIDA
        // -------------------------------------------------

        if (receta.getConsultaMedica() == null
                || receta.getConsultaMedica().getId() == null) {

            throw new IllegalStateException(
                    "La receta no está asociada a una consulta médica válida."
            );
        }


        // -------------------------------------------------
        // CONSULTA FINALIZADA
        // -------------------------------------------------

        if (receta.getConsultaMedica().getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada para despachar medicamentos."
            );
        }
    }
}
