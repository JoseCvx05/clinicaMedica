package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.caja.CobroCajaDTO;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.FormaPago;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.SucursalEspecialidad;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.caja.ResultadoProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.SolicitudProcesamientoCobro;
import com.proyecto.clinicamedica.model.caja.TipoCobroCaja;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;
import com.proyecto.clinicamedica.repository.FormaPagoRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;

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
 * SERVICIO: REGISTRO DE PAGO EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * Responsabilidades:
 *
 * - Validar al Cajero autenticado.
 * - Bloquear la cita durante el cobro.
 * - Verificar que siga Pendiente de pago.
 * - Evitar cobros duplicados.
 * - Resolver la forma de pago.
 * - Delegar el procesamiento al Strategy correspondiente.
 * - Registrar el Pago.
 * - Cambiar la Cita a Confirmada.
 * - Generar número de transacción único.
 *
 * NO:
 *
 * - Implementa las reglas específicas de efectivo.
 * - Implementa las reglas específicas de tarjeta.
 * - Construye HTML.
 * - Genera directamente el comprobante.
 *
 * =========================================================
 */
@Service
public class CobroCajaPagoService {


    // =====================================================
    // ESTADOS DE CITA
    // =====================================================

    private static final String ESTADO_PENDIENTE_PAGO =
            "Pendiente de pago";

    private static final String ESTADO_CONFIRMADA =
            "Confirmada";


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

    private final CitaRepository citaRepository;

    private final PagoRepository pagoRepository;

    private final FormaPagoRepository formaPagoRepository;

    private final EstadoCitaRepository estadoCitaRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final UsuarioActualService usuarioActualService;

    private final List<ProcesadorCobroCaja>
            procesadores;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CobroCajaPagoService(

            CitaRepository citaRepository,

            PagoRepository pagoRepository,

            FormaPagoRepository formaPagoRepository,

            EstadoCitaRepository estadoCitaRepository,

            SucursalEspecialidadRepository
                    sucursalEspecialidadRepository,

            UsuarioActualService usuarioActualService,

            List<ProcesadorCobroCaja> procesadores,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.pagoRepository =
                pagoRepository;

        this.formaPagoRepository =
                formaPagoRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

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
    // REGISTRAR PAGO
    // =====================================================

    @Transactional
    public ResultadoCobroCaja registrarPago(
            CobroCajaDTO formulario
    ) {

        // =================================================
        // 1. VALIDACIÓN BÁSICA
        // =================================================

        if (formulario == null
                || formulario.getIdCita() == null
                || formulario.getIdCita() <= 0) {

            return ResultadoCobroCaja.error(
                    "No se indicó una cita válida para realizar el cobro."
            );
        }


        if (formulario.getTipoCobro() == null) {

            return ResultadoCobroCaja.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 2. IDEMPOTENCY KEY
        // =================================================
        //
        // El controller posteriormente generará esta llave
        // al mostrar el formulario.
        //
        // Si por alguna razón no llega, generamos una
        // defensivamente para respetar NOT NULL.
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
        // 3. ¿YA SE PROCESÓ ESTA MISMA SOLICITUD?
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

            return ResultadoCobroCaja.error(
                    "El usuario autenticado no tiene permisos "
                            + "para realizar cobros en caja."
            );
        }


        // =================================================
        // 5. BLOQUEAR CITA
        // =================================================
        //
        // Dos Cajeros no pueden cobrar la misma cita
        // simultáneamente.
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaCobroCajaConBloqueo(
                                formulario.getIdCita()
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoCobroCaja.error(
                    "La cita seleccionada ya no está disponible para cobro."
            );
        }


        // =================================================
        // 6. REVISAR IDEMPOTENCIA NUEVAMENTE
        // =================================================
        //
        // Es necesario después de adquirir el lock.
        //
        // Request A y Request B pudieron entrar casi
        // simultáneamente.
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
        // 7. VALIDAR ESTADO DE LA CITA
        // =================================================

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null
                || !ESTADO_PENDIENTE_PAGO
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            return ResultadoCobroCaja.error(
                    "La cita ya no se encuentra pendiente de pago. "
                            + "Actualice la búsqueda e intente nuevamente."
            );
        }

        // =================================================
// 8. VALIDAR VIGENCIA DEL PAGO
// =================================================
//
// Una cita de Portal Web deja de ser cobrable
// inmediatamente al vencer sus 10 minutos.
//
// No dependemos únicamente del Scheduler, ya que
// este se ejecuta periódicamente.
// =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        if (pagoPortalExpirado(
                cita,
                ahora
        )) {

            return ResultadoCobroCaja.error(
                    "El tiempo disponible para realizar el pago "
                            + "de esta cita ha expirado."
            );
        }


        // =================================================
        // 9. PROTECCIÓN CONTRA DOBLE COBRO
        // =================================================

        boolean yaTienePagoAprobado =
                pagoRepository
                        .existsByCita_IdAndEstado(
                                cita.getId(),
                                Pago.ESTADO_APROBADO
                        );


        if (yaTienePagoAprobado) {

            return ResultadoCobroCaja.error(
                    "La cita ya posee un pago aprobado "
                            + "y no puede cobrarse nuevamente."
            );
        }


        // =================================================
        // 9. PRECIO REAL
        // =================================================

        BigDecimal montoTotal =
                obtenerMontoConsulta(
                        cita
                );


        if (montoTotal == null) {

            return ResultadoCobroCaja.error(
                    "No fue posible determinar el monto de la consulta."
            );
        }


        // =================================================
        // 10. FORMA DE PAGO
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

            return ResultadoCobroCaja.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 11. RESOLVER STRATEGY
        // =================================================

        ProcesadorCobroCaja procesador =
                resolverProcesador(
                        tipoCobro
                );


        if (procesador == null) {

            return ResultadoCobroCaja.error(
                    MENSAJE_METODO_NO_DISPONIBLE
            );
        }


        // =================================================
        // 12. CONSTRUIR SOLICITUD
        // =================================================

        SolicitudProcesamientoCobro solicitud =
                new SolicitudProcesamientoCobro(

                        tipoCobro,

                        montoTotal,

                        formulario.getMontoRecibido(),

                        formulario.getUltimos4Tarjeta()
                );


        // =================================================
        // 13. PROCESAR
        // =================================================

        ResultadoProcesamientoCobro procesamiento =
                procesador.procesar(
                        solicitud
                );


        // =================================================
        // 14. RESULTADO RECHAZADO
        // =================================================

        if (!procesamiento.aprobado()) {

            /*
             * Solo persistimos cuando realmente existió
             * una transacción hacia el POS.
             *
             * Errores de formulario o efectivo insuficiente
             * NO son pagos procesados.
             */
            if ("TARJETA_RECHAZADA"
                    .equalsIgnoreCase(
                            procesamiento.codigoResultado()
                    )) {

                guardarIntentoRechazado(

                        cita,

                        formaPago,

                        cajero,

                        montoTotal,

                        idempotencyKey,

                        procesamiento
                );
            }


            return ResultadoCobroCaja.rechazado(

                    procesamiento.mensaje(),

                    procesamiento.codigoResultado()
            );
        }


        // =================================================
        // 15. ESTADO CONFIRMADA
        // =================================================

        EstadoCita estadoConfirmada =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_CONFIRMADA
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita "
                                                        + "'Confirmada'."
                                        )
                        );

        // =================================================
        // 16. CREAR PAGO APROBADO
        // =================================================

        Pago pago =
                new Pago();


        pago.setCita(
                cita
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
        // 17. CAMBIAR CITA A CONFIRMADA
        // =================================================

        cita.setEstadoCita(
                estadoConfirmada
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                cajero
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        // =================================================
        // 18. MENSAJE EXACTO DE ÉXITO
        // =================================================

        String mensajeExito =
                "¡Pago registrado exitosamente! Paciente: "
                        + cita.getPaciente()
                        .getNombreCompleto()
                        + ". La cita ha sido actualizada "
                        + "a estado Confirmada.";


        return ResultadoCobroCaja.exito(

                mensajeExito,

                procesamiento.mensaje(),

                pagoGuardado.getId(),

                pagoGuardado.getNumeroTransaccion()
        );
    }


    // =====================================================
    // GUARDAR TARJETA RECHAZADA
    // =====================================================

    private void guardarIntentoRechazado(

            Cita cita,

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
                cita
        );


        pago.setFormaPago(
                formaPago
        );


        pago.setIdempotencyKey(
                idempotencyKey
        );


        /*
         * Una transacción rechazada no cumple la
         * postcondición de pago exitoso, por lo que
         * no recibe número de transacción interno
         * de comprobante.
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
    // RESULTADO IDEMPOTENTE
    // =====================================================

    private ResultadoCobroCaja construirResultadoIdempotente(
            Pago pago
    ) {

        // =================================================
        // YA APROBADO
        // =================================================

        if (Pago.ESTADO_APROBADO
                .equalsIgnoreCase(
                        pago.getEstado()
                )) {

            String mensajeExito =
                    "¡Pago registrado exitosamente! Paciente: "
                            + pago.getCita()
                            .getPaciente()
                            .getNombreCompleto()
                            + ". La cita ha sido actualizada "
                            + "a estado Confirmada.";


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


            return ResultadoCobroCaja.exito(

                    mensajeExito,

                    detalle,

                    pago.getId(),

                    pago.getNumeroTransaccion()
            );
        }


        // =================================================
        // TARJETA YA RECHAZADA
        // =================================================

        if (Pago.ESTADO_RECHAZADO
                .equalsIgnoreCase(
                        pago.getEstado()
                )

                && "TARJETA_RECHAZADA"
                .equalsIgnoreCase(
                        pago.getCodigoResultado()
                )) {

            return ResultadoCobroCaja.rechazado(

                    MENSAJE_TARJETA_RECHAZADA,

                    "TARJETA_RECHAZADA"
            );
        }


        return ResultadoCobroCaja.error(
                "La solicitud de cobro ya fue procesada anteriormente."
        );
    }

    // =====================================================
// ¿PAGO DE PORTAL EXPIRADO?
// =====================================================

    private boolean pagoPortalExpirado(

            Cita cita,

            OffsetDateTime ahora
    ) {

        if (cita == null
                || ahora == null) {

            return false;
        }


        if (cita.getCanalOrigen() == null
                || !"Portal Web"
                .equalsIgnoreCase(
                        cita.getCanalOrigen()
                                .trim()
                )) {

            /*
             * CU-06:
             *
             * Las citas presenciales no tienen
             * expiración automática de pago.
             */
            return false;
        }


        if (cita.getFechaExpiracionPago() == null) {

            return false;
        }


        /*
         * Expirada cuando:
         *
         * fechaExpiracion <= ahora
         */
        return !cita
                .getFechaExpiracionPago()
                .isAfter(
                        ahora
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
    // OBTENER MONTO
    // =====================================================

    private BigDecimal obtenerMontoConsulta(
            Cita cita
    ) {

        SucursalEspecialidad configuracion =
                sucursalEspecialidadRepository
                        .findBySucursal_IdAndEspecialidad_IdAndActivoTrue(

                                cita.getSucursal()
                                        .getId(),

                                cita.getEspecialidad()
                                        .getId()
                        )
                        .orElse(
                                null
                        );


        if (configuracion == null
                || configuracion.getPrecioConsulta() == null
                || configuracion.getPrecioConsulta()
                .compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return null;
        }


        return configuracion
                .getPrecioConsulta()
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =====================================================
    // CAJERO VÁLIDO
    // =====================================================

    private boolean esCajeroValido(
            Usuario usuario
    ) {

        return usuario != null

                && Boolean.TRUE.equals(
                usuario.getActivo()
        )

                && usuario.getRol() != null

                && usuario.getRol().getNombre() != null

                && ROL_CAJERO
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                );
    }


    // =====================================================
    // NÚMERO DE TRANSACCIÓN
    // =====================================================

    private String generarNumeroTransaccion() {

        return "CAJA-"
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

    public record ResultadoCobroCaja(

            boolean exitoso,

            boolean rechazado,

            String mensaje,

            String detalle,

            String codigoResultado,

            Integer idPago,

            String numeroTransaccion

    ) {


        // =================================================
        // ÉXITO
        // =================================================

        public static ResultadoCobroCaja exito(

                String mensaje,

                String detalle,

                Integer idPago,

                String numeroTransaccion
        ) {

            return new ResultadoCobroCaja(

                    true,

                    false,

                    mensaje,

                    detalle,

                    "APROBADO",

                    idPago,

                    numeroTransaccion
            );
        }


        // =================================================
        // RECHAZADO
        // =================================================

        public static ResultadoCobroCaja rechazado(

                String mensaje,

                String codigoResultado
        ) {

            return new ResultadoCobroCaja(

                    false,

                    true,

                    mensaje,

                    null,

                    codigoResultado,

                    null,

                    null
            );
        }


        // =================================================
        // ERROR
        // =================================================

        public static ResultadoCobroCaja error(
                String mensaje
        ) {

            return new ResultadoCobroCaja(

                    false,

                    false,

                    mensaje,

                    null,

                    null,

                    null,

                    null
            );
        }
    }
}