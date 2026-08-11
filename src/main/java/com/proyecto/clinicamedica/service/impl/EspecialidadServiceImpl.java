package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.repository.EspecialidadRepository;
import com.proyecto.clinicamedica.service.EspecialidadService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO: ESPECIALIDAD
 * =========================================================
 *
 * Implementa las operaciones definidas en
 * EspecialidadService.
 *
 * Responsabilidades:
 *
 * - Consultar especialidades activas.
 * - Buscar especialidades.
 * - Mantener la lógica fuera del controlador.
 * - Utilizar caché para el catálogo.
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
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    /**
     * Inyección de dependencias por constructor.
     */
    public EspecialidadServiceImpl(
            EspecialidadRepository especialidadRepository
    ) {
        this.especialidadRepository = especialidadRepository;
    }


    /**
     * Obtiene todas las especialidades activas.
     *
     * La primera consulta obtiene los datos desde
     * PostgreSQL.
     *
     * Las siguientes consultas utilizan la caché
     * "especialidades".
     */
    @Override
    @Cacheable(CacheConfig.ESPECIALIDADES)
    public List<Especialidad> listarActivas() {

        return especialidadRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    /**
     * Busca una especialidad activa por ID.
     */
    @Override
    public Optional<Especialidad> buscarActivaPorId(Integer id) {

        if (id == null) {
            return Optional.empty();
        }

        return especialidadRepository
                .findById(id)
                .filter(Especialidad::getActivo);
    }


    /**
     * Busca una especialidad activa por nombre.
     */
    @Override
    public Optional<Especialidad> buscarActivaPorNombre(
            String nombre
    ) {

        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }

        return especialidadRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    /**
     * Comprueba si existe una especialidad activa
     * con determinado nombre.
     */
    @Override
    public boolean existeActivaPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return false;
        }

        return especialidadRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}