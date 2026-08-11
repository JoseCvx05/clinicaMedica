package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import com.proyecto.clinicamedica.service.AuditoriaService;
import com.proyecto.clinicamedica.service.UsuarioEliminacionService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * =========================================================
 * ELIMINACIÓN LÓGICA DE USUARIOS
 * =========================================================
 *
 * CU-01 - FA05
 *
 * Nunca elimina físicamente usuarios.
 *
 * La operación consiste en:
 *
 * activo = false
 *
 * y registra la acción en bitacora_auditoria.
 *
 * =========================================================
 */
@Service
public class UsuarioEliminacionServiceImpl
        implements UsuarioEliminacionService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final AuditoriaService auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioEliminacionServiceImpl(
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

    @Override
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
                        .orElse(null);


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
        // VALORES ANTERIORES
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
        // VALORES NUEVOS
        // =================================================

        Map<String, Object> valoresNuevos =
                crearSnapshot(
                        actualizado
                );


        // =================================================
        // BITÁCORA
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
         * Nunca incluimos:
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