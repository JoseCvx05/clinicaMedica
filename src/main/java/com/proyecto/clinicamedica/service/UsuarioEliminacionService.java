package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;

import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * =========================================================
 * SERVICIO: ELIMINACIÓN LÓGICA DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA05.
 *
 * La eliminación nunca borra físicamente un usuario.
 *
 * La operación consiste únicamente en:
 *
 * activo = false
 *
 * También registra la operación en bitacora_auditoria.
 *
 * =========================================================
 */
@Service
public class UsuarioEliminacionService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final AuditoriaService auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioEliminacionService(
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaService =
                auditoriaService;
    }


    // =====================================================
    // FA05 - ELIMINAR LÓGICAMENTE
    // =====================================================

    @Transactional
    public ResultadoValidacionUsuario eliminar(
            Integer idUsuario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        ResultadoValidacionUsuario resultado =
                new ResultadoValidacionUsuario();


        // =================================================
        // VALIDAR ID
        // =================================================

        if (idUsuario == null) {

            resultado.agregarError(
                    "usuario",
                    "El usuario seleccionado no es válido."
            );

            return resultado;
        }


        // =================================================
        // BUSCAR USUARIO
        // =================================================

        Usuario usuario =
                usuarioRepository
                        .findById(
                                idUsuario
                        )
                        .orElse(
                                null
                        );


        if (usuario == null) {

            resultado.agregarError(
                    "usuario",
                    "El usuario seleccionado no existe."
            );

            return resultado;
        }


        // =================================================
        // EVITAR ELIMINACIÓN REPETIDA
        // =================================================

        if (!Boolean.TRUE.equals(
                usuario.getActivo()
        )) {

            resultado.agregarError(
                    "usuario",
                    "El usuario seleccionado ya se encuentra inactivo."
            );

            return resultado;
        }


        // =================================================
        // IDENTIFICAR EJECUTOR
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
        // BORRADO LÓGICO
        // =================================================

        usuario.setActivo(
                false
        );


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
                        AccionAuditoria.ELIMINAR,
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
    // OBTENER EJECUTOR
    // =====================================================

    private Usuario obtenerEjecutor(
            String nombreUsuarioEjecutor
    ) {

        if (nombreUsuarioEjecutor == null
                || nombreUsuarioEjecutor.isBlank()) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario autenticado."
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
    // SNAPSHOT SEGURO PARA AUDITORÍA
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
         * Nunca incluimos datos sensibles:
         *
         * - contraseña
         * - contrasenaHash
         * - DPI
         * - dpiCifrado
         * - dpiHash
         * - NIT
         * - nitCifrado
         * - nitHash
         */

        return datos;
    }
}