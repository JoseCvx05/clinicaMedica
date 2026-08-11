package com.proyecto.clinicamedica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * DTO: RESPUESTA DE INICIO DE SESIÓN
 * =========================================================
 *
 * Representa la respuesta enviada al frontend después
 * de intentar autenticar a un paciente.
 *
 * Permite manejar:
 *
 * - Flujo normal: autenticación exitosa.
 * - FA06: credenciales incorrectas.
 * - FA07: cuenta bloqueada.
 * - FA09: rol no autorizado.
 *
 * IMPORTANTE:
 *
 * El JWT NO se devuelve dentro de este DTO.
 * Posteriormente será enviado mediante una cookie
 * protegida.
 *
 * Tampoco se exponen:
 *
 * - Contraseña.
 * - Hash de contraseña.
 * - DPI.
 * - Hash del DPI.
 * - NIT.
 * =========================================================
 */
@Getter
@AllArgsConstructor
public class LoginResponse {

    /**
     * Resultado del intento de autenticación.
     */
    private EstadoLogin estado;


    /**
     * Mensaje que será mostrado al usuario.
     */
    private String mensaje;


    /**
     * Ruta hacia la que debe dirigirse el usuario.
     *
     * Ejemplos:
     *
     * /paciente/dashboard
     * /panel-administrativo
     *
     * Puede ser null cuando debe permanecer
     * en la pantalla de login.
     */
    private String redireccion;


    /**
     * Cantidad de intentos disponibles antes
     * de bloquear la cuenta.
     *
     * FA06:
     *
     * "Usuario o contraseña incorrectos.
     * Intentos restantes: [N]."
     *
     * Puede ser null cuando no aplica.
     */
    private Integer intentosRestantes;


    /**
     * Fecha y hora hasta la cual permanece
     * bloqueada la cuenta.
     *
     * Se utiliza para FA07.
     *
     * Puede ser null cuando la cuenta
     * no se encuentra bloqueada.
     */
    private OffsetDateTime bloqueadoHasta;
}