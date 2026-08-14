package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.ErrorValidacionUsuario;
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
 * SERVICIO: ACTUALIZACIÓN DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA04 Actualizar Usuario.
 *
 * Responsabilidades:
 *
 * - Validar formulario.
 * - Validar catálogos.
 * - Validar duplicados excluyendo el propio usuario.
 * - Actualizar datos.
 * - Cambiar contraseña únicamente si se proporciona.
 * - Aplicar AES-GCM y HMAC a DPI/NIT.
 * - Registrar auditoría.
 *
 * =========================================================
 */
@Service
public class UsuarioActualizacionService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";


    private static final Set<String> ROLES_INTERNOS =
            Set.of(
                    "Médico",
                    "Enfermero",
                    "Recepcionista",
                    "Cajero",
                    "Laboratorista",
                    "Farmacéutico",
                    "Administrador"
            );


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

    private final UsuarioFormularioValidator validator;

    private final AuditoriaService auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioActualizacionService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            SucursalRepository sucursalRepository,
            EspecialidadRepository especialidadRepository,
            HashService hashService,
            CifradoService cifradoService,
            PasswordEncoder passwordEncoder,
            UsuarioFormularioValidator validator,
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

        this.validator =
                validator;

        this.auditoriaService =
                auditoriaService;
    }


    // =====================================================
    // FA04 - ACTUALIZAR USUARIO
    // =====================================================

    @Transactional
    public ResultadoValidacionUsuario actualizar(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        ResultadoValidacionUsuario resultado =
                new ResultadoValidacionUsuario();


        if (formulario == null
                || formulario.getId() == null) {

            resultado.agregarError(
                    "formulario",
                    "El usuario seleccionado no es válido."
            );

            return resultado;
        }


        Usuario usuario =
                usuarioRepository
                        .findById(
                                formulario.getId()
                        )
                        .orElse(
                                null
                        );


        if (usuario == null) {

            resultado.agregarError(
                    "formulario",
                    "El usuario seleccionado no existe."
            );

            return resultado;
        }


        // =================================================
        // ROL
        // =================================================

        Rol rol =
                resolverRol(
                        formulario.getIdRol()
                );


        boolean esMedico =
                rol != null
                        && rol.getNombre() != null
                        && ROL_MEDICO.equalsIgnoreCase(
                        rol.getNombre()
                );


        // =================================================
        // VALIDACIÓN DEL FORMULARIO
        // =================================================

        copiarErrores(
                validator.validarEdicion(
                        formulario,
                        esMedico
                ),
                resultado
        );


        validarRol(
                formulario.getIdRol(),
                rol,
                usuario,
                resultado
        );


        // =================================================
        // SUCURSAL
        // =================================================

        Sucursal sucursal =
                resolverSucursal(
                        formulario.getIdSucursal()
                );


        validarSucursal(
                formulario.getIdSucursal(),
                sucursal,
                resultado
        );


        // =================================================
        // ESPECIALIDAD
        // =================================================

        Especialidad especialidad =
                null;


        if (esMedico) {

            especialidad =
                    resolverEspecialidad(
                            formulario.getIdEspecialidad()
                    );


            validarEspecialidad(
                    formulario.getIdEspecialidad(),
                    especialidad,
                    resultado
            );
        }


        if (resultado.tieneErrores()) {

            return resultado;
        }


        // =================================================
        // NORMALIZACIÓN
        // =================================================

        normalizar(
                formulario
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
        // SNAPSHOT ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(
                        usuario
                );


        // =================================================
        // ACTUALIZAR DATOS
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


        usuario.setTelefono(
                formulario.getTelefono()
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
                esMedico
                        ? especialidad
                        : null
        );


        usuario.setActivo(
                Boolean.TRUE.equals(
                        formulario.getActivo()
                )
        );


        // =================================================
        // DPI / NIT
        // =================================================

        asignarDpi(
                usuario,
                formulario.getDpi(),
                dpiHash
        );


        asignarNit(
                usuario,
                formulario.getNit(),
                nitHash
        );


        // =================================================
        // CONTRASEÑA OPCIONAL
        // =================================================

        if (formulario.getContrasena() != null
                && !formulario
                .getContrasena()
                .isBlank()) {

            usuario.setContrasenaHash(
                    passwordEncoder.encode(
                            formulario.getContrasena()
                    )
            );
        }


        usuario.setModificadoPor(
                ejecutor
        );


        // =================================================
        // GUARDAR
        // =================================================

        Usuario actualizado =
                usuarioRepository
                        .saveAndFlush(
                                usuario
                        );


        // =================================================
        // SNAPSHOT NUEVO
        // =================================================

        Map<String, Object> valoresNuevos =
                crearSnapshot(
                        actualizado
                );


        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "usuario",
                        String.valueOf(
                                actualizado.getId()
                        ),
                        AccionAuditoria.ACTUALIZAR,
                        ejecutor.getId(),
                        ejecutor.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        direccionIp
                )
        );


        return resultado;
    }


    // =====================================================
    // COPIAR ERRORES
    // =====================================================

    private void copiarErrores(
            ResultadoValidacionUsuario origen,
            ResultadoValidacionUsuario destino
    ) {

        for (ErrorValidacionUsuario error :
                origen.getErrores()) {

            destino.agregarError(
                    error.campo(),
                    error.mensaje()
            );
        }
    }


    // =====================================================
    // RESOLVER ROL
    // =====================================================

    private Rol resolverRol(
            Integer id
    ) {

        if (id == null) {

            return null;
        }


        return rolRepository
                .findById(
                        id
                )
                .orElse(
                        null
                );
    }


    // =====================================================
    // RESOLVER SUCURSAL
    // =====================================================

    private Sucursal resolverSucursal(
            Integer id
    ) {

        if (id == null) {

            return null;
        }


        return sucursalRepository
                .findById(
                        id
                )
                .orElse(
                        null
                );
    }


    // =====================================================
    // RESOLVER ESPECIALIDAD
    // =====================================================

    private Especialidad resolverEspecialidad(
            Integer id
    ) {

        if (id == null) {

            return null;
        }


        return especialidadRepository
                .findById(
                        id
                )
                .orElse(
                        null
                );
    }


    // =====================================================
    // VALIDAR ROL
    // =====================================================

    private void validarRol(
            Integer idRol,
            Rol rol,
            Usuario usuarioActual,
            ResultadoValidacionUsuario resultado
    ) {

        if (idRol == null) {

            return;
        }


        if (rol == null
                || !Boolean.TRUE.equals(
                rol.getActivo()
        )) {

            resultado.agregarError(
                    "idRol",
                    "Debe seleccionar un rol válido para el usuario."
            );

            return;
        }


        if (esRolInterno(
                rol
        )) {

            return;
        }


        /*
         * Permite conservar el rol actual de un usuario
         * aunque no sea un rol interno.
         *
         * Ejemplo:
         *
         * Paciente -> Paciente
         */
        boolean conservaRolActual =
                usuarioActual != null
                        && usuarioActual.getRol() != null
                        && usuarioActual
                        .getRol()
                        .getId()
                        .equals(
                                rol.getId()
                        );


        if (!conservaRolActual) {

            resultado.agregarError(
                    "idRol",
                    "Debe seleccionar un rol válido para el usuario."
            );
        }
    }


    // =====================================================
    // VALIDAR SUCURSAL
    // =====================================================

    private void validarSucursal(
            Integer idSucursal,
            Sucursal sucursal,
            ResultadoValidacionUsuario resultado
    ) {

        if (idSucursal == null) {

            return;
        }


        if (sucursal == null
                || !Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            resultado.agregarError(
                    "idSucursal",
                    "Debe seleccionar una sucursal válida."
            );
        }
    }


    // =====================================================
    // VALIDAR ESPECIALIDAD
    // =====================================================

    private void validarEspecialidad(
            Integer idEspecialidad,
            Especialidad especialidad,
            ResultadoValidacionUsuario resultado
    ) {

        if (idEspecialidad == null) {

            return;
        }


        if (especialidad == null
                || !Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            resultado.agregarError(
                    "idEspecialidad",
                    "Debe seleccionar una especialidad para el médico."
            );
        }
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizar(
            UsuarioFormularioDTO formulario
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

        Integer id =
                formulario.getId();


        if (usuarioRepository
                .existsByNombreUsuarioIgnoreCaseAndIdNot(
                        formulario.getNombreUsuario(),
                        id
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
                .existsByCorreoElectronicoIgnoreCaseAndIdNot(
                        formulario.getCorreoElectronico(),
                        id
                )) {

            resultado.agregarError(
                    "correoElectronico",
                    "El correo electrónico ya se encuentra registrado."
            );
        }


        if (dpiHash != null
                && usuarioRepository
                .existsByDpiHashAndIdNot(
                        dpiHash,
                        id
                )) {

            resultado.agregarError(
                    "dpi",
                    "El DPI ingresado ya se encuentra registrado."
            );
        }


        if (nitHash != null
                && usuarioRepository
                .existsByNitHashAndIdNot(
                        nitHash,
                        id
                )) {

            resultado.agregarError(
                    "nit",
                    "El NIT ingresado ya se encuentra registrado."
            );
        }
    }


    // =====================================================
    // ASIGNAR DPI
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
    // ASIGNAR NIT
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
    // USUARIO EJECUTOR
    // =====================================================

    private Usuario obtenerEjecutor(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario autenticado."
            );
        }


        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario.trim()
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
    // ¿ES ROL INTERNO?
    // =====================================================

    private boolean esRolInterno(
            Rol rol
    ) {

        if (rol == null
                || rol.getNombre() == null) {

            return false;
        }


        return ROLES_INTERNOS
                .stream()
                .anyMatch(
                        nombre ->
                                nombre.equalsIgnoreCase(
                                        rol.getNombre()
                                )
                );
    }


    // =====================================================
    // LIMPIAR OPCIONAL
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


    // =====================================================
    // SNAPSHOT AUDITORÍA
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
}