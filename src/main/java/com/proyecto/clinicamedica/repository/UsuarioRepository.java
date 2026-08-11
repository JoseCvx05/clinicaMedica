package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
 * - Consulta de usuarios activos.
 * =========================================================
 */
@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por el hash/HMAC del DPI.
     *
     * Este método será utilizado en CU-00 para verificar
     * si el DPI ingresado pertenece a un usuario registrado.
     */
    Optional<Usuario> findByDpiHash(String dpiHash);


    /**
     * Busca únicamente usuarios activos mediante dpi_hash.
     *
     * Útil cuando queramos ignorar usuarios inactivos
     * durante ciertos procesos del sistema.
     */
    Optional<Usuario> findByDpiHashAndActivoTrue(String dpiHash);


    /**
     * Busca un usuario por nombre de usuario,
     * ignorando mayúsculas y minúsculas.
     *
     * Se utilizará durante el inicio de sesión.
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );


    /**
     * Busca un usuario activo por nombre de usuario.
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndActivoTrue(
            String nombreUsuario
    );


    /**
     * Verifica si un nombre de usuario ya existe,
     * ignorando mayúsculas y minúsculas.
     *
     * Será utilizado posteriormente en CU-01 y CU-02.
     */
    boolean existsByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );


    /**
     * Verifica si ya existe un correo electrónico,
     * ignorando mayúsculas y minúsculas.
     */
    boolean existsByCorreoElectronicoIgnoreCase(
            String correoElectronico
    );


    /**
     * Comprueba si ya existe un DPI registrado.
     *
     * Como el DPI original no se almacena en texto plano,
     * la validación se realiza mediante su HMAC.
     */
    boolean existsByDpiHash(String dpiHash);


    /**
     * Comprueba si ya existe un NIT registrado.
     */
    boolean existsByNitHash(String nitHash);
}