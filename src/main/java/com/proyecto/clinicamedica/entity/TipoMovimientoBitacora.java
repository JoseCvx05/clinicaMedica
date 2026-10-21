
package com.proyecto.clinicamedica.entity;

import java.util.Arrays;
import java.util.List;

/**
 * =========================================================
 * CU-15 - TIPOS DE MOVIMIENTO DE BITÁCORA
 * =========================================================
 *
 * RN-CU13-01
 *
 * Movimientos manuales:
 * 0 - Compra
 * 1 - Devolución
 * 2 - Venta
 * 3 - Reclamo
 * 4 - Ajuste+
 * 5 - Ajuste-
 *
 * Movimiento automático:
 * 6 - Despacho
 *
 * IMPORTANTE:
 *
 * No reemplaza los tipos históricos del CU-14.
 * No modifica inventarios ni despachos existentes.
 *
 * =========================================================
 */
public enum TipoMovimientoBitacora {

    COMPRA(0, "Compra", false),

    DEVOLUCION(1, "Devolución", false),

    VENTA(2, "Venta", true),

    RECLAMO(3, "Reclamo", true),

    AJUSTE_POSITIVO(4, "Ajuste+", false),

    AJUSTE_NEGATIVO(5, "Ajuste-", true),

    DESPACHO(6, "Despacho", true);

    private final int codigo;
    private final String nombre;
    private final boolean salida;

    TipoMovimientoBitacora(
            int codigo,
            String nombre,
            boolean salida
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.salida = salida;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esSalida() {
        return salida;
    }

    public boolean esManual() {
        return this != DESPACHO;
    }

    /**
     * El formulario únicamente muestra tipos 0 a 5.
     */
    public static List<TipoMovimientoBitacora> tiposManuales() {

        return Arrays.stream(values())
                .filter(TipoMovimientoBitacora::esManual)
                .toList();
    }

    /**
     * Obtiene el movimiento mediante su código.
     */
    public static TipoMovimientoBitacora desdeCodigo(
            Integer codigo
    ) {

        if (codigo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de movimiento."
            );
        }

        for (TipoMovimientoBitacora tipo : values()) {

            if (tipo.codigo == codigo) {
                return tipo;
            }
        }

        throw new IllegalArgumentException(
                "El tipo de movimiento no es válido."
        );
    }

    /**
     * Impide crear despachos manualmente,
     * incluso si se manipula el formulario.
     */
    public static TipoMovimientoBitacora desdeCodigoManual(
            Integer codigo
    ) {

        TipoMovimientoBitacora tipo =
                desdeCodigo(codigo);

        if (!tipo.esManual()) {
            throw new IllegalArgumentException(
                    "El despacho se registra automáticamente."
            );
        }

        return tipo;
    }
}
