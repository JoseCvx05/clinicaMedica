package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * ENTIDAD: NOTIFICACIÓN DE CORREO
 * =========================================================
 *
 * Representa los correos pendientes, enviados o fallidos
 * del sistema.
 *
 * Permite conservar el estado del envío y realizar
 * reintentos automáticos posteriormente.
 * =========================================================
 */
@Entity
@Table(name = "notificacion_correo")
public class NotificacionCorreo {


    // =====================================================
    // ESTADOS PERMITIDOS POR POSTGRESQL
    // =====================================================

    public static final String ESTADO_PENDIENTE =
            "Pendiente";

    public static final String ESTADO_ENVIADO =
            "Enviado";

    public static final String ESTADO_FALLIDO =
            "Fallido";

    public static final String ESTADO_REINTENTANDO =
            "Reintentando";


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // INFORMACIÓN DEL CORREO
    // =====================================================

    @Column(
            name = "tipo_notificacion",
            nullable = false,
            length = 50
    )
    private String tipoNotificacion;


    @Column(
            name = "destinatario_correo",
            nullable = false,
            length = 150
    )
    private String destinatarioCorreo;


    @Column(
            name = "asunto",
            nullable = false,
            length = 200
    )
    private String asunto;


    // =====================================================
    // REFERENCIA
    // =====================================================

    @Column(
            name = "tabla_referencia",
            length = 100
    )
    private String tablaReferencia;


    @Column(
            name = "id_referencia"
    )
    private Integer idReferencia;


    // =====================================================
    // ESTADO DE ENVÍO
    // =====================================================

    @Column(
            name = "estado_envio",
            nullable = false,
            length = 20
    )
    private String estadoEnvio =
            ESTADO_PENDIENTE;


    @Column(
            name = "intentos_envio",
            nullable = false
    )
    private Short intentosEnvio =
            0;


    @Column(
            name = "ultimo_error",
            length = 500
    )
    private String ultimoError;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_hora_envio"
    )
    private OffsetDateTime fechaHoraEnvio;


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

        if (estadoEnvio == null) {

            estadoEnvio =
                    ESTADO_PENDIENTE;
        }


        if (intentosEnvio == null) {

            intentosEnvio =
                    0;
        }


        if (fechaCreacion == null) {

            fechaCreacion =
                    OffsetDateTime.now();
        }
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }


    public String getTipoNotificacion() {
        return tipoNotificacion;
    }

    public void setTipoNotificacion(
            String tipoNotificacion
    ) {
        this.tipoNotificacion =
                tipoNotificacion;
    }


    public String getDestinatarioCorreo() {
        return destinatarioCorreo;
    }

    public void setDestinatarioCorreo(
            String destinatarioCorreo
    ) {
        this.destinatarioCorreo =
                destinatarioCorreo;
    }


    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(
            String asunto
    ) {
        this.asunto =
                asunto;
    }


    public String getTablaReferencia() {
        return tablaReferencia;
    }

    public void setTablaReferencia(
            String tablaReferencia
    ) {
        this.tablaReferencia =
                tablaReferencia;
    }


    public Integer getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(
            Integer idReferencia
    ) {
        this.idReferencia =
                idReferencia;
    }


    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(
            String estadoEnvio
    ) {
        this.estadoEnvio =
                estadoEnvio;
    }


    public Short getIntentosEnvio() {
        return intentosEnvio;
    }

    public void setIntentosEnvio(
            Short intentosEnvio
    ) {
        this.intentosEnvio =
                intentosEnvio;
    }


    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(
            String ultimoError
    ) {
        this.ultimoError =
                ultimoError;
    }


    public OffsetDateTime getFechaHoraEnvio() {
        return fechaHoraEnvio;
    }

    public void setFechaHoraEnvio(
            OffsetDateTime fechaHoraEnvio
    ) {
        this.fechaHoraEnvio =
                fechaHoraEnvio;
    }


    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}