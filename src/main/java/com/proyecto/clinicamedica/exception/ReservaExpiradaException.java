package com.proyecto.clinicamedica.exception;

/**
 * =========================================================
 * EXCEPCIÓN: RESERVA EXPIRADA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Se utiliza cuando el tiempo de una reserva temporal
 * finaliza antes de que el paciente confirme la cita.
 *
 * =========================================================
 */
public class ReservaExpiradaException
        extends RuntimeException {


    public ReservaExpiradaException(
            String mensaje
    ) {

        super(mensaje);
    }
}