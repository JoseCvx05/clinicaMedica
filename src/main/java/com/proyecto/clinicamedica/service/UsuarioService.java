package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

import java.util.Optional;

/**
 * =========================================================
 * SERVICIO: USUARIO
 * =========================================================
 *
 * Define operaciones generales de consulta sobre usuarios.
 *
 * IMPORTANTE:
 *
 * Esta interfaz NO contiene la lógica de:
 *
 * - Validación de contraseña.
 * - Generación de JWT.
 * - Incremento de intentos fallidos.
 * - Bloqueo temporal.
 * - Cifrado o descifrado de DPI/NIT.
 *
 * Esas responsabilidades pertenecen a servicios
 * especializados.
 *
 * Esto permite aplicar el principio de responsabilidad
 * única (SRP) de SOLID.
 * =========================================================
 */
public interface UsuarioService {

    /**
     * Busca un usuario por su identificador.
     */
    Optional<Usuario> buscarPorId(Integer id);


    /**
     * Busca un usuario utilizando el HMAC-SHA-256
     * previamente generado a partir del DPI.
     *
     * El DPI original nunca se utiliza directamente
     * para consultar PostgreSQL.
     */
    Optional<Usuario> buscarPorDpiHash(String dpiHash);


    /**
     * Busca únicamente un usuario activo mediante
     * el hash/HMAC del DPI.
     */
    Optional<Usuario> buscarActivoPorDpiHash(String dpiHash);


    /**
     * Busca un usuario por nombre de usuario,
     * ignorando mayúsculas y minúsculas.
     *
     * Será utilizado posteriormente por el servicio
     * de autenticación.
     */
    Optional<Usuario> buscarPorNombreUsuario(
            String nombreUsuario
    );


    /**
     * Busca únicamente un usuario activo mediante
     * su nombre de usuario.
     */
    Optional<Usuario> buscarActivoPorNombreUsuario(
            String nombreUsuario
    );


    /**
     * Comprueba si ya existe determinado nombre
     * de usuario.
     */
    boolean existeNombreUsuario(String nombreUsuario);


    /**
     * Comprueba si ya existe determinado correo
     * electrónico.
     */
    boolean existeCorreoElectronico(String correoElectronico);


    /**
     * Comprueba si ya existe un DPI mediante
     * su HMAC.
     */
    boolean existeDpiHash(String dpiHash);


    /**
     * Comprueba si ya existe un NIT mediante
     * su HMAC.
     */
    boolean existeNitHash(String nitHash);
    /**
     * Guarda un usuario en PostgreSQL.
     *
     * La preparación de datos sensibles, contraseñas
     * y reglas específicas debe realizarse antes de
     * llamar este método.
     *
     * @param usuario usuario a persistir
     * @return usuario persistido
     */
    Usuario guardar(Usuario usuario);
}