package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Especialidad;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * SERVICIO: ESPECIALIDAD
 * =========================================================
 *
 * Define las operaciones disponibles para trabajar
 * con el catálogo de especialidades médicas.
 *
 * Las demás capas dependerán de esta interfaz y no
 * directamente de una implementación concreta.
 *
 * Esto permite aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Inversión de dependencias (SOLID)
 * =========================================================
 */
public interface EspecialidadService {

    /**
     * Obtiene todas las especialidades activas
     * ordenadas alfabéticamente.
     *
     * Esta información será almacenada en caché.
     */
    List<Especialidad> listarActivas();


    /**
     * Busca una especialidad activa por identificador.
     */
    Optional<Especialidad> buscarActivaPorId(Integer id);


    /**
     * Busca una especialidad activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Especialidad> buscarActivaPorNombre(String nombre);


    /**
     * Verifica si existe una especialidad activa
     * con determinado nombre.
     */
    boolean existeActivaPorNombre(String nombre);
}