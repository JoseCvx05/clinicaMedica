
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.MovimientoBitacoraFormularioDTO;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.MovimientoBitacoraDetalle;
import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;
import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.MovimientoBitacoraDetalleRepository;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================
 * CU-15 - SERVICE: REGISTRO DE MOVIMIENTOS
 * =========================================================
 *
 * Flujo normal:
 *
 * - Validar formulario.
 * - Verificar permisos del usuario.
 * - Bloquear inventario.
 * - Validar medicamento, sucursal e inventario.
 * - Verificar stock suficiente.
 * - Actualizar existencias.
 * - Registrar movimiento histórico.
 * - Registrar detalle CU-15.
 * - Registrar auditoría.
 * - Generar mensaje de confirmación.
 *
 * FA02 - Stock insuficiente.
 * RN-CU13-01 - Validaciones del formulario.
 * RN-CU13-02 - Stock y concurrencia.
 * RNF-025 - Control de concurrencia.
 *
 * No modifica los despachos existentes del CU-11.
 * No elimina movimientos históricos del CU-14.
 *
 * =========================================================
 */
@Service
public class MovimientoBitacoraRegistroService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MovimientoBitacoraValidacionService
            validacionService;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    private final InventarioFarmaciaRepository
            inventarioRepository;

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    private final MovimientoBitacoraDetalleRepository
            detalleRepository;

    private final AuditoriaService
            auditoriaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraRegistroService(
            MovimientoBitacoraValidacionService validacionService,
            MovimientoBitacoraSeguridadService seguridadService,
            InventarioFarmaciaRepository inventarioRepository,
            MovimientoInventarioFarmaciaRepository movimientoRepository,
            MovimientoBitacoraDetalleRepository detalleRepository,
            AuditoriaService auditoriaService
    ) {

        this.validacionService = validacionService;
        this.seguridadService = seguridadService;
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.detalleRepository = detalleRepository;
        this.auditoriaService = auditoriaService;
    }

    // =====================================================
    // RESULTADO DEL REGISTRO
    // =====================================================

    public record ResultadoMovimiento(
            Long idMovimiento,
            Integer stockAnterior,
            Integer stockNuevo,
            String mensaje,
            String alertaStockMinimo
    ) {
    }

    // =====================================================
    // REGISTRAR MOVIMIENTO MANUAL
    // =====================================================

    @Transactional
    public ResultadoMovimiento registrarMovimiento(
            MovimientoBitacoraFormularioDTO formulario
    ) {

        // =================================================
        // 1. VALIDAR CAMPOS Y TIPO DE MOVIMIENTO
        // =================================================

        TipoMovimientoBitacora tipo =
                validacionService.validar(formulario);

        // =================================================
        // 2. VALIDAR SEGURIDAD Y SUCURSAL
        // =================================================

        Usuario usuario =
                seguridadService.validarRegistro(
                        formulario.getIdSucursal(),
                        tipo
                );

        // =================================================
        // 3. BLOQUEAR INVENTARIO
        // =================================================
        //
        // PESSIMISTIC_WRITE:
        // protege contra cambios simultáneos.
        //
        // @Version:
        // proporciona protección adicional.
        //
        // =================================================

        InventarioFarmacia inventario =
                inventarioRepository
                        .bloquearPorMedicamentoYSucursal(
                                formulario.getIdMedicamento(),
                                formulario.getIdSucursal()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe inventario registrado "
                                                + "para el medicamento y la "
                                                + "sucursal seleccionados."
                                )
                        );

        // =================================================
        // 4. VALIDAR INVENTARIO ACTIVO
        // =================================================

        if (!Boolean.TRUE.equals(
                inventario.getActivo()
        )) {

            throw new IllegalStateException(
                    "No se pueden registrar movimientos "
                            + "en un inventario inactivo."
            );
        }

        // =================================================
        // 5. VALIDAR MEDICAMENTO ACTIVO
        // =================================================

        if (inventario.getMedicamento() == null
                || !inventario.getMedicamento().estaActivo()) {

            throw new IllegalStateException(
                    "No se pueden registrar movimientos "
                            + "de un medicamento inactivo."
            );
        }

        // =================================================
        // 6. VALIDAR SUCURSAL ACTIVA
        // =================================================

        if (inventario.getSucursal() == null
                || !Boolean.TRUE.equals(
                inventario.getSucursal().getActivo()
        )) {

            throw new IllegalStateException(
                    "No se pueden registrar movimientos "
                            + "en una sucursal inactiva."
            );
        }

        // =================================================
        // 7. OBTENER CANTIDADES REALES
        // =================================================

        Integer stockAnterior =
                inventario.getCantidadDisponible();

        Integer cantidad =
                formulario.getCantidad();

        // =================================================
        // 8. FA02 - VALIDAR STOCK SUFICIENTE
        // =================================================
        //
        // Venta, Reclamo y Ajuste-:
        // no pueden superar las existencias.
        //
        // Se utiliza el stock real obtenido
        // de PostgreSQL con bloqueo.
        //
        // =================================================

        if (tipo.esSalida()
                && cantidad > stockAnterior) {

            throw new IllegalStateException(
                    "Stock insuficiente. Stock actual: "
                            + stockAnterior
                            + ". No se puede registrar una salida de "
                            + cantidad
                            + " unidades."
            );
        }

        // =================================================
        // 9. ACTUALIZAR EXISTENCIAS
        // =================================================

        if (tipo.esSalida()) {

            inventario.descontar(cantidad);

        } else {

            inventario.agregar(cantidad);
        }

        Integer stockNuevo =
                inventario.getCantidadDisponible();

        // =================================================
        // 10. GUARDAR INVENTARIO
        // =================================================

        inventarioRepository.saveAndFlush(
                inventario
        );

        // =================================================
        // 11. DETERMINAR TIPO HISTÓRICO
        // =================================================
        //
        // Se conservan los tipos anteriores
        // utilizados por CU-11 y CU-14.
        //
        // El tipo específico del CU-15 se
        // guarda en MovimientoBitacoraDetalle.
        //
        // =================================================

        MovimientoInventarioFarmacia.TipoMovimiento
                tipoHistorico = obtenerTipoHistorico(tipo);

        // =================================================
        // 12. REGISTRAR MOVIMIENTO HISTÓRICO
        // =================================================

        MovimientoInventarioFarmacia movimiento =
                new MovimientoInventarioFarmacia(
                        inventario,
                        usuario,
                        tipoHistorico,
                        cantidad,
                        stockAnterior,
                        stockNuevo,
                        "CU15_BITACORA_INVENTARIO",
                        formulario.getNumeroReferencia(),
                        formulario.getMotivoNotas()
                );

        movimiento =
                movimientoRepository.saveAndFlush(
                        movimiento
                );

        // =================================================
        // 13. REGISTRAR DETALLE CU-15
        // =================================================
        //
        // Primero se guarda el movimiento porque
        // @MapsId necesita su ID generado.
        //
        // =================================================

        MovimientoBitacoraDetalle detalle =
                new MovimientoBitacoraDetalle(
                        movimiento,
                        tipo,
                        formulario.getCostoUnitario()
                );

        detalleRepository.saveAndFlush(
                detalle
        );

        // =================================================
        // 14. REGISTRAR AUDITORÍA
        // =================================================

        Map<String, Object> datosAuditoria =
                new LinkedHashMap<>();

        datosAuditoria.put(
                "idInventario",
                inventario.getId()
        );

        datosAuditoria.put(
                "idMedicamento",
                inventario.getMedicamento().getId()
        );

        datosAuditoria.put(
                "idSucursal",
                inventario.getSucursal().getId()
        );

        datosAuditoria.put(
                "tipo",
                tipo.getNombre()
        );

        datosAuditoria.put(
                "cantidad",
                cantidad
        );

        datosAuditoria.put(
                "stockAnterior",
                stockAnterior
        );

        datosAuditoria.put(
                "stockNuevo",
                stockNuevo
        );

        if (formulario.getCostoUnitario() != null) {

            datosAuditoria.put(
                    "costoUnitario",
                    formulario.getCostoUnitario().toPlainString()
            );
        }

        if (formulario.getNumeroReferencia() != null) {

            datosAuditoria.put(
                    "referencia",
                    formulario.getNumeroReferencia()
            );
        }

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "movimiento_inventario_farmacia",
                        String.valueOf(movimiento.getId()),
                        AccionAuditoria.CREAR,
                        usuario.getId(),
                        usuario.getNombreUsuario(),
                        null,
                        datosAuditoria,
                        null
                )
        );

        // =================================================
        // 15. MENSAJE EXACTO DEL FLUJO NORMAL
        // =================================================

        String nombreMedicamento =
                inventario.getMedicamento().getNombre();

        String mensaje =
                "Movimiento registrado exitosamente. Medicamento: "
                        + nombreMedicamento
                        + ". Tipo: "
                        + tipo.getNombre()
                        + ". Cantidad: "
                        + cantidad
                        + ". Stock actualizado: "
                        + stockNuevo
                        + ".";

        // =================================================
        // 16. PREPARAR ALERTA DE STOCK MÍNIMO
        // =================================================
        //
        // La alerta preventiva ANTES de guardar
        // se conectará en el formulario FA05.
        //
        // Aquí devolvemos también la alerta
        // después del registro si corresponde.
        //
        // =================================================

        String alerta = null;

        Integer stockMinimo =
                inventario.getMedicamento().getStockMinimo();

        if (stockMinimo != null
                && stockNuevo < stockMinimo) {

            alerta =
                    nombreMedicamento
                            + ": Stock bajo — disponible: "
                            + stockNuevo
                            + " (mínimo: "
                            + stockMinimo
                            + ")";
        }

        // =================================================
        // 17. RETORNAR RESULTADO
        // =================================================

        return new ResultadoMovimiento(
                movimiento.getId(),
                stockAnterior,
                stockNuevo,
                mensaje,
                alerta
        );
    }

    // =====================================================
    // CONVERTIR AL TIPO HISTÓRICO
    // =====================================================

    private MovimientoInventarioFarmacia.TipoMovimiento
    obtenerTipoHistorico(
            TipoMovimientoBitacora tipo
    ) {

        return switch (tipo) {

            case COMPRA, DEVOLUCION ->
                    MovimientoInventarioFarmacia
                            .TipoMovimiento.ENTRADA;

            case VENTA, RECLAMO ->
                    MovimientoInventarioFarmacia
                            .TipoMovimiento.SALIDA;

            case AJUSTE_POSITIVO, AJUSTE_NEGATIVO ->
                    MovimientoInventarioFarmacia
                            .TipoMovimiento.AJUSTE;

            case DESPACHO ->
                    throw new IllegalArgumentException(
                            "El despacho se registra automáticamente."
                    );
        };
    }
}
