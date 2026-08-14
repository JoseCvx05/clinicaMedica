package com.proyecto.clinicamedica.model.cita;

/**
 * =========================================================
 * PASOS DEL WIZARD DE AGENDAMIENTO
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Evita utilizar números mágicos dentro de Controllers
 * y Services.
 * =========================================================
 */
public enum PasoCita {

    SUCURSAL(1),

    ESPECIALIDAD(2),

    MEDICO(3),

    FECHA_HORA(4),

    CONFIRMACION(5);


    private final int numero;


    PasoCita(
            int numero
    ) {

        this.numero =
                numero;
    }


    public int getNumero() {

        return numero;
    }


    public PasoCita anterior() {

        return switch (this) {

            case SUCURSAL ->
                    SUCURSAL;

            case ESPECIALIDAD ->
                    SUCURSAL;

            case MEDICO ->
                    ESPECIALIDAD;

            case FECHA_HORA ->
                    MEDICO;

            case CONFIRMACION ->
                    FECHA_HORA;
        };
    }


    public PasoCita siguiente() {

        return switch (this) {

            case SUCURSAL ->
                    ESPECIALIDAD;

            case ESPECIALIDAD ->
                    MEDICO;

            case MEDICO ->
                    FECHA_HORA;

            case FECHA_HORA,
                 CONFIRMACION ->
                    CONFIRMACION;
        };
    }
}