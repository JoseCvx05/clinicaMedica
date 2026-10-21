
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.MovimientoBitacoraFormularioDTO;
import com.proyecto.clinicamedica.entity.TipoMovimientoBitacora;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * =========================================================
 * CU-15 - VALIDACIONES DE MOVIMIENTOS DE INVENTARIO
 * =========================================================
 *
 * RN-CU13-01 - Campos del movimiento.
 * FA03 - Validación de campos fallida.
 *
 * Responsabilidades:
 *
 * - Validar medicamento obligatorio.
 * - Validar sucursal obligatoria.
 * - Validar tipo de movimiento.
 * - Impedir despachos manuales.
 * - Validar cantidad positiva.
 * - Validar costo unitario de compras.
 * - Validar referencia opcional.
 * - Validar motivo y notas según el tipo.
 *
 * IMPORTANTE:
 *
 * - No consulta ni modifica inventarios.
 * - No registra movimientos.
 * - No modifica despachos del CU-11.
 * - No modifica movimientos del CU-14.
 *
 * Las validaciones de stock y seguridad
 * se realizarán en el Service transaccional.
 *
 * =========================================================
 */
@Service
public class MovimientoBitacoraValidacionService {

    private static final int MAX_REFERENCIA = 50;

    private static final int MIN_MOTIVO = 10;

    private static final int MAX_MOTIVO = 500;

    // =====================================================
    // VALIDAR FORMULARIO COMPLETO
    // =====================================================

    public TipoMovimientoBitacora validar(
            MovimientoBitacoraFormularioDTO formulario
    ) {

        if (formulario == null) {
            throw new IllegalArgumentException(
                    "Los datos del movimiento son obligatorios."
            );
        }

        // =================================================
        // MEDICAMENTO OBLIGATORIO
        // =================================================

        if (formulario.getIdMedicamento() == null
                || formulario.getIdMedicamento() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento."
            );
        }

        // =================================================
        // SUCURSAL OBLIGATORIA
        // =================================================

        if (formulario.getIdSucursal() == null
                || formulario.getIdSucursal() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal."
            );
        }

        // =================================================
        // TIPO DE MOVIMIENTO
        // =================================================
        //
        // Solo permite:
        // 0, 1, 2, 3, 4 y 5.
        //
        // El tipo 6 es automático.
        // =================================================

        TipoMovimientoBitacora tipo =
                TipoMovimientoBitacora.desdeCodigoManual(
                        formulario.getTipoMovimiento()
                );

        // =================================================
        // CANTIDAD ENTERA POSITIVA
        // =================================================

        if (formulario.getCantidad() == null
                || formulario.getCantidad() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser un número entero positivo."
            );
        }

        // =================================================
        // COSTO UNITARIO
        // =================================================

        validarCostoUnitario(
                tipo,
                formulario.getCostoUnitario()
        );

        // =================================================
        // NÚMERO DE REFERENCIA
        // =================================================

        String referencia =
                limpiar(formulario.getNumeroReferencia());

        if (referencia != null
                && referencia.length() > MAX_REFERENCIA) {

            throw new IllegalArgumentException(
                    "El número de referencia no puede superar los 50 caracteres."
            );
        }

        formulario.setNumeroReferencia(referencia);

        // =================================================
        // MOTIVO / NOTAS
        // =================================================

        String motivo =
                limpiar(formulario.getMotivoNotas());

        switch (tipo) {

            // ---------------------------------------------
            // AJUSTES: JUSTIFICACIÓN OBLIGATORIA
            // ---------------------------------------------

            case AJUSTE_POSITIVO, AJUSTE_NEGATIVO -> {

                validarMotivoObligatorio(motivo);
            }

            // ---------------------------------------------
            // DEVOLUCIÓN Y RECLAMO
            // ---------------------------------------------

            case DEVOLUCION, RECLAMO -> {

                validarMotivoObligatorio(motivo);
            }

            // ---------------------------------------------
            // COMPRA: NOTAS OPCIONALES
            // ---------------------------------------------

            case COMPRA -> {

                if (motivo != null
                        && motivo.length() > MAX_MOTIVO) {

                    throw new IllegalArgumentException(
                            "El motivo debe contener entre 10 y 500 caracteres."
                    );
                }
            }

            // ---------------------------------------------
            // VENTA: NO PERMITE MOTIVO
            // ---------------------------------------------

            case VENTA -> {

                if (motivo != null) {

                    throw new IllegalArgumentException(
                            "El movimiento de venta no permite motivo o notas."
                    );
                }
            }

            // ---------------------------------------------
            // DESPACHO AUTOMÁTICO
            // ---------------------------------------------

            case DESPACHO -> {

                throw new IllegalArgumentException(
                        "El despacho se registra automáticamente."
                );
            }
        }

        formulario.setMotivoNotas(motivo);

        return tipo;
    }

    // =====================================================
    // VALIDAR COSTO UNITARIO
    // =====================================================

    private void validarCostoUnitario(
            TipoMovimientoBitacora tipo,
            BigDecimal costo
    ) {

        // =================================================
        // COSTO OBLIGATORIO SOLO PARA COMPRA
        // =================================================

        if (tipo == TipoMovimientoBitacora.COMPRA) {

            if (costo == null) {

                throw new IllegalArgumentException(
                        "El costo unitario es obligatorio para compras"
                );
            }

            if (costo.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "El costo unitario debe ser mayor a 0"
                );
            }

            if (costo.scale() > 2) {

                throw new IllegalArgumentException(
                        "El costo unitario debe tener máximo 2 decimales"
                );
            }

            // NUMERIC(12,2): máximo 10 dígitos enteros.

            if (costo.precision() - costo.scale() > 10) {

                throw new IllegalArgumentException(
                        "El costo unitario supera el límite permitido."
                );
            }

            return;
        }

        // =================================================
        // OTROS MOVIMIENTOS
        // =================================================

        if (costo != null) {

            throw new IllegalArgumentException(
                    "El costo unitario solo se permite para compras."
            );
        }
    }

    // =====================================================
    // VALIDAR MOTIVO OBLIGATORIO
    // =====================================================

    private void validarMotivoObligatorio(
            String motivo
    ) {

        if (motivo == null
                || motivo.length() < MIN_MOTIVO
                || motivo.length() > MAX_MOTIVO) {

            throw new IllegalArgumentException(
                    "El motivo debe contener entre 10 y 500 caracteres."
            );
        }
    }

    // =====================================================
    // LIMPIAR TEXTOS
    // =====================================================

    private String limpiar(
            String valor
    ) {

        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }
}
