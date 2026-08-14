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
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Representa un intento de pago asociado a una cita.
 *
 * IMPORTANTE:
 *
 * Nunca almacena:
 *
 * - Número completo de tarjeta.
 * - CVV.
 * - Fecha de vencimiento.
 *
 * El campo idempotencyKey protege contra solicitudes
 * duplicadas del mismo pago.
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
    // ESTADOS
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

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            columnDefinition = "uuid"
    )
    private UUID idempotencyKey;


    // =====================================================
    // TRANSACCIÓN
    // =====================================================

    @Column(
            name = "numero_transaccion",
            length = 100,
            unique = true
    )
    private String numeroTransaccion;


    // =====================================================
    // MONTO
    // =====================================================

    @Column(
            name = "monto",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal monto;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private String estado;


    /**
     * Resultado normalizado retornado por la pasarela.
     *
     * Ejemplos:
     *
     * APROBADO
     * FONDOS_INSUFICIENTES
     * TARJETA_INVALIDA
     * TARJETA_VENCIDA
     * ERROR_COMUNICACION
     * ERROR_PROCESAMIENTO
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


    public Cita getCita() {
        return cita;
    }

    public void setCita(
            Cita cita
    ) {
        this.cita = cita;
    }


    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(
            FormaPago formaPago
    ) {
        this.formaPago = formaPago;
    }


    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(
            UUID idempotencyKey
    ) {
        this.idempotencyKey =
                idempotencyKey;
    }


    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(
            String numeroTransaccion
    ) {
        this.numeroTransaccion =
                numeroTransaccion;
    }


    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(
            BigDecimal monto
    ) {
        this.monto =
                monto;
    }


    public String getEstado() {
        return estado;
    }

    public void setEstado(
            String estado
    ) {
        this.estado =
                estado;
    }


    public String getCodigoResultado() {
        return codigoResultado;
    }

    public void setCodigoResultado(
            String codigoResultado
    ) {
        this.codigoResultado =
                codigoResultado;
    }


    public OffsetDateTime getFechaHoraPago() {
        return fechaHoraPago;
    }

    public void setFechaHoraPago(
            OffsetDateTime fechaHoraPago
    ) {
        this.fechaHoraPago =
                fechaHoraPago;
    }


    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }


    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}