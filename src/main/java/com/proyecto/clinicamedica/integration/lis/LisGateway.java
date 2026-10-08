package com.proyecto.clinicamedica.integration.lis;

import com.proyecto.clinicamedica.dto.ResultadoLisDTO;

import java.util.List;


/**
 * =========================================================
 * GATEWAY: INTEGRACIÓN CON LIS
 * =========================================================
 *
 * RNF-030
 *
 * Define el contrato que deberá cumplir cualquier
 * integración futura con analizadores de laboratorio
 * o sistemas LIS.
 *
 * IMPORTANTE:
 *
 * La lógica principal de CU-09 NO debe conocer:
 *
 * - Marca del analizador.
 * - Protocolo HL7.
 * - Protocolo ASTM.
 * - API REST.
 * - Dirección IP del equipo.
 * - Librerías específicas del fabricante.
 *
 * Todo eso pertenecerá a una implementación concreta
 * de esta interfaz.
 *
 * Ejemplos futuros:
 *
 * Hl7LisGateway
 * AstmLisGateway
 * RestLisGateway
 *
 * =========================================================
 */
public interface LisGateway {


    // =====================================================
    // IDENTIFICAR INTEGRACIÓN
    // =====================================================
    //
    // Permite saber qué conector LIS está trabajando.
    //
    // Ejemplo:
    //
    // "HL7"
    // "ASTM"
    // "REST"
    //
    // =====================================================

    String obtenerNombreIntegracion();


    // =====================================================
    // DISPONIBILIDAD
    // =====================================================
    //
    // Permite comprobar si el sistema externo se encuentra
    // disponible antes de intentar obtener resultados.
    //
    // =====================================================

    boolean estaDisponible();


    // =====================================================
    // CONSULTAR RESULTADOS PENDIENTES
    // =====================================================
    //
    // La implementación concreta obtiene resultados desde
    // el analizador y los transforma a ResultadoLisDTO.
    //
    // El resto del sistema solamente trabaja con el DTO
    // normalizado.
    //
    // =====================================================

    List<ResultadoLisDTO> obtenerResultadosPendientes();


    // =====================================================
    // CONFIRMAR PROCESAMIENTO
    // =====================================================
    //
    // Se utilizará únicamente después de que nuestro
    // sistema haya procesado correctamente el resultado.
    //
    // Esto permitirá que una futura implementación LIS
    // marque el mensaje como recibido/procesado.
    //
    // =====================================================

    void confirmarProcesamiento(
            String idMensajeLis
    );
}