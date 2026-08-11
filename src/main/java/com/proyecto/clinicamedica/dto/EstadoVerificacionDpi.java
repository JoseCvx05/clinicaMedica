package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ESTADO DE VERIFICACIÓN DE DPI
 * =========================================================
 *
 * Representa los posibles resultados obtenidos después
 * de verificar el DPI ingresado en el CU-00.
 *
 * Permite evitar el uso de textos "mágicos" como:
 *
 * "PACIENTE"
 * "NO_ENCONTRADO"
 * "INTERNO"
 *
 * dispersos por diferentes partes del sistema.
 * =========================================================
 */
public enum EstadoVerificacionDpi {

    /**
     * El DPI pertenece a un usuario registrado
     * con rol Paciente.
     *
     * Flujo:
     * continuar hacia el inicio de sesión.
     */
    PACIENTE_REGISTRADO,


    /**
     * No existe ningún usuario asociado al DPI.
     *
     * Corresponde al flujo alterno FA03.
     *
     * Flujo:
     * redirigir al registro de paciente (CU-02).
     */
    NO_REGISTRADO,


    /**
     * El DPI existe, pero pertenece a un usuario
     * interno del hospital.
     *
     * Corresponde al flujo alterno FA04.
     *
     * Ejemplos:
     * - Médico
     * - Enfermero
     * - Recepcionista
     * - Administrador
     */
    USUARIO_INTERNO
}