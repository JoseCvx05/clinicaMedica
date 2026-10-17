
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.EstadoCita;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORY: ESTADO DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Permite consultar los estados disponibles de una cita.
 *
 * =========================================================
 * CU-14 - MANTENIMIENTO DE CATÁLOGOS
 * =========================================================
 *
 * Funcionalidades administrativas:
 *
 * - Consultar estados activos e inactivos.
 * - Buscar por nombre o descripción.
 * - Filtrar por estado.
 * - Paginar resultados.
 * - Conservar consultas utilizadas por otros CU.
 *
 * IMPORTANTE:
 *
 * No modifica estados existentes.
 * No altera el orden del flujo de citas.
 * No elimina registros históricos.
 *
 * =========================================================
 */
@Repository
public interface EstadoCitaRepository
        extends JpaRepository<EstadoCita, Integer> {

    // =====================================================
    // CONSULTAS EXISTENTES - CONSERVADAS
    // =====================================================

    Optional<EstadoCita>
    findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );

    List<EstadoCita> findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // CU-14 - BÚSQUEDA ADMINISTRATIVA
    // =====================================================
    //
    // Permite:
    //
    // - Buscar por nombre.
    // - Buscar por descripción.
    // - Consultar todos los estados.
    // - Filtrar activos.
    // - Filtrar inactivos.
    // - Aplicar paginación.
    //
    // El ordenamiento se define mediante Pageable.
    //
    // No modifica los datos.
    //
    // =====================================================

    @Query("""
            SELECT e
            FROM EstadoCita e
            WHERE
                (
                    :texto = ''
                    OR LOWER(e.nombre)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(COALESCE(e.descripcion, ''))
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                )
                AND
                (
                    :filtrarActivo = false
                    OR e.activo = :valorActivo
                )
            """)
    Page<EstadoCita> buscarParaMantenimiento(
            @Param("texto")
            String texto,

            @Param("filtrarActivo")
            boolean filtrarActivo,

            @Param("valorActivo")
            boolean valorActivo,

            Pageable pageable
    );
}
