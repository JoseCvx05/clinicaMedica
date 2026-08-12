package com.proyecto.clinicamedica.event;

/**
 * =========================================================
 * EVENTO: USUARIO EXTERNO REGISTRADO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Contiene únicamente los datos necesarios para generar
 * y persistir la notificación de bienvenida.
 *
 * No contiene DPI, NIT ni contraseña.
 * =========================================================
 */
public record UsuarioExternoRegistradoEvent(

        Integer idUsuario,

        String nombreCompleto,

        String correoElectronico

) {
}