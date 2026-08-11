package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORIO: SUCURSAL
 * =========================================================
 *
 * Responsable únicamente del acceso a datos
 * relacionados con la entidad Sucursal.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface SucursalRepository
        extends JpaRepository<Sucursal, Integer> {

    /**
     * Obtiene todas las sucursales activas
     * ordenadas alfabéticamente.
     *
     * Este resultado será utilizado posteriormente
     * para alimentar la caché del catálogo.
     */
    List<Sucursal> findByActivoTrueOrderByNombreAsc();


    /**
     * Busca una sucursal activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Sucursal> findByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );


    /**
     * Comprueba si ya existe una sucursal activa
     * con determinado nombre.
     *
     * Será utilizado posteriormente en el
     * mantenimiento de catálogos.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );
}