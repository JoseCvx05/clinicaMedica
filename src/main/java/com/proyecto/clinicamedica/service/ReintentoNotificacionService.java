package com.proyecto.clinicamedica.service;

/**
 * =========================================================
 * SERVICIO: REINTENTO DE NOTIFICACIONES
 * =========================================================
 *
 * Procesa correos pendientes o fallidos que todavía
 * pueden volver a intentarse.
 *
 * =========================================================
 */
public interface ReintentoNotificacionService {

    void procesarPendientes();
}