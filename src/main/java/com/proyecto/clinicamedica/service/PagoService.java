package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.pago.PagoFormularioDTO;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.FormaPago;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.SucursalEspecialidad;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;
import com.proyecto.clinicamedica.repository.FormaPagoRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;

import com.proyecto.clinicamedica.service.pago.PasarelaPago;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import com.proyecto.clinicamedica.event.PagoAprobadoEvent;

import org.springframework.context.ApplicationEventPublisher;

/**
 * =========================================================
 * SERVICIO: PAGO EN LÍNEA
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Responsabilidades:
 *
 * - Obtener el resumen de pago de la cita.
 * - Validar que la cita pertenezca al paciente.
 * - Validar estado y expiración.
 * - Obtener el precio real desde PostgreSQL.
 * - Aplicar idempotencia.
 * - Crear el intento de pago.
 * - Invocar la pasarela.
 * - Registrar el resultado.
 * - Cambiar la cita a "Pagada" cuando corresponda.
 *
 * IMPORTANTE:
 *
 * La comunicación con la pasarela se realiza FUERA
 * de una transacción larga de PostgreSQL.
 *
 * Nunca se almacenan:
 *
 * - Número completo de tarjeta.
 * - CVV.
 * - Fecha de vencimiento.
 *
 * =========================================================
 */
@Service
public class PagoService {


    // =====================================================
    // ESTADOS DE CITA
    // =====================================================

    private static final String ESTADO_CITA_PENDIENTE =
            "Pendiente de pago";

    private static final String ESTADO_CITA_PAGADA =
            "Pagada";

    private static final String ESTADO_CITA_CANCELADA =
            "Cancelada";


    // =====================================================
    // FORMAS DE PAGO
    // =====================================================

    private static final String FORMA_CREDITO =
            "Tarjeta de crédito";

    private static final String FORMA_DEBITO =
            "Tarjeta de débito";


    // =====================================================
    // MENSAJES
    // =====================================================

    private static final String MENSAJE_EXPIRADO =
            "El tiempo para confirmar su cita ha expirado. "
                    + "El horario seleccionado ha sido liberado. "
                    + "Por favor, seleccione un nuevo horario. "
                    + "Será redirigido en unos segundos...";


    private static final String MENSAJE_FONDOS_INSUFICIENTES =
            "Su tarjeta fue rechazada por fondos insuficientes. "
                    + "Verifique su saldo e intente nuevamente.";


    private static final String MENSAJE_TARJETA_INVALIDA =
            "Su tarjeta fue rechazada. El número de tarjeta es inválido. "
                    + "Verifique los datos e intente nuevamente.";


    private static final String MENSAJE_TARJETA_VENCIDA =
            "Su tarjeta fue rechazada. La tarjeta está vencida. "
                    + "Utilice otra tarjeta de crédito o débito.";


    private static final String MENSAJE_RECHAZO_BANCARIO =
            "La transacción con tarjeta fue rechazada por el banco. "
                    + "Por favor, verifique los datos de su tarjeta "
                    + "o intente con una tarjeta diferente.";


    private static final String MENSAJE_ERROR_PROCESAMIENTO =
            "El pago no pudo ser procesado. "
                    + "Por favor, intente nuevamente o utilice otra tarjeta.";


    private static final String MENSAJE_ERROR_COMUNICACION =
            "Error al procesar el pago. "
                    + "Por favor, intente nuevamente o contacte a su banco.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioActualService usuarioActualService;

    private final CitaRepository citaRepository;

    private final PagoRepository pagoRepository;

    private final EstadoCitaRepository estadoCitaRepository;

    private final FormaPagoRepository formaPagoRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final PasarelaPago pasarelaPago;

    private final TransactionTemplate transactionTemplate;

    private final ZoneId zonaHoraria;

    private final ApplicationEventPublisher
            eventPublisher;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PagoService(
            UsuarioActualService usuarioActualService,
            CitaRepository citaRepository,
            PagoRepository pagoRepository,
            EstadoCitaRepository estadoCitaRepository,
            FormaPagoRepository formaPagoRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            PasarelaPago pasarelaPago,
            PlatformTransactionManager transactionManager,
            ApplicationEventPublisher eventPublisher,
            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.usuarioActualService =
                usuarioActualService;

        this.citaRepository =
                citaRepository;

        this.pagoRepository =
                pagoRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.formaPagoRepository =
                formaPagoRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.pasarelaPago =
                pasarelaPago;

        this.transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );

        this.eventPublisher =
                eventPublisher;
    }


    // =====================================================
    // OBTENER RESUMEN DE PAGO
    // =====================================================

    public ResumenPago obtenerResumen(
            Integer idCita
    ) {

        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        CargaResumenPago carga =
                Objects.requireNonNull(
                        transactionTemplate.execute(
                                status ->
                                        construirResumen(
                                                idCita,
                                                paciente.getId()
                                        )
                        )
                );


        /*
         * La transacción ya terminó correctamente.
         *
         * Si la cita venció, el cambio a Cancelada ya quedó
         * confirmado antes de lanzar la excepción que utilizará
         * el Controller para mostrar FA02.
         */
        if (carga.pagoExpirado()) {

            throw new IllegalStateException(
                    MENSAJE_EXPIRADO
            );
        }


        return carga.resumen();
    }


    // =====================================================
    // PROCESAR PAGO
    // =====================================================

    public ResultadoPago procesarPago(
            Integer idCita,
            PagoFormularioDTO formulario
    ) {

        if (formulario == null) {

            return ResultadoPago.error(
                    MENSAJE_ERROR_PROCESAMIENTO
            );
        }


        if (formulario.getIdempotencyKey() == null) {

            return ResultadoPago.error(
                    "No fue posible validar la solicitud de pago. "
                            + "Recargue la página e intente nuevamente."
            );
        }


        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        PreparacionPago preparacion;


        try {

            preparacion =
                    Objects.requireNonNull(
                            transactionTemplate.execute(
                                    status ->
                                            prepararPago(
                                                    idCita,
                                                    paciente.getId(),
                                                    formulario.getIdempotencyKey()
                                            )
                            )
                    );

        } catch (DataIntegrityViolationException ex) {

            /*
             * Protección adicional ante solicitudes
             * concurrentes.
             *
             * Nunca se expone el detalle técnico de
             * PostgreSQL.
             */
            return ResultadoPago.enProceso(
                    "Existe una operación de pago en procesamiento "
                            + "para esta cita. Por favor, espere."
            );
        }


        // =================================================
        // RESULTADO YA EXISTENTE
        // =================================================

        if (preparacion.resultadoAnterior() != null) {

            return preparacion.resultadoAnterior();
        }


        // =================================================
        // LLAMAR PASARELA
        // =================================================
        //
        // IMPORTANTE:
        //
        // Aquí NO existe una transacción abierta contra
        // PostgreSQL.
        // =================================================

        PasarelaPago.Resultado resultadoPasarela;


        try {

            resultadoPasarela =
                    pasarelaPago.procesar(
                            new PasarelaPago.Solicitud(

                                    limpiar(
                                            formulario.getNumeroTarjeta()
                                    ),

                                    limpiar(
                                            formulario.getNombreTitular()
                                    ).toUpperCase(
                                            Locale.ROOT
                                    ),

                                    limpiar(
                                            formulario.getVencimiento()
                                    ),

                                    limpiar(
                                            formulario.getCvv()
                                    ),

                                    preparacion.monto(),

                                    formulario.getIdempotencyKey()
                            )
                    );

        } catch (RuntimeException ex) {

            /*
             * No se registra el contenido de la excepción,
             * porque una integración externa podría incluir
             * accidentalmente información sensible.
             */
            resultadoPasarela =
                    new PasarelaPago.Resultado(
                            PasarelaPago.CodigoResultado.ERROR_COMUNICACION,
                            null,
                            null
                    );
        }


        // =================================================
        // FINALIZAR EN NUEVA TRANSACCIÓN
        // =================================================

        PasarelaPago.Resultado resultadoFinal =
                resultadoPasarela;


        return Objects.requireNonNull(
                transactionTemplate.execute(
                        status ->
                                finalizarPago(
                                        idCita,
                                        paciente.getId(),
                                        preparacion.idPago(),
                                        resultadoFinal
                                )
                )
        );
    }


    // =====================================================
    // CONSTRUIR RESUMEN
    // =====================================================

    private CargaResumenPago construirResumen(
            Integer idCita,
            Integer idPaciente
    ) {

        Cita cita =
                citaRepository
                        .buscarParaPago(
                                idCita,
                                idPaciente
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró una cita disponible para pago."
                                        )
                        );


        validarCitaPendiente(
                cita
        );


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // FA02 - EXPIRACIÓN
        // =================================================

        if (estaExpirada(
                cita,
                ahora
        )) {

            cancelarCitaExpirada(
                    cita,
                    ahora
            );


            return CargaResumenPago.expirado();
        }


        BigDecimal monto =
                obtenerMontoReal(
                        cita
                );


        return CargaResumenPago.disponible(
                new ResumenPago(
                        cita.getId(),

                        cita.getMedico()
                                .getNombreCompleto(),

                        cita.getEspecialidad()
                                .getNombre(),

                        cita.getSucursal()
                                .getNombre(),

                        cita.getFechaHoraCita(),

                        cita.getFechaExpiracionPago(),

                        monto
                )
        );
    }


    // =====================================================
    // PREPARAR PAGO
    // =====================================================

    private PreparacionPago prepararPago(
            Integer idCita,
            Integer idPaciente,
            UUID idempotencyKey
    ) {

        // =================================================
        // IDEMPOTENCIA - PRIMERA REVISIÓN
        // =================================================

        Pago existente =
                pagoRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(
                                null
                        );


        if (existente != null) {

            validarIdempotenciaPerteneceACita(
                    existente,
                    idCita,
                    idPaciente
            );


            return PreparacionPago.anterior(
                    resultadoDesdePagoExistente(
                            existente
                    )
            );
        }


        // =================================================
        // BLOQUEAR CITA
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaPagoConBloqueo(
                                idCita,
                                idPaciente
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró una cita disponible para pago."
                                        )
                        );


        // =================================================
        // IDEMPOTENCIA - SEGUNDA REVISIÓN
        // =================================================
        //
        // Se repite después del lock para cubrir carreras.
        // =================================================

        existente =
                pagoRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(
                                null
                        );


        if (existente != null) {

            validarIdempotenciaPerteneceACita(
                    existente,
                    idCita,
                    idPaciente
            );


            return PreparacionPago.anterior(
                    resultadoDesdePagoExistente(
                            existente
                    )
            );
        }


        // =================================================
        // ¿YA ESTÁ PAGADA?
        // =================================================

        Pago aprobado =
                pagoRepository
                        .findFirstByCita_IdAndEstadoOrderByFechaCreacionDesc(
                                idCita,
                                Pago.ESTADO_APROBADO
                        )
                        .orElse(
                                null
                        );


        if (aprobado != null) {

            return PreparacionPago.anterior(
                    resultadoDesdePagoExistente(
                            aprobado
                    )
            );
        }


        // =================================================
        // ¿YA HAY PAGO PROCESANDO?
        // =================================================

        if (pagoRepository
                .existsByCita_IdAndEstado(
                        idCita,
                        Pago.ESTADO_PROCESANDO
                )) {

            return PreparacionPago.anterior(
                    ResultadoPago.enProceso(
                            "Existe una operación de pago en procesamiento "
                                    + "para esta cita. Por favor, espere."
                    )
            );
        }


        // =================================================
        // VALIDAR ESTADO CITA
        // =================================================

        validarCitaPendiente(
                cita
        );


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // FA02 - EXPIRACIÓN
        // =================================================

        if (estaExpirada(
                cita,
                ahora
        )) {

            cancelarCitaExpirada(
                    cita,
                    ahora
            );


            return PreparacionPago.anterior(
                    ResultadoPago.expirado(
                            MENSAJE_EXPIRADO
                    )
            );
        }


        // =================================================
        // MONTO REAL
        // =================================================

        BigDecimal monto =
                obtenerMontoReal(
                        cita
                );


        // =================================================
        // CREAR INTENTO
        // =================================================

        Pago pago =
                new Pago();


        pago.setCita(
                cita
        );


        /*
         * Todavía no conocemos si la tarjeta es
         * crédito o débito.
         *
         * La pasarela lo indicará después.
         */
        pago.setFormaPago(
                null
        );


        pago.setIdempotencyKey(
                idempotencyKey
        );


        pago.setMonto(
                monto
        );


        pago.setEstado(
                Pago.ESTADO_PROCESANDO
        );


        pago.setCodigoResultado(
                null
        );


        Pago guardado =
                pagoRepository
                        .saveAndFlush(
                                pago
                        );


        return PreparacionPago.nuevo(
                guardado.getId(),
                monto
        );
    }
    // =====================================================
// CARGA DEL RESUMEN
// =====================================================

    private record CargaResumenPago(

            ResumenPago resumen,

            boolean pagoExpirado

    ) {

        private static CargaResumenPago disponible(
                ResumenPago resumen
        ) {

            return new CargaResumenPago(
                    resumen,
                    false
            );
        }


        private static CargaResumenPago expirado() {

            return new CargaResumenPago(
                    null,
                    true
            );
        }
    }


    // =====================================================
    // FINALIZAR PAGO
    // =====================================================

    private ResultadoPago finalizarPago(
            Integer idCita,
            Integer idPaciente,
            Integer idPago,
            PasarelaPago.Resultado resultadoPasarela
    ) {

        Pago pago =
                pagoRepository
                        .findById(
                                idPago
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No se encontró la operación de pago."
                                        )
                        );


        // =================================================
        // YA FINALIZADO
        // =================================================

        if (!Pago.ESTADO_PROCESANDO.equals(
                pago.getEstado()
        )) {

            return resultadoDesdePagoExistente(
                    pago
            );
        }


        // =================================================
        // BLOQUEAR CITA NUEVAMENTE
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaPagoConBloqueo(
                                idCita,
                                idPaciente
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No se encontró la cita asociada al pago."
                                        )
                        );


        // =================================================
        // PROTECCIÓN DE INTEGRIDAD
        // =================================================

        if (pago.getCita() == null
                || !Objects.equals(
                pago.getCita().getId(),
                cita.getId()
        )) {

            throw new IllegalStateException(
                    "La operación de pago no corresponde a la cita indicada."
            );
        }


        // =================================================
        // RESULTADO NULO
        // =================================================

        if (resultadoPasarela == null
                || resultadoPasarela.codigo() == null) {

            marcarError(
                    pago,
                    PasarelaPago.CodigoResultado.ERROR_PROCESAMIENTO
            );


            return ResultadoPago.error(
                    MENSAJE_ERROR_PROCESAMIENTO
            );
        }


        // =================================================
        // APROBADO
        // =================================================

        if (resultadoPasarela.aprobado()) {

            return confirmarPago(
                    cita,
                    pago,
                    resultadoPasarela
            );
        }


        // =================================================
        // RECHAZADO / ERROR
        // =================================================

        return registrarResultadoFallido(
                pago,
                resultadoPasarela.codigo()
        );
    }


    // =====================================================
    // CONFIRMAR PAGO
    // =====================================================

    private ResultadoPago confirmarPago(
            Cita cita,
            Pago pago,
            PasarelaPago.Resultado resultado
    ) {

        if (resultado.numeroTransaccion() == null
                || resultado.numeroTransaccion().isBlank()
                || resultado.tipoTarjeta() == null) {

            marcarError(
                    pago,
                    PasarelaPago.CodigoResultado.ERROR_PROCESAMIENTO
            );


            return ResultadoPago.error(
                    MENSAJE_ERROR_PROCESAMIENTO
            );
        }

        // =================================================
// PUBLICAR EVENTO
// =================================================
//
// El listener NO enviará el correo todavía.
//
// @TransactionalEventListener(AFTER_COMMIT)
// esperará a que esta transacción termine
// correctamente.
// =================================================

        eventPublisher.publishEvent(
                new PagoAprobadoEvent(
                        pago.getId()
                )
        );


        // =================================================
        // FORMA DE PAGO
        // =================================================

        String nombreFormaPago =
                resultado.tipoTarjeta()
                        == PasarelaPago.TipoTarjeta.DEBITO

                        ? FORMA_DEBITO
                        : FORMA_CREDITO;


        FormaPago formaPago =
                formaPagoRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                nombreFormaPago
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe la forma de pago activa '"
                                                        + nombreFormaPago
                                                        + "'."
                                        )
                        );


        // =================================================
        // ESTADO PAGADA
        // =================================================

        EstadoCita estadoPagada =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_CITA_PAGADA
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita 'Pagada'."
                                        )
                        );


        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // ACTUALIZAR PAGO
        // =================================================

        pago.setFormaPago(
                formaPago
        );


        pago.setNumeroTransaccion(
                resultado.numeroTransaccion()
        );


        pago.setEstado(
                Pago.ESTADO_APROBADO
        );


        pago.setCodigoResultado(
                PasarelaPago.CodigoResultado
                        .APROBADO
                        .name()
        );


        pago.setFechaHoraPago(
                ahora
        );


        // =================================================
        // ACTUALIZAR CITA
        // =================================================

        cita.setEstadoCita(
                estadoPagada
        );


        cita.setFechaModificacion(
                ahora
        );


        // =================================================
        // PERSISTIR
        // =================================================

        pagoRepository.saveAndFlush(
                pago
        );


        citaRepository.saveAndFlush(
                cita
        );


        // =================================================
        // ÉXITO
        // =================================================

        String mensaje =
                "¡Pago realizado exitosamente! Número de transacción: "
                        + resultado.numeroTransaccion()
                        + ". Su cita ha sido confirmada.";


        return ResultadoPago.aprobado(
                mensaje,
                resultado.numeroTransaccion()
        );
    }


    // =====================================================
    // REGISTRAR RESULTADO FALLIDO
    // =====================================================

    private ResultadoPago registrarResultadoFallido(
            Pago pago,
            PasarelaPago.CodigoResultado codigo
    ) {

        String estadoPago;


        switch (codigo) {

            case FONDOS_INSUFICIENTES,
                 TARJETA_INVALIDA,
                 TARJETA_VENCIDA,
                 RECHAZO_BANCARIO ->

                    estadoPago =
                            Pago.ESTADO_RECHAZADO;


            case ERROR_PROCESAMIENTO,
                 ERROR_COMUNICACION ->

                    estadoPago =
                            Pago.ESTADO_ERROR;


            default ->

                    estadoPago =
                            Pago.ESTADO_ERROR;
        }


        pago.setEstado(
                estadoPago
        );


        pago.setCodigoResultado(
                codigo.name()
        );


        pago.setFechaHoraPago(
                null
        );


        pagoRepository.saveAndFlush(
                pago
        );


        return switch (codigo) {

            case FONDOS_INSUFICIENTES ->
                    ResultadoPago.rechazado(
                            MENSAJE_FONDOS_INSUFICIENTES
                    );


            case TARJETA_INVALIDA ->
                    ResultadoPago.rechazado(
                            MENSAJE_TARJETA_INVALIDA
                    );


            case TARJETA_VENCIDA ->
                    ResultadoPago.rechazado(
                            MENSAJE_TARJETA_VENCIDA
                    );


            case RECHAZO_BANCARIO ->
                    ResultadoPago.rechazado(
                            MENSAJE_RECHAZO_BANCARIO
                    );


            case ERROR_COMUNICACION ->
                    ResultadoPago.error(
                            MENSAJE_ERROR_COMUNICACION
                    );


            default ->
                    ResultadoPago.error(
                            MENSAJE_ERROR_PROCESAMIENTO
                    );
        };
    }


    // =====================================================
    // MARCAR ERROR
    // =====================================================

    private void marcarError(
            Pago pago,
            PasarelaPago.CodigoResultado codigo
    ) {

        pago.setEstado(
                Pago.ESTADO_ERROR
        );


        pago.setCodigoResultado(
                codigo.name()
        );


        pago.setFechaHoraPago(
                null
        );


        pagoRepository.saveAndFlush(
                pago
        );
    }


    // =====================================================
    // RESULTADO DESDE PAGO EXISTENTE
    // =====================================================

    private ResultadoPago resultadoDesdePagoExistente(
            Pago pago
    ) {

        if (Pago.ESTADO_APROBADO.equals(
                pago.getEstado()
        )) {

            String numero =
                    pago.getNumeroTransaccion();


            return ResultadoPago.aprobado(
                    "¡Pago realizado exitosamente! Número de transacción: "
                            + numero
                            + ". Su cita ha sido confirmada.",
                    numero
            );
        }


        if (Pago.ESTADO_PROCESANDO.equals(
                pago.getEstado()
        )) {

            return ResultadoPago.enProceso(
                    "El pago ya se encuentra en procesamiento. "
                            + "Por favor, espere el resultado."
            );
        }


        String codigo =
                pago.getCodigoResultado();


        if (codigo == null
                || codigo.isBlank()) {

            return ResultadoPago.error(
                    MENSAJE_ERROR_PROCESAMIENTO
            );
        }


        try {

            PasarelaPago.CodigoResultado codigoResultado =
                    PasarelaPago.CodigoResultado.valueOf(
                            codigo
                    );


            return switch (codigoResultado) {

                case FONDOS_INSUFICIENTES ->
                        ResultadoPago.rechazado(
                                MENSAJE_FONDOS_INSUFICIENTES
                        );


                case TARJETA_INVALIDA ->
                        ResultadoPago.rechazado(
                                MENSAJE_TARJETA_INVALIDA
                        );


                case TARJETA_VENCIDA ->
                        ResultadoPago.rechazado(
                                MENSAJE_TARJETA_VENCIDA
                        );


                case RECHAZO_BANCARIO ->
                        ResultadoPago.rechazado(
                                MENSAJE_RECHAZO_BANCARIO
                        );


                case ERROR_COMUNICACION ->
                        ResultadoPago.error(
                                MENSAJE_ERROR_COMUNICACION
                        );


                default ->
                        ResultadoPago.error(
                                MENSAJE_ERROR_PROCESAMIENTO
                        );
            };

        } catch (IllegalArgumentException ex) {

            return ResultadoPago.error(
                    MENSAJE_ERROR_PROCESAMIENTO
            );
        }
    }


    // =====================================================
// VALIDAR IDEMPOTENCIA / CITA / PACIENTE
// =====================================================

    private void validarIdempotenciaPerteneceACita(
            Pago pago,
            Integer idCita,
            Integer idPaciente
    ) {

        if (pago == null
                || pago.getCita() == null
                || pago.getCita().getPaciente() == null

                || !Objects.equals(
                pago.getCita().getId(),
                idCita
        )

                || !Objects.equals(
                pago.getCita()
                        .getPaciente()
                        .getId(),
                idPaciente
        )) {

            throw new IllegalArgumentException(
                    "La clave de idempotencia no corresponde a esta cita."
            );
        }
    }


    // =====================================================
    // VALIDAR CITA PENDIENTE
    // =====================================================

    private void validarCitaPendiente(
            Cita cita
    ) {

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null) {

            throw new IllegalStateException(
                    "La cita no posee un estado válido."
            );
        }


        String estado =
                cita.getEstadoCita()
                        .getNombre()
                        .trim();


        if (ESTADO_CITA_PAGADA
                .equalsIgnoreCase(
                        estado
                )) {

            throw new IllegalStateException(
                    "La cita ya se encuentra pagada."
            );
        }


        if (!ESTADO_CITA_PENDIENTE
                .equalsIgnoreCase(
                        estado
                )) {

            throw new IllegalStateException(
                    "La cita no se encuentra disponible para pago."
            );
        }
    }


    // =====================================================
    // ¿ESTÁ EXPIRADA?
    // =====================================================

    private boolean estaExpirada(
            Cita cita,
            OffsetDateTime ahora
    ) {

        return cita.getFechaExpiracionPago() == null

                || !cita
                .getFechaExpiracionPago()
                .isAfter(
                        ahora
                );
    }


    // =====================================================
    // CANCELAR CITA EXPIRADA
    // =====================================================

    private void cancelarCitaExpirada(
            Cita cita,
            OffsetDateTime ahora
    ) {

        EstadoCita cancelada =
                estadoCitaRepository
                        .findByNombreIgnoreCaseAndActivoTrue(
                                ESTADO_CITA_CANCELADA
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el estado de cita 'Cancelada'."
                                        )
                        );


        cita.setEstadoCita(
                cancelada
        );


        cita.setFechaModificacion(
                ahora
        );


        citaRepository.saveAndFlush(
                cita
        );
    }


    // =====================================================
    // OBTENER MONTO REAL
    // =====================================================

    private BigDecimal obtenerMontoReal(
            Cita cita
    ) {

        SucursalEspecialidad configuracion =
                sucursalEspecialidadRepository
                        .findBySucursal_IdAndEspecialidad_IdAndActivoTrue(
                                cita.getSucursal().getId(),
                                cita.getEspecialidad().getId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No se encontró la tarifa de la consulta."
                                        )
                        );


        BigDecimal monto =
                configuracion.getPrecioConsulta();


        if (monto == null
                || monto.signum() <= 0) {

            throw new IllegalStateException(
                    "La tarifa de la consulta no es válida."
            );
        }


        return monto;
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // DTO INTERNO: PREPARACIÓN
    // =====================================================

    private record PreparacionPago(

            Integer idPago,

            BigDecimal monto,

            ResultadoPago resultadoAnterior

    ) {


        private static PreparacionPago nuevo(
                Integer idPago,
                BigDecimal monto
        ) {

            return new PreparacionPago(
                    idPago,
                    monto,
                    null
            );
        }


        private static PreparacionPago anterior(
                ResultadoPago resultado
        ) {

            return new PreparacionPago(
                    null,
                    null,
                    resultado
            );
        }
    }


    // =====================================================
    // RESUMEN PARA LA PANTALLA
    // =====================================================

    public record ResumenPago(

            Integer idCita,

            String medico,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            OffsetDateTime fechaExpiracionPago,

            BigDecimal monto

    ) {
    }


    // =====================================================
    // ESTADO DEL RESULTADO
    // =====================================================

    public enum EstadoResultadoPago {

        APROBADO,

        RECHAZADO,

        ERROR,

        EXPIRADO,

        EN_PROCESO
    }


    // =====================================================
    // RESULTADO DEL PAGO
    // =====================================================

    public record ResultadoPago(

            EstadoResultadoPago estado,

            String mensaje,

            String numeroTransaccion

    ) {


        public boolean exitoso() {

            return estado ==
                    EstadoResultadoPago.APROBADO;
        }


        public boolean expirado() {

            return estado ==
                    EstadoResultadoPago.EXPIRADO;
        }


        public static ResultadoPago aprobado(
                String mensaje,
                String numeroTransaccion
        ) {

            return new ResultadoPago(
                    EstadoResultadoPago.APROBADO,
                    mensaje,
                    numeroTransaccion
            );
        }


        public static ResultadoPago rechazado(
                String mensaje
        ) {

            return new ResultadoPago(
                    EstadoResultadoPago.RECHAZADO,
                    mensaje,
                    null
            );
        }


        public static ResultadoPago error(
                String mensaje
        ) {

            return new ResultadoPago(
                    EstadoResultadoPago.ERROR,
                    mensaje,
                    null
            );
        }


        public static ResultadoPago expirado(
                String mensaje
        ) {

            return new ResultadoPago(
                    EstadoResultadoPago.EXPIRADO,
                    mensaje,
                    null
            );
        }


        public static ResultadoPago enProceso(
                String mensaje
        ) {

            return new ResultadoPago(
                    EstadoResultadoPago.EN_PROCESO,
                    mensaje,
                    null
            );
        }
    }
    // =====================================================
// OBTENER COMPROBANTE
// =====================================================

    public ComprobantePago obtenerComprobante(
            String numeroTransaccion
    ) {

        if (numeroTransaccion == null
                || numeroTransaccion.isBlank()) {

            throw new IllegalArgumentException(
                    "El número de transacción es obligatorio."
            );
        }


        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        return Objects.requireNonNull(
                transactionTemplate.execute(
                        status -> {

                            Pago pago =
                                    pagoRepository
                                            .buscarComprobante(
                                                    numeroTransaccion.trim(),
                                                    paciente.getId()
                                            )
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalArgumentException(
                                                                    "No se encontró el comprobante solicitado."
                                                            )
                                            );


                            Cita cita =
                                    pago.getCita();


                            return new ComprobantePago(

                                    pago.getNumeroTransaccion(),

                                    cita.getPaciente()
                                            .getNombreCompleto(),

                                    cita.getMedico()
                                            .getNombreCompleto(),

                                    cita.getEspecialidad()
                                            .getNombre(),

                                    cita.getSucursal()
                                            .getNombre(),

                                    cita.getFechaHoraCita(),

                                    pago.getMonto(),

                                    pago.getFormaPago()
                                            .getNombre(),

                                    pago.getFechaHoraPago()
                            );
                        }
                )
        );
    }
    // =====================================================
// COMPROBANTE DE PAGO
// =====================================================

    public record ComprobantePago(

            String numeroTransaccion,

            String paciente,

            String medico,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            BigDecimal monto,

            String formaPago,

            OffsetDateTime fechaHoraPago

    ) {
    }
}