package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/**
 * =========================================================
 * REPOSITORIO: SUCURSAL
 * =========================================================
 *
 * Responsable únicamente del acceso a datos
 * relacionados con la entidad Sucursal.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface SucursalRepository
        extends JpaRepository<Sucursal, Integer> {

    /**
     * Obtiene todas las sucursales activas
     * ordenadas alfabéticamente.
     *
     * Este resultado será utilizado posteriormente
     * para alimentar la caché del catálogo.
     */
    List<Sucursal> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca una sucursal activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Sucursal> findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );



    /**
     * Comprueba si ya existe una sucursal activa
     * con determinado nombre.
     *
     * Será utilizado posteriormente en el
     * mantenimiento de catálogos.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );

    // =====================================================
    // CU-14 - LISTADO ADMINISTRATIVO DE SUCURSALES
    // =====================================================
    //
    // Permite:
    //
    // - Buscar por nombre.
    // - Buscar por dirección.
    // - Buscar por descripción.
    // - Filtrar sucursales activas e inactivas.
    // - Consultar todos los estados.
    // - Aplicar paginación y ordenamiento.
    //
    // No modifica registros de PostgreSQL.
    //
    // =====================================================

    @Query("""
            SELECT s
            FROM Sucursal s

            WHERE (
                :filtrarActivo = false
                OR s.activo = :activo
            )

            AND (
                LOWER(s.nombre)
                    LIKE CONCAT('%', LOWER(:texto), '%')

                OR LOWER(COALESCE(s.direccion, ''))
                    LIKE CONCAT('%', LOWER(:texto), '%')

                OR LOWER(COALESCE(s.descripcion, ''))
                    LIKE CONCAT('%', LOWER(:texto), '%')
            )
            """)
    Page<Sucursal> buscarParaMantenimiento(

            @Param("texto")
            String texto,

            @Param("filtrarActivo")
            boolean filtrarActivo,

            @Param("activo")
            boolean activo,

            Pageable pageable
    );


    // =====================================================
    // CU-14 - BLOQUEO PARA DESACTIVACIÓN DE SUCURSAL
    // =====================================================
    //
    // Obtiene una sucursal aplicando bloqueo de escritura.
    //
    // Se utilizará durante la desactivación lógica
    // para evitar que dos administradores modifiquen
    // simultáneamente el estado de la misma sucursal.
    //
    // IMPORTANTE:
    //
    // - Debe ejecutarse dentro de @Transactional.
    // - No modifica registros por sí mismo.
    // - El bloqueo dura hasta finalizar la transacción.
    // - El Service verificará todas las dependencias.
    //
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Sucursal s
            WHERE s.id = :id
            """)
    Optional<Sucursal> buscarPorIdConBloqueo(
            @Param("id") Integer id
    );


}