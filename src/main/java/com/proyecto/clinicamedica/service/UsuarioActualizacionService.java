package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;

/**
 * =========================================================
 * SERVICIO: ACTUALIZACIÓN DE USUARIOS
 * =========================================================
 *
 * Responsabilidad única:
 *
 * - Validar edición.
 * - Validar catálogos.
 * - Validar duplicados excluyendo el propio usuario.
 * - Actualizar datos.
 * - Cambiar contraseña solo si se proporciona una nueva.
 * - Aplicar AES-GCM y HMAC a DPI/NIT.
 * - Registrar auditoría.
 *
 * =========================================================
 */
public interface UsuarioActualizacionService {

    ResultadoValidacionUsuario actualizar(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );
}