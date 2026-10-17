
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ExamenLaboratorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: EXAMEN DE LABORATORIO
 * =========================================================
 *
 * Catálogo utilizado por:
 *
 * CU-08 - FA01 Orden de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 * CU-14 - Mantenimiento de Catálogos.
 *
 * Responsabilidades:
 *
 * - Listar exámenes activos.
 * - Listar exámenes activos por laboratorio.
 * - Buscar un examen activo por ID.
 *
 * CU-14:
 *
 * - Verificar exámenes asociados a un laboratorio.
 * - Verificar exámenes activos asociados.
 * - Proteger relaciones antes de desactivar.
 *
 * - Buscar exámenes administrativamente.
 * - Filtrar por laboratorio.
 * - Filtrar por estado activo o inactivo.
 * - Paginar resultados.
 *
 * IMPORTANTE:
 *
 * - Se conservan los métodos originales.
 * - No se eliminan registros.
 * - No se modifican órdenes de laboratorio.
 *
 * =========================================================
 */
@Repository
public interface ExamenLaboratorioRepository
        extends JpaRepository<ExamenLaboratorio, Integer> {


    // =====================================================
    // LISTAR TODOS LOS EXÁMENES ACTIVOS
    // =====================================================

    List<ExamenLaboratorio>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // LISTAR EXÁMENES ACTIVOS DE UN LABORATORIO
    // =====================================================

    List<ExamenLaboratorio>
    findByLaboratorio_IdAndActivoTrueOrderByNombreAsc(
            Integer idLaboratorio
    );


    // =====================================================
    // BUSCAR EXAMEN ACTIVO POR ID
    // =====================================================

    Optional<ExamenLaboratorio>
    findByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // CU-14 - VERIFICAR EXÁMENES ASOCIADOS
    // =====================================================
    //
    // Comprueba si existe al menos un examen relacionado
    // con el laboratorio indicado.
    //
    // Incluye exámenes activos e inactivos.
    //
    // Se utilizará para proteger la integridad de las
    // relaciones del catálogo.
    //
    // =====================================================

    boolean existsByLaboratorio_Id(
            Integer idLaboratorio
    );


    // =====================================================
    // CU-14 - VERIFICAR EXÁMENES ACTIVOS ASOCIADOS
    // =====================================================
    //
    // Comprueba si el laboratorio tiene exámenes activos.
    //
    // Evita cargar todos los exámenes en memoria.
    //
    // Este método será utilizado por el servicio de
    // mantenimiento para validar dependencias activas.
    //
    // =====================================================

    boolean existsByLaboratorio_IdAndActivoTrue(
            Integer idLaboratorio
    );


    // =====================================================
    // CU-14 - BÚSQUEDA ADMINISTRATIVA DE EXÁMENES
    // =====================================================
    //
    // Permite:
    //
    // - Buscar por nombre del examen.
    // - Buscar por descripción.
    // - Buscar por nombre del laboratorio.
    // - Filtrar por laboratorio específico.
    // - Filtrar exámenes activos e inactivos.
    // - Consultar todos los estados.
    // - Paginar resultados.
    //
    // La relación con Laboratorio se carga mediante
    // EntityGraph para poder mostrar su nombre
    // en la vista administrativa.
    //
    // No altera las consultas existentes utilizadas
    // por CU-08 ni CU-09.
    //
    // =====================================================

    @EntityGraph(
            attributePaths = {"laboratorio"}
    )
    @Query("""
            SELECT e
            FROM ExamenLaboratorio e
            JOIN e.laboratorio l
            WHERE
                (
                    :texto = ''
                    OR LOWER(e.nombre)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(COALESCE(e.descripcion, ''))
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(l.nombre)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                )
                AND
                (
                    :filtrarActivo = false
                    OR e.activo = :valorActivo
                )
                AND
                (
                    :filtrarLaboratorio = false
                    OR l.id = :idLaboratorio
                )
            """)
    Page<ExamenLaboratorio> buscarParaMantenimiento(

            @Param("texto")
            String texto,

            @Param("filtrarActivo")
            boolean filtrarActivo,

            @Param("valorActivo")
            boolean valorActivo,

            @Param("filtrarLaboratorio")
            boolean filtrarLaboratorio,

            @Param("idLaboratorio")
            Integer idLaboratorio,

            Pageable pageable
    );
}
