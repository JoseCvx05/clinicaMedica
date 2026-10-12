
package com.proyecto.clinicamedica.event;

import com.proyecto.clinicamedica.service.NotificacionSustitucionMedicamentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * =========================================================
 * CU-11 - FA02
 * LISTENER: DESPACHO CONFIRMADO EN FARMACIA
 * =========================================================
 *
 * Procesa las notificaciones después de que
 * PostgreSQL confirma la transacción del despacho.
 *
 * SEGURIDAD:
 *
 * - No se ejecuta si la transacción se revierte.
 * - No modifica inventario.
 * - No modifica recetas médicas.
 * - No revierte despachos confirmados.
 * - Delega la notificación al servicio correspondiente.
 *
 * =========================================================
 */
@Component
public class DespachoConfirmadoFarmaciaEventListener {

    // =====================================================
    // LOG
    // =====================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    DespachoConfirmadoFarmaciaEventListener.class
            );


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final NotificacionSustitucionMedicamentoService
            notificacionService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DespachoConfirmadoFarmaciaEventListener(
            NotificacionSustitucionMedicamentoService
                    notificacionService
    ) {

        this.notificacionService =
                notificacionService;
    }


    // =====================================================
    // PROCESAR DESPUÉS DEL COMMIT
    // =====================================================

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void procesarDespachoConfirmado(
            DespachoConfirmadoFarmaciaEvent evento
    ) {

        if (evento == null
                || evento.idDespacho() == null) {

            return;
        }

        try {

            // El servicio verificará si existen
            // sustituciones registradas.

            notificacionService.enviarNotificacion(
                    evento.idDespacho()
            );

        } catch (Exception ex) {

            // El despacho ya está confirmado.
            // Un error de correo no debe revertirlo.

            log.error(
                    "CU-11 FA02: error al procesar la "
                            + "notificación del despacho {}.",
                    evento.idDespacho(),
                    ex
            );
        }
    }
}
