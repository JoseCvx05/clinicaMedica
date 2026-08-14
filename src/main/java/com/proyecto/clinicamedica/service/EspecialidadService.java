package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.repository.EspecialidadRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: ESPECIALIDAD
 * =========================================================
 *
 * Gestiona las consultas relacionadas con el catálogo
 * de especialidades médicas.
 *
 * Responsabilidades:
 *
 * - Obtener especialidades activas.
 * - Buscar especialidades activas por ID.
 * - Buscar especialidades activas por nombre.
 * - Verificar existencia de especialidades activas.
 * - Mantener en caché el catálogo de especialidades.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class EspecialidadService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final EspecialidadRepository especialidadRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EspecialidadService(
            EspecialidadRepository especialidadRepository
    ) {

        this.especialidadRepository =
                especialidadRepository;
    }


    // =====================================================
    // LISTAR ESPECIALIDADES ACTIVAS
    // =====================================================

    /**
     * La primera llamada consulta PostgreSQL.
     *
     * Las llamadas posteriores utilizan la caché
     * mientras esta continúe vigente.
     */
    @Cacheable(CacheConfig.ESPECIALIDADES)
    public List<Especialidad> listarActivas() {

        return especialidadRepository
                .findByActivoTrueOrderByNombreAsc();
    }


    // =====================================================
    // BUSCAR ACTIVA POR ID
    // =====================================================

    public Optional<Especialidad> buscarActivaPorId(
            Integer id
    ) {

        if (id == null) {

            return Optional.empty();
        }


        return especialidadRepository
                .findById(
                        id
                )
                .filter(
                        especialidad ->
                                Boolean.TRUE.equals(
                                        especialidad.getActivo()
                                )
                );
    }


    // =====================================================
    // BUSCAR ACTIVA POR NOMBRE
    // =====================================================

    public Optional<Especialidad> buscarActivaPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return Optional.empty();
        }


        return especialidadRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }


    // =====================================================
    // EXISTE ESPECIALIDAD ACTIVA POR NOMBRE
    // =====================================================

    public boolean existeActivaPorNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return false;
        }


        return especialidadRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombre.trim()
                );
    }
}