package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: USUARIO
 * =========================================================
 *
 * Contiene operaciones generales y reutilizables
 * relacionadas con Usuario.
 *
 * No contiene lógica específica de:
 *
 * - Contraseñas.
 * - JWT.
 * - Bloqueos.
 * - Intentos fallidos.
 * - Cifrado AES-GCM.
 * - Generación de HMAC.
 *
 * Esas responsabilidades permanecen en servicios
 * especializados.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final UsuarioRepository usuarioRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioService(
            UsuarioRepository usuarioRepository
    ) {

        this.usuarioRepository =
                usuarioRepository;
    }


    // =====================================================
    // BUSCAR POR ID
    // =====================================================

    public Optional<Usuario> buscarPorId(
            Integer id
    ) {

        if (id == null) {

            return Optional.empty();
        }


        return usuarioRepository
                .findById(
                        id
                );
    }


    // =====================================================
    // BUSCAR POR DPI HASH
    // =====================================================

    public Optional<Usuario> buscarPorDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null
                || dpiHash.isBlank()) {

            return Optional.empty();
        }


        return usuarioRepository
                .findByDpiHash(
                        dpiHash.trim()
                );
    }


    // =====================================================
    // BUSCAR ACTIVO POR DPI HASH
    // =====================================================

    public Optional<Usuario> buscarActivoPorDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null
                || dpiHash.isBlank()) {

            return Optional.empty();
        }


        return usuarioRepository
                .findByDpiHashAndActivoTrue(
                        dpiHash.trim()
                );
    }


    // =====================================================
    // BUSCAR POR NOMBRE DE USUARIO
    // =====================================================

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


    // =====================================================
    // BUSCAR ACTIVO POR NOMBRE DE USUARIO
    // =====================================================

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


    // =====================================================
    // EXISTE NOMBRE DE USUARIO
    // =====================================================

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


    // =====================================================
    // EXISTE CORREO
    // =====================================================

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


    // =====================================================
    // EXISTE DPI HASH
    // =====================================================

    public boolean existeDpiHash(
            String dpiHash
    ) {

        if (dpiHash == null
                || dpiHash.isBlank()) {

            return false;
        }


        return usuarioRepository
                .existsByDpiHash(
                        dpiHash.trim()
                );
    }


    // =====================================================
    // EXISTE NIT HASH
    // =====================================================

    public boolean existeNitHash(
            String nitHash
    ) {

        if (nitHash == null
                || nitHash.isBlank()) {

            return false;
        }


        return usuarioRepository
                .existsByNitHash(
                        nitHash.trim()
                );
    }


    // =====================================================
    // GUARDAR
    // =====================================================

    @Transactional
    public Usuario guardar(
            Usuario usuario
    ) {

        if (usuario == null) {

            throw new IllegalArgumentException(
                    "El usuario que se desea guardar no puede ser nulo."
            );
        }


        return usuarioRepository
                .save(
                        usuario
                );
    }
}