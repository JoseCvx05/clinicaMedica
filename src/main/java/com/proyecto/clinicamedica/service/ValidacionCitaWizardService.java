package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;
import com.proyecto.clinicamedica.validator.cita.ValidadorPasoCita;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * =========================================================
 * SERVICIO: VALIDACIÓN DEL WIZARD DE CITAS
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Coordina las validaciones correspondientes
 * a cada paso del wizard.
 *
 * El polimorfismo se mantiene mediante:
 *
 * List<ValidadorPasoCita>
 *
 * Cada implementación de ValidadorPasoCita define:
 *
 * - El paso que valida.
 * - Su orden de ejecución.
 * - Su propia regla de validación.
 *
 * =========================================================
 */
@Service
public class ValidacionCitaWizardService {


    // =====================================================
    // VALIDADORES AGRUPADOS POR PASO
    // =====================================================

    private final Map<PasoCita, List<ValidadorPasoCita>>
            validadoresPorPaso;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ValidacionCitaWizardService(
            List<ValidadorPasoCita> validadores
    ) {

        this.validadoresPorPaso =
                construirMapaValidadores(
                        validadores
                );
    }


    // =====================================================
    // VALIDAR PASO
    // =====================================================

    public ResultadoValidacionCita validar(
            PasoCita paso,
            CitaWizardDTO formulario
    ) {

        ResultadoValidacionCita resultado =
                new ResultadoValidacionCita();


        // =================================================
        // VALIDAR PASO
        // =================================================

        if (paso == null) {

            resultado.agregarError(
                    "paso",
                    "No fue posible determinar el paso actual del agendamiento."
            );

            return resultado;
        }


        // =================================================
        // OBTENER VALIDADORES DEL PASO
        // =================================================

        List<ValidadorPasoCita> validadores =
                validadoresPorPaso
                        .getOrDefault(
                                paso,
                                List.of()
                        );


        // =================================================
        // EJECUTAR VALIDACIONES
        // =================================================

        for (ValidadorPasoCita validador :
                validadores) {

            validador.validar(
                    formulario,
                    resultado
            );
        }


        return resultado;
    }


    // =====================================================
    // CONSTRUIR MAPA DE VALIDADORES
    // =====================================================

    private Map<PasoCita, List<ValidadorPasoCita>>
    construirMapaValidadores(
            List<ValidadorPasoCita> validadores
    ) {

        Map<PasoCita, List<ValidadorPasoCita>> mapa =
                new EnumMap<>(
                        PasoCita.class
                );


        for (PasoCita paso :
                PasoCita.values()) {

            List<ValidadorPasoCita> validadoresDelPaso =
                    validadores
                            .stream()
                            .filter(
                                    validador ->
                                            validador.paso()
                                                    == paso
                            )
                            .sorted(
                                    Comparator.comparingInt(
                                            ValidadorPasoCita::orden
                                    )
                            )
                            .toList();


            mapa.put(
                    paso,
                    validadoresDelPaso
            );
        }


        return mapa;
    }
}