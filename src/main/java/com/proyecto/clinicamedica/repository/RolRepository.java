package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;


/**
 * =========================================================
 * REPOSITORIO: ROL
 * =========================================================
 *
 * Se encarga exclusivamente del acceso a datos
 * relacionados con la entidad Rol.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Obtiene únicamente los roles activos.
     *
     * Será utilizado para los catálogos que posteriormente
     * serán almacenados en caché.
     */
    List<Rol> findByActivoTrueOrderByNombreAsc();


    /**
     * Busca un rol activo por nombre ignorando
     * mayúsculas y minúsculas.
     *
     * Ejemplo:
     *
     * "Paciente"
     * "PACIENTE"
     * "paciente"
     *
     * serán considerados el mismo nombre.
     */
    Optional<Rol> findByNombreIgnoreCaseAndActivoTrue(String nombre);


    /**
     * Permite verificar si ya existe un rol activo
     * con determinado nombre.
     *
     * Esto servirá posteriormente para evitar duplicados.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);


    // =====================================================
    // CU-14 - CATÁLOGO 7: MANTENIMIENTO DE ROLES
    // =====================================================
    //
    // Consultas administrativas para:
    // - Buscar roles activos e inactivos.
    // - Filtrar por nombre y estado.
    // - Paginar resultados.
    // - Evitar nombres duplicados durante la edición.
    // - Bloquear registros durante modificaciones.
    //
    // No modifica usuarios ni permisos existentes.
    // =====================================================


    // =====================================================
    // LISTADO Y BÚSQUEDA SIN FILTRO DE ESTADO
    // =====================================================

    Page<Rol> findByNombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );


    // =====================================================
    // LISTADO Y BÚSQUEDA CON FILTRO DE ESTADO
    // =====================================================

    Page<Rol> findByActivoAndNombreContainingIgnoreCase(
            Boolean activo,
            String nombre,
            Pageable pageable
    );


    // =====================================================
    // VALIDAR DUPLICADOS DURANTE EDICIÓN
    // =====================================================

    boolean existsByNombreIgnoreCaseAndActivoTrueAndIdNot(
            String nombre,
            Integer id
    );


    // =====================================================
    // BLOQUEO DE REGISTRO PARA MODIFICACIONES
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Rol r
            WHERE r.id = :id
            """)
    Optional<Rol> findByIdForUpdate(
            @Param("id") Integer id
    );

}