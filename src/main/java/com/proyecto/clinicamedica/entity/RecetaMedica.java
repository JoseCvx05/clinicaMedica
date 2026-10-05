package com.proyecto.clinicamedica.entity;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.Objects;


/**
 * =========================================================
 * ENTIDAD: RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04 Receta Médica.
 *
 * Representa la cabecera de una receta emitida por
 * un Médico después de finalizar la consulta.
 *
 * La receta queda asociada a:
 *
 * - Cita.
 * - Consulta médica.
 * - Médico tratante.
 *
 * Los medicamentos se almacenarán en:
 *
 * detalle_receta_medica
 *
 * =========================================================
 */
@Entity
@Table(
        name = "receta_medica"
)
public class RecetaMedica {


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
    // MÉDICO
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
    // FECHA DE EMISIÓN
    // =====================================================

    @Column(
            name = "fecha_emision",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaEmision;


    // =====================================================
    // ACTIVO
    // =====================================================

    /**
     * Farmacia utilizará este campo para identificar
     * recetas disponibles para despacho.
     */
    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo =
            true;


    // =====================================================
    // NOTAS
    // =====================================================

    @Column(
            name = "notas",
            length = 1000
    )
    private String notas;


    // =====================================================
    // FIRMA DIGITAL
    // =====================================================

    /**
     * Hash SHA-256 del contenido firmado.
     *
     * RNF-031.
     */
    @Column(
            name = "firma_digital_hash",
            length = 64
    )
    private String firmaDigitalHash;


    // =====================================================
    // FECHAS DE CONTROL
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

    public RecetaMedica() {
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaEmision == null) {

            fechaEmision =
                    ahora;
        }


        if (fechaHoraCreacion == null) {

            fechaHoraCreacion =
                    ahora;
        }


        fechaHoraActualizacion =
                ahora;


        if (activo == null) {

            activo =
                    true;
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
    // VALIDACIÓN
    // =====================================================

    public void validar() {


        // -------------------------------------------------
        // CITA
        // -------------------------------------------------

        if (cita == null
                || cita.getId() == null) {

            throw new IllegalStateException(
                    "La receta médica debe estar asociada a una cita válida."
            );
        }


        // -------------------------------------------------
        // CONSULTA
        // -------------------------------------------------

        if (consultaMedica == null
                || consultaMedica.getId() == null) {

            throw new IllegalStateException(
                    "La receta médica debe estar asociada a una consulta válida."
            );
        }


        // -------------------------------------------------
        // CONSULTA FINALIZADA
        // -------------------------------------------------

        if (consultaMedica.getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada antes de generar una receta."
            );
        }


        // -------------------------------------------------
        // CONSULTA Y RECETA DEBEN SER DE LA MISMA CITA
        // -------------------------------------------------

        if (consultaMedica.getCita() == null
                || consultaMedica.getCita().getId() == null
                || !Objects.equals(
                consultaMedica.getCita().getId(),
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
                    "La receta médica debe tener un médico válido."
            );
        }


        // -------------------------------------------------
        // MISMO MÉDICO DE LA CONSULTA
        // -------------------------------------------------

        if (consultaMedica.getMedico() == null
                || consultaMedica.getMedico().getId() == null
                || !Objects.equals(
                consultaMedica.getMedico().getId(),
                medico.getId()
        )) {

            throw new IllegalStateException(
                    "La receta debe ser emitida por el mismo médico responsable de la consulta."
            );
        }


        // -------------------------------------------------
        // ACTIVO
        // -------------------------------------------------

        if (activo == null) {

            throw new IllegalStateException(
                    "El estado de la receta médica es obligatorio."
            );
        }


        // -------------------------------------------------
        // NOTAS
        // -------------------------------------------------

        if (notas != null
                && notas.length() > 1000) {

            throw new IllegalStateException(
                    "Las notas de la receta no pueden superar los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // FIRMA
        // -------------------------------------------------

        if (firmaDigitalHash != null
                && firmaDigitalHash.length() != 64) {

            throw new IllegalStateException(
                    "El hash de la firma digital de la receta no es válido."
            );
        }
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        if (notas != null) {

            notas =
                    notas.trim();


            if (notas.isBlank()) {

                notas =
                        null;
            }
        }


        if (firmaDigitalHash != null) {

            firmaDigitalHash =
                    firmaDigitalHash
                            .trim()
                            .toLowerCase();


            if (firmaDigitalHash.isBlank()) {

                firmaDigitalHash =
                        null;
            }
        }
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaActiva() {

        return Boolean.TRUE.equals(
                activo
        );
    }


    public void desactivar() {

        this.activo =
                false;
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


    public OffsetDateTime getFechaEmision() {

        return fechaEmision;
    }


    public void setFechaEmision(
            OffsetDateTime fechaEmision
    ) {

        this.fechaEmision =
                fechaEmision;
    }


    public Boolean getActivo() {

        return activo;
    }


    public void setActivo(
            Boolean activo
    ) {

        this.activo = activo;
    }


    public String getNotas() {

        return notas;
    }


    public void setNotas(
            String notas
    ) {

        this.notas = notas;
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