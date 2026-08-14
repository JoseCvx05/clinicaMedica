package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * ENTIDAD: DOCUMENTO ADJUNTO DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Registra la metadata de un documento PDF asociado
 * a una cita.
 *
 * El archivo físico se almacena fuera de PostgreSQL.
 * =========================================================
 */
@Entity
@Table(name = "cita_documento_adjunto")
public class CitaDocumentoAdjunto {


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
    // ARCHIVO
    // =====================================================

    @Column(
            name = "nombre_archivo",
            nullable = false,
            length = 255
    )
    private String nombreArchivo;


    @Column(
            name = "ruta_archivo",
            nullable = false,
            length = 500
    )
    private String rutaArchivo;


    @Column(
            name = "tipo_mime",
            nullable = false,
            length = 50
    )
    private String tipoMime =
            "application/pdf";


    @Column(
            name = "tamano_bytes",
            nullable = false
    )
    private Integer tamanoBytes;


    @Column(
            name = "estado_validacion",
            length = 20
    )
    private String estadoValidacion;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_carga",
            nullable = false
    )
    private OffsetDateTime fechaCarga;


    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private OffsetDateTime fechaCreacion;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;


    @Column(
            name = "fecha_modificacion"
    )
    private OffsetDateTime fechaModificacion;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modificado_por")
    private Usuario modificadoPor;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaCarga == null) {
            fechaCarga = ahora;
        }


        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }


        if (tipoMime == null
                || tipoMime.isBlank()) {

            tipoMime =
                    "application/pdf";
        }
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


    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(
            String nombreArchivo
    ) {
        this.nombreArchivo = nombreArchivo;
    }


    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(
            String rutaArchivo
    ) {
        this.rutaArchivo = rutaArchivo;
    }


    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(
            String tipoMime
    ) {
        this.tipoMime = tipoMime;
    }


    public Integer getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(
            Integer tamanoBytes
    ) {
        this.tamanoBytes = tamanoBytes;
    }


    public String getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(
            String estadoValidacion
    ) {
        this.estadoValidacion = estadoValidacion;
    }


    public OffsetDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(
            OffsetDateTime fechaCarga
    ) {
        this.fechaCarga = fechaCarga;
    }


    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }


    public Usuario getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(
            Usuario creadoPor
    ) {
        this.creadoPor = creadoPor;
    }


    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(
            OffsetDateTime fechaModificacion
    ) {
        this.fechaModificacion = fechaModificacion;
    }


    public Usuario getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(
            Usuario modificadoPor
    ) {
        this.modificadoPor = modificadoPor;
    }
}