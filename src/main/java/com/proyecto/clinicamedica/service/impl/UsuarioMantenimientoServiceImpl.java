package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;

import com.proyecto.clinicamedica.service.UsuarioActualizacionService;
import com.proyecto.clinicamedica.service.UsuarioConsultaService;
import com.proyecto.clinicamedica.service.UsuarioCreacionService;
import com.proyecto.clinicamedica.service.UsuarioMantenimientoService;

import org.springframework.data.domain.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.clinicamedica.service.UsuarioEliminacionService;

/**
 * =========================================================
 * FACHADA: MANTENIMIENTO DE USUARIOS
 * =========================================================
 *
 * CU-01 - Mantenimiento de Usuarios.
 *
 * Esta clase coordina los servicios especializados:
 *
 * - UsuarioConsultaService
 * - UsuarioCreacionService
 * - UsuarioActualizacionService
 *
 * No contiene lógica de negocio directamente.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class UsuarioMantenimientoServiceImpl
        implements UsuarioMantenimientoService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioConsultaService
            usuarioConsultaService;

    private final UsuarioCreacionService
            usuarioCreacionService;

    private final UsuarioActualizacionService
            usuarioActualizacionService;

    private final UsuarioEliminacionService
            usuarioEliminacionService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioMantenimientoServiceImpl(
            UsuarioConsultaService usuarioConsultaService,
            UsuarioCreacionService usuarioCreacionService,
            UsuarioActualizacionService usuarioActualizacionService,
            UsuarioEliminacionService usuarioEliminacionService
    ) {

        this.usuarioConsultaService =
                usuarioConsultaService;

        this.usuarioCreacionService =
                usuarioCreacionService;

        this.usuarioActualizacionService =
                usuarioActualizacionService;

        this.usuarioEliminacionService =
                usuarioEliminacionService;
    }

    // =====================================================
    // LISTAR / BUSCAR
    // =====================================================

    @Override
    public Page<UsuarioListadoDTO> listarUsuarios(
            UsuarioBusquedaDTO busqueda
    ) {

        return usuarioConsultaService
                .listarUsuarios(
                        busqueda
                );
    }


    // =====================================================
    // FA01 - CREAR USUARIO
    // =====================================================

    @Override
    @Transactional
    public ResultadoValidacionUsuario crearUsuario(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        return usuarioCreacionService
                .crear(
                        formulario,
                        nombreUsuarioEjecutor,
                        direccionIp
                );
    }


    // =====================================================
    // FA04 - OBTENER USUARIO PARA EDITAR
    // =====================================================

    @Override
    public UsuarioFormularioDTO obtenerUsuarioParaEditar(
            Integer id
    ) {

        return usuarioConsultaService
                .obtenerUsuarioParaEditar(
                        id
                );
    }


    // =====================================================
    // FA04 - ACTUALIZAR USUARIO
    // =====================================================

    @Override
    @Transactional
    public ResultadoValidacionUsuario actualizarUsuario(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        return usuarioActualizacionService
                .actualizar(
                        formulario,
                        nombreUsuarioEjecutor,
                        direccionIp
                );
    }
    // =====================================================
// FA05 - ELIMINAR USUARIO
// =====================================================

    @Override
    @Transactional
    public ResultadoValidacionUsuario eliminarUsuario(
            Integer idUsuario,
            String nombreUsuarioEjecutor,
            String direccionIp
    ) {

        return usuarioEliminacionService
                .eliminar(
                        idUsuario,
                        nombreUsuarioEjecutor,
                        direccionIp
                );
    }
}