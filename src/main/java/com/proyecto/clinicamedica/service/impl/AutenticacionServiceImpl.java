package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.EstadoLogin;
import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.service.AutenticacionService;
import com.proyecto.clinicamedica.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO DE AUTENTICACIÓN
 * =========================================================
 *
 * Implementa el inicio de sesión de pacientes del CU-00.
 *
 * Flujos cubiertos:
 *
 * - Flujo normal:
 *      credenciales correctas + rol Paciente.
 *
 * - FA06:
 *      credenciales incorrectas.
 *
 * - FA07:
 *      máximo 5 intentos y bloqueo temporal
 *      durante 15 minutos.
 *
 * - FA09:
 *      credenciales correctas pero rol distinto
 *      de Paciente.
 *
 * Esta clase NO genera JWT.
 * Esa responsabilidad pertenecerá a JwtService.
 *
 * Aplica:
 *
 * - SRP
 * - DIP
 * - Polimorfismo
 * - Separación de responsabilidades
 * =========================================================
 */
@Service
public class AutenticacionServiceImpl
        implements AutenticacionService {

    // =====================================================
    // REGLAS DEL CU-00
    // =====================================================

    private static final int MAXIMO_INTENTOS = 5;

    private static final int MINUTOS_BLOQUEO = 15;

    private static final String ROL_PACIENTE =
            "Paciente";


    private final UsuarioService usuarioService;

    private final PasswordEncoder passwordEncoder;


    /**
     * Inyección mediante constructor.
     */
    public AutenticacionServiceImpl(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }


    // =====================================================
    // AUTENTICAR
    // =====================================================

    @Override
    @Transactional
    public ResultadoAutenticacion autenticar(
            String nombreUsuario,
            String contrasena
    ) {

        // =================================================
        // 1. VALIDACIÓN DEFENSIVA
        // =================================================

        if (nombreUsuario == null
                || nombreUsuario.isBlank()
                || contrasena == null
                || contrasena.isBlank()) {

            return resultadoCredencialesIncorrectas(
                    MAXIMO_INTENTOS - 1
            );
        }


        String usuarioNormalizado =
                nombreUsuario.trim();


        // =================================================
        // 2. BUSCAR USUARIO
        // =================================================

        Optional<Usuario> usuarioOptional =
                usuarioService.buscarPorNombreUsuario(
                        usuarioNormalizado
                );


        // =================================================
        // 3. USUARIO INEXISTENTE
        // =================================================
        //
        // No revelamos que el nombre de usuario
        // no existe.
        // =================================================

        if (usuarioOptional.isEmpty()) {

            return resultadoCredencialesIncorrectas(
                    MAXIMO_INTENTOS - 1
            );
        }


        Usuario usuario =
                usuarioOptional.get();


        // =================================================
        // 4. USUARIO INACTIVO
        // =================================================
        //
        // No permitimos autenticar cuentas inactivas.
        // Tampoco revelamos ese estado al público.
        // =================================================

        if (!Boolean.TRUE.equals(usuario.getActivo())) {

            return resultadoCredencialesIncorrectas(
                    MAXIMO_INTENTOS - 1
            );
        }


        // =================================================
        // 5. COMPROBAR BLOQUEO ACTUAL
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now();

        OffsetDateTime bloqueadoHasta =
                usuario.getFechaBloqueoHasta();


        if (bloqueadoHasta != null) {

            /*
             * Todavía se encuentra bloqueado.
             */
            if (bloqueadoHasta.isAfter(ahora)) {

                return resultadoCuentaBloqueada(
                        bloqueadoHasta
                );
            }


            /*
             * El bloqueo ya venció.
             *
             * Reiniciamos:
             *
             * intentos = 0
             * fechaBloqueoHasta = null
             */
            usuario.setIntentosFallidosLogin(
                    (short) 0
            );

            usuario.setFechaBloqueoHasta(
                    null
            );

            usuarioService.guardar(
                    usuario
            );
        }


        // =================================================
        // 6. VALIDAR CONTRASEÑA
        // =================================================

        boolean contrasenaCorrecta =
                passwordEncoder.matches(
                        contrasena,
                        usuario.getContrasenaHash()
                );


        // =================================================
        // 7. FA06 / FA07 - CONTRASEÑA INCORRECTA
        // =================================================

        if (!contrasenaCorrecta) {

            return procesarIntentoFallido(
                    usuario
            );
        }


        // =================================================
        // 8. CREDENCIALES CORRECTAS
        // REINICIAR INTENTOS
        // =================================================

        usuario.setIntentosFallidosLogin(
                (short) 0
        );

        usuario.setFechaBloqueoHasta(
                null
        );

        usuarioService.guardar(
                usuario
        );


        // =================================================
        // 9. FA09 - VALIDAR ROL PACIENTE
        // =================================================

        boolean esPaciente =
                usuario.getRol() != null
                        && usuario.getRol().getNombre() != null
                        && ROL_PACIENTE.equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                );


        if (!esPaciente) {

            LoginResponse response =
                    new LoginResponse(
                            EstadoLogin.ROL_NO_AUTORIZADO,

                            "Este acceso es exclusivo para pacientes. "
                                    + "Si es personal del hospital, "
                                    + "use el panel administrativo.",

                            "/panel-administrativo",

                            null,

                            null
                    );


            return new ResultadoAutenticacion(
                    response,
                    null
            );
        }


        // =================================================
        // 10. FLUJO NORMAL
        // =================================================

        LoginResponse response =
                new LoginResponse(
                        EstadoLogin.AUTENTICADO,

                        "Inicio de sesión exitoso.",

                        "/paciente/dashboard",

                        null,

                        null
                );


        return new ResultadoAutenticacion(
                response,
                usuario
        );
    }


    // =====================================================
    // PROCESAR INTENTO FALLIDO
    // =====================================================

    private ResultadoAutenticacion procesarIntentoFallido(
            Usuario usuario
    ) {

        int intentosActuales =
                usuario.getIntentosFallidosLogin() == null
                        ? 0
                        : usuario.getIntentosFallidosLogin();


        int nuevosIntentos =
                intentosActuales + 1;


        usuario.setIntentosFallidosLogin(
                (short) nuevosIntentos
        );


        // =================================================
        // FA07 - QUINTO INTENTO
        // =================================================

        if (nuevosIntentos >= MAXIMO_INTENTOS) {

            OffsetDateTime bloqueadoHasta =
                    OffsetDateTime.now()
                            .plusMinutes(
                                    MINUTOS_BLOQUEO
                            );


            usuario.setFechaBloqueoHasta(
                    bloqueadoHasta
            );


            usuarioService.guardar(
                    usuario
            );


            return resultadoCuentaBloqueada(
                    bloqueadoHasta
            );
        }


        // =================================================
        // FA06 - TODAVÍA QUEDAN INTENTOS
        // =================================================

        usuarioService.guardar(
                usuario
        );


        int intentosRestantes =
                MAXIMO_INTENTOS
                        - nuevosIntentos;


        return resultadoCredencialesIncorrectas(
                intentosRestantes
        );
    }


    // =====================================================
    // RESPUESTA FA06
    // =====================================================

    private ResultadoAutenticacion
    resultadoCredencialesIncorrectas(
            int intentosRestantes
    ) {

        LoginResponse response =
                new LoginResponse(
                        EstadoLogin.CREDENCIALES_INCORRECTAS,

                        "Usuario o contraseña incorrectos. "
                                + "Intentos restantes: "
                                + intentosRestantes
                                + ".",

                        null,

                        intentosRestantes,

                        null
                );


        return new ResultadoAutenticacion(
                response,
                null
        );
    }


    // =====================================================
    // RESPUESTA FA07
    // =====================================================

    private ResultadoAutenticacion
    resultadoCuentaBloqueada(
            OffsetDateTime bloqueadoHasta
    ) {

        LoginResponse response =
                new LoginResponse(
                        EstadoLogin.CUENTA_BLOQUEADA,

                        "Cuenta bloqueada temporalmente. "
                                + "Intente de nuevo en 15 minutos.",

                        null,

                        0,

                        bloqueadoHasta
                );


        return new ResultadoAutenticacion(
                response,
                null
        );
    }
}