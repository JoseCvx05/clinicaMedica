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
 * ENTIDAD: CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Representa el registro clínico realizado por el Médico
 * durante la atención de una cita.
 *
 * Contiene:
 *
 * - Cita atendida.
 * - Médico responsable.
 * - Motivo de visita.
 * - Hallazgos clínicos.
 * - Código CIE-10 opcional.
 * - Diagnóstico.
 * - Plan de tratamiento.
 * - Notas adicionales.
 * - Estado de consulta.
 * - Firma digital.
 * - Versión clínica.
 * - Fechas.
 * - Auditoría.
 *
 * IMPORTANTE:
 *
 * La versión representa la versión clínica actual.
 * Las versiones anteriores se almacenarán en:
 *
 * consulta_medica_historial
 *
 * =========================================================
 */
@Entity
@Table(
        name = "consulta_medica",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "consulta_medica_id_cita_key",
                        columnNames = "id_cita"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConsultaMedica {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // CITA
    // =====================================================

    /**
     * Una cita solamente puede tener una consulta médica.
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
    // MÉDICO RESPONSABLE
    // =====================================================

    /**
     * Médico que realizó la consulta.
     */
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
    // MOTIVO DE VISITA
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * Obligatorio.
     *
     * PostgreSQL:
     * VARCHAR(2000) NOT NULL
     */
    @Column(
            name = "motivo_visita",
            nullable = false,
            length = 2000
    )
    private String motivoVisita;


    // =====================================================
    // HALLAZGOS CLÍNICOS
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * Debe completarse antes de finalizar la consulta.
     *
     * Mientras la consulta está En curso puede permanecer
     * temporalmente vacío.
     */
    @Column(
            name = "hallazgos_clinicos",
            columnDefinition = "TEXT"
    )
    private String hallazgosClinicos;


    // =====================================================
    // CIE-10
    // =====================================================

    /**
     * RN-CU08-01.
     *
     * El código CIE-10 es opcional.
     */
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

    /**
     * RN-CU08-01.
     *
     * Obligatorio para finalizar.
     *
     * Longitud:
     * 10 - 5000 caracteres.
     *
     * PostgreSQL ya posee CHECK para esta regla.
     */
    @Column(
            name = "diagnostico",
            length = 5000
    )
    private String diagnostico;


    // =====================================================
    // PLAN DE TRATAMIENTO
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * Obligatorio para finalizar.
     */
    @Column(
            name = "plan_tratamiento",
            columnDefinition = "TEXT"
    )
    private String planTratamiento;


    // =====================================================
    // NOTAS ADICIONALES
    // =====================================================

    /**
     * Campo opcional.
     */
    @Column(
            name = "notas_adicionales",
            columnDefinition = "TEXT"
    )
    private String notasAdicionales;


    // =====================================================
    // ESTADO DE CONSULTA
    // =====================================================

    /**
     * Valores válidos en PostgreSQL:
     *
     * "En curso"
     * "Finalizada"
     *
     * Utilizamos Converter porque NO podemos almacenar
     * directamente EN_CURSO mediante EnumType.STRING.
     */
    @Convert(
            converter = EstadoConsultaMedicaConverter.class
    )
    @Column(
            name = "estado_consulta",
            nullable = false,
            length = 20
    )
    private EstadoConsultaMedica estadoConsulta =
            EstadoConsultaMedica.EN_CURSO;


    // =====================================================
    // FIRMA DIGITAL
    // =====================================================

    /**
     * RNF-031.
     *
     * Almacena la representación generada por el mecanismo
     * de firma digital que implementaremos posteriormente.
     *
     * La existencia de este campo por sí sola NO significa
     * que la firma digital ya esté implementada.
     */
    @Column(
            name = "firma_digital_hash",
            length = 500
    )
    private String firmaDigitalHash;


    // =====================================================
    // VERSIONAMIENTO
    // =====================================================

    /**
     * RNF-026.
     *
     * Número de versión clínica vigente.
     *
     * NO utilizamos @Version porque este campo representa
     * versionamiento clínico/histórico controlado por el
     * Service y no simplemente el contador optimista de JPA.
     */
    @Column(
            name = "version",
            nullable = false
    )
    private Integer version = 1;


    // =====================================================
    // FECHA / HORA DE INICIO
    // =====================================================

    @Column(
            name = "fecha_hora_inicio",
            nullable = false
    )
    private OffsetDateTime fechaHoraInicio;


    // =====================================================
    // FECHA / HORA DE FIN
    // =====================================================

    @Column(
            name = "fecha_hora_fin"
    )
    private OffsetDateTime fechaHoraFin;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false,
            updatable = false
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


        if (fechaHoraInicio == null) {

            fechaHoraInicio =
                    ahora;
        }


        if (estadoConsulta == null) {

            estadoConsulta =
                    EstadoConsultaMedica.EN_CURSO;
        }


        if (version == null
                || version < 1) {

            version = 1;
        }


        normalizarTextos();
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    protected void preUpdate() {

        fechaModificacion =
                OffsetDateTime.now();

        normalizarTextos();
    }


    // =====================================================
    // NORMALIZAR TEXTOS
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