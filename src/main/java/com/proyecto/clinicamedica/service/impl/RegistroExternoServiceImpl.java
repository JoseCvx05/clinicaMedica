package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.RolRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.service.CifradoService;
import com.proyecto.clinicamedica.service.HashService;
import com.proyecto.clinicamedica.service.RegistroExternoService;

import com.proyecto.clinicamedica.validator.registro.ValidadorRegistroExterno;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.proyecto.clinicamedica.service.RegistroExternoPersistenciaService;

import org.springframework.dao.DataIntegrityViolationException;
/**
 * =========================================================
 * IMPLEMENTACIÓN: REGISTRO DE USUARIOS EXTERNOS
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidades:
 *
 * - Ejecutar validadores polimórficamente.
 * - Normalizar datos.
 * - Asignar rol Paciente.
 * - Cifrar DPI y NIT.
 * - Generar hashes de búsqueda.
 * - Cifrar contraseña con BCrypt.
 * - Registrar al paciente activo.
 *
 * El correo de bienvenida pertenece a otro servicio.
 * =========================================================
 */
@Service
public class RegistroExternoServiceImpl
        implements RegistroExternoService {


    private static final String ROL_PACIENTE =
            "Paciente";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final RolRepository rolRepository;

    private final HashService hashService;

    private final CifradoService cifradoService;

    private final PasswordEncoder passwordEncoder;

    private final List<ValidadorRegistroExterno>
            validadores;

    private final RegistroExternoPersistenciaService
            persistenciaService;
    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RegistroExternoServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            HashService hashService,
            CifradoService cifradoService,
            PasswordEncoder passwordEncoder,
            List<ValidadorRegistroExterno> validadores,
            RegistroExternoPersistenciaService persistenciaService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.rolRepository =
                rolRepository;

        this.hashService =
                hashService;

        this.cifradoService =
                cifradoService;

        this.passwordEncoder =
                passwordEncoder;

        this.validadores =
                validadores;

        this.persistenciaService =
                persistenciaService;
    }


    // =====================================================
    // REGISTRAR
    // =====================================================

    @Override
    public ResultadoValidacionRegistroExterno registrar(
            RegistroExternoDTO formulario
    ) {

        ResultadoValidacionRegistroExterno resultado =
                new ResultadoValidacionRegistroExterno();


        if (formulario == null) {

            resultado.agregarError(
                    "registro",
                    "No fue posible procesar el formulario de registro."
            );

            return resultado;
        }


        // =================================================
        // 1. VALIDACIONES POLIMÓRFICAS
        // =================================================

        ejecutarValidaciones(
                formulario,
                resultado
        );


        if (resultado.tieneErrores()) {

            return resultado;
        }


        // =================================================
        // 2. NORMALIZAR DATOS
        // =================================================

        normalizarFormulario(
                formulario
        );


        // =================================================
        // 3. OBTENER ROL PACIENTE
        // =================================================

        Rol rolPaciente =
                obtenerRolPaciente();


        if (rolPaciente == null) {

            resultado.agregarError(
                    "registro",
                    "No fue posible completar el registro en este momento."
            );

            return resultado;
        }


        // =================================================
        // 4. PROTEGER DPI
        // =================================================

        String dpiHash =
                hashService.generarHash(
                        formulario.getDpi()
                );


        String dpiCifrado =
                cifradoService.cifrar(
                        formulario.getDpi()
                );


        // =================================================
        // 5. PROTEGER NIT
        // =================================================

        String nitHash =
                hashService.generarHash(
                        formulario.getNit()
                );


        String nitCifrado =
                cifradoService.cifrar(
                        formulario.getNit()
                );


        // =================================================
        // 6. CONSTRUIR USUARIO
        // =================================================

        Usuario usuario =
                construirUsuario(
                        formulario,
                        rolPaciente,
                        dpiHash,
                        dpiCifrado,
                        nitHash,
                        nitCifrado
                );

        // =================================================
// 7. PERSISTIR USUARIO
// =================================================

        try {

            persistenciaService.guardar(
                    usuario
            );

        } catch (DataIntegrityViolationException ex) {

            manejarConflictoIntegridad(
                    formulario,
                    resultado
            );
        }


        return resultado;

    }

// =====================================================
// POLIMORFISMO
// =====================================================

    private void ejecutarValidaciones(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        validadores
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                ValidadorRegistroExterno::orden
                        )
                )
                .forEach(
                        validador ->
                                validador.validar(
                                        formulario,
                                        resultado
                                )
                );
    }

    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizarFormulario(
            RegistroExternoDTO formulario
    ) {

        formulario.setNombreCompleto(
                limpiar(
                        formulario.getNombreCompleto()
                )
        );


        formulario.setDpi(
                limpiar(
                        formulario.getDpi()
                )
        );


        formulario.setNit(
                limpiar(
                        formulario.getNit()
                ).toUpperCase(
                        Locale.ROOT
                )
        );


        formulario.setTelefono(
                limpiar(
                        formulario.getTelefono()
                )
        );


        formulario.setNumeroSeguro(
                limpiarOpcional(
                        formulario.getNumeroSeguro()
                )
        );


        formulario.setCorreoElectronico(
                limpiar(
                        formulario.getCorreoElectronico()
                ).toLowerCase(
                        Locale.ROOT
                )
        );


        formulario.setNombreUsuario(
                limpiar(
                        formulario.getNombreUsuario()
                )
        );


        /*
         * La contraseña NO se modifica ni se hace trim.
         */
    }


    // =====================================================
    // OBTENER ROL PACIENTE
    // =====================================================

    private Rol obtenerRolPaciente() {

        return rolRepository
                .findAll()
                .stream()
                .filter(
                        rol ->
                                Boolean.TRUE.equals(
                                        rol.getActivo()
                                )
                )
                .filter(
                        rol ->
                                rol.getNombre() != null
                                        && ROL_PACIENTE
                                        .equalsIgnoreCase(
                                                rol.getNombre()
                                        )
                )
                .findFirst()
                .orElse(null);
    }


    // =====================================================
    // CONSTRUIR USUARIO
    // =====================================================

    private Usuario construirUsuario(
            RegistroExternoDTO formulario,
            Rol rolPaciente,
            String dpiHash,
            String dpiCifrado,
            String nitHash,
            String nitCifrado
    ) {

        Usuario usuario =
                new Usuario();


        usuario.setNombreCompleto(
                formulario.getNombreCompleto()
        );


        usuario.setCorreoElectronico(
                formulario.getCorreoElectronico()
        );


        usuario.setNombreUsuario(
                formulario.getNombreUsuario()
        );


        // =================================================
        // CONTRASEÑA → BCrypt
        // =================================================

        usuario.setContrasenaHash(
                passwordEncoder.encode(
                        formulario.getContrasena()
                )
        );


        // =================================================
        // DPI → AES-GCM + HMAC
        // =================================================

        usuario.setDpiCifrado(
                dpiCifrado
        );

        usuario.setDpiHash(
                dpiHash
        );


        // =================================================
        // TELÉFONO
        // =================================================

        usuario.setTelefono(
                formulario.getTelefono()
        );


        // =================================================
        // NIT → AES-GCM + HMAC
        // =================================================

        usuario.setNitCifrado(
                nitCifrado
        );

        usuario.setNitHash(
                nitHash
        );


        // =================================================
        // SEGURO
        // =================================================

        usuario.setNumeroSeguro(
                formulario.getNumeroSeguro()
        );


        // =================================================
        // ROL
        // =================================================

        usuario.setRol(
                rolPaciente
        );


        /*
         * El paciente externo no selecciona sucursal
         * ni especialidad durante CU-02.
         */
        usuario.setSucursal(
                null
        );

        usuario.setEspecialidad(
                null
        );


        // =================================================
        // ESTADO
        // =================================================

        usuario.setActivo(
                true
        );


        // =================================================
        // SEGURIDAD LOGIN
        // =================================================

        usuario.setIntentosFallidosLogin(
                (short) 0
        );

        usuario.setFechaBloqueoHasta(
                null
        );


        /*
         * Es un autorregistro.
         * No existe un administrador ejecutor.
         */
        usuario.setCreadoPor(
                null
        );

        usuario.setModificadoPor(
                null
        );


        return usuario;
    }


    // =====================================================
    // UTILIDADES
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    private String limpiarOpcional(
            String valor
    ) {

        String limpio =
                limpiar(
                        valor
                );


        return limpio.isEmpty()
                ? null
                : limpio;
    }
        // =====================================================
// CONFLICTO DE INTEGRIDAD
// =====================================================

        private void manejarConflictoIntegridad(
                RegistroExternoDTO formulario,
                ResultadoValidacionRegistroExterno resultado
) {

            // =================================================
            // DPI
            // =================================================

            String dpiHash =
                    hashService.generarHash(
                            formulario.getDpi()
                    );


            if (usuarioRepository.existsByDpiHash(
                    dpiHash
            )) {

                resultado.agregarError(
                        "dpi",

                        "Ya existe una cuenta registrada con este "
                                + "número de DPI. Si ya tiene cuenta, "
                                + "inicie sesión."
                );

                return;
            }


            // =================================================
            // CORREO
            // =================================================

            if (usuarioRepository
                    .existsByCorreoElectronicoIgnoreCase(
                            formulario.getCorreoElectronico()
                    )) {

                resultado.agregarError(
                        "correoElectronico",
                        "Ya existe una cuenta registrada con este correo electrónico."
                );

                return;
            }


            // =================================================
            // USUARIO
            // =================================================

            if (usuarioRepository
                    .existsByNombreUsuarioIgnoreCase(
                            formulario.getNombreUsuario()
                    )) {

                resultado.agregarError(
                        "nombreUsuario",
                        "El nombre de usuario ya se encuentra registrado."
                );

                return;
            }


            // =================================================
            // NIT
            // =================================================

            String nitHash =
                    hashService.generarHash(
                            formulario.getNit()
                    );


            if (usuarioRepository.existsByNitHash(
                    nitHash
            )) {

                resultado.agregarError(
                        "nit",
                        "Ya existe una cuenta registrada con este NIT."
                );

                return;
            }


            /*
             * Si PostgreSQL rechazó el INSERT por alguna otra
             * restricción que no sea una de las anteriores,
             * presentamos un error controlado.
             */
            resultado.agregarError(
                    "registro",
                    "No fue posible completar el registro. Por favor, intente nuevamente."
            );
        }
}