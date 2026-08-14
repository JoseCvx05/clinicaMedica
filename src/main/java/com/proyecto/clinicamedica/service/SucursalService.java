package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.repository.SucursalRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: SUCURSAL
 * =========================================================
 *
 * Gestiona las consultas relacionadas con el catálogo
 * de sucursales.
 *
 * Responsabilidades:
 *
 * - Obtener sucursales activas.
 * - Buscar sucursales activas por ID.
 * - Buscar sucursales activas por nombre.
 * - Verificar existencia de sucursales activas.
 * - Mantener en caché el catálogo de sucursales.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class SucursalService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final SucursalRepository sucursalRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SucursalService(
            SucursalRepository sucursalRepository
    ) {

        this.sucursalRepository =
                sucursalRepository;
    }


    // =====================================================
    // LISTAR SUCURSALES ACTIVAS
    // =====================================================

    /**
     * La primera llamada consulta PostgreSQL.
     *
     * Las llamadas posteriores utilizan la caché
     * mientras esta continúe vigente.
     */
    @Cacheable(CacheConfig.SUCURSALES)
    public List<Sucursal> listarActivas() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    // =====================================================
    // BUSCAR ACTIVA POR ID
    // =====================================================

    public Optional<Sucursal> buscarActivaPorId(
            Integer id
    ) {

        if (id == null) {

            return Optional.empty();
        }


        return sucursalRepository
                .findById(
                        id
                )
                .filter(
                        sucursal ->
                                Boolean.TRUE.equals(
                                        sucursal.getActivo()
                                )
                );
    }


    // =====================================================
    // BUSCAR ACTIVA POR NOMBRE
    // =====================================================

    public Optional<Sucursal> buscarActivaPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return Optional.empty();
        }


        return sucursalRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    // =====================================================
    // EXISTE SUCURSAL ACTIVA POR NOMBRE
    // =====================================================

    public boolean existeActivaPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return false;
        }


        return sucursalRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}