package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * REPOSITORIO: ROL
 * =========================================================
 *
 * Se encarga exclusivamente del acceso a datos
 * relacionados con la entidad Rol.
 *
 * No contiene lógica de negocio.
 * =========================================================
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Obtiene únicamente los roles activos.
     *
     * Será utilizado para los catálogos que posteriormente
     * serán almacenados en caché.
     */
    List<Rol> findByActivoTrueOrderByNombreAsc();


    /**
     * Busca un rol activo por nombre ignorando
     * mayúsculas y minúsculas.
     *
     * Ejemplo:
     *
     * "Paciente"
     * "PACIENTE"
     * "paciente"
     *
     * serán considerados el mismo nombre.
     */
    Optional<Rol> findByNombreIgnoreCaseAndActivoTrue(String nombre);


    /**
     * Permite verificar si ya existe un rol activo
     * con determinado nombre.
     *
     * Esto servirá posteriormente para evitar duplicados.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);
}