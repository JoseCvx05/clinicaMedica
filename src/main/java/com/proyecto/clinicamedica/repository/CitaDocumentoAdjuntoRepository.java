package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.CitaDocumentoAdjunto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * =========================================================
 * REPOSITORY: DOCUMENTOS ADJUNTOS DE CITA
 * =========================================================
 */
@Repository
public interface CitaDocumentoAdjuntoRepository
        extends JpaRepository<CitaDocumentoAdjunto, Integer> {


    List<CitaDocumentoAdjunto>
    findByCita_IdOrderByFechaCargaDesc(
            Integer idCita
    );
}