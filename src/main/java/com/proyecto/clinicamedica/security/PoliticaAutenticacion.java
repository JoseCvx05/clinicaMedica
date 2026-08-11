package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.entity.Usuario;

/**
 * =========================================================
 * STRATEGY: POLÍTICA DE AUTENTICACIÓN
 * =========================================================
 *
 * Define las reglas que cambian dependiendo del portal
 * desde el cual un usuario intenta iniciar sesión.
 *
 * La lógica común de autenticación permanecerá en:
 *
 * AutenticacionServiceImpl
 *
 * Esa lógica común incluye:
 *
 * - Buscar usuario.
 * - Verificar estado activo.
 * - Revisar bloqueo.
 * - Validar contraseña con BCrypt.
 * - Incrementar intentos.
 * - Aplicar bloqueo temporal.
 * - Reiniciar intentos al autenticar correctamente.
 *
 * Esta estrategia solamente define lo que cambia:
 *
 * - Qué tipo de usuario puede utilizar el portal.
 * - Mensajes mostrados.
 * - Redirección después del login.
 *
 * =========================================================
 */
public interface PoliticaAutenticacion {

    /**
     * Identifica el tipo de acceso al que pertenece
     * esta estrategia.
     */
    TipoAcceso getTipoAcceso();


    /**
     * Indica si el usuario autenticado puede utilizar
     * este portal.
     *
     * @param usuario usuario con credenciales válidas
     *
     * @return true si puede utilizar el portal
     */
    boolean permiteAcceso(
            Usuario usuario
    );


    /**
     * Mensaje mostrado cuando las credenciales
     * son incorrectas.
     *
     * Cada portal puede tener el texto requerido
     * por sus reglas de negocio.
     *
     * @param intentosRestantes intentos disponibles
     *
     * @return mensaje para mostrar al usuario
     */
    String getMensajeCredencialesIncorrectas(
            int intentosRestantes
    );


    /**
     * Mensaje mostrado cuando la cuenta se encuentra
     * temporalmente bloqueada.
     */
    String getMensajeCuentaBloqueada();


    /**
     * Mensaje mostrado cuando las credenciales son
     * correctas pero el usuario intenta utilizar un
     * portal que no corresponde a su tipo de cuenta.
     */
    String getMensajeRolNoAutorizado();


    /**
     * Mensaje mostrado después de una autenticación
     * satisfactoria.
     */
    String getMensajeAutenticacionExitosa();


    /**
     * Ruta a la que se enviará al usuario después
     * de una autenticación satisfactoria.
     *
     * Se recibe Usuario porque una implementación
     * puede decidir la ruta según su rol.
     *
     * Ejemplos futuros:
     *
     * Paciente:
     * /paciente/dashboard
     *
     * Médico:
     * /interno/medico/dashboard
     *
     * Administrador:
     * /admin/dashboard
     */
    String getRedireccionExitosa(
            Usuario usuario
    );
}