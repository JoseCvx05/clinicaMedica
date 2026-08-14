package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.FormaPago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: FORMA DE PAGO
 * =========================================================
 *
 * Permite obtener formas de pago activas del catálogo.
 * =========================================================
 */
@Repository
public interface FormaPagoRepository
        extends JpaRepository<FormaPago, Integer> {


    Optional<FormaPago>
    findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );
}