package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * ENTIDAD: CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Representa una cita registrada por el wizard
 * inicialmente con estado "Pendiente de pago".
 * =========================================================
 */
@Entity
@Table(name = "cita")
public class Cita {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // PARTICIPANTES Y CATÁLOGOS
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


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_medico",
            nullable = false
    )
    private Usuario medico;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_sucursal",
            nullable = false
    )
    private Sucursal sucursal;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_especialidad",
            nullable = false
    )
    private Especialidad especialidad;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_estado_cita",
            nullable = false
    )
    private EstadoCita estadoCita;


    // =====================================================
    // FECHA Y HORA DE LA CITA
    // =====================================================

    @Column(
            name = "fecha_hora_cita",
            nullable = false
    )
    private OffsetDateTime fechaHoraCita;


    @Column(
            name = "fecha_hora_fin"
    )
    private OffsetDateTime fechaHoraFin;


    // =====================================================
    // EXPIRACIÓN DEL PAGO
    // =====================================================

    @Column(
            name = "fecha_expiracion_pago"
    )
    private OffsetDateTime fechaExpiracionPago;


    // =====================================================
    // CONSULTA
    // =====================================================

    @Column(
            name = "motivo_consulta",
            nullable = false,
            length = 2000
    )
    private String motivoConsulta;


    @Column(
            name = "prioridad",
            nullable = false,
            length = 20
    )
    private String prioridad =
            "Normal";


    @Column(
            name = "canal_origen",
            nullable = false,
            length = 20
    )
    private String canalOrigen =
            "Portal Web";


    // =====================================================
    // RECEPCIÓN / SEGUIMIENTO
    // =====================================================

    @Column(
            name = "hora_llegada"
    )
    private OffsetDateTime horaLlegada;


    @Column(
            name = "es_seguimiento",
            nullable = false
    )
    private Boolean esSeguimiento =
            false;


    @Column(
            name = "tipo_seguimiento",
            length = 50
    )
    private String tipoSeguimiento;


    /*
     * Se mantiene como ID para evitar por ahora una
     * dependencia circular con ConsultaMedica.
     */
    @Column(
            name = "id_consulta_origen"
    )
    private Integer idConsultaOrigen;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private OffsetDateTime fechaCreacion;


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

        if (fechaCreacion == null) {

            fechaCreacion =
                    OffsetDateTime.now();
        }


        if (prioridad == null) {

            prioridad =
                    "Normal";
        }


        if (canalOrigen == null) {

            canalOrigen =
                    "Portal Web";
        }


        if (esSeguimiento == null) {

            esSeguimiento =
                    false;
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


    public Usuario getMedico() {
        return medico;
    }

    public void setMedico(
            Usuario medico
    ) {
        this.medico = medico;
    }


    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal = sucursal;
    }


    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(
            Especialidad especialidad
    ) {
        this.especialidad = especialidad;
    }


    public EstadoCita getEstadoCita() {
        return estadoCita;
    }

    public void setEstadoCita(
            EstadoCita estadoCita
    ) {
        this.estadoCita = estadoCita;
    }


    public OffsetDateTime getFechaHoraCita() {
        return fechaHoraCita;
    }

    public void setFechaHoraCita(
            OffsetDateTime fechaHoraCita
    ) {
        this.fechaHoraCita = fechaHoraCita;
    }


    public OffsetDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(
            OffsetDateTime fechaHoraFin
    ) {
        this.fechaHoraFin = fechaHoraFin;
    }


    public OffsetDateTime getFechaExpiracionPago() {
        return fechaExpiracionPago;
    }

    public void setFechaExpiracionPago(
            OffsetDateTime fechaExpiracionPago
    ) {
        this.fechaExpiracionPago = fechaExpiracionPago;
    }


    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(
            String motivoConsulta
    ) {
        this.motivoConsulta = motivoConsulta;
    }


    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(
            String prioridad
    ) {
        this.prioridad = prioridad;
    }


    public String getCanalOrigen() {
        return canalOrigen;
    }

    public void setCanalOrigen(
            String canalOrigen
    ) {
        this.canalOrigen = canalOrigen;
    }


    public OffsetDateTime getHoraLlegada() {
        return horaLlegada;
    }

    public void setHoraLlegada(
            OffsetDateTime horaLlegada
    ) {
        this.horaLlegada = horaLlegada;
    }


    public Boolean getEsSeguimiento() {
        return esSeguimiento;
    }

    public void setEsSeguimiento(
            Boolean esSeguimiento
    ) {
        this.esSeguimiento = esSeguimiento;
    }


    public String getTipoSeguimiento() {
        return tipoSeguimiento;
    }

    public void setTipoSeguimiento(
            String tipoSeguimiento
    ) {
        this.tipoSeguimiento = tipoSeguimiento;
    }


    public Integer getIdConsultaOrigen() {
        return idConsultaOrigen;
    }

    public void setIdConsultaOrigen(
            Integer idConsultaOrigen
    ) {
        this.idConsultaOrigen = idConsultaOrigen;
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