package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.RolRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.validator.registro.ValidadorRegistroExterno;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;


/**
 * =========================================================
 * SERVICIO: REGISTRO DE USUARIOS EXTERNOS
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidades:
 *
 * - Ejecutar validadores polimórficamente.
 * - Normalizar los datos.
 * - Asignar el rol Paciente.
 * - Proteger DPI y NIT.
 * - Generar hashes para búsquedas.
 * - Proteger la contraseña con BCrypt.
 * - Crear al paciente como usuario activo.
 * - Coordinar la persistencia.
 *
 * La persistencia transaccional se delega a:
 *
 * RegistroExternoPersistenciaService.
 *
 * El correo de bienvenida pertenece a otro servicio.
 *
 * =========================================================
 */
@Service
public class RegistroExternoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

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

    public RegistroExternoService(
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

    public ResultadoValidacionRegistroExterno registrar(
            RegistroExternoDTO formulario
    ) {

        ResultadoValidacionRegistroExterno resultado =
                new ResultadoValidacionRegistroExterno();


        // =================================================
        // VALIDACIÓN DEFENSIVA
        // =================================================

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
    // VALIDACIONES POLIMÓRFICAS
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
    // NORMALIZAR FORMULARIO
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
         * La contraseña NO se modifica.
         *
         * No se aplica trim porque los espacios pueden
         * formar parte de la contraseña elegida.
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
                                                rol
                                                        .getNombre()
                                                        .trim()
                                        )
                )

                .findFirst()

                .orElse(
                        null
                );
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


        // =================================================
        // DATOS GENERALES
        // =================================================

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
        // CONTRASEÑA -> BCrypt
        // =================================================

        usuario.setContrasenaHash(
                passwordEncoder.encode(
                        formulario.getContrasena()
                )
        );


        // =================================================
        // DPI -> CIFRADO + HASH
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
        // NIT -> CIFRADO + HASH
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
         * En CU-02 el paciente externo no selecciona
         * sucursal ni especialidad.
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
        // SEGURIDAD DE LOGIN
        // =================================================

        usuario.setIntentosFallidosLogin(
                (short) 0
        );


        usuario.setFechaBloqueoHasta(
                null
        );


        // =================================================
        // AUDITORÍA DE CREACIÓN
        // =================================================
        //
        // Es un autorregistro.
        // No existe administrador ejecutor.
        // =================================================

        usuario.setCreadoPor(
                null
        );


        usuario.setModificadoPor(
                null
        );


        return usuario;
    }


    // =====================================================
    // LIMPIAR CAMPO OBLIGATORIO
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // LIMPIAR CAMPO OPCIONAL
    // =====================================================

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
    // MANEJAR CONFLICTO DE INTEGRIDAD
    // =====================================================

    private void manejarConflictoIntegridad(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        // =================================================
        // DPI DUPLICADO
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
        // CORREO DUPLICADO
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
        // NOMBRE DE USUARIO DUPLICADO
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
        // NIT DUPLICADO
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


        // =================================================
        // OTRA RESTRICCIÓN DE POSTGRESQL
        // =================================================

        resultado.agregarError(
                "registro",
                "No fue posible completar el registro. Por favor, intente nuevamente."
        );
    }
}