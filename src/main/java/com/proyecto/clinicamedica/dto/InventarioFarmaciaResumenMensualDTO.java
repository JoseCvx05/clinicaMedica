
package com.proyecto.clinicamedica.dto;

import java.time.YearMonth;

/**
 * =========================================================
 * CU-14 - CATÁLOGO 8
 * DTO: RESUMEN MENSUAL DE INVENTARIO DE MEDICAMENTOS
 * =========================================================
 *
 * Contiene los resultados de la consulta mensual:
 *
 * - Inventario y medicamento.
 * - Sucursal.
 * - Mes y año consultados.
 * - Existencias iniciales.
 * - Altas iniciales del período.
 * - Entradas de medicamentos.
 * - Salidas por despachos.
 * - Ajustes positivos y negativos.
 * - Existencias finales.
 * - Total de movimientos.
 *
 * Esta clase NO modifica las existencias.
 * Su responsabilidad es transportar los resultados
 * calculados por el servicio.
 *
 * =========================================================
 */
public class InventarioFarmaciaResumenMensualDTO {

    // =====================================================
    // IDENTIFICACIÓN DEL INVENTARIO
    // =====================================================

    private final Integer idInventario;

    private final String medicamento;

    private final String sucursal;

    // =====================================================
    // PERÍODO CONSULTADO
    // =====================================================

    private final YearMonth periodo;

    // =====================================================
    // EXISTENCIAS Y MOVIMIENTOS
    // =====================================================

    private final int stockInicial;

    private final int altasIniciales;

    private final int entradas;

    private final int salidas;

    private final int ajustesPositivos;

    private final int ajustesNegativos;

    private final int stockFinal;

    private final long totalMovimientos;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public InventarioFarmaciaResumenMensualDTO(
            Integer idInventario,
            String medicamento,
            String sucursal,
            YearMonth periodo,
            int stockInicial,
            int altasIniciales,
            int entradas,
            int salidas,
            int ajustesPositivos,
            int ajustesNegativos,
            int stockFinal,
            long totalMovimientos
    ) {

        this.idInventario = idInventario;
        this.medicamento = medicamento;
        this.sucursal = sucursal;
        this.periodo = periodo;
        this.stockInicial = stockInicial;
        this.altasIniciales = altasIniciales;
        this.entradas = entradas;
        this.salidas = salidas;
        this.ajustesPositivos = ajustesPositivos;
        this.ajustesNegativos = ajustesNegativos;
        this.stockFinal = stockFinal;
        this.totalMovimientos = totalMovimientos;
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public Integer getIdInventario() {
        return idInventario;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public String getSucursal() {
        return sucursal;
    }

    public YearMonth getPeriodo() {
        return periodo;
    }

    public int getStockInicial() {
        return stockInicial;
    }

    public int getAltasIniciales() {
        return altasIniciales;
    }

    public int getEntradas() {
        return entradas;
    }

    public int getSalidas() {
        return salidas;
    }

    public int getAjustesPositivos() {
        return ajustesPositivos;
    }

    public int getAjustesNegativos() {
        return ajustesNegativos;
    }

    public int getStockFinal() {
        return stockFinal;
    }

    public long getTotalMovimientos() {
        return totalMovimientos;
    }

    // =====================================================
    // VARIACIÓN NETA DEL MES
    // =====================================================

    public int getVariacionNeta() {

        return stockFinal - stockInicial;
    }
}
