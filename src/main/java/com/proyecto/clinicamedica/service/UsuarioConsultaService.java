package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;
import org.springframework.data.domain.Page;

/**
 * =========================================================
 * SERVICIO: CONSULTA DE USUARIOS
 * =========================================================
 *
 * Responsabilidad única:
 *
 * - Listar usuarios.
 * - Buscar usuarios.
 * - Aplicar filtros.
 * - Aplicar paginación y ordenamiento.
 * - Obtener un usuario para edición.
 *
 * No crea, modifica ni elimina usuarios.
 *
 * =========================================================
 */
public interface UsuarioConsultaService {

    // =====================================================
    // LISTAR / BUSCAR
    // =====================================================

    Page<UsuarioListadoDTO> listarUsuarios(
            UsuarioBusquedaDTO busqueda
    );


    // =====================================================
    // OBTENER PARA EDICIÓN
    // =====================================================

    UsuarioFormularioDTO obtenerUsuarioParaEditar(
            Integer id
    );
}