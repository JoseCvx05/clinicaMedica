package com.proyecto.clinicamedica.integration.lis;

import com.proyecto.clinicamedica.dto.ResultadoLisDTO;

import org.springframework.stereotype.Component;

import java.util.List;


/**
 * =========================================================
 * GATEWAY LIS: SIN INTEGRACIÓN CONFIGURADA
 * =========================================================
 *
 * RNF-030
 *
 * Implementación segura utilizada mientras el hospital
 * no tenga conectado un analizador o sistema LIS real.
 *
 * OBJETIVO:
 *
 * - Permitir que la arquitectura soporte LIS.
 * - No afectar el funcionamiento actual de CU-09.
 * - No generar conexiones ficticias.
 * - No lanzar errores únicamente porque no existe
 *   hardware LIS instalado.
 *
 * En el futuro podrán agregarse implementaciones como:
 *
 * - Hl7LisGateway
 * - AstmLisGateway
 * - RestLisGateway
 *
 * sin modificar esta interfaz ni la lógica clínica.
 *
 * =========================================================
 */
@Component
public class LisNoDisponibleGateway
        implements LisGateway {


    // =====================================================
    // NOMBRE DE LA INTEGRACIÓN
    // =====================================================

    @Override
    public String obtenerNombreIntegracion() {

        return "SIN_LIS_CONFIGURADO";
    }


    // =====================================================
    // DISPONIBILIDAD
    // =====================================================
    //
    // Actualmente no existe ningún analizador físico
    // conectado al sistema.
    //
    // =====================================================

    @Override
    public boolean estaDisponible() {

        return false;
    }


    // =====================================================
    // RESULTADOS PENDIENTES
    // =====================================================
    //
    // No devuelve null.
    //
    // Esto evita NullPointerException en los servicios
    // que posteriormente consuman esta integración.
    //
    // =====================================================

    @Override
    public List<ResultadoLisDTO>
    obtenerResultadosPendientes() {

        return List.of();
    }


    // =====================================================
    // CONFIRMAR PROCESAMIENTO
    // =====================================================
    //
    // Al no existir un LIS conectado no hay ningún
    // sistema externo al cual confirmar recepción.
    //
    // Por seguridad se deja como operación vacía.
    //
    // =====================================================

    @Override
    public void confirmarProcesamiento(
            String idMensajeLis
    ) {

        // No existe integración LIS activa actualmente.
    }
}