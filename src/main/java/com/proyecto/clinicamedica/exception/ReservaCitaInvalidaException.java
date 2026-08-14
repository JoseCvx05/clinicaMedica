package com.proyecto.clinicamedica.exception;

/**
 * =========================================================
 * EXCEPCIÓN: RESERVA DE CITA INVÁLIDA
 * =========================================================
 *
 * Indica que los datos enviados para crear una reserva
 * temporal no corresponden con una selección válida
 * del CU-03.
 * =========================================================
 */
public class ReservaCitaInvalidaException
        extends RuntimeException {


    public ReservaCitaInvalidaException(
            String mensaje
    ) {

        super(mensaje);
    }
}