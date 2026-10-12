
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;

import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;
import com.proyecto.clinicamedica.repository.DetalleRecetaMedicaRepository;
import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================
 * SERVICE: DETALLE DE RECETA PARA FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite consultar:
 *
 * - Medicamentos recetados.
 * - Cantidades prescritas.
 * - Cantidades previamente entregadas.
 * - Cantidades pendientes.
 * - Inventario de la sucursal.
 * - Stock minimo.
 * - Medicamentos controlados.
 * - Precio historico de la receta.
 *
 * SEGURIDAD:
 *
 * Todas las validaciones se realizan en Java.
 *
 * Este servicio no modifica existencias.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class FarmaciaDetalleRecetaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final RecetaMedicaRepository recetaRepository;

    private final DetalleRecetaMedicaRepository
            detalleRecetaRepository;

    private final InventarioFarmaciaRepository
            inventarioRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;

    private final FarmaciaSeguridadService
            farmaciaSeguridadService;

    private final FarmaciaRecetaValidacionService
            recetaValidacionService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaDetalleRecetaService(
            RecetaMedicaRepository recetaRepository,
            DetalleRecetaMedicaRepository detalleRecetaRepository,
            InventarioFarmaciaRepository inventarioRepository,
            DetalleDespachoRepository detalleDespachoRepository,
            FarmaciaSeguridadService farmaciaSeguridadService,
            FarmaciaRecetaValidacionService recetaValidacionService
    ) {

        this.recetaRepository = recetaRepository;
        this.detalleRecetaRepository = detalleRecetaRepository;
        this.inventarioRepository = inventarioRepository;
        this.detalleDespachoRepository = detalleDespachoRepository;
        this.farmaciaSeguridadService = farmaciaSeguridadService;
        this.recetaValidacionService = recetaValidacionService;
    }


    // =====================================================
    // DTO: MEDICAMENTO DE LA RECETA
    // =====================================================

    public record MedicamentoRecetado(

            Integer idDetalleReceta,

            Integer idMedicamento,

            String nombreMedicamento,

            String dosis,

            String frecuencia,

            String duracion,

            String unidad,

            int cantidadRecetada,

            long cantidadEntregada,

            int cantidadPendiente,

            int stockDisponible,

            Integer stockMinimo,

            boolean inventarioRegistrado,

            boolean stockBajo,

            boolean medicamentoActivo,

            boolean medicamentoControlado,

            int cantidadMaximaDespachable,

            BigDecimal precioUnitario

    ) {
    }


    // =====================================================
    // DTO: RESULTADO DEL DETALLE
    // =====================================================

    public record DetalleReceta(

            Integer idReceta,

            Integer idConsulta,

            OffsetDateTime fechaEmision,

            boolean vigente,

            long diasTranscurridos,

            String notas,

            List<MedicamentoRecetado> medicamentos

    ) {
    }


    // =====================================================
    // CONSULTAR DETALLE DE RECETA
    // =====================================================

    public DetalleReceta consultarDetalle(
            Integer idReceta
    ) {

        // -------------------------------------------------
        // VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idReceta == null || idReceta <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una receta médica válida."
            );
        }


        // -------------------------------------------------
        // VALIDAR FARMACEUTICO Y SUCURSAL
        // -------------------------------------------------

        Integer idSucursal =
                farmaciaSeguridadService
                        .obtenerIdSucursalAutorizada();


        // -------------------------------------------------
        // BUSCAR RECETA ACTIVA
        // -------------------------------------------------

        RecetaMedica receta =
                recetaRepository
                        .findByIdAndActivoTrue(idReceta)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "No se encontró una receta médica activa."
                                )
                        );


        // -------------------------------------------------
        // VALIDAR ACCESO A LA RECETA
        // -------------------------------------------------

        farmaciaSeguridadService
                .validarAccesoReceta(receta);


        // -------------------------------------------------
        // CONSULTAR VIGENCIA
        // -------------------------------------------------

        FarmaciaRecetaValidacionService.VigenciaReceta
                vigencia =
                recetaValidacionService
                        .consultarVigencia(receta);


        // -------------------------------------------------
        // VALIDAR CONSULTA
        // -------------------------------------------------

        if (receta.getConsultaMedica() == null
                || receta.getConsultaMedica().getId() == null) {

            throw new IllegalStateException(
                    "La receta no tiene una consulta médica válida."
            );
        }


        // -------------------------------------------------
        // CONSULTAR MEDICAMENTOS RECETADOS
        // -------------------------------------------------

        List<DetalleRecetaMedica> detalles =
                detalleRecetaRepository
                        .findByRecetaMedica_IdOrderByIdAsc(
                                idReceta
                        );


        if (detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La receta médica no tiene medicamentos registrados."
            );
        }


        List<MedicamentoRecetado> medicamentos =
                new ArrayList<>();


        // -------------------------------------------------
        // PROCESAR CADA MEDICAMENTO
        // -------------------------------------------------

        for (DetalleRecetaMedica detalle : detalles) {

            medicamentos.add(
                    construirMedicamento(
                            detalle,
                            idSucursal,
                            vigencia.vigente()
                    )
            );
        }


        // -------------------------------------------------
        // RETORNAR DETALLE COMPLETO
        // -------------------------------------------------

        return new DetalleReceta(

                receta.getId(),

                receta.getConsultaMedica().getId(),

                receta.getFechaEmision(),

                vigencia.vigente(),

                vigencia.diasTranscurridos(),

                receta.getNotas(),

                List.copyOf(medicamentos)
        );
    }


    // =====================================================
    // CONSTRUIR DETALLE DEL MEDICAMENTO
    // =====================================================

    private MedicamentoRecetado construirMedicamento(
            DetalleRecetaMedica detalle,
            Integer idSucursal,
            boolean recetaVigente
    ) {

        // -------------------------------------------------
        // VALIDAR DETALLE
        // -------------------------------------------------

        if (detalle == null
                || detalle.getId() == null) {

            throw new IllegalStateException(
                    "La receta contiene un detalle inválido."
            );
        }


        // -------------------------------------------------
        // VALIDAR MEDICAMENTO
        // -------------------------------------------------

        Medicamento medicamento =
                detalle.getMedicamento();

        if (medicamento == null
                || medicamento.getId() == null) {

            throw new IllegalStateException(
                    "La receta contiene un medicamento inválido."
            );
        }


        // -------------------------------------------------
        // VALIDAR CANTIDAD PRESCRITA
        // -------------------------------------------------

        Integer cantidadRecetada =
                detalle.getCantidad();

        if (cantidadRecetada == null
                || cantidadRecetada <= 0) {

            throw new IllegalStateException(
                    "La cantidad prescrita no es válida."
            );
        }


        // -------------------------------------------------
        // CONSULTAR CANTIDAD YA ENTREGADA
        // -------------------------------------------------

        Long cantidadEntregada =
                detalleDespachoRepository
                        .sumarCantidadEntregadaPorDetalleYEstado(
                                detalle.getId(),
                                Despacho.EstadoDespacho.CONFIRMADO
                        );


        if (cantidadEntregada == null) {
            cantidadEntregada = 0L;
        }


        // -------------------------------------------------
        // VALIDAR HISTORIAL DE DESPACHOS
        // -------------------------------------------------

        if (cantidadEntregada < 0
                || cantidadEntregada > cantidadRecetada) {

            throw new IllegalStateException(
                    "El historial de cantidades entregadas "
                            + "presenta una inconsistencia."
            );
        }


        // -------------------------------------------------
        // CALCULAR CANTIDAD PENDIENTE
        // -------------------------------------------------

        int cantidadPendiente =
                (int) (cantidadRecetada - cantidadEntregada);


        // -------------------------------------------------
        // CONSULTAR INVENTARIO DE LA SUCURSAL
        // -------------------------------------------------

        InventarioFarmacia inventario =
                inventarioRepository
                        .findByMedicamento_IdAndSucursal_Id(
                                medicamento.getId(),
                                idSucursal
                        )
                        .orElse(null);


        boolean inventarioRegistrado =
                inventario != null;

        int stockDisponible = 0;


        if (inventarioRegistrado) {

            Integer cantidadDisponible =
                    inventario.getCantidadDisponible();

            if (cantidadDisponible == null
                    || cantidadDisponible < 0) {

                throw new IllegalStateException(
                        "El inventario del medicamento "
                                + "contiene una cantidad inválida."
                );
            }

            stockDisponible = cantidadDisponible;
        }


        // -------------------------------------------------
        // VERIFICAR STOCK MINIMO
        // -------------------------------------------------

        Integer stockMinimo =
                medicamento.getStockMinimo();

        boolean stockBajo =
                inventarioRegistrado
                        && stockMinimo != null
                        && stockDisponible <= stockMinimo;


        // -------------------------------------------------
        // ESTADO DEL MEDICAMENTO
        // -------------------------------------------------

        boolean medicamentoActivo =
                medicamento.estaActivo();

        boolean medicamentoControlado =
                medicamento.esControlado();


        // -------------------------------------------------
        // CALCULAR MAXIMO DESPACHABLE
        // -------------------------------------------------
        //
        // No permite superar:
        //
        // - Cantidad pendiente.
        // - Stock disponible.
        //
        // Recetas vencidas o medicamentos inactivos
        // no tendrán cantidad despachable.
        //
        // =================================================

        int cantidadMaximaDespachable = 0;

        if (recetaVigente
                && medicamentoActivo
                && inventarioRegistrado) {

            cantidadMaximaDespachable =
                    Math.min(
                            cantidadPendiente,
                            stockDisponible
                    );
        }


        // -------------------------------------------------
        // VALIDAR PRECIO HISTORICO
        // -------------------------------------------------

        BigDecimal precioUnitario =
                detalle.getPrecioUnitario();

        if (precioUnitario == null
                || precioUnitario.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalStateException(
                    "El medicamento no tiene un precio histórico válido."
            );
        }


        // -------------------------------------------------
        // RETORNAR INFORMACION DEL MEDICAMENTO
        // -------------------------------------------------

        return new MedicamentoRecetado(

                detalle.getId(),

                medicamento.getId(),

                detalle.getNombreMedicamento(),

                detalle.getDosis(),

                detalle.getFrecuencia(),

                detalle.getDuracion(),

                detalle.getUnidad(),

                cantidadRecetada,

                cantidadEntregada,

                cantidadPendiente,

                stockDisponible,

                stockMinimo,

                inventarioRegistrado,

                stockBajo,

                medicamentoActivo,

                medicamentoControlado,

                cantidadMaximaDespachable,

                precioUnitario
        );
    }
}
