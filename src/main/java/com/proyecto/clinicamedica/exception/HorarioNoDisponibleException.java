package com.proyecto.clinicamedica.exception;

/**
 * =========================================================
 * EXCEPCIÓN: HORARIO NO DISPONIBLE
 * =========================================================
 *
 * Se utiliza cuando un horario que estaba disponible
 * deja de estarlo antes de crear la reserva temporal.
 * =========================================================
 */
public class HorarioNoDisponibleException
        extends RuntimeException {


    public HorarioNoDisponibleException(
            String mensaje
    ) {

        super(mensaje);
    }
}