package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
/**
 * =========================================================
 * REPOSITORIO: ESPECIALIDAD
 * =========================================================
 *
 * Se encarga únicamente del acceso a datos de la entidad
 * Especialidad.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface EspecialidadRepository
        extends JpaRepository<Especialidad, Integer> {

    /**
     * Obtiene las especialidades activas ordenadas
     * alfabéticamente.
     *
     * Este método alimentará el catálogo almacenado
     * en caché.
     */
    List<Especialidad> findByActivoTrueOrderByNombreAsc();


    /**
     * Busca una especialidad activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Especialidad> findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );


    /**
     * Comprueba si ya existe una especialidad activa
     * con determinado nombre.
     *
     * Será útil cuando lleguemos al mantenimiento
     * de catálogos.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );


    // =====================================================
    // CU-14 - BLOQUEO PARA DESACTIVACIÓN DE ESPECIALIDAD
    // =====================================================
    //
    // Obtiene una especialidad y bloquea su registro
    // mientras se realizan las validaciones necesarias.
    //
    // Evita que dos administradores desactiven
    // simultáneamente la misma especialidad.
    //
    // El bloqueo permanece durante la transacción.
    //
    // No elimina ni modifica ningún registro.
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM Especialidad e
            WHERE e.id = :id
            """)
    Optional<Especialidad> buscarPorIdConBloqueo(
            @Param("id") Integer id
    );


    // =====================================================
    // CU-14 - LISTADO ADMINISTRATIVO DE ESPECIALIDADES
    // =====================================================
    //
    // Permite:
    // - Buscar por nombre o descripción.
    // - Filtrar especialidades activas e inactivas.
    // - Mostrar todos los estados.
    // - Obtener resultados paginados.
    //
    // filtrarActivo = false -> Todos los estados.
    // filtrarActivo = true  -> Filtrar por activo.
    //
    // No modifica registros de PostgreSQL.
    // =====================================================

    @Query("""
            SELECT e
            FROM Especialidad e
            WHERE (
                :filtrarActivo = false
                OR e.activo = :activo
            )
            AND (
                LOWER(e.nombre)
                    LIKE CONCAT('%', LOWER(:texto), '%')
                OR
                LOWER(e.descripcion)
                    LIKE CONCAT('%', LOWER(:texto), '%')
            )
            """)
    Page<Especialidad> buscarParaMantenimiento(

            @Param("texto")
            String texto,

            @Param("filtrarActivo")
            boolean filtrarActivo,

            @Param("activo")
            boolean activo,

            Pageable pageable
    );


}