package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * ENTIDAD: EVENTO DE AGENDA
 * =========================================================
 *
 * Representa un evento registrado en la agenda
 * de un médico.
 *
 * Ejemplos:
 *
 * - Bloqueo de disponibilidad.
 * - Evento personal.
 * - Capacitación.
 * - Vacaciones.
 *
 * CU-03 utiliza estos eventos para excluir horarios
 * que no pueden ofrecerse al paciente.
 * =========================================================
 */
@Entity
@Table(name = "evento_agenda")
public class EventoAgenda {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


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
    // INFORMACIÓN DEL EVENTO
    // =====================================================

    @Column(
            name = "titulo",
            nullable = false,
            length = 200
    )
    private String titulo;


    @Column(
            name = "descripcion",
            length = 2000
    )
    private String descripcion;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_inicio",
            nullable = false
    )
    private OffsetDateTime fechaInicio;


    @Column(
            name = "fecha_fin",
            nullable = false
    )
    private OffsetDateTime fechaFin;


    // =====================================================
    // TIPO
    // =====================================================

    @Column(
            name = "tipo_evento",
            nullable = false,
            length = 50
    )
    private String tipoEvento;


    // =====================================================
    // TODO EL DÍA
    // =====================================================

    @Column(
            name = "todo_el_dia",
            nullable = false
    )
    private Boolean todoElDia =
            false;


    // =====================================================
    // COLOR
    // =====================================================

    @Column(
            name = "color",
            nullable = false,
            length = 7
    )
    private String color;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private OffsetDateTime fechaCreacion;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "creado_por"
    )
    private Usuario creadoPor;


    @Column(
            name = "fecha_modificacion"
    )
    private OffsetDateTime fechaModificacion;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "modificado_por"
    )
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


        if (todoElDia == null) {

            todoElDia =
                    false;
        }
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public Usuario getMedico() {
        return medico;
    }

    public void setMedico(
            Usuario medico
    ) {
        this.medico = medico;
    }


    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(
            String titulo
    ) {
        this.titulo = titulo;
    }


    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion
    ) {
        this.descripcion = descripcion;
    }


    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(
            OffsetDateTime fechaInicio
    ) {
        this.fechaInicio = fechaInicio;
    }


    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(
            OffsetDateTime fechaFin
    ) {
        this.fechaFin = fechaFin;
    }


    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(
            String tipoEvento
    ) {
        this.tipoEvento = tipoEvento;
    }


    public Boolean getTodoElDia() {
        return todoElDia;
    }

    public void setTodoElDia(
            Boolean todoElDia
    ) {
        this.todoElDia = todoElDia;
    }


    public String getColor() {
        return color;
    }

    public void setColor(
            String color
    ) {
        this.color = color;
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