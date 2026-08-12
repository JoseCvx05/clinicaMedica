package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

/**
 * =========================================================
 * SERVICIO: PERSISTENCIA DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad exclusiva:
 *
 * - Persistir el nuevo paciente.
 * - Ejecutar la operación dentro de una transacción.
 *
 * Separar esta responsabilidad permite manejar de forma
 * segura posibles conflictos de integridad provenientes
 * de PostgreSQL.
 * =========================================================
 */
public interface RegistroExternoPersistenciaService {

    Usuario guardar(
            Usuario usuario
    );
}