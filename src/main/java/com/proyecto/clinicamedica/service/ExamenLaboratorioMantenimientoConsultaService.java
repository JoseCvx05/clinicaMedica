
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.ExamenLaboratorio;
import com.proyecto.clinicamedica.entity.Laboratorio;

import com.proyecto.clinicamedica.repository.ExamenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.LaboratorioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * =========================================================
 * CU-14 - CONSULTAS ADMINISTRATIVAS DE EXÁMENES
 * =========================================================
 *
 * Catálogo 5 - Exámenes de Laboratorio.
 *
 * Responsabilidades:
 *
 * - Listar exámenes registrados.
 * - Buscar por nombre del examen.
 * - Buscar por descripción.
 * - Buscar por nombre del laboratorio.
 * - Filtrar por laboratorio.
 * - Filtrar exámenes activos e inactivos.
 * - Paginar resultados.
 * - Buscar exámenes por ID.
 * - Consultar laboratorios para filtros.
 * - Consultar laboratorios activos para formularios.
 *
 * SEGURIDAD:
 *
 * - lab-exam/read
 *
 * IMPORTANTE:
 *
 * - Servicio exclusivamente de consultas.
 * - No registra exámenes.
 * - No modifica precios.
 * - No desactiva exámenes.
 * - No modifica órdenes de laboratorio.
 * - No modifica información histórica.
 *
 * Se conserva el funcionamiento de:
 *
 * CU-08 - Órdenes de Laboratorio.
 * CU-09 - Gestión de Laboratorio.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class ExamenLaboratorioMantenimientoConsultaService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_MAXIMO_PAGINA = 100;

    private static final int MAX_TEXTO_BUSQUEDA = 200;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ExamenLaboratorioRepository
            examenLaboratorioRepository;

    private final LaboratorioRepository
            laboratorioRepository;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ExamenLaboratorioMantenimientoConsultaService(

            ExamenLaboratorioRepository examenLaboratorioRepository,

            LaboratorioRepository laboratorioRepository,

            PermisoCatalogoService permisoCatalogoService
    ) {

        this.examenLaboratorioRepository =
                examenLaboratorioRepository;

        this.laboratorioRepository =
                laboratorioRepository;

        this.permisoCatalogoService =
                permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR EXÁMENES DE LABORATORIO
    // =====================================================
    //
    // Parámetros:
    //
    // texto:
    //   Búsqueda por examen, descripción o laboratorio.
    //
    // activo:
    //   null  = Todos.
    //   true  = Activos.
    //   false = Inactivos.
    //
    // idLaboratorio:
    //   null = Todos los laboratorios.
    //   ID   = Laboratorio específico.
    //
    // pagina:
    //   Número de página desde cero.
    //
    // tamano:
    //   Cantidad de registros por página.
    //
    // =====================================================

    public Page<ExamenLaboratorio> listar(

            String texto,

            Boolean activo,

            Integer idLaboratorio,

            int pagina,

            int tamano
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/read"
        );

        // =================================================
        // VALIDAR PÁGINA
        // =================================================

        if (pagina < 0) {

            throw new IllegalArgumentException(
                    "El número de página no es válido."
            );
        }

        // =================================================
        // VALIDAR TAMAÑO
        // =================================================

        if (tamano <= 0
                || tamano > TAMANIO_MAXIMO_PAGINA) {

            throw new IllegalArgumentException(
                    "El tamaño de página no es válido."
            );
        }

        // =================================================
        // NORMALIZAR TEXTO
        // =================================================

        String textoLimpio =
                texto == null
                        ? ""
                        : texto.trim();

        if (textoLimpio.length() > MAX_TEXTO_BUSQUEDA) {

            throw new IllegalArgumentException(
                    "La búsqueda no puede superar los "
                            + MAX_TEXTO_BUSQUEDA
                            + " caracteres."
            );
        }

        // =================================================
        // VALIDAR LABORATORIO
        // =================================================

        if (idLaboratorio != null
                && idLaboratorio <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del laboratorio no es válido."
            );
        }

        // =================================================
        // PREPARAR FILTRO ACTIVO
        // =================================================

        boolean filtrarActivo =
                activo != null;

        boolean valorActivo =
                Boolean.TRUE.equals(activo);

        // =================================================
        // PREPARAR FILTRO LABORATORIO
        // =================================================

        boolean filtrarLaboratorio =
                idLaboratorio != null;

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

        return examenLaboratorioRepository
                .buscarParaMantenimiento(

                        textoLimpio,

                        filtrarActivo,

                        valorActivo,

                        filtrarLaboratorio,

                        idLaboratorio,

                        pageable
                );
    }

    // =====================================================
    // CU-14 - BUSCAR EXAMEN POR ID
    // =====================================================
    //
    // Permite consultar exámenes activos e inactivos.
    //
    // Se utilizará para:
    //
    // - Precargar datos de edición.
    // - Consultar información administrativa.
    // - Identificar el laboratorio relacionado.
    //
    // =====================================================

    public ExamenLaboratorio buscarPorId(
            Integer id
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/read"
        );

        // =================================================
        // VALIDAR ID
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del examen no es válido."
            );
        }

        // =================================================
        // CONSULTAR EXAMEN
        // =================================================

        ExamenLaboratorio examen =
                examenLaboratorioRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "El examen de laboratorio solicitado no existe."
                                )
                        );

        // =================================================
        // INICIALIZAR LABORATORIO RELACIONADO
        // =================================================
        //
        // La relación utiliza FetchType.LAZY.
        //
        // Se accede al laboratorio dentro de la
        // transacción de lectura para evitar errores
        // al precargar el formulario administrativo.
        //
        // =================================================

        if (examen.getLaboratorio() != null) {

            examen.getLaboratorio().getNombre();
        }

        return examen;
    }

    // =====================================================
    // CU-14 - LABORATORIOS PARA FILTROS
    // =====================================================
    //
    // Incluye laboratorios activos e inactivos.
    //
    // Esto permite consultar exámenes históricos
    // incluso si su laboratorio está inactivo.
    //
    // =====================================================

    public List<Laboratorio> listarLaboratoriosParaFiltro() {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/read"
        );

        // =================================================
        // CONSULTA
        // =================================================

        return laboratorioRepository.findAll(
                Sort.by(
                        Sort.Order.asc("nombre"),
                        Sort.Order.asc("id")
                )
        );
    }

    // =====================================================
    // CU-14 - LABORATORIOS ACTIVOS
    // =====================================================
    //
    // Utilizado posteriormente para seleccionar
    // el laboratorio de un nuevo examen.
    //
    // No deben ofrecerse laboratorios inactivos
    // para registrar nuevos exámenes.
    //
    // =====================================================

    public List<Laboratorio> listarLaboratoriosActivos() {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/read"
        );

        // =================================================
        // CONSULTA
        // =================================================

        return laboratorioRepository
                .findByActivoTrueOrderByNombreAsc();
    }
}
