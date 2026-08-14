package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: RESERVA TEMPORAL DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Representa un horario bloqueado temporalmente mientras
 * el paciente completa la confirmación/pago.
 *
 * Permite:
 *
 * - evitar doble reserva;
 * - controlar expiración;
 * - liberar el horario;
 * - cumplir FA03 y FA04.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "reserva_temporal_cita"
)
public class ReservaTemporalCita {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // PACIENTE
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_paciente",
            nullable = false
    )
    private Usuario paciente;


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
    // ESPECIALIDAD
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_especialidad",
            nullable = false
    )
    private Especialidad especialidad;


    // =====================================================
    // HORARIO
    // =====================================================

    @Column(
            name = "fecha_hora_inicio",
            nullable = false
    )
    private OffsetDateTime fechaHoraInicio;


    @Column(
            name = "fecha_hora_fin",
            nullable = false
    )
    private OffsetDateTime fechaHoraFin;


    // =====================================================
    // TOKEN
    // =====================================================

    @Column(
            name = "token_reserva",
            nullable = false,
            length = 100,
            unique = true
    )
    private String tokenReserva;


    // =====================================================
    // EXPIRACIÓN
    // =====================================================

    @Column(
            name = "fecha_expiracion",
            nullable = false
    )
    private OffsetDateTime fechaExpiracion;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "activa",
            nullable = false
    )
    private Boolean activa =
            true;


    // =====================================================
    // FECHA DE CREACIÓN
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private OffsetDateTime fechaCreacion;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        if (fechaCreacion == null) {

            fechaCreacion =
                    OffsetDateTime.now();
        }


        if (activa == null) {

            activa =
                    true;
        }
    }


    // =====================================================
    // COMPORTAMIENTO
    // =====================================================

    /**
     * Indica si la reserva ya expiró.
     */
    @Transient
    public boolean estaExpirada() {

        return fechaExpiracion != null
                && !fechaExpiracion.isAfter(
                OffsetDateTime.now()
        );
    }


    /**
     * Libera lógicamente el horario.
     *
     * Nunca eliminamos físicamente la reserva.
     */
    public void liberar() {

        this.activa =
                false;
    }


    // =====================================================
    // GETTERS Y SETTERS
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
        this.paciente =
                paciente;
    }


    public Usuario getMedico() {
        return medico;
    }

    public void setMedico(
            Usuario medico
    ) {
        this.medico =
                medico;
    }


    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal =
                sucursal;
    }


    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(
            Especialidad especialidad
    ) {
        this.especialidad =
                especialidad;
    }


    public OffsetDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(
            OffsetDateTime fechaHoraInicio
    ) {
        this.fechaHoraInicio =
                fechaHoraInicio;
    }


    public OffsetDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(
            OffsetDateTime fechaHoraFin
    ) {
        this.fechaHoraFin =
                fechaHoraFin;
    }


    public String getTokenReserva() {
        return tokenReserva;
    }

    public void setTokenReserva(
            String tokenReserva
    ) {
        this.tokenReserva =
                tokenReserva;
    }


    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(
            OffsetDateTime fechaExpiracion
    ) {
        this.fechaExpiracion =
                fechaExpiracion;
    }


    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(
            Boolean activa
    ) {
        this.activa =
                activa;
    }


    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}