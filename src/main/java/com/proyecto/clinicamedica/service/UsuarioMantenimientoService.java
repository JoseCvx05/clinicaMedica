package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;

import org.springframework.data.domain.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * =========================================================
 * SERVICIO: MANTENIMIENTO DE USUARIOS
 * =========================================================
 *
 * CU-01 - Mantenimiento de Usuarios.
 *
 * Funciona como fachada para las operaciones del módulo:
 *
 * - Listar y buscar usuarios.
 * - Crear usuarios.
 * - Obtener datos para edición.
 * - Actualizar usuarios.
 * - Realizar eliminación lógica.
 *
 * La lógica especializada continúa delegada en servicios
 * pequeños mientras revisamos cuáles realmente conviene
 * conservar.
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class UsuarioMantenimientoService {


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

    public UsuarioMantenimientoService(
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
    // FA05 - ELIMINACIÓN LÓGICA
    // =====================================================

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