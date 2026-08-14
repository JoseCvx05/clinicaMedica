package com.proyecto.clinicamedica.event;

import com.proyecto.clinicamedica.service.NotificacionPagoService;

import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


/**
 * =========================================================
 * LISTENER: PAGO APROBADO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Ejecuta el envío del comprobante únicamente después
 * de que la transacción del pago haya sido confirmada
 * correctamente en PostgreSQL.
 *
 * =========================================================
 */
@Component
public class PagoAprobadoListener {


    private final NotificacionPagoService
            notificacionPagoService;


    public PagoAprobadoListener(
            NotificacionPagoService notificacionPagoService
    ) {

        this.notificacionPagoService =
                notificacionPagoService;
    }


    // =====================================================
    // DESPUÉS DEL COMMIT
    // =====================================================

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void manejarPagoAprobado(
            PagoAprobadoEvent evento
    ) {

        if (evento == null
                || evento.idPago() == null) {

            return;
        }


        notificacionPagoService
                .enviarComprobante(
                        evento.idPago()
                );
    }
}