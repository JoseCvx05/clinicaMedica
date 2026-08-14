package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.repository.RolRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: ROL
 * =========================================================
 *
 * Gestiona las consultas relacionadas con el catálogo
 * de roles.
 *
 * Responsabilidades:
 *
 * - Obtener roles activos.
 * - Buscar roles activos por ID.
 * - Buscar roles activos por nombre.
 * - Verificar existencia de roles activos.
 * - Mantener en caché el catálogo de roles.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class RolService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final RolRepository rolRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RolService(
            RolRepository rolRepository
    ) {

        this.rolRepository =
                rolRepository;
    }


    // =====================================================
    // LISTAR ROLES ACTIVOS
    // =====================================================

    /**
     * La primera consulta obtiene los datos desde PostgreSQL.
     *
     * Las siguientes llamadas utilizan la caché
     * mientras esta continúe vigente.
     */
    @Cacheable(CacheConfig.ROLES)
    public List<Rol> listarActivos() {

        return rolRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    // =====================================================
    // BUSCAR ACTIVO POR ID
    // =====================================================

    public Optional<Rol> buscarActivoPorId(
            Integer id
    ) {

        if (id == null) {

            return Optional.empty();
        }


        return rolRepository
                .findById(
                        id
                )
                .filter(
                        rol ->
                                Boolean.TRUE.equals(
                                        rol.getActivo()
                                )
                );
    }


    // =====================================================
    // BUSCAR ACTIVO POR NOMBRE
    // =====================================================

    public Optional<Rol> buscarActivoPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return Optional.empty();
        }


        return rolRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    // =====================================================
    // EXISTE ROL ACTIVO POR NOMBRE
    // =====================================================

    public boolean existeActivoPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return false;
        }


        return rolRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}