
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.repository.EspecialidadRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-14 - CONSULTAS ADMINISTRATIVAS DE ESPECIALIDADES
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Consultar especialidades para mantenimiento.
 * - Buscar por nombre o descripción.
 * - Filtrar por estado activo o inactivo.
 * - Mostrar todos los estados.
 * - Aplicar paginación y ordenamiento.
 * - Verificar el permiso de consulta.
 *
 * Este servicio es únicamente de lectura.
 *
 * No modifica especialidades ni afecta las consultas
 * utilizadas por CU-03 y CU-13.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class EspecialidadMantenimientoConsultaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EspecialidadRepository especialidadRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EspecialidadMantenimientoConsultaService(
            EspecialidadRepository especialidadRepository,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.especialidadRepository = especialidadRepository;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR ESPECIALIDADES
    // =====================================================

    public Page<Especialidad> listar(
            String texto,
            Boolean activo,
            int pagina,
            int tamano
    ) {

        // Verificar autorización de lectura
        permisoCatalogoService.exigirPermiso(
                "specialty/read"
        );

        // =================================================
        // NORMALIZAR BÚSQUEDA
        // =================================================

        String textoNormalizado =
                texto == null ? "" : texto.trim();

        // Limitar el texto recibido
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
        // activo = null  -> Todos
        // activo = true  -> Activos
        // activo = false -> Inactivos
        //
        // =================================================

        boolean filtrarActivo = activo != null;

        boolean valorActivo = Boolean.TRUE.equals(activo);

        // =================================================
        // ORDENAMIENTO Y PAGINACIÓN
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

        return especialidadRepository
                .buscarParaMantenimiento(
                        textoNormalizado,
                        filtrarActivo,
                        valorActivo,
                        pageable
                );
    }


    // =====================================================
    // CU-14 - BUSCAR ESPECIALIDAD PARA EDICIÓN
    // =====================================================
    //
    // Permite consultar especialidades activas e
    // inactivas desde el mantenimiento administrativo.
    //
    // No modifica registros.
    // =====================================================

    public Especialidad buscarPorId(Integer id) {

        permisoCatalogoService.exigirPermiso(
                "specialty/read"
        );

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad válida."
            );
        }

        return especialidadRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "La especialidad seleccionada no existe."
                        )
                );
    }

}
