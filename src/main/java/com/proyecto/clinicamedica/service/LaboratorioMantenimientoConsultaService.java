
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Laboratorio;
import com.proyecto.clinicamedica.repository.LaboratorioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-14 - SERVICIO DE CONSULTAS DE LABORATORIOS
 * =========================================================
 *
 * Catálogo 4 - Laboratorios.
 *
 * Responsabilidades:
 *
 * - Listar laboratorios registrados.
 * - Buscar por nombre o descripción.
 * - Filtrar laboratorios activos e inactivos.
 * - Consultar todos los registros.
 * - Paginar resultados.
 * - Buscar laboratorio por ID.
 * - Validar permisos administrativos de lectura.
 *
 * SEGURIDAD:
 *
 * - laboratory/read
 *
 * IMPORTANTE:
 *
 * - Servicio exclusivamente de consulta.
 * - No crea laboratorios.
 * - No modifica registros.
 * - No desactiva laboratorios.
 * - No modifica órdenes de laboratorio.
 * - No afecta CU-08 ni CU-09.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class LaboratorioMantenimientoConsultaService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_MAXIMO_PAGINA = 100;

    private static final int LONGITUD_MAXIMA_BUSQUEDA = 200;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final LaboratorioRepository laboratorioRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LaboratorioMantenimientoConsultaService(
            LaboratorioRepository laboratorioRepository,
            PermisoCatalogoService permisoCatalogoService
    ) {

        this.laboratorioRepository = laboratorioRepository;

        this.permisoCatalogoService = permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR LABORATORIOS
    // =====================================================
    //
    // Parámetros:
    //
    // texto:
    //   Buscar por nombre o descripción.
    //
    // activo:
    //   null  = Todos.
    //   true  = Activos.
    //   false = Inactivos.
    //
    // pagina:
    //   Número de página, iniciando en cero.
    //
    // tamano:
    //   Cantidad de registros por página.
    //
    // =====================================================

    public Page<Laboratorio> listar(

            String texto,

            Boolean activo,

            int pagina,

            int tamano
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/read"
        );

        // =================================================
        // VALIDAR PAGINACIÓN
        // =================================================

        if (pagina < 0) {

            throw new IllegalArgumentException(
                    "El número de página no es válido."
            );
        }

        if (tamano <= 0
                || tamano > TAMANIO_MAXIMO_PAGINA) {

            throw new IllegalArgumentException(
                    "El tamaño de página no es válido."
            );
        }

        // =================================================
        // NORMALIZAR BÚSQUEDA
        // =================================================

        String textoLimpio =
                texto == null
                        ? ""
                        : texto.trim();

        if (textoLimpio.length()
                > LONGITUD_MAXIMA_BUSQUEDA) {

            throw new IllegalArgumentException(
                    "El texto de búsqueda no puede superar "
                            + "los 200 caracteres."
            );
        }

        // =================================================
        // PREPARAR FILTRO DE ESTADO
        // =================================================

        boolean filtrarActivo =
                activo != null;

        boolean valorActivo =
                Boolean.TRUE.equals(activo);

        // =================================================
        // ORDENAMIENTO Y PAGINACIÓN
        // =================================================

        Pageable pageable =
                PageRequest.of(

                        pagina,

                        tamano,

                        Sort.by(
                                Sort.Order.asc("nombre"),
                                Sort.Order.asc("id")
                        )
                );

        // =================================================
        // CONSULTAR POSTGRESQL
        // =================================================

        return laboratorioRepository
                .buscarParaMantenimiento(

                        textoLimpio,

                        filtrarActivo,

                        valorActivo,

                        pageable
                );
    }

    // =====================================================
    // CU-14 - BUSCAR LABORATORIO POR ID
    // =====================================================
    //
    // Permite consultar registros activos e inactivos.
    //
    // Se utilizará posteriormente para:
    //
    // - Precargar el formulario de edición.
    // - Consultar los datos administrativos.
    //
    // No modifica registros.
    //
    // =====================================================

    public Laboratorio buscarPorId(
            Integer id
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/read"
        );

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null
                || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del laboratorio no es válido."
            );
        }

        // =================================================
        // BUSCAR REGISTRO
        // =================================================

        return laboratorioRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "El laboratorio solicitado no existe."
                        )
                );
    }
}
