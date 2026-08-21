package com.proyecto.clinicamedica.service.caja;

import com.proyecto.clinicamedica.model.caja.ResultadoPos;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import java.math.BigDecimal;


/**
 * =========================================================
 * CONTRATO: TERMINAL POS
 * =========================================================
 *
 * CU-06.
 *
 * Abstrae la comunicación con el dispositivo/proveedor POS.
 *
 * Actualmente:
 *
 * TerminalPosSimulada
 *
 * Futuro:
 *
 * TerminalPosProveedorReal
 *
 * Esto permite sustituir la integración sin modificar
 * ProcesadorCobroTarjeta ni CobroCajaService.
 *
 * =========================================================
 */
public interface TerminalPos {


    ResultadoPos procesar(

            BigDecimal monto,

            TipoCobroCaja tipoTarjeta,

            String ultimos4Tarjeta
    );
}