package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.caja.CobroLaboratorioDTO;

import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.entity.FormaPago;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.caja.ResultadoProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.SolicitudProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.FormaPagoRepository;
import com.proyecto.clinicamedica.repository.OrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;

import com.proyecto.clinicamedica.service.caja.ProcesadorCobroCaja;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.List;
import java.util.UUID;


/**
 * =========================================================
 * SERVICIO: COBRO DE LABORATORIO
 * =========================================================
 *
 * CU-09 / CU-16.
 *
 * Responsabilidades:
 *
 * - Validar al Cajero autenticado.
 * - Validar la sucursal del Cajero.
 * - Bloquear la OrdenLaboratorio durante el cobro.
 * - Impedir cobro de órdenes externas.
 * - Exigir estado PENDIENTE.
 * - Evitar doble cobro.
 * - Calcular el monto desde los detalles de la orden.
 * - Reutilizar las estrategias de Caja:
 *
 *      EFECTIVO
 *      VISA
 *      MASTERCARD
 *      DEBITO
 *
 * - Registrar Pago asociado a:
 *
 *      Cita
 *      OrdenLaboratorio
 *
 * - Cambiar:
 *
 *      PENDIENTE -> EN_PROCESO
 *
 * cuando el pago sea aprobado.
 *
 * IMPORTANTE:
 *
 * Este servicio NO cambia el estado clínico de la Cita.
 *
 * =========================================================
 */
@Service
public class CobroLaboratorioService {


    // =====================================================
    // ROL
    // =====================================================

    private static final String ROL_CAJERO =
            "Cajero";


    // =====================================================
    // MENSAJES
    // =====================================================

    private static final String MENSAJE_METODO_NO_DISPONIBLE =
            "El método de pago seleccionado no está disponible. "
                    + "Los métodos aceptados son: efectivo (Quetzales), "
                    + "tarjeta de crédito (Visa/Mastercard) "
                    + "o tarjeta de débito.";


    private static final String MENSAJE_TARJETA_RECHAZADA =
            "La transacción con tarjeta fue rechazada por el banco. "
                    + "Solicite al paciente otro método de pago.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final OrdenLaboratorioRepository
            ordenLaboratorioRepository;

    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;

    private final PagoRepository
            pagoRepository;

    private final FormaPagoRepository
            formaPagoRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final List<ProcesadorCobroCaja>
            procesadores;

    private final ZoneId
            zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CobroLaboratorioService(

            OrdenLaboratorioRepository
                    ordenLaboratorioRepository,

            DetalleOrdenLaboratorioRepository
                    detalleOrdenLaboratorioRepository,

            PagoRepository
                    pagoRepository,

            FormaPagoRepository
                    formaPagoRepository,

            UsuarioActualService
                    usuarioActualService,

            List<ProcesadorCobroCaja>
                    procesadores,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.pagoRepository =
                pagoRepository;

        this.formaPagoRepository =
                formaPagoRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.procesadores =
                procesadores;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }

    // =====================================================
// CONTEXTO PARA PANTALLA DE COBRO
// =====================================================

    public record ContextoCobroLaboratorio(

            Integer idOrdenLaboratorio,

            String numeroOrden,

            String nombrePaciente,

            String nombreMedico,

            String nombreSucursal,

            String observaciones,

            BigDecimal montoTotal,

            List<ExamenCobroLaboratorio> examenes

    ) {
    }


// =====================================================
// EXAMEN MOSTRADO EN CAJA
// =====================================================

    public record ExamenCobroLaboratorio(

            String nombreExamen,

            BigDecimal precioExamen

    ) {
    }

    // =====================================================
// CU-09 / CU-16
// OBTENER ORDEN PARA COBRO
// =====================================================
//
// Se utiliza al abrir la pantalla de cobro.
//
// Valida:
//
// - Cajero autenticado.
// - Usuario activo.
// - Misma sucursal.
// - Orden existente.
// - Orden interna.
// - Estado PENDIENTE.
// - Sin pago aprobado previo.
// - Exámenes válidos.
// - Monto calculado en backend.
//
// =====================================================

    @Transactional(
            readOnly = true
    )
    public ContextoCobroLaboratorio obtenerContextoCobro(
            Integer idOrdenLaboratorio
    ) {

        // =================================================
        // ID
        // =================================================

        if (idOrdenLaboratorio == null
                || idOrdenLaboratorio <= 0) {

            throw new IllegalArgumentException(
                    "La orden de laboratorio indicada no es válida."
            );
        }


        // =================================================
        // CAJERO
        // =================================================

        Usuario cajero =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (!esCajeroValido(
                cajero
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para realizar cobros de laboratorio."
            );
        }


        Integer idSucursal =
                obtenerIdSucursal(
                        cajero
                );


        // =================================================
        // ORDEN SEGURA POR SUCURSAL
        // =================================================

        OrdenLaboratorio orden =
                ordenLaboratorioRepository
                        .findByIdAndCita_Sucursal_Id(

                                idOrdenLaboratorio,

                                idSucursal
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "La orden de laboratorio no existe "
                                                        + "o pertenece a otra sucursal."
                                        )
                        );


        // =================================================
        // ORDEN EXTERNA
        // =================================================

        if (orden.isOrdenExterna()) {

            throw new IllegalStateException(
                    "La orden seleccionada es externa y no puede "
                            + "ser cobrada como servicio de laboratorio interno."
            );
        }


        // =================================================
        // ESTADO
        // =================================================

        if (orden.getEstado()
                != OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .PENDIENTE) {

            throw new IllegalStateException(
                    "La orden de laboratorio ya no se encuentra "
                            + "pendiente de pago."
            );
        }


        // =================================================
        // PAGO DUPLICADO
        // =================================================

        if (pagoRepository
                .existsByOrdenLaboratorio_IdAndEstado(

                        orden.getId(),

                        Pago.ESTADO_APROBADO
                )) {

            throw new IllegalStateException(
                    "La orden de laboratorio ya posee un pago aprobado "
                            + "y no puede cobrarse nuevamente."
            );
        }


        // =================================================
        // DATOS PRINCIPALES
        // =================================================

        if (orden.getCita() == null
                || orden.getCita().getPaciente() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee "
                            + "un paciente válido."
            );
        }


        if (orden.getMedico() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee "
                            + "un Médico solicitante válido."
            );
        }


        if (orden.getCita().getSucursal() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee "
                            + "una sucursal válida."
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


        List<ExamenCobroLaboratorio> examenes =
                detalles
                        .stream()

                        .map(
                                detalle ->
                                        new ExamenCobroLaboratorio(

                                                detalle.getNombreExamen(),

                                                detalle.getPrecioExamen()
                                        )
                        )

                        .toList();


        // =================================================
        // MONTO REAL
        // =================================================

        BigDecimal montoTotal =
                calcularMontoOrden(
                        orden
                );


        if (montoTotal == null
                || montoTotal.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalStateException(
                    "No fue posible determinar el monto "
                            + "de la orden de laboratorio."
            );
        }


        // =================================================
        // CONTEXTO
        // =================================================

        return new ContextoCobroLaboratorio(

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

                orden.getObservaciones(),

                montoTotal,

                List.copyOf(
                        examenes
                )
        );
    }


    // =====================================================
    // REGISTRAR PAGO DE LABORATORIO
    // =====================================================

    @Transactional
    public ResultadoCobroLaboratorio registrarPago(
            CobroLaboratorioDTO formulario
    ) {

        // =================================================
        // 1. VALIDACIÓN BÁSICA
        // =================================================

        if (formulario == null
                || formulario.getIdOrdenLaboratorio() == null
                || formulario.getIdOrdenLaboratorio() <= 0) {

            return ResultadoCobroLaboratorio.error(
                    "No se indicó una orden de laboratorio válida."
            );
        }


        if (formulario.getTipoCobro() == null) {

            return ResultadoCobroLaboratorio.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 2. IDEMPOTENCIA
        // =================================================

        UUID idempotencyKey =
                formulario.getIdempotencyKey();


        if (idempotencyKey == null) {

            idempotencyKey =
                    UUID.randomUUID();

            formulario.setIdempotencyKey(
                    idempotencyKey
            );
        }


        // =================================================
        // 3. ¿MISMA SOLICITUD YA PROCESADA?
        // =================================================

        Pago pagoExistente =
                pagoRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(
                                null
                        );


        if (pagoExistente != null) {

            return construirResultadoIdempotente(
                    pagoExistente
            );
        }


        // =================================================
        // 4. CAJERO AUTENTICADO
        // =================================================

        Usuario cajero =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (!esCajeroValido(
                cajero
        )) {

            return ResultadoCobroLaboratorio.error(
                    "El usuario autenticado no tiene permisos "
                            + "para realizar cobros de laboratorio."
            );
        }


        Integer idSucursal =
                obtenerIdSucursal(
                        cajero
                );


        // =================================================
        // 5. BLOQUEAR ORDEN
        // =================================================
        //
        // El lock evita que dos Cajeros cobren
        // simultáneamente la misma orden.
        //
        // También limita la operación a la sucursal
        // del Cajero autenticado.
        // =================================================

        OrdenLaboratorio orden =
                ordenLaboratorioRepository
                        .buscarPorIdYSucursalConBloqueo(

                                formulario
                                        .getIdOrdenLaboratorio(),

                                idSucursal
                        )
                        .orElse(
                                null
                        );


        if (orden == null) {

            return ResultadoCobroLaboratorio.error(
                    "La orden de laboratorio no existe "
                            + "o pertenece a otra sucursal."
            );
        }


        // =================================================
        // 6. REVISAR IDEMPOTENCIA DESPUÉS DEL LOCK
        // =================================================

        pagoExistente =
                pagoRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(
                                null
                        );


        if (pagoExistente != null) {

            return construirResultadoIdempotente(
                    pagoExistente
            );
        }


        // =================================================
        // 7. ORDEN EXTERNA
        // =================================================

        if (orden.isOrdenExterna()) {

            return ResultadoCobroLaboratorio.error(
                    "La orden seleccionada es externa y no puede "
                            + "ser cobrada como servicio de laboratorio interno."
            );
        }


        // =================================================
        // 8. ESTADO DE LA ORDEN
        // =================================================

        if (orden.getEstado()
                != OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .PENDIENTE) {

            return ResultadoCobroLaboratorio.error(
                    "La orden de laboratorio ya no se encuentra "
                            + "pendiente de pago."
            );
        }


        // =================================================
        // 9. PROTECCIÓN CONTRA DOBLE COBRO
        // =================================================

        boolean yaTienePagoAprobado =
                pagoRepository
                        .existsByOrdenLaboratorio_IdAndEstado(

                                orden.getId(),

                                Pago.ESTADO_APROBADO
                        );


        if (yaTienePagoAprobado) {

            return ResultadoCobroLaboratorio.error(
                    "La orden de laboratorio ya posee un pago aprobado "
                            + "y no puede cobrarse nuevamente."
            );
        }


        // =================================================
        // 10. CALCULAR MONTO REAL
        // =================================================

        BigDecimal montoTotal =
                calcularMontoOrden(
                        orden
                );


        if (montoTotal == null
                || montoTotal.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return ResultadoCobroLaboratorio.error(
                    "No fue posible determinar el monto "
                            + "de la orden de laboratorio."
            );
        }


        // =================================================
        // 11. FORMA DE PAGO
        // =================================================

        TipoCobroCaja tipoCobro =
                formulario.getTipoCobro();


        FormaPago formaPago =
                formaPagoRepository
                        .findByNombreIgnoreCaseAndActivoTrue(

                                tipoCobro
                                        .getNombreFormaPago()
                        )
                        .orElse(
                                null
                        );


        if (formaPago == null) {

            return ResultadoCobroLaboratorio.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 12. RESOLVER STRATEGY
        // =================================================

        ProcesadorCobroCaja procesador =
                resolverProcesador(
                        tipoCobro
                );


        if (procesador == null) {

            return ResultadoCobroLaboratorio.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 13. CONSTRUIR SOLICITUD
        // =================================================

        SolicitudProcesamientoCobro solicitud =
                new SolicitudProcesamientoCobro(

                        tipoCobro,

                        montoTotal,

                        formulario.getMontoRecibido(),

                        formulario.getUltimos4Tarjeta()
                );


        // =================================================
        // 14. PROCESAR COBRO
        // =================================================

        ResultadoProcesamientoCobro procesamiento =
                procesador.procesar(
                        solicitud
                );


        // =================================================
        // 15. TRANSACCIÓN RECHAZADA
        // =================================================

        if (!procesamiento.aprobado()) {

            /*
             * Igual que en CU-06:
             *
             * únicamente persistimos el intento si
             * realmente ocurrió una transacción de tarjeta
             * que fue rechazada.
             *
             * Efectivo insuficiente u otros errores de
             * formulario no representan un Pago procesado.
             */
            if ("TARJETA_RECHAZADA"
                    .equalsIgnoreCase(
                            procesamiento.codigoResultado()
                    )) {

                guardarIntentoRechazado(

                        orden,

                        formaPago,

                        cajero,

                        montoTotal,

                        idempotencyKey,

                        procesamiento
                );
            }


            return ResultadoCobroLaboratorio.rechazado(

                    procesamiento.mensaje(),

                    procesamiento.codigoResultado()
            );
        }


        // =================================================
        // 16. FECHA / HORA
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // 17. CREAR PAGO APROBADO
        // =================================================

        Pago pago =
                new Pago();


        /*
         * Pago conserva la Cita original porque la columna
         * id_cita continúa siendo obligatoria.
         */
        pago.setCita(
                orden.getCita()
        );


        /*
         * Diferencia este pago del pago de consulta.
         */
        pago.setOrdenLaboratorio(
                orden
        );


        pago.setFormaPago(
                formaPago
        );


        pago.setIdempotencyKey(
                idempotencyKey
        );


        pago.setNumeroTransaccion(
                generarNumeroTransaccion()
        );


        pago.setMonto(
                montoTotal
        );


        pago.setMontoRecibido(
                procesamiento.montoRecibido()
        );


        pago.setCambio(
                procesamiento.cambio()
        );


        pago.setTipoTarjeta(
                procesamiento.tipoTarjeta()
        );


        pago.setUltimos4Tarjeta(
                procesamiento.ultimos4Tarjeta()
        );


        pago.setCanalPago(
                Pago.CANAL_CAJA
        );


        pago.setCajero(
                cajero
        );


        pago.setEstado(
                Pago.ESTADO_APROBADO
        );


        pago.setCodigoResultado(
                procesamiento.codigoResultado()
        );


        pago.setFechaHoraPago(
                ahora
        );


        Pago pagoGuardado =
                pagoRepository
                        .saveAndFlush(
                                pago
                        );


        // =================================================
        // 18. CAMBIAR ORDEN A EN_PROCESO
        // =================================================
        //
        // CU-09:
        //
        // El pago NO modifica el estado clínico de Cita.
        //
        // Únicamente cambia:
        //
        // OrdenLaboratorio
        // PENDIENTE -> EN_PROCESO
        //
        // =================================================

        orden.setEstado(
                OrdenLaboratorio
                        .EstadoOrdenLaboratorio
                        .EN_PROCESO
        );


        ordenLaboratorioRepository
                .saveAndFlush(
                        orden
                );


        // =================================================
        // 19. RESULTADO
        // =================================================

        String mensajeExito =
                "¡Pago de laboratorio registrado exitosamente! "
                        + "Paciente: "
                        + orden.getCita()
                        .getPaciente()
                        .getNombreCompleto()
                        + ". La orden "
                        + orden.getNumeroOrden()
                        + " ha sido actualizada a estado En proceso.";


        return ResultadoCobroLaboratorio.exito(

                mensajeExito,

                construirDetallePago(
                        procesamiento
                ),

                pagoGuardado.getId(),

                pagoGuardado.getNumeroTransaccion(),

                orden.getId()
        );
    }


    // =====================================================
    // GUARDAR TARJETA RECHAZADA
    // =====================================================

    private void guardarIntentoRechazado(

            OrdenLaboratorio orden,

            FormaPago formaPago,

            Usuario cajero,

            BigDecimal montoTotal,

            UUID idempotencyKey,

            ResultadoProcesamientoCobro procesamiento
    ) {

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        Pago pago =
                new Pago();


        pago.setCita(
                orden.getCita()
        );


        pago.setOrdenLaboratorio(
                orden
        );


        pago.setFormaPago(
                formaPago
        );


        pago.setIdempotencyKey(
                idempotencyKey
        );


        /*
         * No existe comprobante para una operación
         * rechazada.
         */
        pago.setNumeroTransaccion(
                null
        );


        pago.setMonto(
                montoTotal
        );


        pago.setMontoRecibido(
                null
        );


        pago.setCambio(
                null
        );


        pago.setTipoTarjeta(
                procesamiento.tipoTarjeta()
        );


        pago.setUltimos4Tarjeta(
                procesamiento.ultimos4Tarjeta()
        );


        pago.setCanalPago(
                Pago.CANAL_CAJA
        );


        pago.setCajero(
                cajero
        );


        pago.setEstado(
                Pago.ESTADO_RECHAZADO
        );


        pago.setCodigoResultado(
                procesamiento.codigoResultado()
        );


        pago.setFechaHoraPago(
                ahora
        );


        pagoRepository
                .saveAndFlush(
                        pago
                );
    }


    // =====================================================
    // MONTO REAL DE LA ORDEN
    // =====================================================

    private BigDecimal calcularMontoOrden(
            OrdenLaboratorio orden
    ) {

        if (orden == null
                || orden.getId() == null) {

            return null;
        }


        List<DetalleOrdenLaboratorio> detalles =
                detalleOrdenLaboratorioRepository
                        .findByOrdenLaboratorio_IdOrderByNombreExamenAsc(
                                orden.getId()
                        );


        if (detalles == null
                || detalles.isEmpty()) {

            return null;
        }


        BigDecimal total =
                BigDecimal.ZERO;


        for (DetalleOrdenLaboratorio detalle :
                detalles) {

            if (detalle == null
                    || detalle.getPrecioExamen() == null
                    || detalle.getPrecioExamen()
                    .compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                throw new IllegalStateException(
                        "La orden contiene un examen "
                                + "con un monto inválido."
                );
            }


            total =
                    total.add(
                            detalle.getPrecioExamen()
                    );
        }


        return total.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    // =====================================================
    // RESULTADO IDEMPOTENTE
    // =====================================================

    private ResultadoCobroLaboratorio
    construirResultadoIdempotente(
            Pago pago
    ) {

        if (pago == null) {

            return ResultadoCobroLaboratorio.error(
                    "No fue posible recuperar la solicitud procesada."
            );
        }


        /*
         * Protección adicional:
         *
         * Este servicio solo debe reutilizar idempotencias
         * asociadas a órdenes de laboratorio.
         */
        if (pago.getOrdenLaboratorio() == null) {

            return ResultadoCobroLaboratorio.error(
                    "La solicitud de cobro ya fue utilizada "
                            + "para otra operación."
            );
        }


        // =================================================
        // YA APROBADO
        // =================================================

        if (Pago.ESTADO_APROBADO
                .equalsIgnoreCase(
                        pago.getEstado()
                )) {

            OrdenLaboratorio orden =
                    pago.getOrdenLaboratorio();


            String mensaje =
                    "El pago de laboratorio ya fue "
                            + "registrado exitosamente.";


            String detalle;


            if (pago.getMontoRecibido() != null
                    && pago.getCambio() != null) {

                detalle =
                        "Monto recibido: Q"
                                + formatearMonto(
                                pago.getMontoRecibido()
                        )
                                + ". Cambio a devolver: Q"
                                + formatearMonto(
                                pago.getCambio()
                        )
                                + ".";

            } else {

                detalle =
                        "Transacción con tarjeta aprobada.";
            }


            return ResultadoCobroLaboratorio.exito(

                    mensaje,

                    detalle,

                    pago.getId(),

                    pago.getNumeroTransaccion(),

                    orden.getId()
            );
        }


        // =================================================
        // YA RECHAZADO
        // =================================================

        if (Pago.ESTADO_RECHAZADO
                .equalsIgnoreCase(
                        pago.getEstado()
                )

                && "TARJETA_RECHAZADA"
                .equalsIgnoreCase(
                        pago.getCodigoResultado()
                )) {

            return ResultadoCobroLaboratorio.rechazado(

                    MENSAJE_TARJETA_RECHAZADA,

                    "TARJETA_RECHAZADA"
            );
        }


        return ResultadoCobroLaboratorio.error(
                "La solicitud de cobro ya fue procesada anteriormente."
        );
    }


    // =====================================================
    // RESOLVER PROCESADOR
    // =====================================================

    private ProcesadorCobroCaja resolverProcesador(
            TipoCobroCaja tipoCobro
    ) {

        for (ProcesadorCobroCaja procesador :
                procesadores) {

            if (procesador.soporta(
                    tipoCobro
            )) {

                return procesador;
            }
        }


        return null;
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
    // SUCURSAL DEL CAJERO
    // =====================================================

    private Integer obtenerIdSucursal(
            Usuario cajero
    ) {

        if (cajero == null
                || cajero.getSucursal() == null
                || cajero.getSucursal().getId() == null
                || cajero.getSucursal().getId() <= 0) {

            throw new IllegalStateException(
                    "No fue posible identificar la sucursal "
                            + "del Cajero autenticado."
            );
        }


        return cajero
                .getSucursal()
                .getId();
    }


    // =====================================================
    // DETALLE DEL PAGO
    // =====================================================

    private String construirDetallePago(
            ResultadoProcesamientoCobro procesamiento
    ) {

        if (procesamiento.montoRecibido() != null
                && procesamiento.cambio() != null) {

            return "Monto recibido: Q"
                    + formatearMonto(
                    procesamiento.montoRecibido()
            )
                    + ". Cambio a devolver: Q"
                    + formatearMonto(
                    procesamiento.cambio()
            )
                    + ".";
        }


        return "Transacción con tarjeta aprobada.";
    }


    // =====================================================
    // NÚMERO DE TRANSACCIÓN
    // =====================================================

    private String generarNumeroTransaccion() {

        return "LAB-CAJA-"
                + UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .toUpperCase();
    }


    // =====================================================
    // FORMATEAR MONTO
    // =====================================================

    private String formatearMonto(
            BigDecimal monto
    ) {

        if (monto == null) {

            return "0.00";
        }


        return monto
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }


    // =====================================================
    // RESULTADO DEL COBRO
    // =====================================================

    public record ResultadoCobroLaboratorio(

            boolean exitoso,

            boolean rechazado,

            String mensaje,

            String detalle,

            String codigoResultado,

            Integer idPago,

            String numeroTransaccion,

            Integer idOrdenLaboratorio

    ) {


        // =============================================
        // ÉXITO
        // =============================================

        public static ResultadoCobroLaboratorio exito(

                String mensaje,

                String detalle,

                Integer idPago,

                String numeroTransaccion,

                Integer idOrdenLaboratorio
        ) {

            return new ResultadoCobroLaboratorio(

                    true,

                    false,

                    mensaje,

                    detalle,

                    "APROBADO",

                    idPago,

                    numeroTransaccion,

                    idOrdenLaboratorio
            );
        }


        // =============================================
        // RECHAZADO
        // =============================================

        public static ResultadoCobroLaboratorio rechazado(

                String mensaje,

                String codigoResultado
        ) {

            return new ResultadoCobroLaboratorio(

                    false,

                    true,

                    mensaje,

                    null,

                    codigoResultado,

                    null,

                    null,

                    null
            );
        }


        // =============================================
        // ERROR
        // =============================================

        public static ResultadoCobroLaboratorio error(
                String mensaje
        ) {

            return new ResultadoCobroLaboratorio(

                    false,

                    false,

                    mensaje,

                    null,

                    null,

                    null,

                    null,

                    null
            );
        }
    }
}