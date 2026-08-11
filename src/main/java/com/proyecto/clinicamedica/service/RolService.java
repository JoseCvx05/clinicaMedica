package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Rol;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * SERVICIO: ROL
 * =========================================================
 *
 * Define las operaciones disponibles para trabajar
 * con el catálogo de roles.
 *
 * Las clases que necesiten trabajar con roles dependerán
 * de esta interfaz y no de una implementación concreta.
 *
 * Esto permite aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Principio de inversión de dependencias (SOLID)
 * =========================================================
 */
public interface RolService {

    /**
     * Obtiene todos los roles activos ordenados
     * alfabéticamente.
     *
     * Esta operación será utilizada por los catálogos
     * almacenados en caché.
     */
    List<Rol> listarActivos();


    /**
     * Busca un rol activo por su identificador.
     */
    Optional<Rol> buscarActivoPorId(Integer id);


    /**
     * Busca un rol activo por nombre ignorando
     * mayúsculas y minúsculas.
     */
    Optional<Rol> buscarActivoPorNombre(String nombre);


    /**
     * Verifica si existe un rol activo con
     * determinado nombre.
     */
    boolean existeActivoPorNombre(String nombre);
}