
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * CU-14 - REPOSITORY: MOVIMIENTOS DE INVENTARIO
 * =========================================================
 *
 * Conserva:
 * - Historial por inventario.
 * - Historial por sucursal.
 * - Consultas por período.
 * - Resumen mensual.
 *
 * CU-15:
 * - Búsqueda combinada.
 * - Filtros opcionales.
 * - Compatibilidad con movimientos históricos.
 * - Paginación.
 *
 * No modifica existencias ni movimientos.
 * =========================================================
 */
@Repository
public interface MovimientoInventarioFarmaciaRepository
        extends JpaRepository<MovimientoInventarioFarmacia, Long> {

    // =====================================================
    // CU-14 - HISTORIAL DE UN INVENTARIO
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Page<MovimientoInventarioFarmacia>
    findByInventario_IdOrderByFechaHoraDescIdDesc(
            Integer idInventario,
            Pageable pageable
    );

    // =====================================================
    // CU-14 - HISTORIAL POR SUCURSAL
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Page<MovimientoInventarioFarmacia>
    findByInventario_Sucursal_IdOrderByFechaHoraDescIdDesc(
            Integer idSucursal,
            Pageable pageable
    );

    // =====================================================
    // CU-14 - MOVIMIENTOS POR PERÍODO
    // =====================================================

    @Query("""
            SELECT m
            FROM MovimientoInventarioFarmacia m
            WHERE m.inventario.id = :idInventario
              AND m.fechaHora >= :desde
              AND m.fechaHora < :hasta
            ORDER BY m.fechaHora ASC, m.id ASC
            """)
    List<MovimientoInventarioFarmacia>
    buscarPorInventarioYPeriodo(
            @Param("idInventario") Integer idInventario,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta
    );

    // =====================================================
    // CU-14 - RESUMEN MENSUAL DE INVENTARIO
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    Optional<MovimientoInventarioFarmacia>
    findFirstByInventario_IdAndFechaHoraLessThanOrderByFechaHoraDescIdDesc(
            Integer idInventario,
            OffsetDateTime fechaInicio
    );

    // =====================================================
    // CU-15 - BÚSQUEDA COMBINADA DE MOVIMIENTOS
    // =====================================================
    //
    // CORRECCIÓN 1:
    // CAST(:referencia AS String)
    // evita problemas de tipo con LOWER().
    //
    // CORRECCIÓN 2:
    // CAST(:desde AS OffsetDateTime)
    // CAST(:hasta AS OffsetDateTime)
    //
    // Permiten evaluar fechas nulas sin errores
    // de inferencia de tipos en PostgreSQL.
    //
    // Cuando una fecha es NULL:
    // no se aplica ese límite de búsqueda.
    //
    // Se mantiene:
    // - Fecha inicial inclusiva.
    // - Fecha final exclusiva.
    //
    // =====================================================

    @EntityGraph(attributePaths = {
            "inventario",
            "inventario.medicamento",
            "inventario.sucursal",
            "usuario"
    })
    @Query(
            value = """
                    SELECT m
                    FROM MovimientoInventarioFarmacia m

                    LEFT JOIN MovimientoBitacoraDetalle d
                        ON d.movimiento = m

                    WHERE (
                        :idMedicamento IS NULL
                        OR m.inventario.medicamento.id = :idMedicamento
                    )

                    AND (
                        :idSucursal IS NULL
                        OR m.inventario.sucursal.id = :idSucursal
                    )

                    AND (
                        :codigoTipo IS NULL
                        OR d.codigoTipo = :codigoTipo
                        OR (
                            :codigoTipo = 6
                            AND d.codigoTipo IS NULL
                            AND m.origen = 'CU11_DESPACHO_FARMACIA'
                        )
                    )

                    AND (
                        CAST(:referencia AS String) IS NULL
                        OR LOWER(m.idReferencia)
                           LIKE LOWER(
                               CONCAT(
                                   '%',
                                   CAST(:referencia AS String),
                                   '%'
                               )
                           )
                    )

                    AND (
                        :idUsuario IS NULL
                        OR m.usuario.id = :idUsuario
                    )

                    AND (
                        CAST(:desde AS OffsetDateTime) IS NULL
                        OR m.fechaHora >= :desde
                    )

                    AND (
                        CAST(:hasta AS OffsetDateTime) IS NULL
                        OR m.fechaHora < :hasta
                    )
                    """,

            countQuery = """
                    SELECT COUNT(m)
                    FROM MovimientoInventarioFarmacia m

                    LEFT JOIN MovimientoBitacoraDetalle d
                        ON d.movimiento = m

                    WHERE (
                        :idMedicamento IS NULL
                        OR m.inventario.medicamento.id = :idMedicamento
                    )

                    AND (
                        :idSucursal IS NULL
                        OR m.inventario.sucursal.id = :idSucursal
                    )

                    AND (
                        :codigoTipo IS NULL
                        OR d.codigoTipo = :codigoTipo
                        OR (
                            :codigoTipo = 6
                            AND d.codigoTipo IS NULL
                            AND m.origen = 'CU11_DESPACHO_FARMACIA'
                        )
                    )

                    AND (
                        CAST(:referencia AS String) IS NULL
                        OR LOWER(m.idReferencia)
                           LIKE LOWER(
                               CONCAT(
                                   '%',
                                   CAST(:referencia AS String),
                                   '%'
                               )
                           )
                    )

                    AND (
                        :idUsuario IS NULL
                        OR m.usuario.id = :idUsuario
                    )

                    AND (
                        CAST(:desde AS OffsetDateTime) IS NULL
                        OR m.fechaHora >= :desde
                    )

                    AND (
                        CAST(:hasta AS OffsetDateTime) IS NULL
                        OR m.fechaHora < :hasta
                    )
                    """
    )
    Page<MovimientoInventarioFarmacia> buscarMovimientosCu15(
            @Param("idMedicamento") Integer idMedicamento,
            @Param("idSucursal") Integer idSucursal,
            @Param("codigoTipo") Integer codigoTipo,
            @Param("referencia") String referencia,
            @Param("idUsuario") Integer idUsuario,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            Pageable pageable
    );


    // =====================================================
    // CU-15 - FILTRO POR MEDICAMENTO
    // =====================================================
    //
    // Incluye medicamentos activos e inactivos
    // que tengan movimientos históricos.
    //
    // Respeta la sucursal autorizada.
    // =====================================================

    @Query("""
            SELECT DISTINCT med
            FROM MovimientoInventarioFarmacia m
            JOIN m.inventario i
            JOIN i.medicamento med
            JOIN i.sucursal s
            WHERE (
                CAST(:idSucursal AS Integer) IS NULL
                OR s.id = :idSucursal
            )
            ORDER BY med.nombre ASC
            """)
    List<Medicamento> listarMedicamentosConMovimientosCu15(
            @Param("idSucursal") Integer idSucursal
    );

    // =====================================================
    // CU-15 - FILTRO POR SUCURSAL
    // =====================================================
    //
    // Incluye sucursales con movimientos registrados.
    //
    // Administrador: todas las sucursales del historial.
    // Farmacéutico: únicamente su sucursal.
    // =====================================================

    @Query("""
            SELECT DISTINCT s
            FROM MovimientoInventarioFarmacia m
            JOIN m.inventario i
            JOIN i.sucursal s
            WHERE (
                CAST(:idSucursal AS Integer) IS NULL
                OR s.id = :idSucursal
            )
            ORDER BY s.nombre ASC
            """)
    List<Sucursal> listarSucursalesConMovimientosCu15(
            @Param("idSucursal") Integer idSucursal
    );

    // =====================================================
    // CU-15 - FILTRO POR USUARIO RESPONSABLE
    // =====================================================
    //
    // Incluye usuarios activos e inactivos
    // que hayan registrado movimientos.
    //
    // No incluye movimientos de sucursales ajenas
    // al farmacéutico.
    // =====================================================

    @Query("""
            SELECT DISTINCT u
            FROM MovimientoInventarioFarmacia m
            JOIN m.usuario u
            JOIN m.inventario i
            JOIN i.sucursal s
            WHERE (
                CAST(:idSucursal AS Integer) IS NULL
                OR s.id = :idSucursal
            )
            ORDER BY u.nombreUsuario ASC
            """)
    List<Usuario> listarUsuariosConMovimientosCu15(
            @Param("idSucursal") Integer idSucursal
    );

}
