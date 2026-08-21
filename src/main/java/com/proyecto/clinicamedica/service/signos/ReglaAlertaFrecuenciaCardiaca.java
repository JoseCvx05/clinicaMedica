package com.proyecto.clinicamedica.service.signos;

import org.springframework.stereotype.Component;


/**
 * =========================================================
 * ALERTA CLÍNICA: FRECUENCIA CARDÍACA
 * =========================================================
 *
 * RN-CU07-06.
 *
 * Rango clínico normal:
 *
 * 60 - 100 lpm.
 *
 * =========================================================
 */
@Component
public class ReglaAlertaFrecuenciaCardiaca
        implements ReglaAlertaVital {


    public static final String CODIGO =
            "FRECUENCIA_CARDIACA";


    private static final String MENSAJE =
            "Frecuencia cardíaca fuera de rango normal.";


    @Override
    public EvaluacionAlertaVital evaluar(
            DatosSignosVitalesEntrada datos
    ) {

        if (datos == null
                || datos.getFrecuenciaCardiaca() == null) {

            return new EvaluacionAlertaVital(
                    CODIGO,
                    false,
                    null
            );
        }


        int frecuencia =
                datos.getFrecuenciaCardiaca();


        boolean fueraDeRango =
                frecuencia < 60
                        || frecuencia > 100;


        return new EvaluacionAlertaVital(

                CODIGO,

                fueraDeRango,

                fueraDeRango
                        ? MENSAJE
                        : null
        );
    }
}