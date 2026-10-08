package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: REGISTRO DE MENSAJE LIS
 * =========================================================
 *
 * RNF-030
 *
 * Mantiene trazabilidad de los mensajes recibidos desde
 * analizadores o sistemas LIS.
 *
 * OBJETIVOS:
 *
 * - Evitar procesar dos veces el mismo mensaje.
 * - Registrar qué analizador envió el resultado.
 * - Registrar orden y examen relacionados.
 * - Conservar fecha de recepción.
 * - Conservar fecha de procesamiento.
 * - Registrar errores de integración.
 *
 * IMPORTANTE:
 *
 * idMensajeLis es UNIQUE.
 *
 * Si un analizador reenvía el mismo mensaje, el sistema
 * podrá detectar que ya fue recibido anteriormente.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "registro_mensaje_lis",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_registro_mensaje_lis_id_mensaje",
                        columnNames = "id_mensaje_lis"
                )
        }
)
public class RegistroMensajeLis {


    // =====================================================
    // ESTADOS
    // =====================================================

    public enum EstadoMensajeLis {

        RECIBIDO,

        PROCESADO,

        FALLIDO
    }


    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    // =====================================================
    // IDENTIFICADOR EXTERNO
    // =====================================================

    @Column(
            name = "id_mensaje_lis",
            nullable = false,
            unique = true,
            length = 150
    )
    private String idMensajeLis;


    // =====================================================
    // ANALIZADOR
    // =====================================================

    @Column(
            name = "identificador_analizador",
            nullable = false,
            length = 150
    )
    private String identificadorAnalizador;


    // =====================================================
    // ORDEN
    // =====================================================

    @Column(
            name = "numero_orden",
            nullable = false,
            length = 40
    )
    private String numeroOrden;


    // =====================================================
    // EXAMEN
    // =====================================================

    @Column(
            name = "codigo_examen",
            nullable = false,
            length = 30
    )
    private String codigoExamen;


    // =====================================================
    // ESTADO
    // =====================================================

    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private EstadoMensajeLis estado =
            EstadoMensajeLis.RECIBIDO;


    // =====================================================
    // FECHAS
    // =====================================================

    @Column(
            name = "fecha_hora_recepcion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHoraRecepcion;


    @Column(
            name = "fecha_hora_procesamiento"
    )
    private OffsetDateTime fechaHoraProcesamiento;


    // =====================================================
    // ERROR
    // =====================================================

    @Column(
            name = "ultimo_error",
            length = 1000
    )
    private String ultimoError;


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
    // CONSTRUCTOR
    // =====================================================

    public RegistroMensajeLis() {
    }


    public RegistroMensajeLis(
            String idMensajeLis,
            String identificadorAnalizador,
            String numeroOrden,
            String codigoExamen
    ) {

        this.idMensajeLis =
                limpiarObligatorio(
                        idMensajeLis,
                        "El identificador del mensaje LIS es obligatorio."
                );

        this.identificadorAnalizador =
                limpiarObligatorio(
                        identificadorAnalizador,
                        "El identificador del analizador es obligatorio."
                );

        this.numeroOrden =
                limpiarObligatorio(
                        numeroOrden,
                        "El número de orden LIS es obligatorio."
                );

        this.codigoExamen =
                limpiarObligatorio(
                        codigoExamen,
                        "El código del examen LIS es obligatorio."
                );

        this.estado =
                EstadoMensajeLis.RECIBIDO;
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        if (fechaHoraRecepcion == null) {

            fechaHoraRecepcion =
                    OffsetDateTime.now();
        }


        if (estado == null) {

            estado =
                    EstadoMensajeLis.RECIBIDO;
        }


        validar();
    }


    // =====================================================
    // VALIDAR
    // =====================================================

    public void validar() {

        idMensajeLis =
                limpiarObligatorio(
                        idMensajeLis,
                        "El identificador del mensaje LIS es obligatorio."
                );


        identificadorAnalizador =
                limpiarObligatorio(
                        identificadorAnalizador,
                        "El identificador del analizador es obligatorio."
                );


        numeroOrden =
                limpiarObligatorio(
                        numeroOrden,
                        "El número de orden LIS es obligatorio."
                );


        codigoExamen =
                limpiarObligatorio(
                        codigoExamen,
                        "El código del examen LIS es obligatorio."
                );


        if (idMensajeLis.length() > 150) {

            throw new IllegalStateException(
                    "El identificador del mensaje LIS "
                            + "supera la longitud permitida."
            );
        }


        if (identificadorAnalizador.length() > 150) {

            throw new IllegalStateException(
                    "El identificador del analizador "
                            + "supera la longitud permitida."
            );
        }


        if (numeroOrden.length() > 40) {

            throw new IllegalStateException(
                    "El número de orden LIS "
                            + "supera la longitud permitida."
            );
        }


        if (codigoExamen.length() > 30) {

            throw new IllegalStateException(
                    "El código del examen LIS "
                            + "supera la longitud permitida."
            );
        }


        if (ultimoError != null
                && ultimoError.trim().length() > 1000) {

            throw new IllegalStateException(
                    "El detalle del error LIS "
                            + "supera los 1000 caracteres."
            );
        }
    }


    // =====================================================
    // MARCAR PROCESADO
    // =====================================================

    public void marcarProcesado() {

        this.estado =
                EstadoMensajeLis.PROCESADO;

        this.fechaHoraProcesamiento =
                OffsetDateTime.now();

        this.ultimoError =
                null;
    }


    // =====================================================
    // MARCAR FALLIDO
    // =====================================================

    public void marcarFallido(
            String error
    ) {

        this.estado =
                EstadoMensajeLis.FALLIDO;

        this.fechaHoraProcesamiento =
                OffsetDateTime.now();


        if (error == null
                || error.trim().isBlank()) {

            this.ultimoError =
                    "Error desconocido durante el procesamiento LIS.";

            return;
        }


        String limpio =
                error.trim();


        this.ultimoError =
                limpio.length() <= 1000
                        ? limpio
                        : limpio.substring(
                        0,
                        1000
                );
    }


    // =====================================================
    // TEXTO OBLIGATORIO
    // =====================================================

    private String limpiarObligatorio(
            String valor,
            String mensaje
    ) {

        if (valor == null
                || valor.trim().isBlank()) {

            throw new IllegalArgumentException(
                    mensaje
            );
        }


        return valor.trim();
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {

        return id;
    }


    public String getIdMensajeLis() {

        return idMensajeLis;
    }


    public String getIdentificadorAnalizador() {

        return identificadorAnalizador;
    }


    public String getNumeroOrden() {

        return numeroOrden;
    }


    public String getCodigoExamen() {

        return codigoExamen;
    }


    public EstadoMensajeLis getEstado() {

        return estado;
    }


    public OffsetDateTime getFechaHoraRecepcion() {

        return fechaHoraRecepcion;
    }


    public OffsetDateTime getFechaHoraProcesamiento() {

        return fechaHoraProcesamiento;
    }


    public String getUltimoError() {

        return ultimoError;
    }


    public Long getVersion() {

        return version;
    }
}