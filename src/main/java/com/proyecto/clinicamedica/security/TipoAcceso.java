package com.proyecto.clinicamedica.security;

/**
 * =========================================================
 * TIPO DE ACCESO AL SISTEMA
 * =========================================================
 *
 * Define desde qué portal se está intentando realizar
 * la autenticación.
 *
 * Esto permite reutilizar el mismo proceso de:
 *
 * - Búsqueda del usuario.
 * - Validación de contraseña.
 * - Intentos fallidos.
 * - Bloqueo temporal.
 *
 * y cambiar únicamente las reglas de autorización
 * correspondientes a cada tipo de acceso.
 *
 * =========================================================
 */
public enum TipoAcceso {

    /**
     * Portal público utilizado exclusivamente
     * por usuarios con rol Paciente.
     */
    PACIENTE,


    /**
     * Portal utilizado por personal interno:
     *
     * - Médico
     * - Enfermero
     * - Recepcionista
     * - Cajero
     * - Laboratorista
     * - Farmacéutico
     * - Administrador
     *
     * La autorización específica de cada módulo
     * se realizará posteriormente con roles.
     *
     * Por ejemplo:
     *
     * /admin/usuarios/**
     *
     * requerirá ROLE_ADMINISTRADOR.
     */
    INTERNO
}