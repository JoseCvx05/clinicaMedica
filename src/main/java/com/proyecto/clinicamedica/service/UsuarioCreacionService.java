package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.EspecialidadRepository;
import com.proyecto.clinicamedica.repository.RolRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.validator.UsuarioFormularioValidator;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * =========================================================
 * SERVICIO: CREACIÓN DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA01 Crear Usuario.
 *
 * Responsabilidades:
 *
 * - Validar formulario.
 * - Validar catálogos.
 * - Validar duplicados.
 * - Aplicar BCrypt.
 * - Cifrar DPI/NIT.
 * - Generar HMAC para DPI/NIT.
 * - Guardar usuario.
 * - Registrar auditoría.
 *
 * =========================================================
 */
@Service
public class UsuarioCreacionService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final Set<String> ROLES_INTERNOS_PERMITIDOS =
            Set.of(
                    "Médico",
                    "Enfermero",
                    "Recepcionista",
                    "Cajero",
                    "Laboratorista",
                    "Farmacéutico",
                    "Administrador"
            );


    private static final String ROL_MEDICO =
            "Médico";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final RolRepository rolRepository;

    private final SucursalRepository sucursalRepository;

    private final EspecialidadRepository especialidadRepository;

    private final HashService hashService;

    private final CifradoService cifradoService;

    private final PasswordEncoder passwordEncoder;

    private final UsuarioFormularioValidator usuarioFormularioValidator;

    private final AuditoriaService auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioCreacionService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            SucursalRepository sucursalRepository,
            EspecialidadRepository especialidadRepository,
            HashService hashService,
            CifradoService cifradoService,
            PasswordEncoder passwordEncoder,
            UsuarioFormularioValidator usuarioFormularioValidator,
            AuditoriaService auditoriaService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.rolRepository =
                rolRepository;

        this.sucursalRepository =
                sucursalRepository;

        this.especialidadRepository =
                especialidadRepository;

        this.hashService =
                hashService;

        this.cifradoService =
                cifradoService;

        this.passwordEncoder =
                passwordEncoder;

        this.usuarioFormularioValidator =
                usuarioFormularioValidator;

        this.auditoriaService =
                auditoriaService;
    }


    // =====================================================
    // FA01 - CREAR USUARIO
    // =====================================================

    @Transactional
    public ResultadoValidacionUsuario crear(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        Rol rol =
                resolverRol(
                        formulario
                );


        boolean rolEsMedico =
                esRolMedico(
                        rol
                );


        ResultadoValidacionUsuario resultado =
                usuarioFormularioValidator
                        .validarCreacion(
                                formulario,
                                rolEsMedico
                        );


        validarRol(
                formulario,
                rol,
                resultado
        );


        Sucursal sucursal =
                resolverSucursal(
                        formulario
                );


        validarSucursal(
                formulario,
                sucursal,
                resultado
        );


        Especialidad especialidad =
                resolverEspecialidad(
                        formulario
                );


        validarEspecialidad(
                formulario,
                rolEsMedico,
                especialidad,
                resultado
        );


        if (resultado.tieneErrores()) {

            return resultado;
        }


        // =================================================
        // NORMALIZAR
        // =================================================

        normalizarFormulario(
                formulario,
                rolEsMedico
        );


        // =================================================
        // HASH DPI / NIT
        // =================================================

        String dpiHash =
                generarHashOpcional(
                        formulario.getDpi()
                );


        String nitHash =
                generarHashOpcional(
                        formulario.getNit()
                );


        // =================================================
        // DUPLICADOS
        // =================================================

        validarDuplicados(
                formulario,
                dpiHash,
                nitHash,
                resultado
        );


        if (resultado.tieneErrores()) {

            return resultado;
        }


        // =================================================
        // USUARIO EJECUTOR
        // =================================================

        Usuario ejecutor =
                obtenerEjecutor(
                        nombreUsuarioEjecutor
                );


        // =================================================
        // CREAR ENTIDAD
        // =================================================

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


        usuario.setContrasenaHash(
                passwordEncoder.encode(
                        formulario.getContrasena()
                )
        );


        asignarDpi(
                usuario,
                formulario.getDpi(),
                dpiHash
        );


        usuario.setTelefono(
                formulario.getTelefono()
        );


        asignarNit(
                usuario,
                formulario.getNit(),
                nitHash
        );


        usuario.setNumeroSeguro(
                formulario.getNumeroSeguro()
        );


        usuario.setRol(
                rol
        );


        usuario.setSucursal(
                sucursal
        );


        usuario.setEspecialidad(
                rolEsMedico
                        ? especialidad
                        : null
        );


        usuario.setActivo(
                Boolean.TRUE.equals(
                        formulario.getActivo()
                )
        );


        usuario.setIntentosFallidosLogin(
                (short) 0
        );


        usuario.setFechaBloqueoHasta(
                null
        );


        usuario.setCreadoPor(
                ejecutor
        );


        usuario.setModificadoPor(
                null
        );


        Usuario guardado =
                usuarioRepository
                        .saveAndFlush(
                                usuario
                        );


        registrarAuditoria(
                guardado,
                ejecutor,
                direccionIp
        );


        return resultado;
    }


    // =====================================================
    // RESOLVER CATÁLOGOS
    // =====================================================

    private Rol resolverRol(
            UsuarioFormularioDTO formulario
    ) {

        if (formulario == null
                || formulario.getIdRol() == null) {

            return null;
        }


        return rolRepository
                .findById(
                        formulario.getIdRol()
                )
                .orElse(
                        null
                );
    }


    private Sucursal resolverSucursal(
            UsuarioFormularioDTO formulario
    ) {

        if (formulario == null
                || formulario.getIdSucursal() == null) {

            return null;
        }


        return sucursalRepository
                .findById(
                        formulario.getIdSucursal()
                )
                .orElse(
                        null
                );
    }


    private Especialidad resolverEspecialidad(
            UsuarioFormularioDTO formulario
    ) {

        if (formulario == null
                || formulario.getIdEspecialidad() == null) {

            return null;
        }


        return especialidadRepository
                .findById(
                        formulario.getIdEspecialidad()
                )
                .orElse(
                        null
                );
    }


    // =====================================================
    // VALIDAR CATÁLOGOS
    // =====================================================

    private void validarRol(
            UsuarioFormularioDTO formulario,
            Rol rol,
            ResultadoValidacionUsuario resultado
    ) {

        if (formulario == null
                || formulario.getIdRol() == null) {

            return;
        }


        if (rol == null
                || !Boolean.TRUE.equals(
                rol.getActivo()
        )) {

            resultado.agregarError(
                    "idRol",
                    "El rol seleccionado no se encuentra disponible."
            );

            return;
        }


        if (!esRolInternoPermitido(
                rol
        )) {

            resultado.agregarError(
                    "idRol",
                    "El rol seleccionado no está permitido para este usuario."
            );
        }
    }


    private void validarSucursal(
            UsuarioFormularioDTO formulario,
            Sucursal sucursal,
            ResultadoValidacionUsuario resultado
    ) {

        if (formulario == null
                || formulario.getIdSucursal() == null) {

            return;
        }


        if (sucursal == null
                || !Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            resultado.agregarError(
                    "idSucursal",
                    "La sucursal seleccionada no se encuentra disponible."
            );
        }
    }


    private void validarEspecialidad(
            UsuarioFormularioDTO formulario,
            boolean rolEsMedico,
            Especialidad especialidad,
            ResultadoValidacionUsuario resultado
    ) {

        if (!rolEsMedico) {

            return;
        }


        /*
         * La obligatoriedad ya es validada
         * por UsuarioFormularioValidator.
         */
        if (formulario == null
                || formulario.getIdEspecialidad() == null) {

            return;
        }


        if (especialidad == null
                || !Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            resultado.agregarError(
                    "idEspecialidad",
                    "La especialidad seleccionada no se encuentra disponible."
            );
        }
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizarFormulario(
            UsuarioFormularioDTO formulario,
            boolean rolEsMedico
    ) {

        formulario.setNombreCompleto(
                formulario
                        .getNombreCompleto()
                        .trim()
        );


        formulario.setCorreoElectronico(
                formulario
                        .getCorreoElectronico()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
        );


        formulario.setNombreUsuario(
                formulario
                        .getNombreUsuario()
                        .trim()
        );


        formulario.setDpi(
                limpiarOpcional(
                        formulario.getDpi()
                )
        );


        formulario.setTelefono(
                limpiarOpcional(
                        formulario.getTelefono()
                )
        );


        String nit =
                limpiarOpcional(
                        formulario.getNit()
                );


        if (nit != null) {

            nit =
                    nit.toUpperCase(
                            Locale.ROOT
                    );
        }


        formulario.setNit(
                nit
        );


        formulario.setNumeroSeguro(
                limpiarOpcional(
                        formulario.getNumeroSeguro()
                )
        );


        if (!rolEsMedico) {

            formulario.setIdEspecialidad(
                    null
            );
        }
    }


    // =====================================================
    // VALIDAR DUPLICADOS
    // =====================================================

    private void validarDuplicados(
            UsuarioFormularioDTO formulario,
            String dpiHash,
            String nitHash,
            ResultadoValidacionUsuario resultado
    ) {

        if (usuarioRepository
                .existsByNombreUsuarioIgnoreCase(
                        formulario.getNombreUsuario()
                )) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El nombre de usuario "
                            + formulario.getNombreUsuario()
                            + " ya se encuentra registrado. "
                            + "Por favor, elija otro."
            );
        }


        if (usuarioRepository
                .existsByCorreoElectronicoIgnoreCase(
                        formulario.getCorreoElectronico()
                )) {

            resultado.agregarError(
                    "correoElectronico",
                    "El correo electrónico ingresado "
                            + "ya se encuentra registrado."
            );
        }


        if (dpiHash != null
                && usuarioRepository
                .existsByDpiHash(
                        dpiHash
                )) {

            resultado.agregarError(
                    "dpi",
                    "El DPI ingresado ya se encuentra registrado."
            );
        }


        if (nitHash != null
                && usuarioRepository
                .existsByNitHash(
                        nitHash
                )) {

            resultado.agregarError(
                    "nit",
                    "El NIT ingresado ya se encuentra registrado."
            );
        }
    }


    // =====================================================
    // DPI
    // =====================================================

    private void asignarDpi(
            Usuario usuario,
            String dpi,
            String dpiHash
    ) {

        if (dpi == null) {

            usuario.setDpiCifrado(
                    null
            );


            usuario.setDpiHash(
                    null
            );


            return;
        }


        usuario.setDpiCifrado(
                cifradoService.cifrar(
                        dpi
                )
        );


        usuario.setDpiHash(
                dpiHash
        );
    }


    // =====================================================
    // NIT
    // =====================================================

    private void asignarNit(
            Usuario usuario,
            String nit,
            String nitHash
    ) {

        if (nit == null) {

            usuario.setNitCifrado(
                    null
            );


            usuario.setNitHash(
                    null
            );


            return;
        }


        usuario.setNitCifrado(
                cifradoService.cifrar(
                        nit
                )
        );


        usuario.setNitHash(
                nitHash
        );
    }


    // =====================================================
    // GENERAR HASH OPCIONAL
    // =====================================================

    private String generarHashOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }


        return hashService.generarHash(
                valor
        );
    }


    // =====================================================
    // OBTENER EJECUTOR
    // =====================================================

    private Usuario obtenerEjecutor(
            String nombreUsuarioEjecutor
    ) {

        if (nombreUsuarioEjecutor == null
                || nombreUsuarioEjecutor.isBlank()) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario "
                            + "que realiza la operación."
            );
        }


        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuarioEjecutor.trim()
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No se encontró al usuario "
                                                + "que realiza la operación."
                                )
                );
    }


    // =====================================================
    // AUDITORÍA
    // =====================================================

    private void registrarAuditoria(
            Usuario creado,
            Usuario ejecutor,
            String direccionIp
    ) {

        RegistroAuditoria registro =
                new RegistroAuditoria(
                        "usuario",
                        String.valueOf(
                                creado.getId()
                        ),
                        AccionAuditoria.CREAR,
                        ejecutor.getId(),
                        ejecutor.getNombreUsuario(),
                        null,
                        crearSnapshot(
                                creado
                        ),
                        direccionIp
                );


        auditoriaService.registrar(
                registro
        );
    }


    // =====================================================
    // SNAPSHOT PARA AUDITORÍA
    // =====================================================

    private Map<String, Object> crearSnapshot(
            Usuario usuario
    ) {

        Map<String, Object> datos =
                new LinkedHashMap<>();


        datos.put(
                "nombreCompleto",
                usuario.getNombreCompleto()
        );


        datos.put(
                "correoElectronico",
                usuario.getCorreoElectronico()
        );


        datos.put(
                "nombreUsuario",
                usuario.getNombreUsuario()
        );


        datos.put(
                "rol",
                usuario.getRol() != null
                        ? usuario.getRol().getNombre()
                        : null
        );


        datos.put(
                "sucursal",
                usuario.getSucursal() != null
                        ? usuario.getSucursal().getNombre()
                        : null
        );


        datos.put(
                "especialidad",
                usuario.getEspecialidad() != null
                        ? usuario.getEspecialidad().getNombre()
                        : null
        );


        datos.put(
                "activo",
                usuario.getActivo()
        );


        /*
         * Nunca incluimos:
         *
         * - contraseña;
         * - DPI;
         * - NIT;
         * - hashes;
         * - valores cifrados.
         */

        return datos;
    }


    // =====================================================
    // ¿ES MÉDICO?
    // =====================================================

    private boolean esRolMedico(
            Rol rol
    ) {

        return rol != null
                && rol.getNombre() != null
                && ROL_MEDICO.equalsIgnoreCase(
                rol.getNombre()
        );
    }


    // =====================================================
    // ¿ES ROL INTERNO PERMITIDO?
    // =====================================================

    private boolean esRolInternoPermitido(
            Rol rol
    ) {

        if (rol == null
                || rol.getNombre() == null) {

            return false;
        }


        return ROLES_INTERNOS_PERMITIDOS
                .stream()
                .anyMatch(
                        permitido ->
                                permitido
                                        .equalsIgnoreCase(
                                                rol.getNombre()
                                        )
                );
    }


    // =====================================================
    // LIMPIAR VALORES OPCIONALES
    // =====================================================

    private String limpiarOpcional(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isBlank()
                ? null
                : limpio;
    }
}