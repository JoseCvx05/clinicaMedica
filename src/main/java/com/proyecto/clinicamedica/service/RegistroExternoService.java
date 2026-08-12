package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

/**
 * =========================================================
 * SERVICIO: REGISTRO DE USUARIOS EXTERNOS
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad:
 *
 * - Coordinar validaciones.
 * - Registrar al paciente.
 * - Asignar rol Paciente.
 * - Dejar la cuenta activa.
 * - Proteger DPI y NIT.
 * - Proteger la contraseña con BCrypt.
 *
 * El envío del correo de bienvenida se delegará
 * a un servicio especializado.
 *
 * =========================================================
 */
public interface RegistroExternoService {

    /**
     * Registra un nuevo usuario externo.
     *
     * Si alguna regla falla, no se guarda nada y
     * el resultado contiene los errores por campo.
     *
     * @param formulario datos ingresados por el paciente
     * @return resultado de las validaciones
     */
    ResultadoValidacionRegistroExterno registrar(
            RegistroExternoDTO formulario
    );
}