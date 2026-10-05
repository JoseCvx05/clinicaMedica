package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: FIRMA DE DOCUMENTO CLÍNICO
 * =========================================================
 *
 * CU-08 - RNF-031
 *
 * Conserva la evidencia criptográfica asociada a un
 * documento clínico firmado por un Médico.
 *
 * Guarda:
 *
 * - Médico responsable.
 * - Tipo de documento.
 * - ID del documento.
 * - Hash SHA-256 del contenido.
 * - Firma criptográfica Base64.
 * - Llave pública utilizada.
 * - Algoritmo.
 * - Fecha y hora.
 *
 * La llave privada NO se almacena en esta tabla.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "firma_documento_clinico",

        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_firma_documento",
                        columnNames = {
                                "tipo_documento",
                                "id_documento"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FirmaDocumentoClinico {


    // =====================================================
    // TIPOS DE DOCUMENTO CLÍNICO
    // =====================================================

    public enum TipoDocumentoClinico {

        CONSULTA_MEDICA,

        RECETA_MEDICA,

        ORDEN_LABORATORIO
    }


    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private Long id;


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
    // TIPO DE DOCUMENTO
    // =====================================================

    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "tipo_documento",
            nullable = false,
            length = 40
    )
    private TipoDocumentoClinico
            tipoDocumento;


    // =====================================================
    // DOCUMENTO
    // =====================================================

    /**
     * Identificador del documento firmado.
     *
     * Ejemplo:
     *
     * tipo_documento = CONSULTA_MEDICA
     * id_documento   = consulta_medica.id
     */
    @Column(
            name = "id_documento",
            nullable = false
    )
    private Long idDocumento;


    // =====================================================
    // HASH DEL CONTENIDO
    // =====================================================

    /**
     * SHA-256 en representación hexadecimal.
     *
     * 32 bytes SHA-256
     *      ↓
     * 64 caracteres hexadecimal.
     */
    @Column(
            name = "hash_contenido",
            nullable = false,
            length = 64
    )
    private String hashContenido;


    // =====================================================
    // FIRMA CRIPTOGRÁFICA
    // =====================================================

    /**
     * Firma generada utilizando SHA256withRSA y codificada
     * posteriormente en Base64.
     */
    @Column(
            name = "firma_base64",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String firmaBase64;


    // =====================================================
    // LLAVE PÚBLICA
    // =====================================================

    /**
     * Llave pública utilizada para verificar la firma.
     *
     * Nunca almacena la llave privada.
     */
    @Column(
            name = "llave_publica_base64",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String llavePublicaBase64;


    // =====================================================
    // ALGORITMO
    // =====================================================

    @Column(
            name = "algoritmo",
            nullable = false,
            length = 50
    )
    private String algoritmo =
            "SHA256withRSA";


    // =====================================================
    // FECHA / HORA
    // =====================================================

    @Column(
            name = "fecha_hora_firma",
            nullable = false
    )
    private OffsetDateTime
            fechaHoraFirma;


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
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        validar();


        if (fechaHoraFirma == null) {

            fechaHoraFirma =
                    OffsetDateTime.now();
        }


        if (algoritmo == null
                || algoritmo.isBlank()) {

            algoritmo =
                    "SHA256withRSA";
        }


        hashContenido =
                hashContenido
                        .trim()
                        .toLowerCase();


        firmaBase64 =
                firmaBase64.trim();


        llavePublicaBase64 =
                llavePublicaBase64.trim();


        algoritmo =
                algoritmo.trim();
    }


    // =====================================================
    // VALIDACIONES
    // =====================================================

    private void validar() {

        // =================================================
        // MÉDICO
        // =================================================

        if (medico == null
                || medico.getId() == null
                || medico.getId() <= 0) {

            throw new IllegalStateException(
                    "La firma digital requiere "
                            + "un Médico válido."
            );
        }


        // =================================================
        // TIPO
        // =================================================

        if (tipoDocumento == null) {

            throw new IllegalStateException(
                    "El tipo de documento clínico "
                            + "es obligatorio."
            );
        }


        // =================================================
        // DOCUMENTO
        // =================================================

        if (idDocumento == null
                || idDocumento <= 0) {

            throw new IllegalStateException(
                    "El documento clínico a firmar "
                            + "no es válido."
            );
        }


        // =================================================
        // HASH
        // =================================================

        if (hashContenido == null
                || hashContenido.isBlank()) {

            throw new IllegalStateException(
                    "El hash del documento clínico "
                            + "es obligatorio."
            );
        }


        String hash =
                hashContenido.trim();


        if (!hash.matches(
                "^[0-9a-fA-F]{64}$"
        )) {

            throw new IllegalStateException(
                    "El hash SHA-256 del documento "
                            + "no posee un formato válido."
            );
        }


        // =================================================
        // FIRMA
        // =================================================

        if (firmaBase64 == null
                || firmaBase64.isBlank()) {

            throw new IllegalStateException(
                    "La firma criptográfica "
                            + "es obligatoria."
            );
        }


        // =================================================
        // LLAVE PÚBLICA
        // =================================================

        if (llavePublicaBase64 == null
                || llavePublicaBase64.isBlank()) {

            throw new IllegalStateException(
                    "La llave pública utilizada "
                            + "para verificar la firma "
                            + "es obligatoria."
            );
        }
    }
}