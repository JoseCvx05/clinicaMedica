package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.RegistroMensajeLis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: REGISTRO DE MENSAJES LIS
 * =========================================================
 *
 * RNF-030
 *
 * Permite controlar los mensajes recibidos desde
 * analizadores de laboratorio o sistemas LIS.
 *
 * Principalmente se utiliza para:
 *
 * - Evitar procesar mensajes duplicados.
 * - Consultar un mensaje por su identificador externo.
 * - Mantener trazabilidad del procesamiento LIS.
 *
 * =========================================================
 */
@Repository
public interface RegistroMensajeLisRepository
        extends JpaRepository<RegistroMensajeLis, Long> {


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================
    //
    // Permite comprobar rápidamente si un mensaje enviado
    // por LIS ya fue recibido anteriormente.
    //
    // =====================================================

    boolean existsByIdMensajeLis(
            String idMensajeLis
    );


    // =====================================================
    // BUSCAR MENSAJE
    // =====================================================

    Optional<RegistroMensajeLis>
    findByIdMensajeLis(
            String idMensajeLis
    );
}