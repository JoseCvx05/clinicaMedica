package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * =========================================================
 * ENTIDAD: PAGO
 * =========================================================
 *
 * Representa un pago asociado a una cita.
 *
 * Utilizada por:
 *
 * CU-04 - Pago en Línea con Tarjeta.
 * CU-06 - Cobro de Consulta en Caja.
 *
 * IMPORTANTE:
 *
 * Nunca almacena:
 *
 * - Número completo de tarjeta.
 * - CVV.
 * - Fecha de vencimiento.
 *
 * Para pagos presenciales únicamente se almacenan
 * los últimos cuatro dígitos como referencia.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "pago",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uq_pago_idempotency_key",
                        columnNames = "idempotency_key"
                ),

                @UniqueConstraint(
                        name = "uq_pago_numero_transaccion",
                        columnNames = "numero_transaccion"
                )
        }
)
public class Pago {


    // =====================================================
    // ESTADOS DEL PAGO
    // =====================================================

    public static final String ESTADO_PROCESANDO =
            "PROCESANDO";

    public static final String ESTADO_APROBADO =
            "APROBADO";

    public static final String ESTADO_RECHAZADO =
            "RECHAZADO";

    public static final String ESTADO_ERROR =
            "ERROR";


    // =====================================================
    // CANALES
    // =====================================================

    /**
     * CU-04.
     */
    public static final String CANAL_EN_LINEA =
            "EN_LINEA";


    /**
     * CU-06.
     */
    public static final String CANAL_CAJA =
            "CAJA";


    // =====================================================
    // TIPOS DE TARJETA PARA CAJA
    // =====================================================

    public static final String TIPO_TARJETA_VISA =
            "VISA";

    public static final String TIPO_TARJETA_MASTERCARD =
            "MASTERCARD";

    public static final String TIPO_TARJETA_DEBITO =
            "DEBITO";


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // CITA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_cita",
            nullable = false
    )
    private Cita cita;


    // =====================================================
    // FORMA DE PAGO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_forma_pago"
    )
    private FormaPago formaPago;


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================

    /**
     * Protege contra solicitudes repetidas.
     *
     * CU-04 lo exige explícitamente mediante RNF-016.
     *
     * CU-06 también lo reutilizará como protección
     * adicional contra doble envío del formulario.
     */
    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            columnDefinition = "uuid"
    )
    private UUID idempotencyKey;


    // =====================================================
    // NÚMERO DE TRANSACCIÓN
    // =====================================================

    @Column(
            name = "numero_transaccion",
            length = 100,
            unique = true
    )
    private String numeroTransaccion;


    // =====================================================
    // MONTO TOTAL PAGADO
    // =====================================================

    @Column(
            name = "monto",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal monto;


    // =====================================================
    // CU-06 - EFECTIVO
    // =====================================================

    /**
     * Cantidad entregada físicamente por el paciente.
     *
     * Solo aplica a pagos en efectivo.
     */
    @Column(
            name = "monto_recibido",
            precision = 10,
            scale = 2
    )
    private BigDecimal montoRecibido;


    /**
     * Cambio entregado al paciente.
     *
     * cambio = montoRecibido - monto
     */
    @Column(
            name = "cambio",
            precision = 10,
            scale = 2
    )
    private BigDecimal cambio;


    // =====================================================
    // CU-06 - TARJETA PRESENCIAL
    // =====================================================

    /**
     * VISA
     * MASTERCARD
     * DEBITO
     *
     * No se utiliza para pagos en efectivo.
     */
    @Column(
            name = "tipo_tarjeta",
            length = 20
    )
    private String tipoTarjeta;


    /**
     * Única referencia de tarjeta persistida
     * durante el cobro presencial.
     *
     * Nunca almacenar el número completo.
     */
    @Column(
            name = "ultimos4_tarjeta",
            length = 4
    )
    private String ultimos4Tarjeta;


    // =====================================================
    // CANAL DEL PAGO
    // =====================================================

    /**
     * EN_LINEA -> CU-04
     * CAJA     -> CU-06
     */
    @Column(
            name = "canal_pago",
            length = 20
    )
    private String canalPago;


    // =====================================================
    // CU-06 - CAJERO
    // =====================================================

    /**
     * Usuario interno que realizó el cobro presencial.
     *
     * En CU-04 permanece NULL porque el paciente
     * realiza el pago directamente.
     */
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_usuario_cajero"
    )
    private Usuario cajero;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private String estado;


    // =====================================================
    // RESULTADO DEL PROCESAMIENTO
    // =====================================================

    /**
     * Resultado normalizado del procesamiento persistido.
     *
     * Ejemplos CU-04:
     *
     * APROBADO
     * FONDOS_INSUFICIENTES
     * TARJETA_INVALIDA
     * TARJETA_VENCIDA
     * ERROR_COMUNICACION
     *
     * Ejemplos CU-06:
     *
     * APROBADO
     * TARJETA_RECHAZADA
     * ERROR_POS
     *
     * Las validaciones previas que no representan una
     * transacción, como efectivo insuficiente, no generan
     * un registro Pago.
     */
    @Column(
            name = "codigo_resultado",
            length = 50
    )
    private String codigoResultado;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_hora_pago"
    )
    private OffsetDateTime fechaHoraPago;


    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private OffsetDateTime fechaCreacion;


    @Column(
            name = "fecha_modificacion"
    )
    private OffsetDateTime fechaModificacion;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        if (fechaCreacion == null) {

            fechaCreacion =
                    OffsetDateTime.now();
        }


        /*
         * Compatibilidad con CU-04.
         *
         * Los pagos existentes fueron implementados
         * originalmente como pagos en línea.
         *
         * CU-06 establecerá explícitamente CAJA antes
         * de guardar el pago.
         */
        if (canalPago == null
                || canalPago.isBlank()) {

            canalPago =
                    CANAL_EN_LINEA;
        }
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    protected void preUpdate() {

        fechaModificacion =
                OffsetDateTime.now();
    }


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    // =====================================================
    // CITA
    // =====================================================

    public Cita getCita() {

        return cita;
    }


    public void setCita(
            Cita cita
    ) {

        this.cita =
                cita;
    }


    // =====================================================
    // FORMA DE PAGO
    // =====================================================

    public FormaPago getFormaPago() {

        return formaPago;
    }


    public void setFormaPago(
            FormaPago formaPago
    ) {

        this.formaPago =
                formaPago;
    }


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================

    public UUID getIdempotencyKey() {

        return idempotencyKey;
    }


    public void setIdempotencyKey(
            UUID idempotencyKey
    ) {

        this.idempotencyKey =
                idempotencyKey;
    }


    // =====================================================
    // NÚMERO DE TRANSACCIÓN
    // =====================================================

    public String getNumeroTransaccion() {

        return numeroTransaccion;
    }


    public void setNumeroTransaccion(
            String numeroTransaccion
    ) {

        this.numeroTransaccion =
                numeroTransaccion;
    }


    // =====================================================
    // MONTO
    // =====================================================

    public BigDecimal getMonto() {

        return monto;
    }


    public void setMonto(
            BigDecimal monto
    ) {

        this.monto =
                monto;
    }


    // =====================================================
    // MONTO RECIBIDO
    // =====================================================

    public BigDecimal getMontoRecibido() {

        return montoRecibido;
    }


    public void setMontoRecibido(
            BigDecimal montoRecibido
    ) {

        this.montoRecibido =
                montoRecibido;
    }


    // =====================================================
    // CAMBIO
    // =====================================================

    public BigDecimal getCambio() {

        return cambio;
    }


    public void setCambio(
            BigDecimal cambio
    ) {

        this.cambio =
                cambio;
    }


    // =====================================================
    // TIPO DE TARJETA
    // =====================================================

    public String getTipoTarjeta() {

        return tipoTarjeta;
    }


    public void setTipoTarjeta(
            String tipoTarjeta
    ) {

        this.tipoTarjeta =
                tipoTarjeta;
    }


    // =====================================================
    // ÚLTIMOS 4
    // =====================================================

    public String getUltimos4Tarjeta() {

        return ultimos4Tarjeta;
    }


    public void setUltimos4Tarjeta(
            String ultimos4Tarjeta
    ) {

        this.ultimos4Tarjeta =
                ultimos4Tarjeta;
    }


    // =====================================================
    // CANAL
    // =====================================================

    public String getCanalPago() {

        return canalPago;
    }


    public void setCanalPago(
            String canalPago
    ) {

        this.canalPago =
                canalPago;
    }


    // =====================================================
    // CAJERO
    // =====================================================

    public Usuario getCajero() {

        return cajero;
    }


    public void setCajero(
            Usuario cajero
    ) {

        this.cajero =
                cajero;
    }


    // =====================================================
    // ESTADO
    // =====================================================

    public String getEstado() {

        return estado;
    }


    public void setEstado(
            String estado
    ) {

        this.estado =
                estado;
    }


    // =====================================================
    // CÓDIGO RESULTADO
    // =====================================================

    public String getCodigoResultado() {

        return codigoResultado;
    }


    public void setCodigoResultado(
            String codigoResultado
    ) {

        this.codigoResultado =
                codigoResultado;
    }


    // =====================================================
    // FECHA/HORA DEL PAGO
    // =====================================================

    public OffsetDateTime getFechaHoraPago() {

        return fechaHoraPago;
    }


    public void setFechaHoraPago(
            OffsetDateTime fechaHoraPago
    ) {

        this.fechaHoraPago =
                fechaHoraPago;
    }


    // =====================================================
    // AUDITORÍA TEMPORAL
    // =====================================================

    public OffsetDateTime getFechaCreacion() {

        return fechaCreacion;
    }


    public OffsetDateTime getFechaModificacion() {

        return fechaModificacion;
    }
}