package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.EstadoLogin;
import com.proyecto.clinicamedica.dto.LoginResponse;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.security.PoliticaAutenticacion;
import com.proyecto.clinicamedica.security.PoliticaAutenticacionResolver;
import com.proyecto.clinicamedica.security.ResultadoAutenticacion;
import com.proyecto.clinicamedica.security.TipoAcceso;
import com.proyecto.clinicamedica.service.AutenticacionService;
import com.proyecto.clinicamedica.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN COMÚN DEL SERVICIO DE AUTENTICACIÓN
 * =========================================================
 *
 * Contiene la lógica común utilizada por:
 *
 * - Portal de pacientes.
 * - Portal del personal interno.
 *
 * La lógica que cambia según el portal se delega a:
 *
 * PoliticaAutenticacion
 *
 * Implementaciones actuales:
 *
 * - PoliticaPaciente
 * - PoliticaInterna
 *
 * =========================================================
 */
@Service
public class AutenticacionServiceImpl
        implements AutenticacionService {

    // =====================================================
    // REGLAS DE BLOQUEO
    // =====================================================

    /**
     * CU-00 establece máximo 5 intentos consecutivos.
     *
     * Esta infraestructura se reutiliza también para
     * autenticación interna.
     */
    private static final int MAXIMO_INTENTOS = 5;


    /**
     * CU-00 establece bloqueo temporal de 15 minutos.
     */
    private static final int MINUTOS_BLOQUEO = 15;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioService usuarioService;

    private final PasswordEncoder passwordEncoder;

    private final PoliticaAutenticacionResolver
            politicaAutenticacionResolver;


    public AutenticacionServiceImpl(
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

    @Override
    @Transactional
    public ResultadoAutenticacion autenticar(
            String nombreUsuario,
            String contrasena,
            TipoAcceso tipoAcceso
    ) {

        // =================================================
        // 1. RESOLVER POLÍTICA
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
        // No revelamos si el nombre ingresado realmente
        // existe en el sistema.
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
        // 5. USUARIO INACTIVO
        // =================================================
        //
        // Tampoco revelamos públicamente que la cuenta
        // se encuentra desactivada.
        // =================================================

        if (!Boolean.TRUE.equals(
                usuario.getActivo()
        )) {

            return resultadoCredencialesIncorrectas(
                    politica,
                    MAXIMO_INTENTOS - 1
            );
        }


        // =================================================
        // 6. COMPROBAR BLOQUEO VIGENTE
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now();


        OffsetDateTime bloqueadoHasta =
                usuario.getFechaBloqueoHasta();


        if (bloqueadoHasta != null) {

            // ---------------------------------------------
            // BLOQUEO AÚN VIGENTE
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
            // BLOQUEO YA VENCIDO
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
        // 8. CONTRASEÑA INCORRECTA
        // =================================================

        if (!contrasenaCorrecta) {

            return procesarIntentoFallido(
                    usuario,
                    politica
            );
        }


        // =================================================
        // 9. CONTRASEÑA CORRECTA
        // =================================================
        //
        // Los intentos fallidos son consecutivos.
        // Una autenticación correcta los reinicia.
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
        // 10. VALIDAR POLÍTICA DEL PORTAL
        // =================================================
        //
        // Ejemplos:
        //
        // PACIENTE:
        // solamente rol Paciente.
        //
        // INTERNO:
        // cualquiera de los roles internos permitidos.
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
        // ALCANZÓ EL MÁXIMO
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
}