package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico.TipoDocumentoClinico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: FIRMA DE DOCUMENTO CLÍNICO
 * =========================================================
 *
 * CU-08 - RNF-031
 *
 * Permite:
 *
 * - Guardar firmas digitales.
 * - Buscar la firma de un documento.
 * - Evitar firmas duplicadas.
 *
 * La BD además protege mediante:
 *
 * UNIQUE(tipo_documento, id_documento)
 *
 * =========================================================
 */
public interface FirmaDocumentoClinicoRepository
        extends JpaRepository<FirmaDocumentoClinico, Long> {


    // =====================================================
    // BUSCAR FIRMA DE UN DOCUMENTO
    // =====================================================

    Optional<FirmaDocumentoClinico>
    findByTipoDocumentoAndIdDocumento(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento
    );


    // =====================================================
    // COMPROBAR EXISTENCIA
    // =====================================================

    boolean existsByTipoDocumentoAndIdDocumento(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento
    );


    // =====================================================
    // BUSCAR FIRMA DEL DOCUMENTO Y DEL MÉDICO
    // =====================================================

    Optional<FirmaDocumentoClinico>
    findByTipoDocumentoAndIdDocumentoAndMedico_Id(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento,

            Integer idMedico
    );
}