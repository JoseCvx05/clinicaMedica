
package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * =========================================================
 * CU-16 - DTO: FORMULARIO DE EVENTOS DE AGENDA
 * =========================================================
 *
 * FLUJO NORMAL:
 * - Crear eventos médicos.
 *
 * FA03:
 * - Validación de campos.
 *
 * FA07:
 * - Edición de eventos.
 *
 * REGLAS DE NEGOCIO:
 *
 * - Título obligatorio: entre 5 y 200 caracteres.
 * - Descripción opcional: máximo 500 caracteres.
 * - Fecha de inicio obligatoria.
 * - Fecha de fin obligatoria.
 * - Fecha de fin posterior a la de inicio.
 *
 * TIPOS DE EVENTO:
 *
 * 0 = Reunión
 * 1 = Descanso
 * 2 = Capacitación
 * 3 = Personal
 * 4 = Otro
 *
 * TODO EL DÍA:
 *
 * false:
 *    Las fechas incluyen hora.
 *
 * true:
 *    Las fechas contienen solamente el día.
 *
 * SEGURIDAD:
 *
 * - No recibe ID del médico.
 * - No recibe ID del propietario.
 * - No permite modificar libremente el color.
 *
 * El médico se obtiene del usuario autenticado.
 *
 * =========================================================
 */
public class EventoAgendaFormularioDTO {

    // =====================================================
    // TÍTULO
    // =====================================================

    @NotBlank(
            message = "El título del evento es obligatorio."
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
            max = 500,
            message = "La descripción no puede exceder los 500 caracteres."
    )
    private String descripcion;

    // =====================================================
    // FECHA DE INICIO
    // =====================================================
    //
    // Si todoElDia = false:
    // yyyy-MM-dd'T'HH:mm
    //
    // Si todoElDia = true:
    // yyyy-MM-dd
    //
    // El Service realizará la conversión segura
    // a America/Guatemala.
    //
    // =====================================================

    @NotBlank(
            message = "La fecha de inicio es obligatoria."
    )
    private String fechaInicio;

    // =====================================================
    // FECHA DE FIN
    // =====================================================

    @NotBlank(
            message = "La fecha de fin es obligatoria."
    )
    private String fechaFin;

    // =====================================================
    // TIPO DE EVENTO
    // =====================================================

    @NotNull(
            message = "Debe seleccionar un tipo de evento."
    )
    @Min(
            value = 0,
            message = "El tipo de evento seleccionado no es válido."
    )
    @Max(
            value = 4,
            message = "El tipo de evento seleccionado no es válido."
    )
    private Integer tipoEvento;

    // =====================================================
    // EVENTO DE TODO EL DÍA
    // =====================================================

    @NotNull(
            message = "Debe indicar si el evento es de todo el día."
    )
    private Boolean todoElDia = false;

    // =====================================================
    // CONSTRUCTOR VACÍO
    // =====================================================

    public EventoAgendaFormularioDTO() {
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

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(Integer tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Boolean getTodoElDia() {
        return todoElDia;
    }

    public void setTodoElDia(Boolean todoElDia) {
        this.todoElDia = todoElDia;
    }
}
