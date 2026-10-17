
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Medicamento;

import com.proyecto.clinicamedica.repository.MedicamentoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-14 - CONSULTAS ADMINISTRATIVAS DE MEDICAMENTOS
 * =========================================================
 *
 * Catálogo 6 de 9.
 *
 * RESPONSABILIDADES:
 *
 * - Listar medicamentos registrados.
 * - Buscar medicamentos por nombre.
 * - Buscar medicamentos por descripción.
 * - Buscar medicamentos por unidad.
 * - Filtrar por estado activo/inactivo.
 * - Filtrar medicamentos controlados.
 * - Filtrar medicamentos no controlados.
 * - Paginar resultados.
 * - Buscar medicamentos por identificador.
 *
 * SEGURIDAD:
 *
 * - medicine/read
 *
 * IMPORTANTE:
 *
 * - Servicio exclusivamente de consultas.
 * - No registra medicamentos.
 * - No modifica precios.
 * - No modifica inventarios.
 * - No descuenta existencias.
 * - No modifica recetas médicas.
 * - No modifica despachos.
 *
 * Se conserva el funcionamiento de:
 *
 * CU-08 - Recetas Médicas.
 * CU-11 - Despacho de Medicamentos.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MedicamentoMantenimientoConsultaService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int TAMANIO_MAXIMO_PAGINA = 100;

    private static final int MAX_TEXTO_BUSQUEDA = 200;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MedicamentoRepository
            medicamentoRepository;

    private final PermisoCatalogoService
            permisoCatalogoService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MedicamentoMantenimientoConsultaService(

            MedicamentoRepository medicamentoRepository,

            PermisoCatalogoService permisoCatalogoService
    ) {

        this.medicamentoRepository =
                medicamentoRepository;

        this.permisoCatalogoService =
                permisoCatalogoService;
    }

    // =====================================================
    // CU-14 - LISTAR MEDICAMENTOS
    // =====================================================
    //
    // Parámetros:
    //
    // texto:
    //   Buscar por nombre, descripción o unidad.
    //
    // activo:
    //   null  = Todos.
    //   true  = Activos.
    //   false = Inactivos.
    //
    // controlado:
    //   null  = Todos.
    //   true  = Medicamentos controlados.
    //   false = Medicamentos no controlados.
    //
    // pagina:
    //   Número de página desde cero.
    //
    // tamano:
    //   Cantidad de registros por página.
    //
    // =====================================================

    public Page<Medicamento> listar(

            String texto,

            Boolean activo,

            Boolean controlado,

            int pagina,

            int tamano
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/read"
        );

        // =================================================
        // VALIDAR NÚMERO DE PÁGINA
        // =================================================

        if (pagina < 0) {

            throw new IllegalArgumentException(
                    "El número de página no es válido."
            );
        }

        // =================================================
        // VALIDAR TAMAÑO DE PÁGINA
        // =================================================

        if (tamano <= 0
                || tamano > TAMANIO_MAXIMO_PAGINA) {

            throw new IllegalArgumentException(
                    "El tamaño de página no es válido."
            );
        }

        // =================================================
        // NORMALIZAR TEXTO DE BÚSQUEDA
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
        // PREPARAR FILTRO DE ESTADO
        // =================================================

        boolean filtrarActivo =
                activo != null;

        boolean valorActivo =
                Boolean.TRUE.equals(activo);

        // =================================================
        // PREPARAR FILTRO DE MEDICAMENTO CONTROLADO
        // =================================================

        boolean filtrarControlado =
                controlado != null;

        boolean valorControlado =
                Boolean.TRUE.equals(controlado);

        // =================================================
        // ORDENAMIENTO Y PAGINACIÓN
        // =================================================
        //
        // Ordenamiento:
        //
        // 1. Nombre ascendente.
        // 2. ID ascendente.
        //
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

        return medicamentoRepository
                .buscarParaMantenimiento(

                        textoLimpio,

                        filtrarActivo,

                        valorActivo,

                        filtrarControlado,

                        valorControlado,

                        pageable
                );
    }

    // =====================================================
    // CU-14 - BUSCAR MEDICAMENTO POR ID
    // =====================================================
    //
    // Permite consultar medicamentos activos e inactivos.
    //
    // Se utilizará para:
    //
    // - Precargar formularios de edición.
    // - Consultar datos administrativos.
    // - Mostrar precios y unidades.
    // - Consultar indicador de controlado.
    // - Consultar stock mínimo configurado.
    //
    // No consulta ni modifica cantidades de inventario.
    //
    // =====================================================

    public Medicamento buscarPorId(
            Integer id
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/read"
        );

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del medicamento no es válido."
            );
        }

        // =================================================
        // CONSULTAR MEDICAMENTO
        // =================================================

        return medicamentoRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "El medicamento solicitado no existe."
                        )
                );
    }
}
