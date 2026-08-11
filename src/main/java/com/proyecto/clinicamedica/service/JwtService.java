package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

import java.time.Duration;

/**
 * =========================================================
 * SERVICIO: JWT
 * =========================================================
 *
 * Define el contrato utilizado por el sistema para
 * generar tokens JWT después de una autenticación
 * satisfactoria.
 *
 * IMPORTANTE:
 *
 * Este servicio NO valida:
 *
 * - Usuario y contraseña.
 * - Intentos fallidos.
 * - Bloqueo temporal.
 * - Rol autorizado para iniciar sesión.
 *
 * Esas responsabilidades pertenecen a
 * AutenticacionService.
 *
 * JwtService solamente trabaja con usuarios que ya
 * fueron autenticados correctamente.
 *
 * La validación posterior de los JWT será realizada
 * por Spring Security mediante JwtDecoder.
 *
 * Aplica:
 *
 * - Abstracción
 * - Polimorfismo
 * - DIP de SOLID
 * =========================================================
 */
public interface JwtService {

    /**
     * Genera un JWT firmado para un usuario que ya fue
     * autenticado correctamente.
     *
     * El token contendrá únicamente la información
     * necesaria para identificar y autorizar al usuario.
     *
     * No incluirá:
     *
     * - Contraseña.
     * - contrasenaHash.
     * - DPI.
     * - dpiHash.
     * - dpiCifrado.
     * - NIT.
     * - nitHash.
     * - nitCifrado.
     *
     * @param usuario usuario autenticado
     * @return JWT firmado
     */
    String generarToken(Usuario usuario);


    /**
     * Devuelve el período durante el cual el token
     * permanece vigente.
     *
     * Nos servirá posteriormente para configurar
     * correctamente la cookie HttpOnly.
     *
     * @return duración del JWT
     */
    Duration obtenerDuracionToken();
}