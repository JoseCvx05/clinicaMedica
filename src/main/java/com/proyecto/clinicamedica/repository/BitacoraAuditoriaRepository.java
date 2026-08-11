package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.BitacoraAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * =========================================================
 * REPOSITORIO: BITÁCORA DE AUDITORÍA
 * =========================================================
 *
 * Permite persistir los registros de auditoría
 * generados por las operaciones del sistema.
 *
 * Para CU-01 se utilizará principalmente para:
 *
 * - Crear usuario.
 * - Actualizar usuario.
 * - Eliminar usuario mediante borrado lógico.
 *
 * IMPORTANTE:
 *
 * Aunque JpaRepository expone métodos como:
 *
 * - delete()
 * - deleteById()
 * - save()
 *
 * la capa Service utilizará este repositorio únicamente
 * para INSERT de nuevos registros.
 *
 * Además, PostgreSQL ya protege la tabla mediante
 * un trigger que impide:
 *
 * - UPDATE
 * - DELETE
 * - TRUNCATE
 *
 * cumpliendo RNF-023.
 * =========================================================
 */
@Repository
public interface BitacoraAuditoriaRepository
        extends JpaRepository<BitacoraAuditoria, Long> {

}