package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;
import org.springframework.data.domain.Page;

/**
 * =========================================================
 * SERVICIO: MANTENIMIENTO DE USUARIOS
 * =========================================================
 *
 * CU-01 - Mantenimiento de Usuarios.
 *
 * Operaciones:
 *
 * - Listar y buscar usuarios.
 * - Crear usuarios.
 * - Consultar usuario para edición.
 * - Actualizar usuarios.
 * - Posteriormente eliminación lógica.
 *
 * =========================================================
 */
public interface UsuarioMantenimientoService {

    // =====================================================
    // LISTAR / BUSCAR
    // =====================================================

    Page<UsuarioListadoDTO> listarUsuarios(
            UsuarioBusquedaDTO busqueda
    );


    // =====================================================
    // FA01 - CREAR
    // =====================================================

    ResultadoValidacionUsuario crearUsuario(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );


    // =====================================================
    // FA04 - OBTENER USUARIO PARA EDITAR
    // =====================================================

    /**
     * Recupera los datos del usuario y los prepara
     * para mostrarlos en el formulario de edición.
     *
     * El Service será responsable de:
     *
     * - Buscar el usuario por ID.
     * - Convertir Entity -> DTO.
     * - Descifrar DPI.
     * - Descifrar NIT.
     * - NO enviar contrasenaHash.
     *
     * La contraseña siempre llegará vacía al formulario.
     *
     * @param id identificador del usuario
     *
     * @return datos preparados para edición
     */
    UsuarioFormularioDTO obtenerUsuarioParaEditar(
            Integer id
    );


    // =====================================================
    // FA04 - ACTUALIZAR
    // =====================================================

    /**
     * Actualiza un usuario existente.
     *
     * Debe:
     *
     * 1. Validar el formulario.
     * 2. Validar que el usuario exista.
     * 3. Validar duplicados excluyendo su propio ID.
     * 4. Validar Rol, Sucursal y Especialidad.
     * 5. Actualizar contraseña solamente si se ingresó
     *    una nueva.
     * 6. Volver a cifrar DPI/NIT cuando corresponda.
     * 7. Actualizar modificadoPor.
     * 8. Registrar valores anteriores y nuevos
     *    en bitacora_auditoria.
     *
     * @param formulario datos modificados
     * @param nombreUsuarioEjecutor administrador autenticado
     * @param direccionIp dirección IP
     *
     * @return resultado de validación
     */
    ResultadoValidacionUsuario actualizarUsuario(
            UsuarioFormularioDTO formulario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );
    // =====================================================
// FA05 - ELIMINAR USUARIO
// =====================================================

    ResultadoValidacionUsuario eliminarUsuario(
            Integer idUsuario,
            String nombreUsuarioEjecutor,
            String direccionIp
    );
}