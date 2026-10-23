
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * =========================================================
 * CU-16 - ENTIDAD: TAREA DE AGENDA MÉDICA
 * =========================================================
 *
 * Representa las tareas y recordatorios personales
 * del médico dentro del TaskPanel.
 *
 * FUNCIONALIDADES:
 * - Crear tareas.
 * - Consultar tareas por médico y fecha.
 * - Filtrar pendientes y completadas.
 * - Completar y descompletar tareas.
 * - Eliminar tareas.
 *
 * REGLAS:
 * - Título obligatorio: 5 a 200 caracteres.
 * - Descripción opcional: máximo 1000 caracteres.
 * - Prioridad obligatoria:
 *      0 = Baja
 *      1 = Normal
 *      2 = Alta
 * - Fecha límite obligatoria.
 * - Estado inicial: Pendiente (false).
 *
 * SEGURIDAD:
 * - Cada tarea pertenece a un médico.
 * - El Service validará al médico autenticado.
 *
 * No modifica citas ni eventos del CU-03.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "tarea_agenda",
        indexes = {
                @Index(
                        name = "idx_tarea_agenda_medico_fecha",
                        columnList = "id_medico, fecha_limite"
                )
        }
)
public class TareaAgenda {

    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // =====================================================
    // MÉDICO RESPONSABLE
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
    // TÍTULO
    // =====================================================

    @Column(
            name = "titulo",
            nullable = false,
            length = 200
    )
    private String titulo;

    // =====================================================
    // DESCRIPCIÓN
    // =====================================================

    @Column(
            name = "descripcion",
            length = 1000
    )
    private String descripcion;

    // =====================================================
    // PRIORIDAD
    // =====================================================
    //
    // 0 = Baja
    // 1 = Normal
    // 2 = Alta
    //
    // =====================================================

    @Column(
            name = "prioridad",
            nullable = false
    )
    private Integer prioridad;

    // =====================================================
    // FECHA LÍMITE
    // =====================================================
    //
    // Se utiliza LocalDate porque el TaskPanel
    // organiza las tareas por día.
    //
    // =====================================================

    @Column(
            name = "fecha_limite",
            nullable = false
    )
    private LocalDate fechaLimite;

    // =====================================================
    // ESTADO DE LA TAREA
    // =====================================================
    //
    // false = Pendiente
    // true  = Completada
    //
    // =====================================================

    @Column(
            name = "is_completed",
            nullable = false
    )
    private Boolean isCompleted = false;

    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaCreacion;

    @Column(
            name = "fecha_modificacion"
    )
    private OffsetDateTime fechaModificacion;

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
    // VALIDACIONES
    // =====================================================

    @PrePersist
    protected void prePersist() {

        if (isCompleted == null) {
            isCompleted = false;
        }

        validar();

        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void preUpdate() {

        validar();

        fechaModificacion = OffsetDateTime.now();
    }

    private void validar() {

        if (medico == null) {
            throw new IllegalArgumentException(
                    "La tarea debe estar asociada a un médico."
            );
        }

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException(
                    "El título de la tarea es obligatorio."
            );
        }

        titulo = titulo.trim();

        if (titulo.length() < 5 || titulo.length() > 200) {
            throw new IllegalArgumentException(
                    "El título debe contener entre 5 y 200 caracteres."
            );
        }

        if (descripcion != null) {

            if (descripcion.length() > 1000) {
                throw new IllegalArgumentException(
                        "La descripción no puede exceder los 1000 caracteres."
                );
            }
        }

        if (prioridad == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una prioridad."
            );
        }

        if (prioridad < 0 || prioridad > 2) {
            throw new IllegalArgumentException(
                    "La prioridad seleccionada no es válida."
            );
        }

        if (fechaLimite == null) {
            throw new IllegalArgumentException(
                    "La fecha límite es obligatoria."
            );
        }

        if (isCompleted == null) {
            throw new IllegalArgumentException(
                    "El estado de la tarea es obligatorio."
            );
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

    public void setMedico(Usuario medico) {
        this.medico = medico;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Integer prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public Long getVersion() {
        return version;
    }
}
