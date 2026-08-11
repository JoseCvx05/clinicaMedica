package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ESTADO DEL INICIO DE SESIÓN
 * =========================================================
 *
 * Representa los posibles resultados del proceso de
 * autenticación del paciente en el CU-00.
 *
 * Evita utilizar cadenas de texto dispersas como:
 *
 * "EXITO"
 * "ERROR"
 * "BLOQUEADO"
 *
 * y permite manejar los flujos de forma tipada.
 * =========================================================
 */
public enum EstadoLogin {

    /**
     * Credenciales correctas y rol Paciente.
     *
     * Flujo normal:
     * se genera JWT y se continúa al dashboard.
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
     * Se alcanzaron los 5 intentos fallidos o la
     * cuenta todavía se encuentra dentro del período
     * de bloqueo de 15 minutos.
     */
    CUENTA_BLOQUEADA,


    /**
     * FA09.
     *
     * Las credenciales son correctas, pero el usuario
     * no tiene rol Paciente.
     */
    ROL_NO_AUTORIZADO
}