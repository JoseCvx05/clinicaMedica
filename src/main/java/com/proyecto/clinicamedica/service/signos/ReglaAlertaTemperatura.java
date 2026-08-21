package com.proyecto.clinicamedica.service.signos;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


/**
 * =========================================================
 * ALERTA CLÍNICA: TEMPERATURA
 * =========================================================
 *
 * RN-CU07-06.
 *
 * Rango clínico normal:
 *
 * 36.0 - 37.5 °C.
 *
 * =========================================================
 */
@Component
public class ReglaAlertaTemperatura
        implements ReglaAlertaVital {


    public static final String CODIGO =
            "TEMPERATURA";


    private static final String MENSAJE =
            "Temperatura fuera de rango normal.";


    private static final BigDecimal MINIMO_NORMAL =
            new BigDecimal("36.0");


    private static final BigDecimal MAXIMO_NORMAL =
            new BigDecimal("37.5");


    @Override
    public EvaluacionAlertaVital evaluar(
            DatosSignosVitalesEntrada datos
    ) {

        if (datos == null
                || datos.getTemperatura() == null) {

            return new EvaluacionAlertaVital(
                    CODIGO,
                    false,
                    null
            );
        }


        BigDecimal temperatura =
                datos.getTemperatura();


        boolean fueraDeRango =
                temperatura.compareTo(
                        MINIMO_NORMAL
                ) < 0

                        || temperatura.compareTo(
                        MAXIMO_NORMAL
                ) > 0;


        return new EvaluacionAlertaVital(

                CODIGO,

                fueraDeRango,

                fueraDeRango
                        ? MENSAJE
                        : null
        );
    }
}