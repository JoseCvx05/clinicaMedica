package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.RegistroAuditoria;

/**
 * =========================================================
 * SERVICIO: AUDITORÍA
 * =========================================================
 *
 * Define el contrato general para registrar operaciones
 * realizadas dentro del sistema.
 *
 * Para CU-01 permitirá registrar:
 *
 * - Crear usuario.
 * - Actualizar usuario.
 * - Eliminar usuario mediante borrado lógico.
 *
 * La implementación se encargará de convertir
 * RegistroAuditoria en una entidad BitacoraAuditoria
 * y almacenarla en PostgreSQL.
 *
 * IMPORTANTE:
 *
 * Este servicio únicamente CREA registros de auditoría.
 *
 * No expone operaciones para:
 *
 * - Modificar auditorías.
 * - Eliminar auditorías.
 *
 * Esto ayuda a respetar RNF-023.
 * =========================================================
 */
public interface AuditoriaService {

    /**
     * Registra una nueva operación en la bitácora
     * de auditoría.
     *
     * @param registro información de la operación
     *                 realizada
     */
    void registrar(
            RegistroAuditoria registro
    );
}