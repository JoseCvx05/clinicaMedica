package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.security.ResultadoAutenticacion;

/**
 * =========================================================
 * SERVICIO: AUTENTICACIÓN
 * =========================================================
 *
 * Define el contrato del proceso de autenticación
 * utilizado por el portal de pacientes.
 *
 * CU-00:
 *
 * - Flujo normal:
 *      credenciales correctas + rol Paciente.
 *
 * - FA06:
 *      credenciales incorrectas.
 *
 * - FA07:
 *      cuenta bloqueada temporalmente.
 *
 * - FA09:
 *      usuario autenticado con rol no autorizado.
 *
 * Este servicio NO se encarga de:
 *
 * - Renderizar vistas.
 * - Manejar HTTP.
 * - Crear cookies.
 * - Generar directamente respuestas REST.
 *
 * La generación del JWT será delegada posteriormente
 * a JwtService.
 *
 * Permite aplicar:
 *
 * - SRP.
 * - DIP.
 * - Abstracción.
 * - Polimorfismo.
 * =========================================================
 */
public interface AutenticacionService {

    /**
     * Autentica a un usuario utilizando nombre de usuario
     * y contraseña.
     *
     * El método deberá:
     *
     * 1. Buscar el usuario.
     * 2. Revisar si existe un bloqueo vigente.
     * 3. Validar la contraseña mediante PasswordEncoder.
     * 4. Incrementar intentos si falla.
     * 5. Bloquear después del quinto intento fallido.
     * 6. Restablecer intentos si las credenciales son válidas.
     * 7. Verificar que el rol sea Paciente.
     * 8. Retornar el resultado correspondiente.
     *
     * @param nombreUsuario nombre de usuario ingresado
     * @param contrasena contraseña ingresada
     *
     * @return resultado interno de autenticación
     */
    ResultadoAutenticacion autenticar(
            String nombreUsuario,
            String contrasena
    );
}