
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-14 - CONSULTAS ADMINISTRATIVAS DE ESTADOS DE CITA
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Consultar estados de cita registrados.
 * - Buscar por nombre o descripción.
 * - Filtrar por estado activo e inactivo.
 * - Mostrar todos los estados.
 * - Aplicar paginación y ordenamiento.
 * - Buscar estados por identificador.
 * - Verificar permisos de consulta.
 *
 * SEGURIDAD:
 *
 * - Requiere permiso appointment-status/read.
 * - No permite modificar registros.
 * - No permite eliminar estados de cita.
 *
 * IMPORTANTE:
 *
 * Este servicio no modifica el flujo de citas.
 *
 * No altera las transiciones utilizadas por
 * los CU-03 al CU-08.
 *
 * No modifica la caché Caffeine utilizada por
 * CatalogoEstadoCitaService.
 *
 * Las consultas administrativas se realizan
 * directamente contra PostgreSQL.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class EstadoCitaMantenimientoConsultaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EstadoCitaRepository estadoCitaRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EstadoCitaMantenimientoConsultaService(
            EstadoCitaRepository estadoCitaRepository,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.estadoCitaRepository = estadoCitaRepository;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR ESTADOS DE CITA
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

    public Page<EstadoCita> listar(
            String texto,
            Boolean activo,
            int pagina,
            int tamano
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/read"
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
        // activo = null  -> Todos.
        // activo = true  -> Activos.
        // activo = false -> Inactivos.
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

        return estadoCitaRepository
                .buscarParaMantenimiento(
                        textoNormalizado,
                        filtrarActivo,
                        valorActivo,
                        pageable
                );
    }

    // =====================================================
    // CU-14 - BUSCAR ESTADO POR ID
    // =====================================================
    //
    // Permite consultar estados activos e inactivos.
    //
    // No modifica datos ni transiciones.
    //
    // =====================================================

    public EstadoCita buscarPorId(Integer id) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/read"
        );

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un estado de cita válido."
            );
        }

        // =================================================
        // CONSULTAR REGISTRO
        // =================================================

        return estadoCitaRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "El estado de cita seleccionado no existe."
                        )
                );
    }
}
