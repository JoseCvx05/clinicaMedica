package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.service.SucursalService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO: SUCURSAL
 * =========================================================
 *
 * Implementa las operaciones definidas en
 * SucursalService.
 *
 * Responsabilidades:
 *
 * - Consultar sucursales activas.
 * - Buscar sucursales activas.
 * - Utilizar caché para el catálogo.
 * - Mantener la lógica fuera del controlador.
 *
 * Aplica:
 *
 * - SOLID
 * - Polimorfismo
 * - Inyección por constructor
 * - Separación de responsabilidades
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository sucursalRepository;

    /**
     * Inyección de dependencias por constructor.
     */
    public SucursalServiceImpl(
            SucursalRepository sucursalRepository
    ) {
        this.sucursalRepository = sucursalRepository;
    }


    /**
     * Obtiene todas las sucursales activas.
     *
     * La primera vez consulta PostgreSQL.
     * Las siguientes veces utiliza la caché.
     */
    @Override
    @Cacheable(CacheConfig.SUCURSALES)
    public List<Sucursal> listarActivas() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    /**
     * Busca una sucursal activa por su ID.
     */
    @Override
    public Optional<Sucursal> buscarActivaPorId(Integer id) {

        if (id == null) {
            return Optional.empty();
        }

        return sucursalRepository
                .findById(id)
                .filter(Sucursal::getActivo);
    }


    /**
     * Busca una sucursal activa por nombre.
     */
    @Override
    public Optional<Sucursal> buscarActivaPorNombre(
            String nombre
    ) {

        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }

        return sucursalRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    /**
     * Comprueba si existe una sucursal activa
     * con determinado nombre.
     */
    @Override
    public boolean existeActivaPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return false;
        }

        return sucursalRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}