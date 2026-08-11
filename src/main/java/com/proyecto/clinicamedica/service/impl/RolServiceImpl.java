package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.repository.RolRepository;
import com.proyecto.clinicamedica.service.RolService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import com.proyecto.clinicamedica.config.CacheConfig;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO: ROL
 * =========================================================
 *
 * Implementa las operaciones definidas en RolService.
 *
 * Responsabilidades:
 *
 * - Consultar roles activos.
 * - Buscar roles activos.
 * - Mantener la lógica fuera del controlador.
 * - Utilizar caché para los catálogos.
 *
 * Aplica:
 *
 * - SOLID
 * - Inyección por constructor
 * - Polimorfismo mediante RolService
 * - Separación de responsabilidades
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    /**
     * Inyección de dependencias por constructor.
     *
     * Evitamos @Autowired sobre atributos para reducir
     * acoplamiento y facilitar futuras pruebas.
     */
    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }


    /**
     * Obtiene todos los roles activos.
     *
     * La primera vez consulta PostgreSQL.
     * Las siguientes solicitudes obtienen los datos
     * desde caché.
     */
    @Override
    @Cacheable(CacheConfig.ROLES)
    public List<Rol> listarActivos() {

        return rolRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    /**
     * Busca un rol activo por su identificador.
     */
    @Override
    public Optional<Rol> buscarActivoPorId(Integer id) {

        if (id == null) {
            return Optional.empty();
        }

        return rolRepository
                .findById(id)
                .filter(Rol::getActivo);
    }


    /**
     * Busca un rol activo por nombre.
     */
    @Override
    public Optional<Rol> buscarActivoPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }

        return rolRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    /**
     * Verifica si existe un rol activo con
     * determinado nombre.
     */
    @Override
    public boolean existeActivoPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return false;
        }

        return rolRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}