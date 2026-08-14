package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.EstadoVerificacionDpi;
import com.proyecto.clinicamedica.dto.VerificacionDpiResponse;

import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: VERIFICACIÓN DE PACIENTE
 * =========================================================
 *
 * CU-00.
 *
 * Verifica si un DPI corresponde a:
 *
 * - Un paciente registrado.
 * - Un usuario no registrado.
 * - Un usuario interno.
 *
 * SEGURIDAD:
 *
 * El DPI original nunca se consulta directamente
 * contra PostgreSQL.
 *
 * Primero se genera su HMAC-SHA-256 mediante HashService.
 *
 * Este servicio NO administra:
 *
 * - Contraseñas.
 * - Intentos fallidos.
 * - Bloqueos.
 * - JWT.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class VerificacionPacienteService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_PACIENTE =
            "Paciente";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final HashService hashService;

    private final UsuarioService usuarioService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public VerificacionPacienteService(
            HashService hashService,
            UsuarioService usuarioService
    ) {

        this.hashService =
                hashService;

        this.usuarioService =
                usuarioService;
    }


    // =====================================================
    // VERIFICAR DPI
    // =====================================================

    public VerificacionDpiResponse verificar(
            String dpi
    ) {

        // =================================================
        // VALIDACIÓN DEFENSIVA
        // =================================================
        //
        // Normalmente el Controller ya habrá aplicado
        // @DpiValido.
        //
        // Esta validación protege el servicio si es
        // utilizado directamente desde otra clase.
        // =================================================

        if (dpi == null
                || dpi.isBlank()) {

            throw new IllegalArgumentException(
                    "El DPI no puede estar vacío."
            );
        }


        // =================================================
        // GENERAR HMAC DEL DPI
        // =================================================

        String dpiHash =
                hashService.generarHash(
                        dpi
                );


        // =================================================
        // BUSCAR USUARIO MEDIANTE HASH
        // =================================================

        Optional<Usuario> usuarioEncontrado =
                usuarioService
                        .buscarPorDpiHash(
                                dpiHash
                        );


        // =================================================
        // FA03 - DPI NO REGISTRADO
        // =================================================

        if (usuarioEncontrado.isEmpty()) {

            return new VerificacionDpiResponse(
                    EstadoVerificacionDpi.NO_REGISTRADO,

                    "No se encontró un registro asociado a este DPI. "
                            + "Será redirigido al formulario de registro.",

                    "/registro"
            );
        }


        Usuario usuario =
                usuarioEncontrado.get();


        // =================================================
        // VERIFICAR ROL
        // =================================================

        boolean esPaciente =
                usuario.getRol() != null

                        && usuario
                        .getRol()
                        .getNombre() != null

                        && ROL_PACIENTE
                        .equalsIgnoreCase(
                                usuario
                                        .getRol()
                                        .getNombre()
                                        .trim()
                        );


        // =================================================
        // FA04 - USUARIO INTERNO
        // =================================================

        if (!esPaciente) {

            return new VerificacionDpiResponse(
                    EstadoVerificacionDpi.USUARIO_INTERNO,

                    "Este DPI pertenece a un usuario del sistema interno. "
                            + "Por favor, contacte a recepción.",

                    null
            );
        }


        // =================================================
        // FLUJO NORMAL - PACIENTE REGISTRADO
        // =================================================

        return new VerificacionDpiResponse(
                EstadoVerificacionDpi.PACIENTE_REGISTRADO,

                "Bienvenido(a), "
                        + usuario.getNombreCompleto()
                        + ". Será redirigido al inicio de sesión "
                        + "para continuar con el agendamiento de cita.",

                "/login"
        );
    }
}