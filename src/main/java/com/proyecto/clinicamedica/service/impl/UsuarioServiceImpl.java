package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import com.proyecto.clinicamedica.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO: USUARIO
 * =========================================================
 *
 * Implementa las operaciones generales de consulta
 * definidas en UsuarioService.
 *
 * IMPORTANTE:
 *
 * Esta clase NO maneja:
 *
 * - Contraseñas.
 * - JWT.
 * - Bloqueos.
 * - Intentos fallidos.
 * - Cifrado AES.
 * - HMAC.
 *
 * Cada una de esas responsabilidades será manejada
 * por servicios especializados.
 *
 * Esto permite mantener el principio SRP de SOLID.
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }


    /**
     * Busca un usuario por su identificador.
     */
    @Override
    public Optional<Usuario> buscarPorId(Integer id) {

        if (id == null) {
            return Optional.empty();
        }

        return usuarioRepository.findById(id);
    }


    /**
     * Busca un usuario mediante el HMAC del DPI.
     *
     * El DPI original nunca se consulta directamente.
     */
    @Override
    public Optional<Usuario> buscarPorDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null || dpiHash.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository.findByDpiHash(
                dpiHash.trim()
        );
    }


    /**
     * Busca únicamente un usuario activo mediante
     * el HMAC del DPI.
     */
    @Override
    public Optional<Usuario> buscarActivoPorDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null || dpiHash.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository
                .findByDpiHashAndActivoTrue(
                        dpiHash.trim()
                );
    }


    /**
     * Busca un usuario por nombre de usuario,
     * ignorando mayúsculas y minúsculas.
     */
    @Override
    public Optional<Usuario> buscarPorNombreUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            return Optional.empty();
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario.trim()
                );
    }


    /**
     * Busca únicamente usuarios activos por
     * nombre de usuario.
     */
    @Override
    public Optional<Usuario> buscarActivoPorNombreUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            return Optional.empty();
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCaseAndActivoTrue(
                        nombreUsuario.trim()
                );
    }


    /**
     * Verifica si ya existe determinado nombre
     * de usuario.
     */
    @Override
    public boolean existeNombreUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            return false;
        }

        return usuarioRepository
                .existsByNombreUsuarioIgnoreCase(
                        nombreUsuario.trim()
                );
    }


    /**
     * Verifica si ya existe determinado correo
     * electrónico.
     */
    @Override
    public boolean existeCorreoElectronico(
            String correoElectronico
    ) {

        if (correoElectronico == null
                || correoElectronico.isBlank()) {

            return false;
        }

        return usuarioRepository
                .existsByCorreoElectronicoIgnoreCase(
                        correoElectronico.trim()
                );
    }


    /**
     * Verifica si un DPI ya existe mediante
     * su HMAC.
     */
    @Override
    public boolean existeDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null || dpiHash.isBlank()) {
            return false;
        }

        return usuarioRepository
                .existsByDpiHash(
                        dpiHash.trim()
                );
    }


    /**
     * Verifica si un NIT ya existe mediante
     * su HMAC.
     */
    @Override
    public boolean existeNitHash(
            String nitHash
    ) {

        if (nitHash == null || nitHash.isBlank()) {
            return false;
        }

        return usuarioRepository
                .existsByNitHash(
                        nitHash.trim()
                );
    }
    /**
     * Guarda un usuario en PostgreSQL.
     *
     * Este método abre una transacción de escritura,
     * sobrescribiendo el readOnly = true definido
     * a nivel de clase.
     */
    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {

        if (usuario == null) {

            throw new IllegalArgumentException(
                    "El usuario que se desea guardar no puede ser nulo."
            );
        }

        return usuarioRepository.save(usuario);
    }
}