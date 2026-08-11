package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * =========================================================
 * RESULTADO INTERNO DE AUTENTICACIÓN
 * =========================================================
 *
 * Representa el resultado interno producido por
 * AutenticacionService.
 *
 * Contiene:
 *
 * - La respuesta que posteriormente recibirá el frontend.
 * - El usuario autenticado cuando las credenciales sean
 *   correctas.
 *
 * IMPORTANTE:
 *
 * Esta clase NO será devuelta directamente como JSON.
 *
 * Esto evita exponer:
 *
 * - Usuario.
 * - Hash de contraseña.
 * - DPI cifrado.
 * - DPI hash.
 * - NIT.
 * - Información interna.
 *
 * Posteriormente el Controller utilizará el Usuario
 * autenticado exclusivamente para generar el JWT.
 * =========================================================
 */
@Getter
@AllArgsConstructor
public class ResultadoAutenticacion {

    /**
     * Respuesta pública que podrá enviarse al frontend.
     */
    private LoginResponse respuesta;


    /**
     * Usuario que fue autenticado correctamente.
     *
     * Será null cuando:
     *
     * - Las credenciales sean incorrectas.
     * - La cuenta esté bloqueada.
     * - El rol no esté autorizado para el portal.
     */
    private Usuario usuarioAutenticado;
}