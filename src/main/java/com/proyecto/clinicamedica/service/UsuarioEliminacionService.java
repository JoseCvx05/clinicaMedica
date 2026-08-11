package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;

/**
 * =========================================================
 * SERVICIO: ELIMINACIÓN LÓGICA DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA05.
 *
 * La eliminación nunca borra físicamente un registro.
 *
 * El usuario se conserva en PostgreSQL y únicamente
 * cambia su estado:
 *
 * activo = false
 *
 * También se registra la operación en la bitácora.
 * =========================================================
 */
public interface UsuarioEliminacionService {

    ResultadoValidacionUsuario eliminar(
            Integer idUsuario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );
}