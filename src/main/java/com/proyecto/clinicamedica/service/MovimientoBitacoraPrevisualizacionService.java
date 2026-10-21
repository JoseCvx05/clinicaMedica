
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;

import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-15 - PREVISUALIZACIÓN DE MOVIMIENTOS
 * =========================================================
 *
 * FA02 - Stock insuficiente.
 * FA05 - Alerta de stock mínimo.
 *
 * Permite consultar:
 *
 * - Stock actual.
 * - Stock mínimo.
 * - Stock proyectado.
 * - Tipo de operación.
 * - Advertencias.
 *
 * SEGURIDAD:
 *
 * - Verifica el usuario autenticado.
 * - Respeta la sucursal autorizada.
 * - Verifica el tipo de movimiento según rol.
 *
 * IMPORTANTE:
 *
 * - No actualiza inventario.
 * - No registra movimientos.
 * - No altera movimientos históricos.
 * - No reemplaza la validación transaccional.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MovimientoBitacoraPrevisualizacionService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final InventarioFarmaciaRepository
            inventarioRepository;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraPrevisualizacionService(
            InventarioFarmaciaRepository inventarioRepository,
            MovimientoBitacoraSeguridadService seguridadService
    ) {

        this.inventarioRepository = inventarioRepository;
        this.seguridadService = seguridadService;
    }

    // =====================================================
    // RESULTADO DE LA PREVISUALIZACIÓN
    // =====================================================

    public record VistaStock(

            Integer stockActual,

            Integer stockMinimo,

            Long stockProyectado,

            String operacion,

            boolean stockInsuficiente,

            String mensajeStockInsuficiente,

            String alertaStockMinimo

    ) {
    }

    // =====================================================
    // CONSULTAR Y PROYECTAR STOCK
    // =====================================================

    public VistaStock previsualizar(
            Integer idMedicamento,
            Integer idSucursal,
            Integer codigoTipo,
            Integer cantidad
    ) {

        // =================================================
        // 1. VALIDAR MEDICAMENTO
        // =================================================

        if (idMedicamento == null || idMedicamento <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento."
            );
        }

        // =================================================
        // 2. VALIDAR SUCURSAL
        // =================================================

        if (idSucursal == null || idSucursal <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal."
            );
        }

        // =================================================
        // 3. VALIDAR AUTORIZACIÓN POR SUCURSAL
        // =================================================

        seguridadService.obtenerSucursalConsulta(
                idSucursal
        );

        // =================================================
        // 4. VALIDAR TIPO, SI FUE SELECCIONADO
        // =================================================

        TipoMovimientoBitacora tipo = null;

        if (codigoTipo != null) {

            tipo = TipoMovimientoBitacora
                    .desdeCodigoManual(codigoTipo);

            seguridadService.validarRegistro(
                    idSucursal,
                    tipo
            );
        }

        // =================================================
        // 5. VALIDAR CANTIDAD, SI FUE INGRESADA
        // =================================================

        if (cantidad != null && cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser un número entero positivo."
            );
        }

        // =================================================
        // 6. CONSULTAR INVENTARIO
        // =================================================

        InventarioFarmacia inventario =
                inventarioRepository
                        .findByMedicamento_IdAndSucursal_Id(
                                idMedicamento,
                                idSucursal
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe inventario registrado "
                                                + "para el medicamento y la "
                                                + "sucursal seleccionados."
                                )
                        );

        // =================================================
        // 7. VALIDAR INVENTARIO ACTIVO
        // =================================================

        if (!Boolean.TRUE.equals(inventario.getActivo())) {

            throw new IllegalStateException(
                    "No se pueden registrar movimientos "
                            + "en un inventario inactivo."
            );
        }

        // =================================================
        // 8. VALIDAR MEDICAMENTO ACTIVO
        // =================================================

        if (inventario.getMedicamento() == null
                || !inventario.getMedicamento().estaActivo()) {

            throw new IllegalStateException(
                    "No se pueden registrar movimientos "
                            + "de un medicamento inactivo."
            );
        }

        // =================================================
        // 9. VALIDAR SUCURSAL ACTIVA
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
        // 10. OBTENER STOCK REAL
        // =================================================

        Integer stockActual =
                inventario.getCantidadDisponible();

        Integer stockMinimo =
                inventario.getMedicamento().getStockMinimo();

        // =================================================
        // 11. CALCULAR PROYECCIÓN
        // =================================================

        Long stockProyectado = null;

        String operacion = "Sin seleccionar";

        boolean insuficiente = false;

        String mensajeInsuficiente = null;

        String alertaMinimo = null;

        // El stock actual puede consultarse incluso
        // antes de seleccionar tipo y cantidad.

        if (tipo != null && cantidad != null) {

            boolean salida = tipo.esSalida();

            operacion = salida
                    ? "Salida"
                    : "Entrada";

            long proyeccion = salida
                    ? (long) stockActual - cantidad
                    : (long) stockActual + cantidad;

            stockProyectado = proyeccion;

            // =============================================
            // 12. FA02 - STOCK INSUFICIENTE
            // =============================================

            if (salida && cantidad > stockActual) {

                insuficiente = true;

                mensajeInsuficiente =
                        "Stock insuficiente. Stock actual: "
                                + stockActual
                                + ". No se puede registrar una salida de "
                                + cantidad
                                + " unidades.";
            }

            // =============================================
            // 13. VALIDAR LÍMITE DEL TIPO INTEGER
            // =============================================

            if (proyeccion > Integer.MAX_VALUE) {

                throw new IllegalArgumentException(
                        "La cantidad solicitada supera "
                                + "el límite permitido para el inventario."
                );
            }

            // =============================================
            // 14. FA05 - ALERTA DE STOCK BAJO
            // =============================================
            //
            // Se muestra cuando:
            //
            // - La operación tiene stock suficiente.
            // - El stock proyectado es inferior
            //   al mínimo configurado.
            //
            // =============================================

            if (!insuficiente
                    && stockMinimo != null
                    && proyeccion < stockMinimo) {

                String nombreMedicamento =
                        inventario.getMedicamento().getNombre();

                alertaMinimo =
                        nombreMedicamento
                                + ": Stock bajo — disponible: "
                                + proyeccion
                                + " (mínimo: "
                                + stockMinimo
                                + ")";
            }
        }

        // =================================================
        // 15. DEVOLVER RESULTADO
        // =================================================

        return new VistaStock(
                stockActual,
                stockMinimo,
                stockProyectado,
                operacion,
                insuficiente,
                mensajeInsuficiente,
                alertaMinimo
        );
    }
}
