package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.List;


/**
 * =========================================================
 * SERVICIO: COMPROBANTE DE LABORATORIO
 * =========================================================
 *
 * CU-09 / CU-16.
 *
 * Responsabilidades:
 *
 * - Recuperar un pago APROBADO de laboratorio.
 * - Validar al Cajero autenticado.
 * - Validar que el pago pertenezca a su sucursal.
 * - Recuperar la OrdenLaboratorio.
 * - Recuperar los exámenes cobrados.
 * - Construir los datos del comprobante.
 *
 * El comprobante se reconstruye desde:
 *
 * Pago
 *   -> OrdenLaboratorio
 *      -> Cita
 *         -> Paciente
 *         -> Sucursal
 *      -> Médico
 *   -> FormaPago
 *   -> Cajero
 *
 * Y:
 *
 * DetalleOrdenLaboratorio
 *
 * =========================================================
 */
@Service
public class ComprobanteLaboratorioService {


    // =====================================================
    // ROL
    // =====================================================

    private static final String ROL_CAJERO =
            "Cajero";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final PagoRepository
            pagoRepository;

    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final ZoneId
            zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ComprobanteLaboratorioService(

            PagoRepository pagoRepository,

            DetalleOrdenLaboratorioRepository
                    detalleOrdenLaboratorioRepository,

            UsuarioActualService
                    usuarioActualService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.pagoRepository =
                pagoRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // OBTENER COMPROBANTE
    // =====================================================

    @Transactional(
            readOnly = true
    )
    public ComprobanteLaboratorio obtener(
            String numeroTransaccion
    ) {

        // =================================================
        // NÚMERO DE TRANSACCIÓN
        // =================================================

        String numero =
                normalizar(
                        numeroTransaccion
                );


        if (numero.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el número de transacción."
            );
        }


        // =================================================
        // CAJERO AUTENTICADO
        // =================================================

        Usuario cajeroActual =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (!esCajeroValido(
                cajeroActual
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para consultar comprobantes de laboratorio."
            );
        }


        Integer idSucursalCajero =
                cajeroActual
                        .getSucursal()
                        .getId();


        // =================================================
        // PAGO APROBADO DE LABORATORIO
        // =================================================

        Pago pago =
                pagoRepository
                        .buscarComprobanteLaboratorio(
                                numero
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró un comprobante "
                                                        + "de laboratorio aprobado "
                                                        + "para la transacción indicada."
                                        )
                        );


        // =================================================
        // VALIDAR ORDEN
        // =================================================

        OrdenLaboratorio orden =
                pago.getOrdenLaboratorio();


        if (orden == null
                || orden.getId() == null) {

            throw new IllegalStateException(
                    "El pago no posee una orden "
                            + "de laboratorio válida."
            );
        }


        if (orden.getCita() == null
                || orden.getCita()
                .getSucursal() == null
                || orden.getCita()
                .getSucursal()
                .getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar la sucursal "
                            + "de la orden de laboratorio."
            );
        }


        // =================================================
        // SEGURIDAD POR SUCURSAL
        // =================================================

        if (!idSucursalCajero.equals(
                orden.getCita()
                        .getSucursal()
                        .getId()
        )) {

            throw new IllegalStateException(
                    "El comprobante de laboratorio no pertenece "
                            + "a la sucursal del Cajero autenticado."
            );
        }


        // =================================================
        // VALIDAR PACIENTE
        // =================================================

        if (orden.getCita()
                .getPaciente() == null) {

            throw new IllegalStateException(
                    "La orden no posee un paciente válido."
            );
        }


        // =================================================
        // VALIDAR MÉDICO
        // =================================================

        if (orden.getMedico() == null) {

            throw new IllegalStateException(
                    "La orden no posee un Médico solicitante válido."
            );
        }


        // =================================================
        // EXÁMENES
        // =================================================

        List<DetalleOrdenLaboratorio> detalles =
                detalleOrdenLaboratorioRepository
                        .findByOrdenLaboratorio_IdOrderByNombreExamenAsc(
                                orden.getId()
                        );


        if (detalles == null
                || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La orden de laboratorio no contiene "
                            + "exámenes registrados."
            );
        }


        List<ExamenComprobanteLaboratorio> examenes =
                detalles
                        .stream()

                        .map(
                                detalle ->
                                        new ExamenComprobanteLaboratorio(

                                                detalle.getNombreExamen(),

                                                normalizarMonto(
                                                        detalle.getPrecioExamen()
                                                )
                                        )
                        )

                        .toList();


        // =================================================
        // FORMA DE PAGO
        // =================================================

        String formaPago =
                construirFormaPagoVisible(
                        pago
                );


        // =================================================
        // REFERENCIA DE TARJETA
        // =================================================

        String referenciaTarjeta =
                construirReferenciaTarjeta(
                        pago
                );


        // =================================================
        // CAJERO QUE REALIZÓ EL COBRO
        // =================================================

        String nombreCajero =
                pago.getCajero() != null
                        ? pago.getCajero()
                        .getNombreCompleto()
                        : null;


        // =================================================
        // DETALLE DEL SERVICIO
        // =================================================

        String detalleServicio =
                "Exámenes de laboratorio - Orden "
                        + orden.getNumeroOrden();


        // =================================================
        // RESULTADO
        // =================================================

        return new ComprobanteLaboratorio(

                pago.getNumeroTransaccion(),

                orden.getId(),

                orden.getNumeroOrden(),

                orden.getCita()
                        .getPaciente()
                        .getNombreCompleto(),

                orden.getMedico()
                        .getNombreCompleto(),

                orden.getCita()
                        .getSucursal()
                        .getNombre(),

                normalizarMonto(
                        pago.getMonto()
                ),

                formaPago,

                normalizarMonto(
                        pago.getMontoRecibido()
                ),

                normalizarMonto(
                        pago.getCambio()
                ),

                referenciaTarjeta,

                convertirAZonaHoraria(
                        pago.getFechaHoraPago()
                ),

                detalleServicio,

                nombreCajero,

                List.copyOf(
                        examenes
                )
        );
    }


    // =====================================================
    // FORMA DE PAGO VISIBLE
    // =====================================================

    private String construirFormaPagoVisible(
            Pago pago
    ) {

        if (pago.getTipoTarjeta() != null
                && !pago.getTipoTarjeta()
                .isBlank()) {

            return switch (
                    pago.getTipoTarjeta()
                            .trim()
                            .toUpperCase()
                    ) {

                case "VISA" ->
                        "Visa";

                case "MASTERCARD" ->
                        "Mastercard";

                case "DEBITO" ->
                        "Débito";

                default ->
                        pago.getFormaPago() != null
                                && pago.getFormaPago()
                                .getNombre() != null

                                ? pago.getFormaPago()
                                .getNombre()

                                : "Tarjeta";
            };
        }


        if (pago.getFormaPago() != null
                && pago.getFormaPago()
                .getNombre() != null) {

            return pago.getFormaPago()
                    .getNombre();
        }


        return "No especificada";
    }


    // =====================================================
    // REFERENCIA DE TARJETA
    // =====================================================

    private String construirReferenciaTarjeta(
            Pago pago
    ) {

        if (pago.getUltimos4Tarjeta() == null
                || pago.getUltimos4Tarjeta()
                .isBlank()) {

            return null;
        }


        /*
         * Únicamente se muestran los últimos
         * cuatro dígitos persistidos.
         *
         * Nunca existe acceso al número completo.
         */
        return "**** "
                + pago.getUltimos4Tarjeta();
    }


    // =====================================================
    // CAJERO VÁLIDO
    // =====================================================

    private boolean esCajeroValido(
            Usuario usuario
    ) {

        return usuario != null

                && usuario.getId() != null

                && Boolean.TRUE.equals(
                usuario.getActivo()
        )

                && usuario.getRol() != null

                && usuario.getRol()
                .getNombre() != null

                && ROL_CAJERO
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                )

                && usuario.getSucursal() != null

                && usuario.getSucursal()
                .getId() != null;
    }


    // =====================================================
    // NORMALIZAR MONTO
    // =====================================================

    private BigDecimal normalizarMonto(
            BigDecimal monto
    ) {

        if (monto == null) {

            return null;
        }


        return monto.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    // =====================================================
    // ZONA HORARIA
    // =====================================================

    private OffsetDateTime convertirAZonaHoraria(
            OffsetDateTime fechaHora
    ) {

        if (fechaHora == null) {

            return null;
        }


        return fechaHora
                .atZoneSameInstant(
                        zonaHoraria
                )
                .toOffsetDateTime();
    }


    // =====================================================
    // NORMALIZAR TEXTO
    // =====================================================

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // COMPROBANTE
    // =====================================================

    public record ComprobanteLaboratorio(

            String numeroTransaccion,

            Integer idOrdenLaboratorio,

            String numeroOrden,

            String nombrePaciente,

            String nombreMedico,

            String sucursal,

            BigDecimal montoPagado,

            String formaPago,

            BigDecimal montoRecibido,

            BigDecimal cambio,

            String referenciaTarjeta,

            OffsetDateTime fechaHoraTransaccion,

            String detalleServicio,

            String cajero,

            List<ExamenComprobanteLaboratorio> examenes

    ) {


        // =================================================
        // ¿EFECTIVO?
        // =================================================

        public boolean esEfectivo() {

            return montoRecibido != null;
        }


        // =================================================
        // ¿TARJETA?
        // =================================================

        public boolean esTarjeta() {

            return referenciaTarjeta != null;
        }
    }


    // =====================================================
    // EXAMEN DEL COMPROBANTE
    // =====================================================

    public record ExamenComprobanteLaboratorio(

            String nombreExamen,

            BigDecimal precioExamen

    ) {
    }
}