
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Medicamento;

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
 * REPOSITORY: MEDICAMENTO
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 * CU-14 - Mantenimiento de Catálogos.
 *
 * Permite:
 *
 * - Consultar medicamentos activos.
 * - Buscar un medicamento activo por ID.
 * - Validar existencia del medicamento.
 * - Validar nombre duplicado.
 *
 * CU-14:
 *
 * - Buscar medicamentos administrativamente.
 * - Filtrar por estado activo/inactivo.
 * - Filtrar medicamentos controlados.
 * - Paginar resultados.
 * - Validar nombres activos duplicados.
 *
 * IMPORTANTE:
 *
 * - Se conservan todos los métodos originales.
 * - No se modifican recetas médicas.
 * - No se modifica inventario.
 * - No se realizan descuentos de existencias.
 *
 * =========================================================
 */
@Repository
public interface MedicamentoRepository
        extends JpaRepository<Medicamento, Integer> {


    // =====================================================
    // LISTAR CATÁLOGO ACTIVO
    // =====================================================

    List<Medicamento>
    findByActivoTrueOrderByNombreAsc();


    // =====================================================
    // BUSCAR MEDICAMENTO ACTIVO
    // =====================================================

    Optional<Medicamento>
    findByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR EXISTENCIA ACTIVA
    // =====================================================

    boolean
    existsByIdAndActivoTrue(
            Integer id
    );


    // =====================================================
    // VALIDAR NOMBRE DUPLICADO EN CATÁLOGO
    // =====================================================
    //
    // MÉTODO ORIGINAL.
    //
    // Se conserva para no afectar módulos existentes.
    //
    // =====================================================

    boolean
    existsByNombreIgnoreCase(
            String nombre
    );


    // =====================================================
    // CU-14 - VALIDAR NOMBRE ACTIVO DUPLICADO
    // =====================================================
    //
    // Solo considera medicamentos activos.
    //
    // Permite distinguir registros activos de
    // registros desactivados lógicamente.
    //
    // La protección definitiva contra concurrencia
    // se realizará mediante PostgreSQL.
    //
    // =====================================================

    boolean existsByNombreIgnoreCaseAndActivoTrue(
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
    // - Buscar por unidad.
    // - Filtrar medicamentos activos.
    // - Filtrar medicamentos inactivos.
    // - Consultar todos los estados.
    // - Filtrar medicamentos controlados.
    // - Filtrar medicamentos no controlados.
    // - Paginar resultados.
    //
    // PARÁMETROS:
    //
    // texto:
    //   Texto de búsqueda.
    //
    // filtrarActivo:
    //   false = Ignorar filtro de estado.
    //   true  = Aplicar filtro de estado.
    //
    // valorActivo:
    //   true  = Activos.
    //   false = Inactivos.
    //
    // filtrarControlado:
    //   false = Ignorar filtro de controlados.
    //   true  = Aplicar filtro de controlados.
    //
    // valorControlado:
    //   true  = Controlados.
    //   false = No controlados.
    //
    // pageable:
    //   Configuración de paginación y ordenamiento.
    //
    // =====================================================

    @Query("""
            SELECT m
            FROM Medicamento m
            WHERE
                (
                    :texto = ''
                    OR LOWER(m.nombre)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(COALESCE(m.descripcion, ''))
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                    OR LOWER(m.unidad)
                        LIKE LOWER(CONCAT('%', :texto, '%'))
                )
                AND
                (
                    :filtrarActivo = false
                    OR m.activo = :valorActivo
                )
                AND
                (
                    :filtrarControlado = false
                    OR m.esControlado = :valorControlado
                )
            """)
    Page<Medicamento> buscarParaMantenimiento(

            @Param("texto")
            String texto,

            @Param("filtrarActivo")
            boolean filtrarActivo,

            @Param("valorActivo")
            boolean valorActivo,

            @Param("filtrarControlado")
            boolean filtrarControlado,

            @Param("valorControlado")
            boolean valorControlado,

            Pageable pageable
    );
}
