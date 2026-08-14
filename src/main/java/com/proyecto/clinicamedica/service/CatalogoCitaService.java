package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.OpcionCatalogoCitaDTO;

import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * =========================================================
 * SERVICIO: CATÁLOGOS DEL WIZARD DE CITAS
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Responsabilidades:
 *
 * - Paso 1: listar sucursales activas.
 * - Paso 2: listar especialidades por sucursal.
 * - Paso 3: listar médicos por sucursal/especialidad.
 * - Validar selecciones realizadas en el wizard.
 *
 * Sucursales y especialidades utilizan caché.
 *
 * Los médicos se consultan directamente para reducir
 * el riesgo de utilizar asignaciones desactualizadas.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class CatalogoCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalRepository sucursalRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final UsuarioRepository usuarioRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CatalogoCitaService(
            SucursalRepository sucursalRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            UsuarioRepository usuarioRepository
    ) {

        this.sucursalRepository =
                sucursalRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.usuarioRepository =
                usuarioRepository;
    }


    // =====================================================
    // PASO 1 - SUCURSALES
    // =====================================================

    @Cacheable("citaSucursalesActivas")
    public List<OpcionCatalogoCitaDTO> listarSucursales() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(
                        sucursal ->
                                new OpcionCatalogoCitaDTO(
                                        sucursal.getId(),
                                        sucursal.getNombre()
                                )
                )
                .toList();
    }


    // =====================================================
    // PASO 2 - ESPECIALIDADES POR SUCURSAL
    // =====================================================

    @Cacheable(
            value = "citaEspecialidadesPorSucursal",
            key = "#idSucursal"
    )
    public List<OpcionCatalogoCitaDTO> listarEspecialidades(
            Integer idSucursal
    ) {

        if (idSucursal == null) {

            return List.of();
        }


        return sucursalEspecialidadRepository
                .findBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrueOrderByEspecialidad_NombreAsc(
                        idSucursal
                )
                .stream()
                .map(
                        relacion ->
                                new OpcionCatalogoCitaDTO(
                                        relacion
                                                .getEspecialidad()
                                                .getId(),

                                        relacion
                                                .getEspecialidad()
                                                .getNombre()
                                )
                )
                .toList();
    }


    // =====================================================
    // PASO 3 - MÉDICOS
    // =====================================================

    public List<OpcionCatalogoCitaDTO> listarMedicos(
            Integer idSucursal,
            Integer idEspecialidad
    ) {

        if (idSucursal == null
                || idEspecialidad == null) {

            return List.of();
        }


        return usuarioRepository
                .findByActivoTrueAndRol_NombreIgnoreCaseAndSucursal_IdAndEspecialidad_IdOrderByNombreCompletoAsc(
                        ROL_MEDICO,
                        idSucursal,
                        idEspecialidad
                )
                .stream()
                .map(
                        medico ->
                                new OpcionCatalogoCitaDTO(
                                        medico.getId(),
                                        medico.getNombreCompleto()
                                )
                )
                .toList();
    }


    // =====================================================
    // VALIDAR SUCURSAL
    // =====================================================

    public boolean existeSucursalActiva(
            Integer idSucursal
    ) {

        if (idSucursal == null) {

            return false;
        }


        return sucursalRepository
                .findById(
                        idSucursal
                )
                .filter(
                        sucursal ->
                                Boolean.TRUE.equals(
                                        sucursal.getActivo()
                                )
                )
                .isPresent();
    }


    // =====================================================
    // VALIDAR ESPECIALIDAD EN SUCURSAL
    // =====================================================

    public boolean especialidadDisponibleEnSucursal(
            Integer idSucursal,
            Integer idEspecialidad
    ) {

        if (idSucursal == null
                || idEspecialidad == null) {

            return false;
        }


        return listarEspecialidades(
                idSucursal
        )
                .stream()
                .anyMatch(
                        opcion ->
                                idEspecialidad.equals(
                                        opcion.id()
                                )
                );
    }


    // =====================================================
    // VALIDAR MÉDICO
    // =====================================================

    public boolean medicoDisponibleParaSeleccion(
            Integer idMedico,
            Integer idSucursal,
            Integer idEspecialidad
    ) {

        if (idMedico == null
                || idSucursal == null
                || idEspecialidad == null) {

            return false;
        }


        return listarMedicos(
                idSucursal,
                idEspecialidad
        )
                .stream()
                .anyMatch(
                        opcion ->
                                idMedico.equals(
                                        opcion.id()
                                )
                );
    }
}