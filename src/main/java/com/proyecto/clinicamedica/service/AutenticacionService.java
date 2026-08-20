package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.EstadoLogin;
import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.security.PoliticaAutenticacion;
import com.proyecto.clinicamedica.security.PoliticaAutenticacionResolver;
import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.security.TipoAcceso;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: AUTENTICACIÓN
 * =========================================================
 *
 * Contiene la lógica común de autenticación utilizada por:
 *
 * - Portal de pacientes.
 * - Portal del personal interno.
 *
 * Las diferencias entre ambos portales se resuelven
 * mediante PoliticaAutenticacion.
 *
 * Políticas actuales:
 *
 * - PoliticaPaciente
 * - PoliticaInterna
 *
 * =========================================================
 */
@Service
public class AutenticacionService {


    // =====================================================
    // REGLAS DE BLOQUEO
    // =====================================================

    /**
     * Máximo de intentos consecutivos permitidos.
     */
    private static final int MAXIMO_INTENTOS =
            5;


    /**
     * Duración del bloqueo temporal.
     */
    private static final int MINUTOS_BLOQUEO =
            15;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioService usuarioService;

    private final PasswordEncoder passwordEncoder;

    private final PoliticaAutenticacionResolver
            politicaAutenticacionResolver;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AutenticacionService(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            PoliticaAutenticacionResolver
                    politicaAutenticacionResolver
    ) {

        this.usuarioService =
                usuarioService;

        this.passwordEncoder =
                passwordEncoder;

        this.politicaAutenticacionResolver =
                politicaAutenticacionResolver;
    }


    // =====================================================
    // AUTENTICAR
    // =====================================================

    @Transactional
    public ResultadoAutenticacion autenticar(
            String nombreUsuario,
            String contrasena,
            TipoAcceso tipoAcceso
    ) {

        // =================================================
        // 1. RESOLVER POLÍTICA DEL PORTAL
        // =================================================

        PoliticaAutenticacion politica =
                politicaAutenticacionResolver
                        .resolver(
                                tipoAcceso
                        );


        // =================================================
        // 2. VALIDACIÓN DEFENSIVA
        // =================================================

        if (nombreUsuario == null
                || nombreUsuario.isBlank()
                || contrasena == null
                || contrasena.isBlank()) {

            return resultadoCredencialesIncorrectas(
                    politica,
                    MAXIMO_INTENTOS - 1
            );
        }


        String usuarioNormalizado =
                nombreUsuario.trim();


        // =================================================
        // 3. BUSCAR USUARIO
        // =================================================

        Optional<Usuario> usuarioOptional =
                usuarioService
                        .buscarPorNombreUsuario(
                                usuarioNormalizado
                        );


        // =================================================
        // 4. USUARIO INEXISTENTE
        // =================================================
        //
        // No se revela públicamente si el nombre de
        // usuario existe o no.
        // =================================================

        if (usuarioOptional.isEmpty()) {

            return resultadoCredencialesIncorrectas(
                    politica,
                    MAXIMO_INTENTOS - 1
            );
        }


        Usuario usuario =
                usuarioOptional.get();

        // =================================================
        // 6. COMPROBAR BLOQUEO
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now();


        OffsetDateTime bloqueadoHasta =
                usuario.getFechaBloqueoHasta();


        if (bloqueadoHasta != null) {

            // ---------------------------------------------
            // BLOQUEO TODAVÍA ACTIVO
            // ---------------------------------------------

            if (bloqueadoHasta.isAfter(
                    ahora
            )) {

                return resultadoCuentaBloqueada(
                        politica,
                        bloqueadoHasta
                );
            }


            // ---------------------------------------------
            // BLOQUEO VENCIDO
            // ---------------------------------------------

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
        // 7. VALIDAR CONTRASEÑA
        // =================================================

        boolean contrasenaCorrecta =
                passwordEncoder.matches(
                        contrasena,
                        usuario.getContrasenaHash()
                );


        // =================================================
// CONTRASEÑA INCORRECTA
// =================================================
//
// Aunque la cuenta esté inactiva, una contraseña
// incorrecta sigue siendo un intento fallido.
// =================================================

        if (!contrasenaCorrecta) {

            return procesarIntentoFallido(
                    usuario,
                    politica
            );
        }


// =================================================
// CONTRASEÑA CORRECTA
// =================================================
//
// Como las credenciales fueron correctas,
// reiniciamos los intentos consecutivos.
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
// USUARIO INACTIVO
// =================================================
//
// La contraseña fue correcta, pero una cuenta
// inactiva no puede acceder al sistema.
// =================================================

        if (!Boolean.TRUE.equals(
                usuario.getActivo()
        )) {

            return resultadoCuentaInactiva();
        }


        // =================================================
        // 10. VALIDAR POLÍTICA DEL PORTAL
        // =================================================

        if (!politica.permiteAcceso(
                usuario
        )) {

            LoginResponse respuesta =
                    new LoginResponse(
                            EstadoLogin.ROL_NO_AUTORIZADO,

                            politica
                                    .getMensajeRolNoAutorizado(),

                            null,

                            null,

                            null
                    );


            return new ResultadoAutenticacion(
                    respuesta,
                    null
            );
        }


        // =================================================
        // 11. AUTENTICACIÓN EXITOSA
        // =================================================

        LoginResponse respuesta =
                new LoginResponse(
                        EstadoLogin.AUTENTICADO,

                        politica
                                .getMensajeAutenticacionExitosa(),

                        politica
                                .getRedireccionExitosa(
                                        usuario
                                ),

                        null,

                        null
                );


        return new ResultadoAutenticacion(
                respuesta,
                usuario
        );
    }


    // =====================================================
    // PROCESAR INTENTO FALLIDO
    // =====================================================

    private ResultadoAutenticacion procesarIntentoFallido(
            Usuario usuario,
            PoliticaAutenticacion politica
    ) {

        int intentosActuales =
                usuario.getIntentosFallidosLogin()
                        == null
                        ? 0
                        : usuario
                        .getIntentosFallidosLogin();


        int nuevosIntentos =
                intentosActuales + 1;


        usuario.setIntentosFallidosLogin(
                (short) nuevosIntentos
        );


        // =================================================
        // ALCANZÓ EL MÁXIMO DE INTENTOS
        // =================================================

        if (nuevosIntentos
                >= MAXIMO_INTENTOS) {

            OffsetDateTime bloqueadoHasta =
                    OffsetDateTime
                            .now()
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
                    politica,
                    bloqueadoHasta
            );
        }


        // =================================================
        // TODAVÍA QUEDAN INTENTOS
        // =================================================

        usuarioService.guardar(
                usuario
        );


        int intentosRestantes =
                MAXIMO_INTENTOS
                        - nuevosIntentos;


        return resultadoCredencialesIncorrectas(
                politica,
                intentosRestantes
        );
    }


    // =====================================================
    // RESPUESTA: CREDENCIALES INCORRECTAS
    // =====================================================

    private ResultadoAutenticacion
    resultadoCredencialesIncorrectas(
            PoliticaAutenticacion politica,
            int intentosRestantes
    ) {

        LoginResponse respuesta =
                new LoginResponse(
                        EstadoLogin.CREDENCIALES_INCORRECTAS,

                        politica
                                .getMensajeCredencialesIncorrectas(
                                        intentosRestantes
                                ),

                        null,

                        intentosRestantes,

                        null
                );


        return new ResultadoAutenticacion(
                respuesta,
                null
        );
    }


    // =====================================================
    // RESPUESTA: CUENTA BLOQUEADA
    // =====================================================

    private ResultadoAutenticacion
    resultadoCuentaBloqueada(
            PoliticaAutenticacion politica,
            OffsetDateTime bloqueadoHasta
    ) {

        LoginResponse respuesta =
                new LoginResponse(
                        EstadoLogin.CUENTA_BLOQUEADA,

                        politica
                                .getMensajeCuentaBloqueada(),

                        null,

                        0,

                        bloqueadoHasta
                );


        return new ResultadoAutenticacion(
                respuesta,
                null
        );
    }
    // =====================================================
// RESPUESTA: CUENTA INACTIVA
// =====================================================

    private ResultadoAutenticacion
    resultadoCuentaInactiva() {

        LoginResponse respuesta =
                new LoginResponse(

                        EstadoLogin.CUENTA_INACTIVA,

                        "Este usuario se encuentra inactivo. "
                                + "No puede iniciar sesión ni acceder al sistema.",

                        null,

                        null,

                        null
                );


        return new ResultadoAutenticacion(
                respuesta,
                null
        );
    }
}