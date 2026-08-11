package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.EstadoVerificacionDpi;
import com.proyecto.clinicamedica.dto.VerificacionDpiResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.service.HashService;
import com.proyecto.clinicamedica.service.UsuarioService;
import com.proyecto.clinicamedica.service.VerificacionPacienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN: VERIFICACIÓN DE PACIENTE
 * =========================================================
 *
 * Implementa la verificación del DPI correspondiente
 * al CU-00.
 *
 * Flujos cubiertos:
 *
 * - Flujo normal:
 *      DPI pertenece a un Paciente.
 *
 * - FA03:
 *      DPI no registrado.
 *
 * - FA04:
 *      DPI pertenece a un usuario interno.
 *
 * SEGURIDAD:
 *
 * El DPI original NO se consulta directamente contra
 * PostgreSQL.
 *
 * Se genera primero su HMAC-SHA-256 mediante HashService.
 *
 * SOLID / POLIMORFISMO:
 *
 * Esta implementación depende de abstracciones:
 *
 * - HashService
 * - UsuarioService
 *
 * y no de sus implementaciones concretas.
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class VerificacionPacienteServiceImpl
        implements VerificacionPacienteService {

    /**
     * Nombre del rol externo autorizado para continuar
     * con el flujo del portal.
     */
    private static final String ROL_PACIENTE = "Paciente";

    private final HashService hashService;
    private final UsuarioService usuarioService;


    /**
     * Inyección de dependencias por constructor.
     */
    public VerificacionPacienteServiceImpl(
            HashService hashService,
            UsuarioService usuarioService
    ) {
        this.hashService = hashService;
        this.usuarioService = usuarioService;
    }


    /**
     * Verifica si el DPI pertenece a:
     *
     * - un paciente registrado;
     * - ningún usuario;
     * - un usuario interno.
     */
    @Override
    public VerificacionDpiResponse verificar(String dpi) {

        // =================================================
        // 1. PROTECCIÓN DEFENSIVA
        // =================================================
        //
        // El Controller ya habrá ejecutado @DpiValido.
        // Esta validación evita utilizar incorrectamente
        // el servicio desde otra parte de la aplicación.
        // =================================================

        if (dpi == null || dpi.isBlank()) {

            throw new IllegalArgumentException(
                    "El DPI no puede estar vacío."
            );
        }


        // =================================================
        // 2. GENERAR HMAC DEL DPI
        // =================================================

        String dpiHash =
                hashService.generarHash(dpi);


        // =================================================
        // 3. BUSCAR USUARIO
        // =================================================

        Optional<Usuario> usuarioEncontrado =
                usuarioService.buscarPorDpiHash(dpiHash);


        // =================================================
        // 4. FA03 - DPI NO REGISTRADO
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
        // 5. VERIFICAR ROL
        // =================================================

        boolean esPaciente =
                usuario.getRol() != null
                        && usuario.getRol().getNombre() != null
                        && ROL_PACIENTE.equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                );


        // =================================================
        // 6. FA04 - USUARIO INTERNO
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
        // 7. FLUJO NORMAL - PACIENTE REGISTRADO
        // =================================================
        //
        // El CU-00 establece posteriormente que el paciente
        // debe autenticarse con usuario y contraseña.
        //
        // Por esa razón se dirige a /login y todavía NO
        // directamente al formulario de citas.
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