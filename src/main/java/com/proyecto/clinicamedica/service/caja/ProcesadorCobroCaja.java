package com.proyecto.clinicamedica.service.caja;

import com.proyecto.clinicamedica.model.caja.ResultadoProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.SolicitudProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;


/**
 * =========================================================
 * CONTRATO: PROCESADOR DE COBRO EN CAJA
 * =========================================================
 *
 * CU-06.
 *
 * Strategy Pattern.
 *
 * Cada implementación conoce únicamente las reglas
 * específicas de su medio de cobro.
 *
 * Implementaciones:
 *
 * - ProcesadorCobroEfectivo
 * - ProcesadorCobroTarjeta
 *
 * =========================================================
 */
public interface ProcesadorCobroCaja {


    /**
     * Determina si esta estrategia puede procesar
     * el tipo de cobro indicado.
     */
    boolean soporta(
            TipoCobroCaja tipoCobro
    );


    /**
     * Procesa la operación.
     */
    ResultadoProcesamientoCobro procesar(
            SolicitudProcesamientoCobro solicitud
    );
}