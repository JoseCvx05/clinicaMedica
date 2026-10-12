
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.Medicamento;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * =========================================================
 * CU-11 - SERVICE: CALCULO DE MONTO DE FARMACIA
 * =========================================================
 *
 * Calcula los precios y montos de medicamentos
 * utilizando exclusivamente datos del backend.
 *
 * Reglas:
 *
 * - Original: precio historico de la receta.
 * - Sustitucion: precio actual del medicamento.
 * - Cantidades positivas.
 * - Montos no negativos.
 * - Maximo dos decimales.
 * - Limite monetario permitido.
 *
 * No registra despachos.
 * No modifica inventario.
 * No envia correos.
 *
 * =========================================================
 */
@Service
public class FarmaciaCalculoMontoService {

    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("9999999999.99");


    // =====================================================
    // OBTENER PRECIO UNITARIO
    // =====================================================

    public BigDecimal obtenerPrecioUnitario(
            DetalleRecetaMedica detalle,
            Medicamento medicamentoEntregado
    ) {

        if (detalle == null
                || detalle.getMedicamento() == null
                || detalle.getMedicamento().getId() == null) {

            throw new IllegalArgumentException(
                    "El detalle de receta no es valido."
            );
        }

        if (medicamentoEntregado == null
                || medicamentoEntregado.getId() == null) {

            throw new IllegalArgumentException(
                    "El medicamento a entregar no es valido."
            );
        }

        boolean esSustitucion =
                !Objects.equals(
                        detalle.getMedicamento().getId(),
                        medicamentoEntregado.getId()
                );

        BigDecimal precioUnitario =
                esSustitucion
                        ? medicamentoEntregado.getPrecio()
                        : detalle.getPrecioUnitario();

        validarPrecio(precioUnitario);

        return precioUnitario.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }


    // =====================================================
    // CALCULAR SUBTOTAL
    // =====================================================

    public BigDecimal calcularSubtotal(
            DetalleRecetaMedica detalle,
            Medicamento medicamentoEntregado,
            Integer cantidad
    ) {

        if (cantidad == null || cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor a cero."
            );
        }

        BigDecimal precioUnitario =
                obtenerPrecioUnitario(
                        detalle,
                        medicamentoEntregado
                );

        BigDecimal subtotal =
                precioUnitario.multiply(
                        BigDecimal.valueOf(cantidad)
                );

        validarMonto(subtotal);

        return subtotal.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }


    // =====================================================
    // ACUMULAR MONTO TOTAL
    // =====================================================

    public BigDecimal sumarMonto(
            BigDecimal totalActual,
            BigDecimal subtotal
    ) {

        validarMonto(totalActual);
        validarMonto(subtotal);

        BigDecimal nuevoTotal =
                totalActual.add(subtotal);

        validarMonto(nuevoTotal);

        return nuevoTotal.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }


    // =====================================================
    // VALIDAR PRECIO
    // =====================================================

    private void validarPrecio(
            BigDecimal precio
    ) {

        if (precio == null
                || precio.compareTo(BigDecimal.ZERO) < 0
                || precio.stripTrailingZeros().scale() > 2) {

            throw new IllegalStateException(
                    "El medicamento tiene un precio invalido."
            );
        }
    }


    // =====================================================
    // VALIDAR MONTO
    // =====================================================

    private void validarMonto(
            BigDecimal monto
    ) {

        if (monto == null
                || monto.compareTo(BigDecimal.ZERO) < 0
                || monto.compareTo(MONTO_MAXIMO) > 0
                || monto.stripTrailingZeros().scale() > 2) {

            throw new IllegalStateException(
                    "El monto del despacho no es valido "
                            + "o supera el limite permitido."
            );
        }
    }
}
