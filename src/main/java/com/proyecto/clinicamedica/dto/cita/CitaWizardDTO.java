package com.proyecto.clinicamedica.dto.cita;

import com.proyecto.clinicamedica.model.cita.PasoCita;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * DTO: WIZARD DE AGENDAMIENTO DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Conserva las selecciones realizadas por el paciente
 * durante los 5 pasos del asistente.
 *
 * No contiene lógica de negocio.
 *
 * No contiene el archivo PDF.
 * Los documentos se manejarán mediante un DTO/servicio
 * independiente en el Paso 5.
 * =========================================================
 */
public class CitaWizardDTO {


    // =====================================================
    // PASO ACTUAL
    // =====================================================

    private PasoCita pasoActual =
            PasoCita.SUCURSAL;


    // =====================================================
    // PASO 1 - SUCURSAL
    // =====================================================

    private Integer idSucursal;


    // =====================================================
    // PASO 2 - ESPECIALIDAD
    // =====================================================

    private Integer idEspecialidad;


    // =====================================================
    // PASO 3 - MÉDICO
    // =====================================================

    private Integer idMedico;


    // =====================================================
    // PASO 4 - FECHA Y HORA
    // =====================================================

    private OffsetDateTime fechaHoraInicio;

    private OffsetDateTime fechaHoraFin;


    /*
     * Identifica la reserva temporal creada para
     * el horario seleccionado.
     *
     * No usamos el ID numérico de la reserva en la UI.
     */
    private String tokenReserva;


    /*
     * Permite mostrar el contador de la reserva
     * en el navegador.
     */
    private OffsetDateTime fechaExpiracionReserva;


    // =====================================================
    // PASO 5 - CONFIRMACIÓN
    // =====================================================

    private String motivoConsulta;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CitaWizardDTO() {
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public PasoCita getPasoActual() {
        return pasoActual;
    }

    public void setPasoActual(
            PasoCita pasoActual
    ) {

        this.pasoActual =
                pasoActual;
    }


    public Integer getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(
            Integer idSucursal
    ) {

        this.idSucursal =
                idSucursal;
    }


    public Integer getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(
            Integer idEspecialidad
    ) {

        this.idEspecialidad =
                idEspecialidad;
    }


    public Integer getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(
            Integer idMedico
    ) {

        this.idMedico =
                idMedico;
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


    public OffsetDateTime getFechaExpiracionReserva() {
        return fechaExpiracionReserva;
    }

    public void setFechaExpiracionReserva(
            OffsetDateTime fechaExpiracionReserva
    ) {

        this.fechaExpiracionReserva =
                fechaExpiracionReserva;
    }


    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(
            String motivoConsulta
    ) {

        this.motivoConsulta =
                motivoConsulta;
    }
}