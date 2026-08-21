package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.EstadoCita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORY: ESTADO DE CITA
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Permite consultar los estados disponibles de una cita.
 *
 * =========================================================
 */
@Repository
public interface EstadoCitaRepository
        extends JpaRepository<EstadoCita, Integer> {


    Optional<EstadoCita>
    findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );
    List<EstadoCita> findByActivoTrueOrderByNombreAsc();
}