package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Representa el registro clínico de signos vitales
 * asociado a una cita.
 *
 * Una cita puede tener únicamente un registro definitivo
 * de signos vitales.
 *
 * Registra:
 *
 * - Presión sistólica.
 * - Presión diastólica.
 * - Temperatura.
 * - Peso.
 * - Talla.
 * - Frecuencia cardíaca.
 * - Indicador de emergencia.
 * - Alertas clínicas.
 * - Enfermero responsable.
 * - Fecha y hora del registro.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "signos_vitales",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "signos_vitales_id_cita_key",
                        columnNames = "id_cita"
                )
        }
)
public class SignosVitales {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Integer id;


    // =====================================================
    // CITA
    // =====================================================

    /**
     * Cada cita puede tener un único registro
     * definitivo de signos vitales.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_cita",
            nullable = false,
            unique = true
    )
    private Cita cita;


    // =====================================================
    // ENFERMERO
    // =====================================================

    /**
     * Empleado de Enfermería responsable de realizar
     * y registrar la toma de signos vitales.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_enfermero",
            nullable = false
    )
    private Usuario enfermero;


    // =====================================================
    // PRESIÓN ARTERIAL
    // =====================================================

    /**
     * RN-CU07-01
     *
     * Rango de captura:
     * 60 - 250 mmHg.
     */
    @Column(
            name = "presion_sistolica",
            nullable = false
    )
    private Short presionSistolica;


    /**
     * RN-CU07-01
     *
     * Rango de captura:
     * 40 - 150 mmHg.
     */
    @Column(
            name = "presion_diastolica",
            nullable = false
    )
    private Short presionDiastolica;


    // =====================================================
    // TEMPERATURA
    // =====================================================

    /**
     * RN-CU07-02
     *
     * Rango:
     * 34.0 - 42.0 °C.
     *
     * Un decimal.
     */
    @Column(
            name = "temperatura",
            nullable = false,
            precision = 3,
            scale = 1
    )
    private BigDecimal temperatura;


    // =====================================================
    // PESO
    // =====================================================

    /**
     * RN-CU07-03
     *
     * Rango:
     * 0.5 - 300 kg.
     *
     * Dos decimales.
     */
    @Column(
            name = "peso",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal peso;


    // =====================================================
    // TALLA
    // =====================================================

    /**
     * RN-CU07-04
     *
     * Rango:
     * 30 - 250 cm.
     *
     * Dos decimales.
     */
    @Column(
            name = "talla",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal talla;


    // =====================================================
    // FRECUENCIA CARDÍACA
    // =====================================================

    /**
     * RN-CU07-05
     *
     * Rango:
     * 30 - 220 lpm.
     */
    @Column(
            name = "frecuencia_cardiaca",
            nullable = false
    )
    private Short frecuenciaCardiaca;


    // =====================================================
    // EMERGENCIA
    // =====================================================

    /**
     * CU-07 - FA01.
     *
     * true:
     * El paciente debe pasar directamente a consulta.
     *
     * false:
     * Regresa a la sala de espera.
     */
    @Column(
            name = "es_emergencia",
            nullable = false
    )
    private Boolean esEmergencia = false;


    // =====================================================
    // ALERTAS CLÍNICAS
    // =====================================================

    /**
     * RN-CU07-06.
     *
     * Presión fuera del rango clínico normal:
     *
     * 90/60 - 140/90.
     *
     * La alerta NO impide guardar el registro.
     */
    @Column(
            name = "alerta_presion",
            nullable = false
    )
    private Boolean alertaPresion = false;


    /**
     * RN-CU07-06.
     *
     * Temperatura fuera del rango clínico normal:
     *
     * 36.0 - 37.5 °C.
     */
    @Column(
            name = "alerta_temperatura",
            nullable = false
    )
    private Boolean alertaTemperatura = false;


    /**
     * RN-CU07-06.
     *
     * Frecuencia cardíaca fuera del rango clínico normal:
     *
     * 60 - 100 lpm.
     */
    @Column(
            name = "alerta_frecuencia_cardiaca",
            nullable = false
    )
    private Boolean alertaFrecuenciaCardiaca = false;


    // =====================================================
    // FECHA/HORA CLÍNICA
    // =====================================================

    /**
     * Momento en que Enfermería realizó el registro.
     */
    @Column(
            name = "fecha_hora_registro",
            nullable = false
    )
    private OffsetDateTime fechaHoraRegistro;


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

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaCreacion == null) {

            fechaCreacion =
                    ahora;
        }


        if (fechaHoraRegistro == null) {

            fechaHoraRegistro =
                    ahora;
        }


        if (esEmergencia == null) {

            esEmergencia =
                    false;
        }


        if (alertaPresion == null) {

            alertaPresion =
                    false;
        }


        if (alertaTemperatura == null) {

            alertaTemperatura =
                    false;
        }


        if (alertaFrecuenciaCardiaca == null) {

            alertaFrecuenciaCardiaca =
                    false;
        }
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    protected void preUpdate() {

        fechaModificacion =
                OffsetDateTime.now();
    }


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    // =====================================================
    // CITA
    // =====================================================

    public Cita getCita() {

        return cita;
    }


    public void setCita(
            Cita cita
    ) {

        this.cita =
                cita;
    }


    // =====================================================
    // ENFERMERO
    // =====================================================

    public Usuario getEnfermero() {

        return enfermero;
    }


    public void setEnfermero(
            Usuario enfermero
    ) {

        this.enfermero =
                enfermero;
    }


    // =====================================================
    // PRESIÓN SISTÓLICA
    // =====================================================

    public Short getPresionSistolica() {

        return presionSistolica;
    }


    public void setPresionSistolica(
            Short presionSistolica
    ) {

        this.presionSistolica =
                presionSistolica;
    }


    // =====================================================
    // PRESIÓN DIASTÓLICA
    // =====================================================

    public Short getPresionDiastolica() {

        return presionDiastolica;
    }


    public void setPresionDiastolica(
            Short presionDiastolica
    ) {

        this.presionDiastolica =
                presionDiastolica;
    }


    // =====================================================
    // TEMPERATURA
    // =====================================================

    public BigDecimal getTemperatura() {

        return temperatura;
    }


    public void setTemperatura(
            BigDecimal temperatura
    ) {

        this.temperatura =
                temperatura;
    }


    // =====================================================
    // PESO
    // =====================================================

    public BigDecimal getPeso() {

        return peso;
    }


    public void setPeso(
            BigDecimal peso
    ) {

        this.peso =
                peso;
    }


    // =====================================================
    // TALLA
    // =====================================================

    public BigDecimal getTalla() {

        return talla;
    }


    public void setTalla(
            BigDecimal talla
    ) {

        this.talla =
                talla;
    }


    // =====================================================
    // FRECUENCIA CARDÍACA
    // =====================================================

    public Short getFrecuenciaCardiaca() {

        return frecuenciaCardiaca;
    }


    public void setFrecuenciaCardiaca(
            Short frecuenciaCardiaca
    ) {

        this.frecuenciaCardiaca =
                frecuenciaCardiaca;
    }


    // =====================================================
    // EMERGENCIA
    // =====================================================

    public Boolean getEsEmergencia() {

        return esEmergencia;
    }


    public void setEsEmergencia(
            Boolean esEmergencia
    ) {

        this.esEmergencia =
                esEmergencia;
    }


    // =====================================================
    // ALERTA PRESIÓN
    // =====================================================

    public Boolean getAlertaPresion() {

        return alertaPresion;
    }


    public void setAlertaPresion(
            Boolean alertaPresion
    ) {

        this.alertaPresion =
                alertaPresion;
    }


    // =====================================================
    // ALERTA TEMPERATURA
    // =====================================================

    public Boolean getAlertaTemperatura() {

        return alertaTemperatura;
    }


    public void setAlertaTemperatura(
            Boolean alertaTemperatura
    ) {

        this.alertaTemperatura =
                alertaTemperatura;
    }


    // =====================================================
    // ALERTA FRECUENCIA CARDÍACA
    // =====================================================

    public Boolean getAlertaFrecuenciaCardiaca() {

        return alertaFrecuenciaCardiaca;
    }


    public void setAlertaFrecuenciaCardiaca(
            Boolean alertaFrecuenciaCardiaca
    ) {

        this.alertaFrecuenciaCardiaca =
                alertaFrecuenciaCardiaca;
    }


    // =====================================================
    // FECHA/HORA DEL REGISTRO
    // =====================================================

    public OffsetDateTime getFechaHoraRegistro() {

        return fechaHoraRegistro;
    }


    public void setFechaHoraRegistro(
            OffsetDateTime fechaHoraRegistro
    ) {

        this.fechaHoraRegistro =
                fechaHoraRegistro;
    }


    // =====================================================
    // FECHA CREACIÓN
    // =====================================================

    public OffsetDateTime getFechaCreacion() {

        return fechaCreacion;
    }


    // =====================================================
    // CREADO POR
    // =====================================================

    public Usuario getCreadoPor() {

        return creadoPor;
    }


    public void setCreadoPor(
            Usuario creadoPor
    ) {

        this.creadoPor =
                creadoPor;
    }


    // =====================================================
    // FECHA MODIFICACIÓN
    // =====================================================

    public OffsetDateTime getFechaModificacion() {

        return fechaModificacion;
    }


    // =====================================================
    // MODIFICADO POR
    // =====================================================

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