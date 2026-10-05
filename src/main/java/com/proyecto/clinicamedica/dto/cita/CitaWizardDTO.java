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
 * Reutilizado también por:
 *
 * CU-08 FA02 - Agendar Cita de Seguimiento.
 *
 * Conserva temporalmente las selecciones realizadas
 * durante el asistente de agendamiento.
 *
 * No contiene lógica de negocio.
 *
 * No contiene el archivo PDF.
 *
 * SEGURIDAD:
 *
 * En citas de seguimiento no se confía en paciente,
 * médico, sucursal ni especialidad recibidos desde HTML.
 * Esos datos se obtendrán nuevamente desde la consulta
 * médica de origen.
 *
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
    // CU-08 FA02 - CITA DE SEGUIMIENTO
    // =====================================================

    /*
     * Indica si el wizard fue iniciado desde una
     * consulta médica para crear una cita de seguimiento.
     */
    private Boolean esSeguimiento =
            false;


    /*
     * Identificador de la Consulta Médica que originó
     * el seguimiento.
     *
     * El backend volverá a consultar esta entidad
     * antes de reservar o crear la cita.
     */
    private Integer idConsultaOrigen;


    /*
     * Valores permitidos serán validados posteriormente
     * por el Service.
     *
     * Ejemplos:
     *
     * - MONITOREO_TRATAMIENTO
     * - REVISION_RESULTADOS_LABORATORIO
     */
    private String tipoSeguimiento;


    /*
     * Prioridad de la cita de seguimiento.
     *
     * No se utilizará sin validación del backend.
     */
    private String prioridad;


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


    // =====================================================
    // CU-08 FA02
    // =====================================================

    public Boolean getEsSeguimiento() {
        return esSeguimiento;
    }

    public void setEsSeguimiento(
            Boolean esSeguimiento
    ) {

        this.esSeguimiento =
                esSeguimiento;
    }


    public Integer getIdConsultaOrigen() {
        return idConsultaOrigen;
    }

    public void setIdConsultaOrigen(
            Integer idConsultaOrigen
    ) {

        this.idConsultaOrigen =
                idConsultaOrigen;
    }


    public String getTipoSeguimiento() {
        return tipoSeguimiento;
    }

    public void setTipoSeguimiento(
            String tipoSeguimiento
    ) {

        this.tipoSeguimiento =
                tipoSeguimiento;
    }


    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(
            String prioridad
    ) {

        this.prioridad =
                prioridad;
    }
}