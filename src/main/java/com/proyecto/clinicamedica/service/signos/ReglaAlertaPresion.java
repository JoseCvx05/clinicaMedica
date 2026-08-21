package com.proyecto.clinicamedica.service.signos;
import org.springframework.stereotype.Component;


/**
 * =========================================================
 * ALERTA CLÍNICA: PRESIÓN ARTERIAL
 * =========================================================
 *
 * RN-CU07-06.
 *
 * Rango clínico normal:
 *
 * Sistólica:
 * 90 - 140 mmHg
 *
 * Diastólica:
 * 60 - 90 mmHg
 *
 * Estar fuera de este rango genera una alerta,
 * pero NO impide registrar los signos vitales.
 *
 * =========================================================
 */
@Component
public class ReglaAlertaPresion
        implements ReglaAlertaVital {


    public static final String CODIGO =
            "PRESION";


    private static final String MENSAJE =
            "Presión arterial fuera de rango normal.";


    @Override
    public EvaluacionAlertaVital evaluar(
            DatosSignosVitalesEntrada datos
    ) {

        if (datos == null
                || datos.getPresionSistolica() == null
                || datos.getPresionDiastolica() == null) {

            return new EvaluacionAlertaVital(
                    CODIGO,
                    false,
                    null
            );
        }


        int sistolica =
                datos.getPresionSistolica();


        int diastolica =
                datos.getPresionDiastolica();


        boolean fueraDeRango =
                sistolica < 90
                        || sistolica > 140
                        || diastolica < 60
                        || diastolica > 90;


        return new EvaluacionAlertaVital(

                CODIGO,

                fueraDeRango,

                fueraDeRango
                        ? MENSAJE
                        : null
        );
    }
}