package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/**
 * =========================================================
 * REPOSITORIO: USUARIO
 * =========================================================
 *
 * Se encarga únicamente del acceso a datos relacionados
 * con la entidad Usuario.
 *
 * No contiene lógica de negocio.
 *
 * Este repositorio será utilizado para:
 *
 * - Verificación de DPI.
 * - Inicio de sesión.
 * - Validación de duplicados.
 * - Consulta de usuarios.
 * - Búsquedas dinámicas del CU-01.
 * - Validaciones de creación y edición.
 *
 * =========================================================
 */
@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer>,
        JpaSpecificationExecutor<Usuario> {


    // =====================================================
    // BÚSQUEDA POR DPI
    // =====================================================

    /**
     * Busca un usuario por el hash/HMAC del DPI.
     *
     * El DPI original no se consulta en texto plano.
     */
    Optional<Usuario> findByDpiHash(
            String dpiHash
    );


    /**
     * Busca únicamente usuarios activos mediante dpi_hash.
     */
    Optional<Usuario> findByDpiHashAndActivoTrue(
            String dpiHash
    );


    // =====================================================
    // BÚSQUEDA POR NOMBRE DE USUARIO
    // =====================================================

    /**
     * Busca un usuario por nombre de usuario,
     * ignorando mayúsculas y minúsculas.
     *
     * Utilizado durante el inicio de sesión.
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );


    /**
     * Busca únicamente un usuario activo por
     * nombre de usuario.
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndActivoTrue(
            String nombreUsuario
    );

    List<Usuario>
    findByActivoTrueAndRol_NombreIgnoreCaseAndSucursal_IdAndEspecialidad_IdOrderByNombreCompletoAsc(
            String nombreRol,
            Integer idSucursal,
            Integer idEspecialidad
    );


    // =====================================================
    // VALIDACIONES DE UNICIDAD - CREACIÓN
    // =====================================================

    /**
     * Verifica si existe otro usuario con el mismo
     * nombre de usuario.
     */
    boolean existsByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );


    /**
     * Verifica si existe otro usuario con el mismo
     * correo electrónico.
     */
    boolean existsByCorreoElectronicoIgnoreCase(
            String correoElectronico
    );


    /**
     * Verifica si existe un DPI registrado.
     *
     * La comparación se realiza mediante dpi_hash.
     */
    boolean existsByDpiHash(
            String dpiHash
    );


    /**
     * Verifica si existe un NIT registrado.
     *
     * La comparación se realiza mediante nit_hash.
     */
    boolean existsByNitHash(
            String nitHash
    );


    // =====================================================
    // VALIDACIONES DE UNICIDAD - EDICIÓN
    // =====================================================
    //
    // Durante la edición debemos ignorar el propio
    // registro que se está modificando.
    //
    // Ejemplo:
    //
    // ID = 5
    // Usuario = admin001
    //
    // admin001 pertenece al mismo ID 5:
    //
    //      NO es duplicado.
    //
    // Solo será duplicado si pertenece a otro ID.
    // =====================================================

    /**
     * Comprueba si otro registro diferente al ID indicado
     * utiliza el mismo nombre de usuario.
     */
    boolean existsByNombreUsuarioIgnoreCaseAndIdNot(
            String nombreUsuario,
            Integer id
    );


    /**
     * Comprueba si otro registro diferente al ID indicado
     * utiliza el mismo correo electrónico.
     */
    boolean existsByCorreoElectronicoIgnoreCaseAndIdNot(
            String correoElectronico,
            Integer id
    );


    /**
     * Comprueba si otro registro diferente al ID indicado
     * utiliza el mismo DPI.
     */
    boolean existsByDpiHashAndIdNot(
            String dpiHash,
            Integer id
    );


    /**
     * Comprueba si otro registro diferente al ID indicado
     * utiliza el mismo NIT.
     */
    boolean existsByNitHashAndIdNot(
            String nitHash,
            Integer id
    );

    // =====================================================
// BLOQUEO PARA RESERVA DE HORARIOS
// =====================================================

    /**
     * Obtiene al médico aplicando un bloqueo de escritura
     * sobre su fila mientras dura la transacción.
     *
     * Se utiliza en CU-03 para evitar que dos pacientes
     * reserven simultáneamente el mismo horario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM Usuario u
        WHERE u.id = :id
        """)
    Optional<Usuario> findByIdForUpdate(
            @Param("id")
            Integer id
    );
}