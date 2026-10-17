
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.repository.SucursalRepository;

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
 *
 * CU-14:
 *
 * Las consultas de este servicio se realizan directamente
 * en PostgreSQL para reflejar los cambios del catálogo.
 *
 * No se utiliza caché en listarActivas(), evitando
 * mostrar sucursales desactualizadas.
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
     * Consulta directamente PostgreSQL.
     *
     * Esto permite reflejar las altas y bajas lógicas
     * realizadas desde el mantenimiento del CU-14.
     */
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
