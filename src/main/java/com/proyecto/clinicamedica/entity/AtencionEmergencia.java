package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: ATENCIÓN DE EMERGENCIA
 * =========================================================
 *
 * CU-05 - FA01.
 *
 * Representa el ingreso inmediato de una persona por
 * emergencia.
 *
 * Puede corresponder a:
 *
 * 1. Un paciente que ya posee cuenta en el sistema.
 * 2. Una persona todavía no registrada completamente.
 *
 * Por esa razón id_paciente puede ser NULL.
 *
 * El DPI nunca se almacena en texto plano:
 *
 * - dpi_cifrado -> recuperación controlada.
 * - dpi_hash     -> búsqueda.
 *
 * =========================================================
 */
@Entity
@Table(name = "atencion_emergencia")
public class AtencionEmergencia {


    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // PACIENTE REGISTRADO
    // =====================================================

    /**
     * Puede ser null cuando la persona todavía no posee
     * una cuenta completa de paciente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente")
    private Usuario paciente;


    // =====================================================
    // SNAPSHOT DEL NOMBRE
    // =====================================================

    @Column(
            name = "nombre_paciente",
            nullable = false,
            length = 100
    )
    private String nombrePaciente;


    // =====================================================
    // DPI PROTEGIDO
    // =====================================================

    @Column(
            name = "dpi_cifrado",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String dpiCifrado;


    @Column(
            name = "dpi_hash",
            nullable = false,
            length = 64
    )
    private String dpiHash;


    // =====================================================
    // SUCURSAL
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_sucursal",
            nullable = false
    )
    private Sucursal sucursal;


    // =====================================================
    // PRIORIDAD
    // =====================================================

    @Column(
            name = "prioridad",
            nullable = false,
            length = 20
    )
    private String prioridad =
            "Emergencia";


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "estado",
            nullable = false,
            length = 50
    )
    private String estado =
            "Pendiente de signos vitales";


    // =====================================================
    // HORA DE LLEGADA
    // =====================================================

    @Column(
            name = "fecha_hora_llegada",
            nullable = false
    )
    private OffsetDateTime fechaHoraLlegada;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaCreacion;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaHoraLlegada == null) {

            fechaHoraLlegada =
                    ahora;
        }


        if (fechaCreacion == null) {

            fechaCreacion =
                    ahora;
        }


        if (prioridad == null) {

            prioridad =
                    "Emergencia";
        }


        if (estado == null) {

            estado =
                    "Pendiente de signos vitales";
        }
    }


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public Usuario getPaciente() {
        return paciente;
    }


    public void setPaciente(
            Usuario paciente
    ) {
        this.paciente = paciente;
    }


    public String getNombrePaciente() {
        return nombrePaciente;
    }


    public void setNombrePaciente(
            String nombrePaciente
    ) {
        this.nombrePaciente = nombrePaciente;
    }


    public String getDpiCifrado() {
        return dpiCifrado;
    }


    public void setDpiCifrado(
            String dpiCifrado
    ) {
        this.dpiCifrado = dpiCifrado;
    }


    public String getDpiHash() {
        return dpiHash;
    }


    public void setDpiHash(
            String dpiHash
    ) {
        this.dpiHash = dpiHash;
    }


    public Sucursal getSucursal() {
        return sucursal;
    }


    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal = sucursal;
    }


    public String getPrioridad() {
        return prioridad;
    }


    public void setPrioridad(
            String prioridad
    ) {
        this.prioridad = prioridad;
    }


    public String getEstado() {
        return estado;
    }


    public void setEstado(
            String estado
    ) {
        this.estado = estado;
    }


    public OffsetDateTime getFechaHoraLlegada() {
        return fechaHoraLlegada;
    }


    public void setFechaHoraLlegada(
            OffsetDateTime fechaHoraLlegada
    ) {
        this.fechaHoraLlegada = fechaHoraLlegada;
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
}