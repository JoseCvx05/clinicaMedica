
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.MovimientoBitacoraDetalle;
import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;

import com.proyecto.clinicamedica.repository.MovimientoBitacoraDetalleRepository;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * =========================================================
 * CU-15 - CONSULTA DE DETALLE DE MOVIMIENTOS
 * =========================================================
 *
 * Flujo normal: Acción "Ver".
 *
 * Permite consultar:
 *
 * - Tipo de movimiento.
 * - Medicamento.
 * - Sucursal.
 * - Cantidad.
 * - Stock anterior.
 * - Stock nuevo.
 * - Costo unitario.
 * - Número de referencia.
 * - Usuario responsable.
 * - Fecha y hora.
 * - Motivo / Notas.
 * - Estado del registro.
 *
 * SEGURIDAD:
 *
 * - Administrador: consultar todas las sucursales.
 * - Farmacéutico: únicamente su sucursal.
 *
 * COMPATIBILIDAD:
 *
 * - Conserva movimientos históricos del CU-11.
 * - Conserva movimientos históricos del CU-14.
 * - No modifica inventarios ni despachos.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MovimientoBitacoraDetalleConsultaService {

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    private final MovimientoBitacoraDetalleRepository
            detalleRepository;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraDetalleConsultaService(
            MovimientoInventarioFarmaciaRepository movimientoRepository,
            MovimientoBitacoraDetalleRepository detalleRepository,
            MovimientoBitacoraSeguridadService seguridadService
    ) {

        this.movimientoRepository = movimientoRepository;
        this.detalleRepository = detalleRepository;
        this.seguridadService = seguridadService;
    }

    // =====================================================
    // DTO DE RESPUESTA
    // =====================================================

    public record DetalleMovimiento(

            Long idMovimiento,

            String tipo,

            String medicamento,

            String sucursal,

            Integer cantidad,

            Integer stockAnterior,

            Integer stockNuevo,

            BigDecimal costoUnitario,

            String referencia,

            String usuarioResponsable,

            OffsetDateTime fechaHora,

            String motivoNotas,

            Boolean activo,

            String origen

    ) {
    }

    // =====================================================
    // OBTENER DETALLE DE MOVIMIENTO
    // =====================================================

    public DetalleMovimiento obtenerDetalle(
            Long idMovimiento
    ) {

        // =================================================
        // 1. VALIDAR IDENTIFICADOR
        // =================================================

        if (idMovimiento == null || idMovimiento <= 0) {

            throw new IllegalArgumentException(
                    "El movimiento seleccionado no es válido."
            );
        }

        // =================================================
        // 2. VALIDAR USUARIO AUTORIZADO
        // =================================================

        seguridadService.obtenerUsuarioAutorizado();

        // =================================================
        // 3. CONSULTAR MOVIMIENTO HISTÓRICO
        // =================================================

        MovimientoInventarioFarmacia movimiento =
                movimientoRepository.findById(
                        idMovimiento
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "El movimiento seleccionado no existe."
                        )
                );

        // =================================================
        // 4. VALIDAR SUCURSAL DEL MOVIMIENTO
        // =================================================
        //
        // Farmacéutico:
        // solo puede ver movimientos de su sucursal.
        //
        // Administrador:
        // puede consultar cualquier sucursal.
        //
        // =================================================

        Integer idSucursal =
                movimiento.getInventario()
                        .getSucursal()
                        .getId();

        seguridadService.obtenerSucursalConsulta(
                idSucursal
        );

        // =================================================
        // 5. CONSULTAR DETALLE CU-15
        // =================================================
        //
        // Puede no existir para registros antiguos.
        //
        // =================================================

        MovimientoBitacoraDetalle detalle =
                detalleRepository.findById(
                        idMovimiento
                ).orElse(null);

        // =================================================
        // 6. DETERMINAR TIPO DE MOVIMIENTO
        // =================================================

        String tipo;

        if (detalle != null
                && detalle.getTipo() != null) {

            tipo = detalle.getTipo().getNombre();

        } else if (
                "CU11_DESPACHO_FARMACIA".equals(
                        movimiento.getOrigen()
                )
        ) {

            tipo = "Despacho";

        } else {

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
        // 7. DATOS COMPLEMENTARIOS
        // =================================================

        BigDecimal costo =
                detalle != null
                        ? detalle.getCostoUnitario()
                        : null;

        Boolean activo =
                detalle != null
                        ? detalle.getActivo()
                        : true;

        // =================================================
        // 8. CONSTRUIR RESPUESTA
        // =================================================

        return new DetalleMovimiento(

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

                costo,

                movimiento.getIdReferencia(),

                movimiento.getUsuario()
                        .getNombreUsuario(),

                movimiento.getFechaHora(),

                movimiento.getObservaciones(),

                activo,

                movimiento.getOrigen()
        );
    }
}
