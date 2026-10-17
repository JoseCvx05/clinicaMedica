
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Laboratorio;

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
 * REPOSITORY: LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 * CU-14 - Mantenimiento de Catálogos.
 *
 * IMPORTANTE:
 *
 * Se conservan todos los métodos existentes.
 * Las nuevas consultas administrativas no modifican
 * el funcionamiento de los módulos anteriores.
 *
 * =========================================================
 */
@Repository
public interface LaboratorioRepository
        extends JpaRepository<Laboratorio, Integer> {


    // =====================================================
    // LISTAR LABORATORIOS ACTIVOS
    // =====================================================

    List<Laboratorio>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // BUSCAR LABORATORIO ACTIVO POR ID
    // =====================================================

    Optional<Laboratorio>
    findByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR NOMBRE DUPLICADO ACTIVO
    // =====================================================

    boolean
    existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );


    // =====================================================
    // CU-14 - BÚSQUEDA ADMINISTRATIVA
    // =====================================================
    //
    // Permite:
    //
    // - Buscar por nombre.
    // - Buscar por descripción.
    // - Filtrar laboratorios activos.
    // - Filtrar laboratorios inactivos.
    // - Mostrar todos los laboratorios.
    // - Paginar los resultados.
    //
    // IMPORTANTE:
    //
    // No modifica información.
    // No altera las consultas de CU-08 y CU-09.
    //
    // El ordenamiento será definido desde el Service.
    //
    // =====================================================

    @Query("""
            SELECT l
            FROM Laboratorio l
            WHERE
                (
                    :texto = ''
                    OR LOWER(l.nombre)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(COALESCE(l.descripcion, ''))
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                )
                AND
                (
                    :filtrarActivo = false
                    OR l.activo = :valorActivo
                )
            """)
    Page<Laboratorio> buscarParaMantenimiento(
            @Param("texto") String texto,
            @Param("filtrarActivo") boolean filtrarActivo,
            @Param("valorActivo") boolean valorActivo,
            Pageable pageable
    );
}
