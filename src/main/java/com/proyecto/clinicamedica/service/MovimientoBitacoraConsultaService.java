
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.MovimientoBitacoraListadoDTO;

import com.proyecto.clinicamedica.entity.MovimientoBitacoraDetalle;
import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;
import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;

import com.proyecto.clinicamedica.repository.MovimientoBitacoraDetalleRepository;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * CU-15 - SERVICE: CONSULTA DE BITÁCORA DE INVENTARIO
 * =========================================================
 *
 * Flujo Normal - Listado de movimientos.
 * FA01 - No se encontraron movimientos.
 *
 * Responsabilidades:
 *
 * - Validar permisos de consulta.
 * - Respetar la sucursal del Farmacéutico.
 * - Permitir consultas al Administrador.
 * - Aplicar los filtros de búsqueda.
 * - Validar fechas.
 * - Aplicar paginación y ordenamiento.
 * - Consultar detalles complementarios del CU-15.
 * - Convertir movimientos históricos a DTO.
 *
 * COMPATIBILIDAD:
 *
 * - CU-11: despachos de medicamentos.
 * - CU-14: inventario de farmacia.
 * - CU-15: bitácora de movimientos.
 *
 * No modifica existencias.
 * No registra movimientos.
 * No elimina registros históricos.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MovimientoBitacoraConsultaService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int TAMANIO_PAGINA = 20;

    private static final int MAX_REFERENCIA = 50;

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    private final MovimientoBitacoraDetalleRepository
            detalleRepository;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraConsultaService(
            MovimientoInventarioFarmaciaRepository movimientoRepository,
            MovimientoBitacoraDetalleRepository detalleRepository,
            MovimientoBitacoraSeguridadService seguridadService
    ) {

        this.movimientoRepository = movimientoRepository;

        this.detalleRepository = detalleRepository;

        this.seguridadService = seguridadService;
    }

    // =====================================================
    // CU-15 - CONSULTAR MOVIMIENTOS CON FILTROS
    // =====================================================
    //
    // Se conserva el método original.
    //
    // El Controller existente todavía lo utiliza.
    //
    // =====================================================

    public Page<MovimientoInventarioFarmacia> buscarMovimientos(
            Integer idMedicamento,
            Integer idSucursal,
            Integer codigoTipo,
            String referencia,
            Integer idUsuario,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int pagina
    ) {

        // =================================================
        // 1. VALIDAR MEDICAMENTO
        // =================================================

        if (idMedicamento != null && idMedicamento <= 0) {

            throw new IllegalArgumentException(
                    "El medicamento seleccionado no es válido."
            );
        }

        // =================================================
        // 2. VALIDAR TIPO DE MOVIMIENTO
        // =================================================

        if (codigoTipo != null) {

            TipoMovimientoBitacora.desdeCodigo(
                    codigoTipo
            );
        }

        // =================================================
        // 3. VALIDAR USUARIO RESPONSABLE
        // =================================================

        if (idUsuario != null && idUsuario <= 0) {

            throw new IllegalArgumentException(
                    "El usuario seleccionado no es válido."
            );
        }

        // =================================================
        // 4. VALIDAR PAGINACIÓN
        // =================================================

        if (pagina < 0) {

            throw new IllegalArgumentException(
                    "La página solicitada no es válida."
            );
        }

        // =================================================
        // 5. VALIDAR RANGO DE FECHAS
        // =================================================

        if (fechaInicio != null
                && fechaFin != null
                && fechaFin.isBefore(fechaInicio)) {

            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la inicial."
            );
        }

        // =================================================
        // 6. VALIDAR NÚMERO DE REFERENCIA
        // =================================================

        String referenciaLimpia =
                limpiarReferencia(referencia);

        if (referenciaLimpia != null
                && referenciaLimpia.length() > MAX_REFERENCIA) {

            throw new IllegalArgumentException(
                    "El número de referencia no puede superar los 50 caracteres."
            );
        }

        // =================================================
        // 7. SEGURIDAD POR SUCURSAL
        // =================================================

        Integer sucursalAutorizada =
                seguridadService.obtenerSucursalConsulta(
                        idSucursal
                );

        // =================================================
        // 8. FECHA INICIAL INCLUSIVA
        // =================================================

        OffsetDateTime desde = null;

        if (fechaInicio != null) {

            desde = fechaInicio
                    .atStartOfDay(ZONA_GUATEMALA)
                    .toOffsetDateTime();
        }

        // =================================================
        // 9. FECHA FINAL INCLUSIVA PARA EL USUARIO
        // =================================================
        //
        // En PostgreSQL utilizamos como límite
        // exclusivo el inicio del día siguiente.
        //
        // =================================================

        OffsetDateTime hasta = null;

        if (fechaFin != null) {

            hasta = fechaFin
                    .plusDays(1)
                    .atStartOfDay(ZONA_GUATEMALA)
                    .toOffsetDateTime();
        }

        // =================================================
        // 10. PAGINACIÓN Y ORDENAMIENTO
        // =================================================

        Pageable pageable =
                PageRequest.of(
                        pagina,
                        TAMANIO_PAGINA,
                        Sort.by(
                                Sort.Direction.DESC,
                                "fechaHora"
                        ).and(
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "id"
                                )
                        )
                );

        // =================================================
        // 11. CONSULTAR POSTGRESQL
        // =================================================

        return movimientoRepository.buscarMovimientosCu15(
                idMedicamento,
                sucursalAutorizada,
                codigoTipo,
                referenciaLimpia,
                idUsuario,
                desde,
                hasta,
                pageable
        );
    }

    // =====================================================
    // CU-15 - OBTENER LISTADO CON DTO
    // =====================================================
    //
    // Combina:
    //
    // MovimientoInventarioFarmacia
    // +
    // MovimientoBitacoraDetalle
    //
    // Incluye movimientos históricos anteriores.
    //
    // =====================================================

    public Page<MovimientoBitacoraListadoDTO> buscarMovimientosListado(
            Integer idMedicamento,
            Integer idSucursal,
            Integer codigoTipo,
            String referencia,
            Integer idUsuario,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int pagina
    ) {

        // =================================================
        // 1. REUTILIZAR CONSULTA VALIDADA
        // =================================================

        Page<MovimientoInventarioFarmacia> movimientos =
                buscarMovimientos(
                        idMedicamento,
                        idSucursal,
                        codigoTipo,
                        referencia,
                        idUsuario,
                        fechaInicio,
                        fechaFin,
                        pagina
                );

        // =================================================
        // 2. OBTENER IDS DE LA PÁGINA
        // =================================================

        List<Long> ids =
                movimientos.getContent()
                        .stream()
                        .map(
                                MovimientoInventarioFarmacia::getId
                        )
                        .toList();

        // =================================================
        // 3. CONSULTAR DETALLES COMPLEMENTARIOS
        // =================================================
        //
        // Una sola consulta para los IDs de la página.
        //
        // Los movimientos históricos pueden no tener
        // un detalle CU-15, lo cual es válido.
        //
        // =================================================

        Map<Long, MovimientoBitacoraDetalle> detalles =
                new HashMap<>();

        if (!ids.isEmpty()) {

            detalleRepository.findAllById(ids)
                    .forEach(
                            detalle -> detalles.put(
                                    detalle.getId(),
                                    detalle
                            )
                    );
        }

        // =================================================
        // 4. ENTITY -> DTO
        // =================================================

        return movimientos.map(
                movimiento ->
                        convertirAListadoDTO(
                                movimiento,
                                detalles.get(
                                        movimiento.getId()
                                )
                        )
        );
    }

    // =====================================================
    // CU-15 - CONVERTIR MOVIMIENTO A DTO
    // =====================================================

    private MovimientoBitacoraListadoDTO convertirAListadoDTO(
            MovimientoInventarioFarmacia movimiento,
            MovimientoBitacoraDetalle detalle
    ) {

        // =================================================
        // 1. IDENTIFICAR TIPO DE MOVIMIENTO
        // =================================================

        String tipo;

        if (detalle != null
                && detalle.getTipo() != null) {

            // Movimiento nuevo CU-15.

            tipo = detalle.getTipo().getNombre();

        } else if (
                "CU11_DESPACHO_FARMACIA".equals(
                        movimiento.getOrigen()
                )
        ) {

            // Despacho histórico CU-11.

            tipo = "Despacho";

        } else {

            // Movimiento histórico CU-14.

            tipo = switch (
                    movimiento.getTipoMovimiento()
                    ) {

                case INICIAL -> "Inicial";

                case ENTRADA -> "Entrada";

                case SALIDA -> "Salida";

                case AJUSTE -> "Ajuste";
            };
        }

        // =================================================
        // 2. COSTO UNITARIO
        // =================================================

        java.math.BigDecimal costoUnitario =
                detalle != null
                        ? detalle.getCostoUnitario()
                        : null;

        // =================================================
        // 3. ESTADO ACTIVO / INACTIVO
        // =================================================
        //
        // Los movimientos anteriores al CU-15
        // se consideran activos para el listado.
        //
        // =================================================

        Boolean activo =
                detalle != null
                        ? detalle.getActivo()
                        : true;

        // =================================================
        // 4. CONSTRUIR DTO
        // =================================================

        return new MovimientoBitacoraListadoDTO(

                movimiento.getId(),

                tipo,

                movimiento.getInventario()
                        .getMedicamento()
                        .getNombre(),

                movimiento.getInventario()
                        .getSucursal()
                        .getNombre(),

                movimiento.getCantidad(),

                movimiento.getStockAnterior(),

                movimiento.getStockNuevo(),

                costoUnitario,

                movimiento.getIdReferencia(),

                movimiento.getUsuario()
                        .getNombreUsuario(),

                movimiento.getFechaHora(),

                activo
        );
    }

    // =====================================================
    // LIMPIAR REFERENCIA
    // =====================================================

    private String limpiarReferencia(
            String referencia
    ) {

        if (referencia == null) {
            return null;
        }

        String limpio = referencia.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }
}
