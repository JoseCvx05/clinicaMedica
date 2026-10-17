
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.repository.SucursalRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-14 - CONSULTAS ADMINISTRATIVAS DE SUCURSALES
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Consultar sucursales registradas.
 * - Buscar por nombre, dirección o descripción.
 * - Filtrar por estado activo e inactivo.
 * - Mostrar todas las sucursales.
 * - Aplicar paginación y ordenamiento.
 * - Buscar una sucursal por su identificador.
 * - Verificar permisos de consulta.
 *
 * SEGURIDAD:
 *
 * - Requiere permiso branch/read.
 * - No permite modificar registros.
 * - No permite eliminar sucursales.
 *
 * Este servicio no modifica las consultas
 * utilizadas por CU-03, CU-04 ni CU-13.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class SucursalMantenimientoConsultaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalRepository sucursalRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SucursalMantenimientoConsultaService(
            SucursalRepository sucursalRepository,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.sucursalRepository = sucursalRepository;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR SUCURSALES
    // =====================================================
    //
    // Permite:
    //
    // - Búsqueda por texto.
    // - Filtro de estado.
    // - Paginación.
    // - Ordenamiento alfabético.
    //
    // =====================================================

    public Page<Sucursal> listar(
            String texto,
            Boolean activo,
            int pagina,
            int tamano
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/read"
        );

        // =================================================
        // NORMALIZAR BÚSQUEDA
        // =================================================

        String textoNormalizado =
                texto == null ? "" : texto.trim();

        if (textoNormalizado.length() > 200) {

            throw new IllegalArgumentException(
                    "La búsqueda no puede superar los 200 caracteres."
            );
        }

        // =================================================
        // VALIDAR PAGINACIÓN
        // =================================================

        int paginaSegura = Math.max(0, pagina);

        int tamanoSeguro = tamano <= 0
                ? 10
                : Math.min(tamano, 50);

        // =================================================
        // FILTRO DE ESTADO
        // =================================================
        //
        // activo = null  -> Todas.
        // activo = true  -> Activas.
        // activo = false -> Inactivas.
        //
        // =================================================

        boolean filtrarActivo = activo != null;

        boolean valorActivo = Boolean.TRUE.equals(activo);

        // =================================================
        // ORDENAMIENTO
        // =================================================

        Pageable pageable = PageRequest.of(
                paginaSegura,
                tamanoSeguro,
                Sort.by(
                        Sort.Order.asc("nombre"),
                        Sort.Order.asc("id")
                )
        );

        // =================================================
        // CONSULTAR POSTGRESQL
        // =================================================

        return sucursalRepository
                .buscarParaMantenimiento(
                        textoNormalizado,
                        filtrarActivo,
                        valorActivo,
                        pageable
                );
    }

    // =====================================================
    // CU-14 - BUSCAR SUCURSAL POR ID
    // =====================================================
    //
    // Permite consultar registros activos e inactivos
    // para el mantenimiento administrativo.
    //
    // No realiza modificaciones.
    //
    // =====================================================

    public Sucursal buscarPorId(Integer id) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/read"
        );

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal válida."
            );
        }

        // =================================================
        // CONSULTAR SUCURSAL
        // =================================================

        return sucursalRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "La sucursal seleccionada no existe."
                        )
                );
    }
}
