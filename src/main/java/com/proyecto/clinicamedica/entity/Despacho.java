package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * =========================================================
 * ENTIDAD: DESPACHO DE MEDICAMENTOS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Registra cada operación realizada por Farmacia.
 *
 * Un despacho está relacionado con:
 *
 * - Receta médica.
 * - Farmacéutico responsable.
 * - Sucursal.
 * - Fecha y hora.
 * - Monto total.
 * - Estado del despacho.
 *
 * FLUJOS:
 *
 * Normal: medicamentos entregados.
 * FA03: paciente no adquiere los medicamentos.
 *
 * IMPORTANTE:
 *
 * El cobro físico es externo al sistema.
 * Esta entidad NO crea pagos ni transacciones POS.
 *
 * Las validaciones de receta, medicamentos,
 * cantidades, autorizaciones y sucursal se harán
 * obligatoriamente en el Service del backend.
 *
 * =========================================================
 */
@Entity
@Table(name = "despacho")
public class Despacho {

    // =====================================================
    // ESTADOS DEL DESPACHO
    // =====================================================

    public enum EstadoDespacho {

        CONFIRMADO,

        NO_ADQUIRIDO
    }


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // RECETA MÉDICA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_receta_medica",
            nullable = false,
            updatable = false
    )
    private RecetaMedica recetaMedica;


    // =====================================================
    // FARMACÉUTICO RESPONSABLE
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_farmaceutico",
            nullable = false,
            updatable = false
    )
    private Usuario farmaceutico;


    // =====================================================
    // SUCURSAL
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_sucursal",
            nullable = false,
            updatable = false
    )
    private Sucursal sucursal;


    // =====================================================
    // ESTADO
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "estado",
            nullable = false,
            length = 30,
            updatable = false
    )
    private EstadoDespacho estado;


    // =====================================================
    // MONTO TOTAL DEL DESPACHO
    // =====================================================

    @Column(
            name = "monto_total",
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal montoTotal;


    // =====================================================
    // FECHA DE REGISTRO
    // =====================================================

    @Column(
            name = "fecha_hora_registro",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHoraRegistro;


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    @Column(
            name = "observaciones",
            length = 1000,
            updatable = false
    )
    private String observaciones;


    // =====================================================
    // CLAVE ÚNICA DE OPERACIÓN
    // PROTECCIÓN CONTRA REGISTROS DUPLICADOS
    // =====================================================

    @Column(
            name = "clave_operacion",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID claveOperacion;


    // =====================================================
    // CONTROL DE CONCURRENCIA
    // =====================================================

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Despacho() {
    }


    // =====================================================
    // CONSTRUCTOR DE DOMINIO
    // =====================================================

    public Despacho(
            RecetaMedica recetaMedica,
            Usuario farmaceutico,
            Sucursal sucursal,
            EstadoDespacho estado,
            BigDecimal montoTotal,
            String observaciones,
            UUID claveOperacion
    ) {

        this.recetaMedica = recetaMedica;
        this.farmaceutico = farmaceutico;
        this.sucursal = sucursal;
        this.estado = estado;
        this.montoTotal = montoTotal;
        this.observaciones = observaciones;
        this.claveOperacion = claveOperacion;

        normalizar();
        validar();
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        if (fechaHoraRegistro == null) {

            fechaHoraRegistro =
                    OffsetDateTime.now();
        }

        normalizar();

        validar();
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizar() {

        if (observaciones != null) {

            observaciones =
                    observaciones.trim();

            if (observaciones.isBlank()) {

                observaciones = null;
            }
        }
    }


    // =====================================================
    // VALIDACIONES BACKEND
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // RECETA MÉDICA OBLIGATORIA
        // -------------------------------------------------

        if (recetaMedica == null
                || recetaMedica.getId() == null) {

            throw new IllegalStateException(
                    "El despacho debe estar asociado a una receta médica válida."
            );
        }


        // -------------------------------------------------
        // FARMACÉUTICO OBLIGATORIO
        // -------------------------------------------------

        if (farmaceutico == null
                || farmaceutico.getId() == null) {

            throw new IllegalStateException(
                    "El despacho debe tener un farmacéutico responsable."
            );
        }


        // -------------------------------------------------
        // SUCURSAL OBLIGATORIA
        // -------------------------------------------------

        if (sucursal == null
                || sucursal.getId() == null) {

            throw new IllegalStateException(
                    "El despacho debe pertenecer a una sucursal válida."
            );
        }


        // -------------------------------------------------
        // ESTADO OBLIGATORIO
        // -------------------------------------------------

        if (estado == null) {

            throw new IllegalStateException(
                    "El estado del despacho es obligatorio."
            );
        }


        // -------------------------------------------------
        // MONTO TOTAL OBLIGATORIO
        // -------------------------------------------------

        if (montoTotal == null) {

            throw new IllegalStateException(
                    "El monto total del despacho es obligatorio."
            );
        }


        // -------------------------------------------------
        // NO PERMITIR MONTOS NEGATIVOS
        // -------------------------------------------------

        if (montoTotal.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalStateException(
                    "El monto total del despacho no puede ser negativo."
            );
        }


        // -------------------------------------------------
        // MÁXIMO DOS DECIMALES
        // -------------------------------------------------

        if (montoTotal
                .stripTrailingZeros()
                .scale() > 2) {

            throw new IllegalStateException(
                    "El monto total debe tener como máximo dos decimales."
            );
        }


        // -------------------------------------------------
        // FA03 - PACIENTE NO ADQUIERE MEDICAMENTOS
        // -------------------------------------------------

        if (estado == EstadoDespacho.NO_ADQUIRIDO
                && montoTotal.compareTo(
                BigDecimal.ZERO
        ) != 0) {

            throw new IllegalStateException(
                    "Un despacho no adquirido debe registrar un monto total de Q0.00."
            );
        }


        // -------------------------------------------------
        // OBSERVACIONES
        // -------------------------------------------------

        if (observaciones != null
                && observaciones.length() > 1000) {

            throw new IllegalStateException(
                    "Las observaciones no pueden superar los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // CLAVE ÚNICA DE OPERACIÓN
        // -------------------------------------------------

        if (claveOperacion == null) {

            throw new IllegalStateException(
                    "La clave de operación del despacho es obligatoria."
            );
        }
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaConfirmado() {

        return estado == EstadoDespacho.CONFIRMADO;
    }


    public boolean noFueAdquirido() {

        return estado == EstadoDespacho.NO_ADQUIRIDO;
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public RecetaMedica getRecetaMedica() {
        return recetaMedica;
    }


    public Usuario getFarmaceutico() {
        return farmaceutico;
    }


    public Sucursal getSucursal() {
        return sucursal;
    }


    public EstadoDespacho getEstado() {
        return estado;
    }


    public BigDecimal getMontoTotal() {
        return montoTotal;
    }


    public OffsetDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }


    public String getObservaciones() {
        return observaciones;
    }


    public UUID getClaveOperacion() {
        return claveOperacion;
    }


    public Long getVersion() {
        return version;
    }
}
