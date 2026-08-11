package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORIO: ESPECIALIDAD
 * =========================================================
 *
 * Se encarga únicamente del acceso a datos de la entidad
 * Especialidad.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface EspecialidadRepository
        extends JpaRepository<Especialidad, Integer> {

    /**
     * Obtiene las especialidades activas ordenadas
     * alfabéticamente.
     *
     * Este método alimentará el catálogo almacenado
     * en caché.
     */
    List<Especialidad> findByActivoTrueOrderByNombreAsc();


    /**
     * Busca una especialidad activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Especialidad> findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );


    /**
     * Comprueba si ya existe una especialidad activa
     * con determinado nombre.
     *
     * Será útil cuando lleguemos al mantenimiento
     * de catálogos.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );
}