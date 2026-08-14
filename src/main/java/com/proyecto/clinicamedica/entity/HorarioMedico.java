package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: HORARIO MÉDICO
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Representa el horario habitual de atención de un médico.
 *
 * Ejemplo:
 *
 * Médico: Dr. Juan Pérez
 * Día: Lunes
 * Inicio: 08:00
 * Fin: 12:00
 * Duración cita: 30 minutos
 *
 * Esta tabla NO representa citas ni bloqueos.
 *
 * Los bloqueos, vacaciones y eventos se administran
 * mediante evento_agenda.
 * =========================================================
 */
@Entity
@Table(
        name = "horario_medico"
)
public class HorarioMedico {


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
    // DÍA DE LA SEMANA
    // =====================================================
    //
    // 1 = Lunes
    // 2 = Martes
    // 3 = Miércoles
    // 4 = Jueves
    // 5 = Viernes
    // 6 = Sábado
    // 7 = Domingo
    // =====================================================

    @Column(
            name = "dia_semana",
            nullable = false
    )
    private Short diaSemana;


    // =====================================================
    // HORARIO
    // =====================================================

    @Column(
            name = "hora_inicio",
            nullable = false
    )
    private LocalTime horaInicio;


    @Column(
            name = "hora_fin",
            nullable = false
    )
    private LocalTime horaFin;


    // =====================================================
    // DURACIÓN DE CITA
    // =====================================================

    @Column(
            name = "duracion_cita_minutos",
            nullable = false
    )
    private Short duracionCitaMinutos =
            30;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo =
            true;


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


        if (activo == null) {

            activo =
                    true;
        }


        if (duracionCitaMinutos == null) {

            duracionCitaMinutos =
                    30;
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
        this.medico =
                medico;
    }


    public Short getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(
            Short diaSemana
    ) {
        this.diaSemana =
                diaSemana;
    }


    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(
            LocalTime horaInicio
    ) {
        this.horaInicio =
                horaInicio;
    }


    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(
            LocalTime horaFin
    ) {
        this.horaFin =
                horaFin;
    }


    public Short getDuracionCitaMinutos() {
        return duracionCitaMinutos;
    }

    public void setDuracionCitaMinutos(
            Short duracionCitaMinutos
    ) {
        this.duracionCitaMinutos =
                duracionCitaMinutos;
    }


    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo =
                activo;
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
        this.creadoPor =
                creadoPor;
    }


    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(
            OffsetDateTime fechaModificacion
    ) {
        this.fechaModificacion =
                fechaModificacion;
    }


    public Usuario getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(
            Usuario modificadoPor
    ) {
        this.modificadoPor =
                modificadoPor;
    }
}