package com.proyecto.clinicamedica.service.signos;
/**
 * =========================================================
 * CONTRATO: REGLA DE ALERTA DE SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - FA03.
 *
 * Cada implementación evalúa un signo vital específico
 * para determinar si se encuentra fuera del rango
 * clínico normal.
 *
 * IMPORTANTE:
 *
 * Estas reglas NO validan el rango permitido de captura.
 *
 * Ejemplo:
 *
 * Presión 160/95
 *
 * - Es válida para captura.
 * - Genera alerta clínica.
 *
 * Presión 300/95
 *
 * - Es inválida para captura.
 * - Debe ser rechazada antes de evaluar alertas.
 *
 * =========================================================
 */
public interface ReglaAlertaVital {


    /**
     * Evalúa la regla clínica correspondiente.
     */
    EvaluacionAlertaVital evaluar(
            DatosSignosVitalesEntrada datos
    );


    /**
     * Resultado normalizado de cualquier regla clínica.
     *
     * Evitamos crear una clase adicional únicamente
     * para transportar este pequeño resultado.
     */
    record EvaluacionAlertaVital(

            String codigo,

            boolean alerta,

            String mensaje

    ) {
    }
}