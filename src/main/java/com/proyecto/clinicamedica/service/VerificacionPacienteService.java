package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.VerificacionDpiResponse;

/**
 * =========================================================
 * SERVICIO: VERIFICACIÓN DE PACIENTE
 * =========================================================
 *
 * Define el contrato encargado de verificar si un DPI:
 *
 * - Pertenece a un paciente registrado.
 * - No se encuentra registrado.
 * - Pertenece a un usuario interno.
 *
 * Corresponde al CU-00:
 *
 * - Flujo normal.
 * - FA03.
 * - FA04.
 *
 * IMPORTANTE:
 *
 * Este servicio NO valida el formato del DPI.
 *
 * La validación RN-GLOBAL-001 se realiza previamente
 * mediante @DpiValido.
 *
 * Tampoco maneja:
 *
 * - Contraseñas.
 * - Intentos fallidos.
 * - Bloqueo temporal.
 * - Generación de JWT.
 *
 * Estas responsabilidades pertenecen al servicio
 * de autenticación.
 *
 * Aplica:
 *
 * - SRP.
 * - DIP.
 * - Abstracción.
 * - Polimorfismo.
 * =========================================================
 */
public interface VerificacionPacienteService {

    /**
     * Verifica el DPI ingresado por el Usuario Externo.
     *
     * El valor recibido ya debe haber superado
     * RN-GLOBAL-001.
     *
     * Flujo interno:
     *
     * DPI
     *  ↓
     * HashService
     *  ↓
     * HMAC-SHA-256
     *  ↓
     * UsuarioService
     *  ↓
     * Validación del rol
     *  ↓
     * VerificacionDpiResponse
     *
     * @param dpi DPI válido de 13 dígitos
     * @return resultado de la verificación
     */
    VerificacionDpiResponse verificar(String dpi);
}