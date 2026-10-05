package com.proyecto.clinicamedica.model;


/**
 * =========================================================
 * ESTADO DE CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Valores exactos admitidos actualmente por PostgreSQL:
 *
 * - En curso
 * - Finalizada
 *
 * =========================================================
 */
public enum EstadoConsultaMedica {


    EN_CURSO(
            "En curso"
    ),

    FINALIZADA(
            "Finalizada"
    );


    private final String valorBaseDatos;


    EstadoConsultaMedica(
            String valorBaseDatos
    ) {

        this.valorBaseDatos =
                valorBaseDatos;
    }


    public String getValorBaseDatos() {

        return valorBaseDatos;
    }


    public static EstadoConsultaMedica desdeValorBaseDatos(
            String valor
    ) {

        if (valor == null) {

            return null;
        }

        for (EstadoConsultaMedica estado :
                values()) {

            if (estado.valorBaseDatos
                    .equalsIgnoreCase(
                            valor.trim()
                    )) {

                return estado;
            }
        }

        throw new IllegalArgumentException(
                "Estado de consulta médica no válido: "
                        + valor
        );
    }
}