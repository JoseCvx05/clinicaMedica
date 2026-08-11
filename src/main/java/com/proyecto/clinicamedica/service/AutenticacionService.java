package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.security.TipoAcceso;

/**
 * =========================================================
 * SERVICIO: AUTENTICACIÓN
 * =========================================================
 *
 * Define el contrato común para autenticar usuarios
 * del sistema.
 *
 * La misma lógica servirá para:
 *
 * - Portal de pacientes.
 * - Portal del personal interno.
 *
 * Las diferencias entre ambos accesos serán resueltas
 * mediante PoliticaAutenticacion.
 * =========================================================
 */
public interface AutenticacionService {

    /**
     * Autentica a un usuario según el tipo de portal
     * desde el cual intenta ingresar.
     *
     * La implementación deberá:
     *
     * 1. Resolver la política correspondiente.
     * 2. Buscar el usuario.
     * 3. Validar estado activo.
     * 4. Revisar bloqueo vigente.
     * 5. Validar contraseña.
     * 6. Incrementar intentos si falla.
     * 7. Aplicar bloqueo si corresponde.
     * 8. Restablecer intentos si acierta.
     * 9. Validar si el usuario puede usar ese portal.
     * 10. Retornar el resultado.
     *
     * @param nombreUsuario nombre de usuario ingresado
     * @param contrasena contraseña ingresada
     * @param tipoAcceso portal desde el que inicia sesión
     *
     * @return resultado de autenticación
     */
    ResultadoAutenticacion autenticar(
            String nombreUsuario,
            String contrasena,
            TipoAcceso tipoAcceso
    );
}