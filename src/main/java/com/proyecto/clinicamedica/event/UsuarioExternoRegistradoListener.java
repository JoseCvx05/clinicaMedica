package com.proyecto.clinicamedica.event;

import com.proyecto.clinicamedica.service.NotificacionRegistroService;

import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


/**
 * =========================================================
 * LISTENER: REGISTRO EXTERNO COMPLETADO
 * =========================================================
 *
 * Escucha el evento solamente DESPUÉS de que la
 * transacción de creación del paciente haya realizado
 * COMMIT correctamente.
 *
 * Esto impide enviar correos de registros que finalmente
 * no quedaron almacenados.
 * =========================================================
 */
@Component
public class UsuarioExternoRegistradoListener {


    private final NotificacionRegistroService
            notificacionRegistroService;


    public UsuarioExternoRegistradoListener(
            NotificacionRegistroService notificacionRegistroService
    ) {

        this.notificacionRegistroService =
                notificacionRegistroService;
    }


    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void procesarRegistroExitoso(
            UsuarioExternoRegistradoEvent evento
    ) {

        if (evento == null) {
            return;
        }


        notificacionRegistroService.enviarBienvenida(
                evento.idUsuario(),
                evento.nombreCompleto(),
                evento.correoElectronico()
        );
    }
}