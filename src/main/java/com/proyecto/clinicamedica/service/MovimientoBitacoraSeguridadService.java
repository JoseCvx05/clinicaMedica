
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;
import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * =========================================================
 * CU-15 - SEGURIDAD DE BITÁCORA DE INVENTARIO
 * =========================================================
 *
 * RN-GLOBAL-007 - Autorización por rol.
 *
 * Permisos:
 *
 * FARMACÉUTICO:
 * - Consultar movimientos de su sucursal.
 * - Registrar Compra, Devolución, Venta y Reclamo.
 * - No registrar ajustes administrativos.
 *
 * ADMINISTRADOR:
 * - Consultar movimientos autorizados.
 * - Registrar Ajuste+ y Ajuste-.
 *
 * DESPACHO:
 * - Exclusivamente automático desde CU-11.
 *
 * IMPORTANTE:
 *
 * - No modifica FarmaciaSeguridadService.
 * - No modifica los permisos del CU-11.
 * - No confía en la sucursal enviada por HTML.
 * - Obtiene el usuario real desde la autenticación.
 * - No actualiza inventario directamente.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MovimientoBitacoraSeguridadService {

    private static final String FARMACEUTICO =
            "FARMACEUTICO";

    private static final String ADMINISTRADOR =
            "ADMINISTRADOR";

    private final UsuarioActualService usuarioActualService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraSeguridadService(
            UsuarioActualService usuarioActualService
    ) {
        this.usuarioActualService = usuarioActualService;
    }

    // =====================================================
    // OBTENER USUARIO AUTORIZADO
    // =====================================================

    public Usuario obtenerUsuarioAutorizado() {

        Usuario usuario =
                usuarioActualService.obtenerUsuarioActual();

        if (usuario == null
                || usuario.getId() == null
                || !Boolean.TRUE.equals(usuario.getActivo())) {

            throw new AccessDeniedException(
                    "El usuario no está autorizado."
            );
        }

        Rol rol = usuario.getRol();

        if (rol == null
                || !Boolean.TRUE.equals(rol.getActivo())) {

            throw new AccessDeniedException(
                    "El usuario no tiene un rol activo autorizado."
            );
        }

        String nombreRol = normalizarRol(rol.getNombre());

        if (!FARMACEUTICO.equals(nombreRol)
                && !ADMINISTRADOR.equals(nombreRol)) {

            throw new AccessDeniedException(
                    "No tiene permisos para acceder a la bitácora."
            );
        }

        return usuario;
    }

    // =====================================================
    // VALIDAR CONSULTA DE MOVIMIENTOS
    // =====================================================
    //
    // ADMINISTRADOR:
    // Puede consultar todas las sucursales.
    //
    // FARMACÉUTICO:
    // Solo puede consultar su sucursal.
    //
    // Retorna el filtro de sucursal autorizado.
    // =====================================================

    public Integer obtenerSucursalConsulta(
            Integer idSucursalSolicitada
    ) {

        Usuario usuario = obtenerUsuarioAutorizado();

        if (idSucursalSolicitada != null
                && idSucursalSolicitada <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal válida."
            );
        }

        String rol = normalizarRol(
                usuario.getRol().getNombre()
        );

        if (ADMINISTRADOR.equals(rol)) {
            return idSucursalSolicitada;
        }

        Integer idSucursalAutorizada =
                obtenerSucursalFarmaceutico(usuario);

        if (idSucursalSolicitada != null
                && !Objects.equals(
                idSucursalSolicitada,
                idSucursalAutorizada
        )) {

            throw new AccessDeniedException(
                    "No tiene autorización para consultar esta sucursal."
            );
        }

        return idSucursalAutorizada;
    }

    // =====================================================
    // VALIDAR REGISTRO MANUAL
    // =====================================================

    public Usuario validarRegistro(
            Integer idSucursal,
            TipoMovimientoBitacora tipo
    ) {

        if (idSucursal == null || idSucursal <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal."
            );
        }

        if (tipo == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de movimiento."
            );
        }

        if (!tipo.esManual()) {

            throw new IllegalArgumentException(
                    "El despacho se registra automáticamente."
            );
        }

        Usuario usuario = obtenerUsuarioAutorizado();

        String rol = normalizarRol(
                usuario.getRol().getNombre()
        );

        // =============================================
        // AJUSTES EXCLUSIVOS DEL ADMINISTRADOR
        // =============================================

        if (tipo == TipoMovimientoBitacora.AJUSTE_POSITIVO
                || tipo == TipoMovimientoBitacora.AJUSTE_NEGATIVO) {

            if (!ADMINISTRADOR.equals(rol)) {

                throw new AccessDeniedException(
                        "Solo el Administrador puede registrar ajustes de inventario."
                );
            }

            return usuario;
        }

        // =============================================
        // OTROS MOVIMIENTOS: FARMACÉUTICO
        // =============================================

        if (!FARMACEUTICO.equals(rol)) {

            throw new AccessDeniedException(
                    "Solo el personal de Farmacia puede registrar este movimiento."
            );
        }

        // =============================================
        // VALIDAR SUCURSAL DEL FARMACÉUTICO
        // =============================================

        Integer sucursalAutorizada =
                obtenerSucursalFarmaceutico(usuario);

        if (!Objects.equals(
                sucursalAutorizada,
                idSucursal
        )) {

            throw new AccessDeniedException(
                    "No tiene autorización para registrar movimientos en esta sucursal."
            );
        }

        return usuario;
    }

    // =====================================================
    // OBTENER SUCURSAL DEL FARMACÉUTICO
    // =====================================================

    private Integer obtenerSucursalFarmaceutico(
            Usuario usuario
    ) {

        Sucursal sucursal = usuario.getSucursal();

        if (sucursal == null
                || sucursal.getId() == null
                || !Boolean.TRUE.equals(sucursal.getActivo())) {

            throw new AccessDeniedException(
                    "El farmacéutico debe pertenecer a una sucursal activa."
            );
        }

        return sucursal.getId();
    }

    // =====================================================
    // NORMALIZAR ROL
    // =====================================================
    //
    // Reconoce FARMACÉUTICO y FARMACEUTICO.
    // =====================================================

    private String normalizarRol(String nombreRol) {

        if (nombreRol == null || nombreRol.isBlank()) {
            return "";
        }

        String normalizado = Normalizer.normalize(
                nombreRol.trim(),
                Normalizer.Form.NFD
        );

        normalizado = normalizado.replaceAll(
                "\\p{M}+",
                ""
        );

        return normalizado.toUpperCase(Locale.ROOT);
    }
}
