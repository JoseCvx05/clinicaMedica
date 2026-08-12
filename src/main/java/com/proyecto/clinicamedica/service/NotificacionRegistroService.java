package com.proyecto.clinicamedica.service;

/**
 * =========================================================
 * SERVICIO: NOTIFICACIÓN DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Gestiona el correo de bienvenida del paciente.
 * =========================================================
 */
public interface NotificacionRegistroService {

    void enviarBienvenida(
            Integer idUsuario,
            String nombreCompleto,
            String correoElectronico
    );
}