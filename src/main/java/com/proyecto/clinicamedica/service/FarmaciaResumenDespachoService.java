
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleDespacho;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DespachoRepository;
import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * CU-11 - SERVICE: RESUMEN DEL DESPACHO
 * =========================================================
 *
 * Consulta el resumen de un despacho confirmado.
 *
 * SEGURIDAD:
 *
 * - Verifica farmacéutico autenticado.
 * - Restringe la consulta a su sucursal.
 * - Consulta únicamente despachos confirmados.
 * - Obtiene datos históricos desde PostgreSQL.
 *
 * Este servicio:
 *
 * - NO modifica inventario.
 * - NO crea despachos.
 * - NO modifica recetas.
 * - NO envía correos.
 *
 * =========================================================
 */
@Service
public class FarmaciaResumenDespachoService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FarmaciaSeguridadService
            seguridadService;

    private final DespachoRepository
            despachoRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaResumenDespachoService(
            FarmaciaSeguridadService seguridadService,
            DespachoRepository despachoRepository,
            DetalleDespachoRepository detalleDespachoRepository
    ) {

        this.seguridadService = seguridadService;
        this.despachoRepository = despachoRepository;
        this.detalleDespachoRepository =
                detalleDespachoRepository;
    }


    // =====================================================
    // DTO: MEDICAMENTO DESPACHADO
    // =====================================================

    public record LineaResumen(

            String medicamentoOriginal,

            String medicamentoEntregado,

            Integer cantidadEntregada,

            BigDecimal precioUnitario,

            BigDecimal subtotal,

            String razonSustitucion

    ) {
    }


    // =====================================================
    // DTO: RESUMEN DEL DESPACHO
    // =====================================================

    public record ResumenDespacho(

            Integer idDespacho,

            Integer idReceta,

            List<LineaResumen> lineas,

            BigDecimal montoTotal

    ) {
    }


    // =====================================================
    // CONSULTAR RESUMEN CONFIRMADO
    // =====================================================

    @Transactional(readOnly = true)
    public ResumenDespacho consultarResumen(
            Integer idDespacho
    ) {

        // -------------------------------------------------
        // 1. VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idDespacho == null || idDespacho <= 0) {

            throw new IllegalArgumentException(
                    "El identificador de despacho no es válido."
            );
        }


        // -------------------------------------------------
        // 2. FARMACÉUTICO AUTORIZADO
        // -------------------------------------------------

        Usuario farmaceutico =
                seguridadService
                        .obtenerFarmaceuticoAutorizado();

        Integer idSucursal =
                farmaceutico.getSucursal().getId();


        // -------------------------------------------------
        // 3. CONSULTAR DESPACHO
        // -------------------------------------------------

        Despacho despacho =
                despachoRepository.findById(
                        idDespacho
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "El despacho no está disponible "
                                        + "para este usuario."
                        )
                );


        // -------------------------------------------------
        // 4. VALIDAR SUCURSAL
        // -------------------------------------------------

        if (despacho.getSucursal() == null
                || !Objects.equals(
                despacho.getSucursal().getId(),
                idSucursal
        )) {

            throw new IllegalStateException(
                    "El despacho no está disponible "
                            + "para este usuario."
            );
        }


        // -------------------------------------------------
        // 5. DEBE SER DESPACHO CONFIRMADO
        // -------------------------------------------------

        if (!despacho.estaConfirmado()) {

            throw new IllegalStateException(
                    "Solo puede consultarse el resumen "
                            + "de un despacho confirmado."
            );
        }


        // -------------------------------------------------
        // 6. CONSULTAR DETALLES GUARDADOS
        // -------------------------------------------------

        List<DetalleDespacho> detalles =
                detalleDespachoRepository
                        .findByDespacho_IdAndDespacho_Sucursal_IdOrderByIdAsc(
                                idDespacho,
                                idSucursal
                        );


        if (detalles.isEmpty()) {

            throw new IllegalStateException(
                    "El despacho confirmado no tiene "
                            + "medicamentos registrados."
            );
        }


        // -------------------------------------------------
        // 7. PREPARAR LÍNEAS DEL RESUMEN
        // -------------------------------------------------

        List<LineaResumen> lineas =
                new ArrayList<>();


        for (DetalleDespacho detalle : detalles) {

            LineaResumen linea =
                    new LineaResumen(

                            detalle.getNombreMedicamentoOriginal(),

                            detalle.getNombreMedicamentoEntregado(),

                            detalle.getCantidadEntregada(),

                            detalle.getPrecioUnitario(),

                            detalle.getSubtotal(),

                            detalle.getRazonSustitucion()
                    );

            lineas.add(linea);
        }


        // -------------------------------------------------
        // 8. DEVOLVER DATOS HISTÓRICOS
        // -------------------------------------------------

        return new ResumenDespacho(

                despacho.getId(),

                despacho.getRecetaMedica().getId(),

                List.copyOf(lineas),

                despacho.getMontoTotal()
        );
    }
}
