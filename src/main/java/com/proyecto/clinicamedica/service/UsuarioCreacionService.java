package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;

/**
 * =========================================================
 * SERVICIO: CREACIÓN DE USUARIOS
 * =========================================================
 *
 * Responsabilidad única:
 *
 * - Validar creación.
 * - Validar catálogos.
 * - Validar duplicados.
 * - Aplicar BCrypt.
 * - Aplicar cifrado AES-GCM.
 * - Generar HMAC para DPI/NIT.
 * - Guardar el usuario.
 * - Registrar auditoría.
 *
 * =========================================================
 */
public interface UsuarioCreacionService {

    ResultadoValidacionUsuario crear(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );
}