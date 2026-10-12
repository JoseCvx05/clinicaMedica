
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.dto.SolicitudDespachoFarmaciaDTO;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleDespacho;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DespachoRepository;
import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;
import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;
import com.proyecto.clinicamedica.event.DespachoConfirmadoFarmaciaEvent;

import org.springframework.context.ApplicationEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * =========================================================
 * SERVICE: DESPACHO DE FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Responsabilidades:
 *
 * - Validar solicitudes.
 * - Verificar farmacéutico y sucursal.
 * - Bloquear recetas contra despachos simultáneos.
 * - Validar vigencia y firma digital.
 * - Verificar autorizaciones de controlados.
 * - Impedir superar cantidades prescritas.
 * - Validar sustituciones.
 * - Calcular precios en el backend.
 * - Bloquear y descontar inventario.
 * - Guardar despacho y detalles.
 * - Controlar reenvíos mediante UUID.
 * - Registrar auditoría.
 *
 * El cobro físico es externo al sistema.
 *
 * Las notificaciones FA02, FA04 y el registro
 * NO_ADQUIRIDO se integrarán por separado.
 *
 * =========================================================
 */
@Service
public class FarmaciaDespachoService {

    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("9999999999.99");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FarmaciaSolicitudDespachoValidador
            solicitudValidador;

    private final FarmaciaSeguridadService
            seguridadService;

    private final FarmaciaRecetaValidacionService
            recetaValidacionService;

    private final FarmaciaFirmaRecetaValidacionService
            firmaRecetaService;

    private final FarmaciaMedicamentoControladoValidacionService
            controladosService;

    private final RecetaMedicaRepository
            recetaRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final InventarioFarmaciaRepository
            inventarioRepository;

    private final DespachoRepository
            despachoRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;

    private final AuditoriaService
            auditoriaService;

    // CU-11 - Cálculo centralizado de precios y montos.
    private final FarmaciaCalculoMontoService
            calculoMontoService;

    // CU-11 FA02 - Publicación del evento de despacho.
    private final ApplicationEventPublisher
            eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaDespachoService(
            FarmaciaSolicitudDespachoValidador solicitudValidador,
            FarmaciaSeguridadService seguridadService,
            FarmaciaRecetaValidacionService recetaValidacionService,
            FarmaciaFirmaRecetaValidacionService firmaRecetaService,
            FarmaciaMedicamentoControladoValidacionService controladosService,
            RecetaMedicaRepository recetaRepository,
            MedicamentoRepository medicamentoRepository,
            InventarioFarmaciaRepository inventarioRepository,
            DespachoRepository despachoRepository,
            DetalleDespachoRepository detalleDespachoRepository,
            AuditoriaService auditoriaService,
            ApplicationEventPublisher eventPublisher,
            FarmaciaCalculoMontoService calculoMontoService
    ) {

        this.solicitudValidador = solicitudValidador;
        this.seguridadService = seguridadService;
        this.recetaValidacionService = recetaValidacionService;
        this.firmaRecetaService = firmaRecetaService;
        this.controladosService = controladosService;
        this.recetaRepository = recetaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.inventarioRepository = inventarioRepository;
        this.despachoRepository = despachoRepository;
        this.detalleDespachoRepository = detalleDespachoRepository;
        this.auditoriaService = auditoriaService;
        this.eventPublisher = eventPublisher;
        this.calculoMontoService = calculoMontoService;

    }

    // =====================================================
    // RESULTADO DEL DESPACHO
    // =====================================================

    public record ResultadoDespacho(
            Integer idDespacho,
            BigDecimal montoTotal,
            int medicamentosDespachados,
            boolean operacionRepetida,
            int sustituciones,
            List<String> alertasStockMinimo
    ) {
    }

    // =====================================================
    // LÍNEA VALIDADA INTERNAMENTE
    // =====================================================

    private record LineaPreparada(
            DetalleRecetaMedica detalleReceta,
            Medicamento medicamentoEntregado,
            Integer cantidad,
            String razonSustitucion,
            BigDecimal subtotal
    ) {
    }

    // =====================================================
    // CONFIRMAR DESPACHO
    // =====================================================
    //
    // Todas las operaciones ocurren en una transacción.
    //
    // Si se produce una excepción, Spring revierte:
    //
    // - El despacho.
    // - Sus detalles.
    // - Los cambios de inventario.
    // - La auditoría realizada en la transacción.
    //
    // =====================================================

    @Transactional
    public ResultadoDespacho confirmarDespacho(
            SolicitudDespachoFarmaciaDTO solicitud
    ) {

        // -------------------------------------------------
        // 1. VALIDAR DATOS RECIBIDOS
        // -------------------------------------------------

        solicitudValidador.validar(solicitud);

        // -------------------------------------------------
        // 2. FARMACÉUTICO AUTENTICADO
        // -------------------------------------------------

        Usuario farmaceutico =
                seguridadService.obtenerFarmaceuticoAutorizado();

        Integer idSucursal =
                farmaceutico.getSucursal().getId();

        // -------------------------------------------------
        // 3. BLOQUEAR RECETA
        // -------------------------------------------------
        //
        // El bloqueo se adquiere antes de calcular
        // las cantidades previamente despachadas.
        //
        // -------------------------------------------------

        RecetaMedica receta =
                recetaRepository.findWithLockById(
                        solicitud.getIdReceta()
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "La receta médica no existe."
                        )
                );

        // -------------------------------------------------
        // 4. VALIDAR ACCESO POR SUCURSAL
        // -------------------------------------------------

        seguridadService.validarAccesoReceta(receta);

        // -------------------------------------------------
        // 5. CONTROLAR REENVÍOS
        // -------------------------------------------------

        Despacho despachoAnterior =
                despachoRepository.findByClaveOperacion(
                        solicitud.getClaveOperacion()
                ).orElse(null);

        if (despachoAnterior != null) {

            return recuperarOperacionAnterior(
                    despachoAnterior,
                    solicitud,
                    farmaceutico,
                    idSucursal
            );
        }

        // -------------------------------------------------
        // 6. VIGENCIA Y CONSULTA FINALIZADA
        // -------------------------------------------------

        recetaValidacionService.validarParaDespacho(
                receta
        );

        // -------------------------------------------------
        // 7. OBTENER TODOS LOS DETALLES DE LA RECETA
        // -------------------------------------------------
        //
        // Se cargan todos porque la firma digital
        // corresponde a la receta completa.
        //
        // -------------------------------------------------

        List<DetalleRecetaMedica> detallesReceta =
                entityManager.createQuery(
                                """
                                SELECT d
                                FROM DetalleRecetaMedica d
                                JOIN FETCH d.medicamento
                                WHERE d.recetaMedica.id = :idReceta
                                ORDER BY d.id ASC
                                """,
                                DetalleRecetaMedica.class
                        )
                        .setParameter(
                                "idReceta",
                                receta.getId()
                        )
                        .getResultList();

        // -------------------------------------------------
        // 8. VALIDAR FIRMA DIGITAL
        // -------------------------------------------------

        firmaRecetaService.validar(
                receta,
                detallesReceta
        );

        // -------------------------------------------------
        // 9. INDEXAR DETALLES REALES
        // -------------------------------------------------

        Map<Integer, DetalleRecetaMedica> detallesPorId =
                new HashMap<>();

        for (DetalleRecetaMedica detalle : detallesReceta) {

            detallesPorId.put(
                    detalle.getId(),
                    detalle
            );
        }

        // -------------------------------------------------
        // 10. PREPARAR LÍNEAS
        // -------------------------------------------------

        List<LineaPreparada> lineas =
                new ArrayList<>();

        // Agrupamos cantidades por medicamento.
        // TreeMap mantiene los IDs ordenados para
        // adquirir los bloqueos en orden estable.

        Map<Integer, Integer> cantidadesPorMedicamento =
                new TreeMap<>();

        BigDecimal montoTotal =
                new BigDecimal("0.00");

        int sustituciones = 0;

        for (SolicitudDespachoFarmaciaDTO.LineaDespachoDTO item
                : solicitud.getMedicamentos()) {

            // ---------------------------------------------
            // DETALLE ORIGINAL DE ESTA RECETA
            // ---------------------------------------------

            DetalleRecetaMedica detalle =
                    detallesPorId.get(
                            item.getIdDetalleReceta()
                    );

            if (detalle == null) {

                throw new IllegalStateException(
                        "Uno de los medicamentos no pertenece "
                                + "a la receta seleccionada."
                );
            }

            // ---------------------------------------------
            // MEDICAMENTO REALMENTE ENTREGADO
            // ---------------------------------------------

            Medicamento entregado =
                    medicamentoRepository.findById(
                            item.getIdMedicamentoEntregado()
                    ).orElseThrow(
                            () -> new IllegalStateException(
                                    "El medicamento seleccionado "
                                            + "no existe."
                            )
                    );

            if (!entregado.estaActivo()) {

                throw new IllegalStateException(
                        "No se pueden despachar "
                                + "medicamentos inactivos."
                );
            }

            entregado.validar();

            // ---------------------------------------------
            // VALIDAR CANTIDAD PENDIENTE
            // ---------------------------------------------

            long cantidadPrescrita =
                    detalle.getCantidad();

            Long cantidadAnterior =
                    detalleDespachoRepository
                            .sumarCantidadEntregadaPorDetalleYEstado(
                                    detalle.getId(),
                                    Despacho.EstadoDespacho.CONFIRMADO
                            );

            long cantidadEntregadaAnteriormente =
                    cantidadAnterior == null
                            ? 0L
                            : cantidadAnterior;

            if (cantidadEntregadaAnteriormente < 0
                    || cantidadEntregadaAnteriormente
                    > cantidadPrescrita) {

                throw new IllegalStateException(
                        "La receta tiene cantidades "
                                + "despachadas inconsistentes."
                );
            }

            long cantidadPendiente =
                    cantidadPrescrita
                            - cantidadEntregadaAnteriormente;

            if (item.getCantidadEntregada()
                    > cantidadPendiente) {

                throw new IllegalStateException(
                        "La cantidad solicitada supera "
                                + "las unidades pendientes "
                                + "del medicamento recetado."
                );
            }

            // ---------------------------------------------
            // VALIDAR MEDICAMENTOS CONTROLADOS
            // ---------------------------------------------

            controladosService.validarParaDespacho(
                    detalle,
                    entregado,
                    item.getCantidadEntregada(),
                    cantidadEntregadaAnteriormente
            );
            // ---------------------------------------------
            // IDENTIFICAR SUSTITUCIÓN
            // ---------------------------------------------

            boolean esSustitucion =
                    !Objects.equals(
                            detalle.getMedicamento().getId(),
                            entregado.getId()
                    );

            String razon =
                    normalizarTexto(
                            item.getRazonSustitucion()
                    );


            // ---------------------------------------------
            // CU-11 FA02 - VALIDAR CASILLA DE SUSTITUCIÓN
            // ---------------------------------------------
            //
            // La casilla se recibe desde el formulario,
            // pero la decisión se valida en el backend.
            //
            // El medicamento original se obtiene desde
            // la receta almacenada en PostgreSQL.
            //
            // ---------------------------------------------

            boolean sustitucionSolicitada =
                    item.isSustitucionSolicitada();


            // ---------------------------------------------
            // CASILLA OBLIGATORIA SI CAMBIA MEDICAMENTO
            // ---------------------------------------------

            if (esSustitucion
                    && !sustitucionSolicitada) {

                throw new IllegalStateException(
                        "Debe marcar la casilla de sustitución "
                                + "para entregar un medicamento "
                                + "diferente al recetado."
                );
            }


            // ---------------------------------------------
            // NO PERMITIR CASILLA SIN CAMBIO REAL
            // ---------------------------------------------

            if (!esSustitucion
                    && sustitucionSolicitada) {

                throw new IllegalStateException(
                        "No puede registrar una sustitución "
                                + "si está entregando "
                                + "el medicamento original."
                );
            }


            if (esSustitucion) {

                if (razon == null) {

                    throw new IllegalStateException(
                            "Debe ingresar la razón "
                                    + "de sustitución del medicamento."
                    );
                }

                sustituciones++;

            } else if (razon != null) {

                throw new IllegalStateException(
                        "No debe registrar una sustitución "
                                + "si entrega el medicamento original."
                );
            }


            // ---------------------------------------------
            // CU-11 - CALCULAR MONTO EN EL BACKEND
            // ---------------------------------------------
            //
            // Medicamento original:
            // utiliza el precio histórico de la receta.
            //
            // Medicamento sustituido:
            // utiliza el precio actual de PostgreSQL.
            //
            // El cálculo se comparte con la futura
            // vista previa del despacho.
            //
            // ---------------------------------------------

            BigDecimal subtotal =
                    calculoMontoService.calcularSubtotal(
                            detalle,
                            entregado,
                            item.getCantidadEntregada()
                    );

            montoTotal =
                    calculoMontoService.sumarMonto(
                            montoTotal,
                            subtotal
                    );


            // ---------------------------------------------
            // ACUMULAR CANTIDAD DE INVENTARIO
            // ---------------------------------------------

            Integer idMedicamento =
                    entregado.getId();

            int totalSolicitado;

            try {

                totalSolicitado =
                        Math.addExact(
                                cantidadesPorMedicamento
                                        .getOrDefault(
                                                idMedicamento,
                                                0
                                        ),
                                item.getCantidadEntregada()
                        );

            } catch (ArithmeticException ex) {

                throw new IllegalStateException(
                        "La cantidad total solicitada "
                                + "supera el límite permitido."
                );
            }

            cantidadesPorMedicamento.put(
                    idMedicamento,
                    totalSolicitado
            );

            lineas.add(
                    new LineaPreparada(
                            detalle,
                            entregado,
                            item.getCantidadEntregada(),
                            razon,
                            subtotal
                    )
            );
        }

        // -------------------------------------------------
        // 11. BLOQUEAR INVENTARIOS
        // -------------------------------------------------
        //
        // Siempre se bloquean en orden de ID.
        // Esto reduce riesgos de interbloqueos.
        //
        // -------------------------------------------------

        Map<Integer, InventarioFarmacia> inventarios =
                new HashMap<>();

        for (Map.Entry<Integer, Integer> entrada
                : cantidadesPorMedicamento.entrySet()) {

            Integer idMedicamento =
                    entrada.getKey();

            Integer cantidadSolicitada =
                    entrada.getValue();

            InventarioFarmacia inventario =
                    inventarioRepository
                            .bloquearPorMedicamentoYSucursal(
                                    idMedicamento,
                                    idSucursal
                            )
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Sin inventario registrado "
                                                    + "para el medicamento #"
                                                    + idMedicamento
                                                    + " en esta sucursal."
                                    )
                            );

            if (!inventario.tieneStockSuficiente(
                    cantidadSolicitada
            )) {

                throw new IllegalStateException(
                        "Stock insuficiente para el medicamento #"
                                + idMedicamento
                                + ". Disponible: "
                                + inventario.getCantidadDisponible()
                                + " unidades."
                );
            }

            inventarios.put(
                    idMedicamento,
                    inventario
            );
        }

        // -------------------------------------------------
        // 12. CREAR CABECERA DEL DESPACHO
        // -------------------------------------------------

        Despacho despacho =
                new Despacho(
                        receta,
                        farmaceutico,
                        farmaceutico.getSucursal(),
                        Despacho.EstadoDespacho.CONFIRMADO,
                        montoTotal,
                        solicitud.getObservaciones(),
                        solicitud.getClaveOperacion()
                );

        despacho =
                despachoRepository.saveAndFlush(
                        despacho
                );

        // -------------------------------------------------
        // 13. GUARDAR CADA DETALLE
        // -------------------------------------------------

        BigDecimal totalGuardado =
                new BigDecimal("0.00");

        for (LineaPreparada linea : lineas) {

            DetalleDespacho detalleDespacho =
                    new DetalleDespacho(
                            despacho,
                            linea.detalleReceta(),
                            linea.medicamentoEntregado(),
                            linea.cantidad(),
                            linea.razonSustitucion()
                    );

            detalleDespacho =
                    detalleDespachoRepository.saveAndFlush(
                            detalleDespacho
                    );

            // El subtotal histórico fue calculado
            // por la entidad en @PrePersist.

            totalGuardado =
                    totalGuardado.add(
                            detalleDespacho.getSubtotal()
                    );
        }

        // -------------------------------------------------
        // 14. COMPROBAR CONSISTENCIA DE MONTOS
        // -------------------------------------------------

        if (totalGuardado.compareTo(
                montoTotal
        ) != 0) {

            throw new IllegalStateException(
                    "El total del despacho no coincide "
                            + "con sus medicamentos."
            );
        }

        // -------------------------------------------------
        // 15. DESCONTAR INVENTARIO
        // -------------------------------------------------

        List<String> alertasStockMinimo =
                new ArrayList<>();

        for (Map.Entry<Integer, Integer> entrada
                : cantidadesPorMedicamento.entrySet()) {

            InventarioFarmacia inventario =
                    inventarios.get(
                            entrada.getKey()
                    );


            // ---------------------------------------------
            // CU-11 FA04 - ALERTA DE STOCK MINIMO
            // ---------------------------------------------
            //
            // Descontar las unidades entregadas.
            // El backend verifica el stock resultante.
            //
            // Si el medicamento queda en el mínimo
            // o por debajo, genera la alerta.
            //
            // ---------------------------------------------

            inventario.descontar(
                    entrada.getValue()
            );

            if (inventario.alcanzoStockMinimo()) {

                alertasStockMinimo.add(
                        "ALERTA: El medicamento "
                                + inventario.getMedicamento().getNombre()
                                + " ha alcanzado el nivel de stock mínimo ("
                                + inventario.getCantidadDisponible()
                                + " unidades restantes). "
                                + "Se recomienda generar orden de reabastecimiento."
                );
            }

        }

        // Guardar todas las actualizaciones
        // y verificar restricciones de PostgreSQL.

        inventarioRepository.saveAllAndFlush(
                inventarios.values()
        );

        // -------------------------------------------------
        // 16. AUDITORÍA
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "despacho",
                        String.valueOf(despacho.getId()),
                        AccionAuditoria.CREAR,
                        farmaceutico.getId(),
                        farmaceutico.getNombreUsuario(),
                        null,
                        Map.of(
                                "recetaId",
                                receta.getId(),
                                "sucursalId",
                                idSucursal,
                                "estado",
                                "CONFIRMADO",
                                "cantidadMedicamentos",
                                lineas.size(),
                                "montoTotal",
                                montoTotal.toPlainString(),
                                "sustituciones",
                                sustituciones,
                                "claveOperacion",
                                solicitud.getClaveOperacion().toString()
                        ),
                        null
                )
        );

        // -------------------------------------------------
// 17. NOTIFICACIÓN FA02
// -------------------------------------------------
//
// Solo se publica el evento cuando:
// - El despacho es nuevo.
// - Contiene sustituciones.
//
// El listener enviará la notificación
// después de confirmar la transacción.
//
// No afecta la auditoría ni el inventario.
// -------------------------------------------------

        if (sustituciones > 0) {

            eventPublisher.publishEvent(
                    new DespachoConfirmadoFarmaciaEvent(
                            despacho.getId()
                    )
            );
        }

// -------------------------------------------------
// 18. RESULTADO
// -------------------------------------------------

        return new ResultadoDespacho(
                despacho.getId(),
                montoTotal,
                lineas.size(),
                false,
                sustituciones,
                List.copyOf(alertasStockMinimo)
        );
    }

    // =====================================================
    // RECUPERAR UNA OPERACIÓN YA REGISTRADA
    // =====================================================
    //
    // Solo devolvemos una operación anterior cuando:
    //
    // - Pertenece al farmacéutico autenticado.
    // - Pertenece a su sucursal.
    // - Corresponde a la misma receta.
    // - Su contenido coincide con la solicitud.
    //
    // Nunca se vuelve a descontar inventario.
    //
    // =====================================================

    private ResultadoDespacho recuperarOperacionAnterior(
            Despacho anterior,
            SolicitudDespachoFarmaciaDTO solicitud,
            Usuario farmaceutico,
            Integer idSucursal
    ) {

        if (!Objects.equals(
                anterior.getFarmaceutico().getId(),
                farmaceutico.getId()
        )
                || !Objects.equals(
                anterior.getSucursal().getId(),
                idSucursal
        )
                || !Objects.equals(
                anterior.getRecetaMedica().getId(),
                solicitud.getIdReceta()
        )
                || !anterior.estaConfirmado()) {

            throw new IllegalStateException(
                    "La clave de operación ya fue utilizada "
                            + "para otro despacho."
            );
        }

        String observacionesActuales =
                normalizarTexto(
                        solicitud.getObservaciones()
                );

        if (!Objects.equals(
                anterior.getObservaciones(),
                observacionesActuales
        )) {

            throw new IllegalStateException(
                    "La clave de operación corresponde "
                            + "a una solicitud diferente."
            );
        }

        List<DetalleDespacho> detallesAnteriores =
                detalleDespachoRepository
                        .findByDespacho_IdAndDespacho_Sucursal_IdOrderByIdAsc(
                                anterior.getId(),
                                idSucursal
                        );

        if (detallesAnteriores.size()
                != solicitud.getMedicamentos().size()) {

            throw new IllegalStateException(
                    "La operación anterior contiene "
                            + "medicamentos diferentes."
            );
        }

        Map<Integer, DetalleDespacho> anterioresPorDetalle =
                new HashMap<>();

        for (DetalleDespacho detalle : detallesAnteriores) {

            anterioresPorDetalle.put(
                    detalle.getDetalleRecetaMedica().getId(),
                    detalle
            );
        }

        int sustituciones = 0;

        for (SolicitudDespachoFarmaciaDTO.LineaDespachoDTO linea
                : solicitud.getMedicamentos()) {

            DetalleDespacho registrado =
                    anterioresPorDetalle.get(
                            linea.getIdDetalleReceta()
                    );

            if (registrado == null
                    || !Objects.equals(
                    registrado.getMedicamentoEntregado().getId(),
                    linea.getIdMedicamentoEntregado()
            )
                    || !Objects.equals(
                    registrado.getCantidadEntregada(),
                    linea.getCantidadEntregada()
            )
                    || !Objects.equals(
                    registrado.getRazonSustitucion(),
                    normalizarTexto(linea.getRazonSustitucion())
            )) {

                throw new IllegalStateException(
                        "La clave de operación ya existe "
                                + "con información distinta."
                );
            }

            if (!Objects.equals(
                    registrado.getDetalleRecetaMedica()
                            .getMedicamento().getId(),
                    registrado.getMedicamentoEntregado().getId()
            )) {

                sustituciones++;
            }
        }

        return new ResultadoDespacho(
                anterior.getId(),
                anterior.getMontoTotal(),
                detallesAnteriores.size(),
                true,
                sustituciones,
                List.of()
        );
    }

    // =====================================================
    // NORMALIZAR TEXTO
    // =====================================================

    private String normalizarTexto(
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
                    "El medicamento tiene un precio inválido."
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
                    "El monto del despacho supera "
                            + "el límite permitido."
            );
        }
    }
}
