
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * =========================================================
 * CU-16 - DTO: FORMULARIO DE TAREAS DE AGENDA
 * =========================================================
 *
 * FA06 - GESTIÓN DE TAREAS DESDE EL TASKPANEL.
 *
 * Permite recibir y validar los datos necesarios
 * para crear tareas personales del médico.
 *
 * REGLAS DE NEGOCIO:
 *
 * - Título obligatorio: 5 a 200 caracteres.
 * - Descripción opcional: máximo 1000 caracteres.
 * - Prioridad obligatoria:
 *       0 = Baja
 *       1 = Normal
 *       2 = Alta
 * - Fecha límite obligatoria.
 *
 * SEGURIDAD:
 *
 * - No recibe el ID del médico.
 * - El médico se obtiene desde la autenticación.
 * - No permite establecer directamente el estado
 *   de una tarea recién creada.
 * - Una tarea nueva inicia como Pendiente.
 *
 * IMPORTANTE:
 *
 * - No representa una entidad de PostgreSQL.
 * - No modifica citas ni eventos médicos.
 * - Las validaciones también serán comprobadas
 *   desde la capa Service.
 *
 * =========================================================
 */
public class TareaAgendaFormularioDTO {

    // =====================================================
    // TÍTULO
    // =====================================================

    @NotBlank(
            message = "El título de la tarea es obligatorio."
    )
    @Size(
            min = 5,
            max = 200,
            message = "El título debe contener entre 5 y 200 caracteres."
    )
    private String titulo;

    // =====================================================
    // DESCRIPCIÓN
    // =====================================================

    @Size(
            max = 1000,
            message = "La descripción no puede exceder los 1000 caracteres."
    )
    private String descripcion;

    // =====================================================
    // PRIORIDAD
    // =====================================================

    @NotNull(
            message = "Debe seleccionar una prioridad."
    )
    @Min(
            value = 0,
            message = "La prioridad seleccionada no es válida."
    )
    @Max(
            value = 2,
            message = "La prioridad seleccionada no es válida."
    )
    private Integer prioridad;

    // =====================================================
    // FECHA LÍMITE
    // =====================================================

    @NotNull(
            message = "La fecha límite es obligatoria."
    )
    private LocalDate fechaLimite;

    // =====================================================
    // CONSTRUCTOR VACÍO
    // =====================================================

    public TareaAgendaFormularioDTO() {
    }

    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

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
}
