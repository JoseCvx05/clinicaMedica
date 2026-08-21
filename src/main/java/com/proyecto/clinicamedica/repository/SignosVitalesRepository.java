package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.SignosVitales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Responsabilidades:
 *
 * - Persistir registros de signos vitales.
 * - Verificar si una cita ya posee un registro.
 * - Recuperar signos vitales asociados a una cita.
 *
 * La BD también protege la unicidad mediante:
 *
 * UNIQUE (id_cita)
 *
 * =========================================================
 */
@Repository
public interface SignosVitalesRepository
        extends JpaRepository<SignosVitales, Integer> {


    // =====================================================
    // ¿YA TIENE SIGNOS VITALES?
    // =====================================================

    boolean existsByCita_Id(
            Integer idCita
    );


    // =====================================================
    // SIGNOS VITALES DE UNA CITA
    // =====================================================

    Optional<SignosVitales> findByCita_Id(
            Integer idCita
    );
}