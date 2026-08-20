package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ESTADO DEL INICIO DE SESIÓN
 * =========================================================
 *
 * Representa los posibles resultados del proceso de
 * autenticación del sistema.
 *
 * Se utiliza tanto para:
 *
 * - Portal de pacientes.
 * - Portal del personal interno.
 *
 * Evita utilizar cadenas de texto dispersas como:
 *
 * "EXITO"
 * "ERROR"
 * "BLOQUEADO"
 * "INACTIVO"
 *
 * y permite manejar los flujos de forma tipada.
 *
 * =========================================================
 */
public enum EstadoLogin {


    /**
     * Credenciales correctas, cuenta activa
     * y rol autorizado para el portal solicitado.
     *
     * Flujo normal:
     *
     * - Se genera el JWT.
     * - Se permite el acceso.
     * - Se redirige al dashboard correspondiente.
     */
    AUTENTICADO,


    /**
     * FA06.
     *
     * Nombre de usuario o contraseña incorrectos,
     * pero todavía quedan intentos disponibles.
     */
    CREDENCIALES_INCORRECTAS,


    /**
     * FA07.
     *
     * Se alcanzaron los 5 intentos fallidos
     * o la cuenta todavía se encuentra dentro
     * del período de bloqueo de 15 minutos.
     */
    CUENTA_BLOQUEADA,


    /**
     * CUENTA INACTIVA.
     *
     * Las credenciales proporcionadas son correctas,
     * pero el usuario tiene:
     *
     * activo = false
     *
     * Por lo tanto, no puede iniciar sesión
     * ni acceder al sistema.
     *
     * La inactividad no se considera por sí misma
     * un intento fallido de autenticación.
     */
    CUENTA_INACTIVA,


    /**
     * FA09.
     *
     * Las credenciales son correctas y la cuenta
     * está activa, pero el usuario no posee un rol
     * autorizado para el portal desde el cual
     * intenta autenticarse.
     */
    ROL_NO_AUTORIZADO
}