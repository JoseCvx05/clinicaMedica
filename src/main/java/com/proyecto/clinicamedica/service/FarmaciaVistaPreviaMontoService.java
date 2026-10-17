
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.SolicitudDespachoFarmaciaDTO;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;
import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * =========================================================
 * CU-11 - VISTA PREVIA DEL MONTO
 * =========================================================
 *
 * Calcula el total antes de confirmar un despacho.
 *
 * SEGURIDAD:
 *
 * - Valida el farmacéutico y su sucursal.
 * - Verifica receta, vigencia y firma digital.
 * - Valida cantidades pendientes.
 * - Verifica sustituciones y justificaciones.
 * - Respeta las restricciones de controlados.
 * - Consulta inventario de la sucursal.
 * - Calcula precios exclusivamente en el backend.
 *
 * NO registra despachos.
 * NO modifica inventario.
 * NO genera correos.
 *
 * La confirmación volverá a validar todos los datos.
 *
 * =========================================================
 */
@Service
public class FarmaciaVistaPreviaMontoService {

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

    private final FarmaciaCalculoMontoService
            calculoMontoService;

    private final RecetaMedicaRepository
            recetaRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;

    private final InventarioFarmaciaRepository
            inventarioRepository;

    @PersistenceContext
    private EntityManager entityManager;


    public FarmaciaVistaPreviaMontoService(
            FarmaciaSolicitudDespachoValidador solicitudValidador,
            FarmaciaSeguridadService seguridadService,
            FarmaciaRecetaValidacionService recetaValidacionService,
            FarmaciaFirmaRecetaValidacionService firmaRecetaService,
            FarmaciaMedicamentoControladoValidacionService controladosService,
            FarmaciaCalculoMontoService calculoMontoService,
            RecetaMedicaRepository recetaRepository,
            MedicamentoRepository medicamentoRepository,
            DetalleDespachoRepository detalleDespachoRepository,
            InventarioFarmaciaRepository inventarioRepository
    ) {

        this.solicitudValidador = solicitudValidador;
        this.seguridadService = seguridadService;
        this.recetaValidacionService = recetaValidacionService;
        this.firmaRecetaService = firmaRecetaService;
        this.controladosService = controladosService;
        this.calculoMontoService = calculoMontoService;
        this.recetaRepository = recetaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.detalleDespachoRepository = detalleDespachoRepository;
        this.inventarioRepository = inventarioRepository;
    }


    // =====================================================
    // CALCULAR VISTA PREVIA
    // =====================================================

    @Transactional(readOnly = true)
    public BigDecimal calcularVistaPrevia(
            SolicitudDespachoFarmaciaDTO solicitud
    ) {

        // 1. Validar estructura del formulario.

        solicitudValidador.validar(solicitud);


        // 2. Obtener farmacéutico y sucursal del backend.

        Usuario farmaceutico =
                seguridadService.obtenerFarmaceuticoAutorizado();

        Integer idSucursal =
                farmaceutico.getSucursal().getId();


        // 3. Consultar y validar receta.

        RecetaMedica receta =
                recetaRepository.findById(
                        solicitud.getIdReceta()
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "La receta médica no existe."
                        )
                );

        seguridadService.validarAccesoReceta(receta);

        recetaValidacionService.validarParaDespacho(receta);


        // 4. Recuperar detalles reales de PostgreSQL.

        List<DetalleRecetaMedica> detalles =
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
                        .setParameter("idReceta", receta.getId())
                        .getResultList();


        // 5. Comprobar firma digital.

        firmaRecetaService.validar(
                receta,
                detalles
        );


        Map<Integer, DetalleRecetaMedica> detallesPorId =
                new HashMap<>();

        for (DetalleRecetaMedica detalle : detalles) {
            detallesPorId.put(detalle.getId(), detalle);
        }


        Map<Integer, Integer> cantidadesPorMedicamento =
                new TreeMap<>();

        BigDecimal montoTotal =
                new BigDecimal("0.00");


        // =================================================
        // VALIDAR Y CALCULAR CADA MEDICAMENTO
        // =================================================

        for (SolicitudDespachoFarmaciaDTO.LineaDespachoDTO linea
                : solicitud.getMedicamentos()) {

            DetalleRecetaMedica detalle =
                    detallesPorId.get(
                            linea.getIdDetalleReceta()
                    );

            if (detalle == null) {

                throw new IllegalStateException(
                        "El medicamento no pertenece a esta receta."
                );
            }


            // ---------------------------------------------
            // CU-14 - PROTEGER MEDICAMENTO ORIGINAL INACTIVO
            // ---------------------------------------------
            //
            // Se valida el medicamento original de la
            // receta directamente desde el backend.
            //
            // Impide calcular el monto de nuevos despachos
            // cuando el medicamento original está inactivo,
            // incluso ante solicitudes manipuladas.
            //
            // No modifica recetas ni inventarios.
            // ---------------------------------------------

            Medicamento medicamentoOriginal =
                    detalle.getMedicamento();

            if (medicamentoOriginal == null
                    || !medicamentoOriginal.estaActivo()) {

                throw new IllegalStateException(
                        "No se puede calcular el despacho "
                                + "porque el medicamento original "
                                + "se encuentra inactivo."
                );
            }


            Medicamento entregado =
                    medicamentoRepository.findById(
                            linea.getIdMedicamentoEntregado()
                    ).orElseThrow(
                            () -> new IllegalStateException(
                                    "El medicamento seleccionado no existe."
                            )
                    );

            if (!entregado.estaActivo()) {

                throw new IllegalStateException(
                        "No se pueden entregar medicamentos inactivos."
                );
            }

            entregado.validar();


            // ---------------------------------------------
            // CANTIDADES PENDIENTES
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
                    cantidadAnterior == null ? 0L : cantidadAnterior;

            if (cantidadEntregadaAnteriormente < 0
                    || cantidadEntregadaAnteriormente
                    > cantidadPrescrita) {

                throw new IllegalStateException(
                        "La receta presenta cantidades inconsistentes."
                );
            }

            long cantidadPendiente =
                    cantidadPrescrita
                            - cantidadEntregadaAnteriormente;

            if (linea.getCantidadEntregada() > cantidadPendiente) {

                throw new IllegalStateException(
                        "La cantidad solicitada supera "
                                + "las unidades pendientes."
                );
            }


            // ---------------------------------------------
            // MEDICAMENTOS CONTROLADOS
            // ---------------------------------------------

            controladosService.validarParaDespacho(
                    detalle,
                    entregado,
                    linea.getCantidadEntregada(),
                    cantidadEntregadaAnteriormente
            );


            // ---------------------------------------------
            // SUSTITUCIÓN FA02
            // ---------------------------------------------

            boolean esSustitucion =
                    !Objects.equals(
                            detalle.getMedicamento().getId(),
                            entregado.getId()
                    );

            if (esSustitucion
                    != linea.isSustitucionSolicitada()) {

                throw new IllegalStateException(
                        "La selección de sustitución "
                                + "no coincide con el medicamento entregado."
                );
            }

            String razon =
                    linea.getRazonSustitucion() == null
                            ? ""
                            : linea.getRazonSustitucion().trim();

            if (esSustitucion && razon.isBlank()) {

                throw new IllegalStateException(
                        "Debe ingresar la razón de sustitución."
                );
            }

            if (!esSustitucion && !razon.isBlank()) {

                throw new IllegalStateException(
                        "No debe ingresar una justificación "
                                + "para el medicamento original."
                );
            }


            // ---------------------------------------------
            // CALCULAR TOTAL CON EL SERVICIO COMPARTIDO
            // ---------------------------------------------

            BigDecimal subtotal =
                    calculoMontoService.calcularSubtotal(
                            detalle,
                            entregado,
                            linea.getCantidadEntregada()
                    );

            montoTotal =
                    calculoMontoService.sumarMonto(
                            montoTotal,
                            subtotal
                    );


            // ---------------------------------------------
            // ACUMULAR EXISTENCIAS NECESARIAS
            // ---------------------------------------------

            Integer idMedicamento =
                    entregado.getId();

            int acumulado;

            try {

                acumulado = Math.addExact(
                        cantidadesPorMedicamento.getOrDefault(
                                idMedicamento,
                                0
                        ),
                        linea.getCantidadEntregada()
                );

            } catch (ArithmeticException ex) {

                throw new IllegalStateException(
                        "La cantidad solicitada supera "
                                + "el límite permitido."
                );
            }

            cantidadesPorMedicamento.put(
                    idMedicamento,
                    acumulado
            );
        }


        // =================================================
        // VERIFICAR INVENTARIO SIN DESCONTAR
        // =================================================

        for (Map.Entry<Integer, Integer> entrada
                : cantidadesPorMedicamento.entrySet()) {

            Integer idMedicamento =
                    entrada.getKey();

            InventarioFarmacia inventario =
                    inventarioRepository
                            .findByMedicamento_IdAndSucursal_Id(
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
                    entrada.getValue()
            )) {

                throw new IllegalStateException(
                        "Stock insuficiente para el medicamento #"
                                + idMedicamento
                                + ". Disponible: "
                                + inventario.getCantidadDisponible()
                );
            }
        }


        // Solo devolver información.
        // No guardar cambios.

        return montoTotal;
    }
}
