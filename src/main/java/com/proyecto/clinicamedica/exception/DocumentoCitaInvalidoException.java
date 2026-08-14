package com.proyecto.clinicamedica.exception;

public class DocumentoCitaInvalidoException
        extends RuntimeException {

    public DocumentoCitaInvalidoException(
            String mensaje
    ) {
        super(mensaje);
    }
}
