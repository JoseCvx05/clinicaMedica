package com.proyecto.clinicamedica.entity;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;


/**
 * =========================================================
 * ENTIDAD: ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01 Orden de Laboratorio.
 *
 * Representa la cabecera de una orden generada por
 * un Médico después de finalizar la consulta médica.
 *
 * La orden queda asociada a:
 *
 * - Cita.
 * - Consulta médica.
 * - Médico solicitante.
 *
 * Los exámenes se almacenarán posteriormente en:
 *
 * detalle_orden_laboratorio
 *
 * =========================================================
 */
@Entity
@Table(
        name = "orden_laboratorio"
)
public class OrdenLaboratorio {


    // =====================================================
    // ESTADOS
    // =====================================================

    public enum EstadoOrdenLaboratorio {

        PENDIENTE,

        EN_PROCESO,

        COMPLETADA,

        CANCELADA
    }


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private Integer id;


    // =====================================================
    // NÚMERO DE ORDEN
    // =====================================================

    @Column(
            name = "numero_orden",
            nullable = false,
            unique = true,
            length = 40
    )
    private String numeroOrden;


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
    // CONSULTA MÉDICA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_consulta_medica",
            nullable = false
    )
    private ConsultaMedica consultaMedica;


    // =====================================================
    // MÉDICO SOLICITANTE
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_medico",
            nullable = false
    )
    private Usuario medico;


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    @Column(
            name = "observaciones",
            length = 5000
    )
    private String observaciones;


    // =====================================================
    // ESTADO
    // =====================================================

    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private EstadoOrdenLaboratorio estado =
            EstadoOrdenLaboratorio.PENDIENTE;


    // =====================================================
    // FIRMA DIGITAL
    // =====================================================

    /**
     * Hash SHA-256 asociado a la firma digital
     * generada mediante FirmaDigitalMedicaService.
     *
     * RNF-031.
     */
    @Column(
            name = "firma_digital_hash",
            length = 64
    )
    private String firmaDigitalHash;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_hora_creacion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHoraCreacion;


    @Column(
            name = "fecha_hora_actualizacion",
            nullable = false
    )
    private OffsetDateTime fechaHoraActualizacion;


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

    public OrdenLaboratorio() {
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaHoraCreacion == null) {

            fechaHoraCreacion =
                    ahora;
        }


        fechaHoraActualizacion =
                ahora;


        if (estado == null) {

            estado =
                    EstadoOrdenLaboratorio.PENDIENTE;
        }


        if (numeroOrden == null
                || numeroOrden.isBlank()) {

            numeroOrden =
                    generarNumeroOrden(
                            ahora
                    );
        }


        normalizar();

        validar();
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    private void prepararActualizacion() {

        fechaHoraActualizacion =
                OffsetDateTime.now();


        normalizar();

        validar();
    }


    // =====================================================
    // VALIDACIÓN DE DOMINIO
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // CITA
        // -------------------------------------------------

        if (cita == null
                || cita.getId() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe estar asociada a una cita válida."
            );
        }


        // -------------------------------------------------
        // CONSULTA
        // -------------------------------------------------

        if (consultaMedica == null
                || consultaMedica.getId() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe estar asociada a una consulta médica válida."
            );
        }


        if (consultaMedica.getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada antes de generar una orden de laboratorio."
            );
        }


        // -------------------------------------------------
        // MISMA CITA
        // -------------------------------------------------

        if (consultaMedica.getCita() == null
                || consultaMedica.getCita().getId() == null
                || !consultaMedica.getCita()
                .getId()
                .equals(
                        cita.getId()
                )) {

            throw new IllegalStateException(
                    "La consulta médica no corresponde a la cita seleccionada."
            );
        }


        // -------------------------------------------------
        // MÉDICO
        // -------------------------------------------------

        if (medico == null
                || medico.getId() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe tener un médico solicitante válido."
            );
        }


        if (consultaMedica.getMedico() == null
                || consultaMedica.getMedico().getId() == null
                || !consultaMedica.getMedico()
                .getId()
                .equals(
                        medico.getId()
                )) {

            throw new IllegalStateException(
                    "La orden debe ser generada por el mismo médico responsable de la consulta."
            );
        }


        // -------------------------------------------------
        // NÚMERO DE ORDEN
        // -------------------------------------------------

        if (numeroOrden == null
                || numeroOrden.isBlank()) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe tener un número de orden."
            );
        }


        if (numeroOrden.length() > 40) {

            throw new IllegalStateException(
                    "El número de orden supera la longitud permitida."
            );
        }


        // -------------------------------------------------
        // OBSERVACIONES
        // -------------------------------------------------

        if (observaciones != null
                && observaciones.length() > 5000) {

            throw new IllegalStateException(
                    "Las observaciones no pueden superar los 5000 caracteres."
            );
        }


        // -------------------------------------------------
        // ESTADO
        // -------------------------------------------------

        if (estado == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe tener un estado válido."
            );
        }


        // -------------------------------------------------
        // FIRMA
        // -------------------------------------------------

        if (firmaDigitalHash != null
                && firmaDigitalHash.length() != 64) {

            throw new IllegalStateException(
                    "El hash de la firma digital de la orden no es válido."
            );
        }
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        if (numeroOrden != null) {

            numeroOrden =
                    numeroOrden
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );
        }


        if (observaciones != null) {

            observaciones =
                    observaciones.trim();

            if (observaciones.isBlank()) {

                observaciones = null;
            }
        }


        if (firmaDigitalHash != null) {

            firmaDigitalHash =
                    firmaDigitalHash
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (firmaDigitalHash.isBlank()) {

                firmaDigitalHash = null;
            }
        }
    }


    // =====================================================
    // GENERAR NÚMERO DE ORDEN
    // =====================================================

    private String generarNumeroOrden(
            OffsetDateTime fecha
    ) {

        String fechaTexto =
                fecha.format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMdd"
                        )
                );


        String aleatorio =
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
                        .substring(
                                0,
                                8
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );


        return "LAB-"
                + fechaTexto
                + "-"
                + aleatorio;
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaPendiente() {

        return estado
                == EstadoOrdenLaboratorio.PENDIENTE;
    }


    public boolean estaCompletada() {

        return estado
                == EstadoOrdenLaboratorio.COMPLETADA;
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    public void setId(
            Integer id
    ) {

        this.id = id;
    }


    public String getNumeroOrden() {

        return numeroOrden;
    }


    public void setNumeroOrden(
            String numeroOrden
    ) {

        this.numeroOrden = numeroOrden;
    }


    public Cita getCita() {

        return cita;
    }


    public void setCita(
            Cita cita
    ) {

        this.cita = cita;
    }


    public ConsultaMedica getConsultaMedica() {

        return consultaMedica;
    }


    public void setConsultaMedica(
            ConsultaMedica consultaMedica
    ) {

        this.consultaMedica =
                consultaMedica;
    }


    public Usuario getMedico() {

        return medico;
    }


    public void setMedico(
            Usuario medico
    ) {

        this.medico = medico;
    }


    public String getObservaciones() {

        return observaciones;
    }


    public void setObservaciones(
            String observaciones
    ) {

        this.observaciones =
                observaciones;
    }


    public EstadoOrdenLaboratorio getEstado() {

        return estado;
    }


    public void setEstado(
            EstadoOrdenLaboratorio estado
    ) {

        this.estado = estado;
    }


    public String getFirmaDigitalHash() {

        return firmaDigitalHash;
    }


    public void setFirmaDigitalHash(
            String firmaDigitalHash
    ) {

        this.firmaDigitalHash =
                firmaDigitalHash;
    }


    public OffsetDateTime getFechaHoraCreacion() {

        return fechaHoraCreacion;
    }


    public void setFechaHoraCreacion(
            OffsetDateTime fechaHoraCreacion
    ) {

        this.fechaHoraCreacion =
                fechaHoraCreacion;
    }


    public OffsetDateTime getFechaHoraActualizacion() {

        return fechaHoraActualizacion;
    }


    public void setFechaHoraActualizacion(
            OffsetDateTime fechaHoraActualizacion
    ) {

        this.fechaHoraActualizacion =
                fechaHoraActualizacion;
    }


    public Long getVersion() {

        return version;
    }


    public void setVersion(
            Long version
    ) {

        this.version = version;
    }
}