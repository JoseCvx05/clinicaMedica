package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cie10;
import com.proyecto.clinicamedica.repository.Cie10Repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * =========================================================
 * SERVICIO: CIE-10
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Responsabilidades:
 *
 * - Buscar sugerencias CIE-10.
 * - Limitar resultados del autocompletado.
 * - Consultar únicamente registros activos.
 * - Evitar búsquedas excesivamente abiertas.
 *
 * RNF:
 * El autocompletado debe responder en menos de 500 ms.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class Cie10Service {


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int MINIMO_CARACTERES =
            2;

    private static final int MAXIMO_RESULTADOS =
            15;


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final Cie10Repository
            cie10Repository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Cie10Service(
            Cie10Repository cie10Repository
    ) {

        this.cie10Repository =
                cie10Repository;
    }


    // =====================================================
    // BUSCAR SUGERENCIAS
    // =====================================================

    public List<SugerenciaCie10> buscarSugerencias(
            String texto
    ) {

        // =================================================
        // NORMALIZAR
        // =================================================

        String criterio =
                texto == null
                        ? ""
                        : texto.trim();


        // =================================================
        // EVITAR CONSULTAS DEMASIADO ABIERTAS
        // =================================================

        if (criterio.length()
                < MINIMO_CARACTERES) {

            return List.of();
        }


        // =================================================
        // LIMITAR RESULTADOS
        // =================================================

        Pageable limite =
                PageRequest.of(
                        0,
                        MAXIMO_RESULTADOS
                );


        // =================================================
        // CONSULTAR
        // =================================================

        return cie10Repository
                .buscarParaAutocompletado(
                        criterio,
                        limite
                )
                .stream()

                .map(
                        this::convertir
                )

                .toList();
    }


    // =====================================================
    // CONVERTIR
    // =====================================================

    private SugerenciaCie10 convertir(
            Cie10 cie10
    ) {

        return new SugerenciaCie10(

                cie10.getId(),

                cie10.getCodigo(),

                cie10.getDescripcion()
        );
    }


    // =====================================================
    // DTO DE RESPUESTA
    // =====================================================

    public record SugerenciaCie10(

            Integer id,

            String codigo,

            String descripcion

    ) {
    }
}