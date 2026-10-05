package com.proyecto.clinicamedica.entity;

import com.proyecto.clinicamedica.mapper.EstadoConsultaMedicaConverter;
import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: HISTORIAL DE CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * RNF-026:
 * El historial de consultas debe mantener versionamiento
 * para auditoría.
 *
 * Esta tabla conserva una fotografía clínica de cada
 * versión anterior de ConsultaMedica.
 *
 * consulta_medica:
 *      versión vigente.
 *
 * consulta_medica_historial:
 *      versiones anteriores.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "consulta_medica_historial",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_consulta_historial_version",
                        columnNames = {
                                "id_consulta",
                                "version"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConsultaMedicaHistorial {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Long id;


    // =====================================================
    // CONSULTA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_consulta",
            nullable = false
    )
    private ConsultaMedica consulta;


    // =====================================================
    // VERSIÓN
    // =====================================================

    @Column(
            name = "version",
            nullable = false
    )
    private Integer version;


    // =====================================================
    // MOTIVO
    // =====================================================

    @Column(
            name = "motivo_visita",
            length = 2000
    )
    private String motivoVisita;


    // =====================================================
    // HALLAZGOS
    // =====================================================

    @Column(
            name = "hallazgos_clinicos",
            columnDefinition = "TEXT"
    )
    private String hallazgosClinicos;


    // =====================================================
    // CIE-10
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_cie10"
    )
    private Cie10 cie10;


    // =====================================================
    // DIAGNÓSTICO
    // =====================================================

    @Column(
            name = "diagnostico",
            length = 5000
    )
    private String diagnostico;


    // =====================================================
    // PLAN DE TRATAMIENTO
    // =====================================================

    @Column(
            name = "plan_tratamiento",
            columnDefinition = "TEXT"
    )
    private String planTratamiento;


    // =====================================================
    // NOTAS
    // =====================================================

    @Column(
            name = "notas_adicionales",
            columnDefinition = "TEXT"
    )
    private String notasAdicionales;


    // =====================================================
    // ESTADO DE LA CONSULTA
    // =====================================================

    @Convert(
            converter = EstadoConsultaMedicaConverter.class
    )
    @Column(
            name = "estado_consulta",
            length = 20
    )
    private EstadoConsultaMedica estadoConsulta;


    // =====================================================
    // FIRMA DIGITAL
    // =====================================================

    /**
     * RNF-031.
     *
     * Conserva también la firma asociada a esa versión.
     */
    @Column(
            name = "firma_digital_hash",
            length = 500
    )
    private String firmaDigitalHash;


    // =====================================================
    // FECHA DE FINALIZACIÓN
    // =====================================================

    @Column(
            name = "fecha_hora_fin"
    )
    private OffsetDateTime fechaHoraFin;


    // =====================================================
    // USUARIO MODIFICADOR
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "modificado_por"
    )
    private Usuario modificadoPor;


    // =====================================================
    // FECHA DE MODIFICACIÓN
    // =====================================================

    @Column(
            name = "fecha_modificacion",
            nullable = false
    )
    private OffsetDateTime fechaModificacion;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        if (version == null
                || version < 1) {

            throw new IllegalStateException(
                    "La versión histórica debe ser mayor o igual a 1."
            );
        }


        if (fechaModificacion == null) {

            fechaModificacion =
                    OffsetDateTime.now();
        }


        normalizarTextos();
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizarTextos() {

        motivoVisita =
                limpiar(
                        motivoVisita
                );

        hallazgosClinicos =
                limpiar(
                        hallazgosClinicos
                );

        diagnostico =
                limpiar(
                        diagnostico
                );

        planTratamiento =
                limpiar(
                        planTratamiento
                );

        notasAdicionales =
                limpiar(
                        notasAdicionales
                );

        firmaDigitalHash =
                limpiar(
                        firmaDigitalHash
                );
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isEmpty()
                ? null
                : limpio;
    }
}