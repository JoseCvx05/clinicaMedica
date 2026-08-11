package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Sucursal;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * SERVICIO: SUCURSAL
 * =========================================================
 *
 * Define las operaciones disponibles para trabajar
 * con el catálogo de sucursales del sistema.
 *
 * Las demás capas dependerán de esta interfaz y no
 * directamente de una implementación concreta.
 *
 * Permite aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Inversión de dependencias (SOLID)
 * =========================================================
 */
public interface SucursalService {

    /**
     * Obtiene todas las sucursales activas
     * ordenadas alfabéticamente.
     *
     * Este resultado será almacenado en caché.
     */
    List<Sucursal> listarActivas();


    /**
     * Busca una sucursal activa por su identificador.
     */
    Optional<Sucursal> buscarActivaPorId(Integer id);


    /**
     * Busca una sucursal activa por nombre,
     * ignorando mayúsculas y minúsculas.
     */
    Optional<Sucursal> buscarActivaPorNombre(String nombre);


    /**
     * Comprueba si existe una sucursal activa
     * con determinado nombre.
     */
    boolean existeActivaPorNombre(String nombre);
}