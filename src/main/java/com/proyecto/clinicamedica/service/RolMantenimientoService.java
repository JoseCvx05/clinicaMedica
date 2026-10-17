
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.RolRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * CU-14 - SERVICIO: MANTENIMIENTO DE ROLES
 * =========================================================
 *
 * Catálogo 7 de 9.
 *
 * Operaciones:
 * - Listar y buscar.
 * - Consultar por ID.
 * - Crear.
 * - Actualizar.
 * - Desactivar lógicamente.
 *
 * SEGURIDAD:
 * - Permisos específicos por operación.
 * - Protección de roles esenciales.
 * - Protección de usuarios asociados.
 * - Prevención de nombres duplicados.
 * - Bloqueo pesimista en modificaciones.
 * - Auditoría transaccional.
 *
 * No modifica los permisos de Spring Security.
 * No modifica usuarios ni contraseñas.
 *
 * =========================================================
 */
@Service
public class RolMantenimientoService {

    // =====================================================
    // ROLES ESENCIALES DEL SISTEMA
    // =====================================================

    private static final Set<String> ROLES_PROTEGIDOS =
            Set.of(
                    "PACIENTE",
                    "MEDICO",
                    "ADMINISTRADOR",
                    "CAJERO",
                    "ENFERMERO",
                    "LABORATORISTA",
                    "RECEPCIONISTA",
                    "FARMACEUTICO"
            );

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final RolRepository rolRepository;

    private final UsuarioRepository usuarioRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RolMantenimientoService(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService
    ) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.permisoCatalogoService = permisoCatalogoService;
        this.usuarioActualService = usuarioActualService;
        this.auditoriaService = auditoriaService;
    }

    // =====================================================
    // LISTAR Y BUSCAR
    // =====================================================

    @Transactional(readOnly = true)
    public Page<Rol> listar(
            String busqueda,
            Boolean activo,
            Pageable pageable
    ) {

        permisoCatalogoService.exigirPermiso("role/read");

        if (pageable == null) {
            throw new IllegalArgumentException(
                    "La paginación es obligatoria."
            );
        }

        String texto =
                busqueda == null ? "" : busqueda.trim();

        if (activo == null) {
            return rolRepository.findByNombreContainingIgnoreCase(
                    texto,
                    pageable
            );
        }

        return rolRepository.findByActivoAndNombreContainingIgnoreCase(
                activo,
                texto,
                pageable
        );
    }

    // =====================================================
    // CONSULTAR POR ID
    // =====================================================

    @Transactional(readOnly = true)
    public Rol buscarPorId(Integer id) {

        permisoCatalogoService.exigirPermiso("role/read");

        if (id == null) {
            throw new IllegalArgumentException(
                    "Debe indicar un rol válido."
            );
        }

        return rolRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El rol solicitado no existe."
                        )
                );
    }

    // =====================================================
    // CREAR ROL
    // =====================================================

    @Transactional
    public Rol crear(
            String nombre,
            String descripcion
    ) {

        permisoCatalogoService.exigirPermiso("role/create");

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        String nombreValidado = validarNombre(nombre);

        String descripcionValidada =
                validarDescripcion(descripcion);

        // Los nombres esenciales no pueden volver a crearse
        // con variantes de mayúsculas, tildes o espacios.

        if (esRolProtegido(nombreValidado)) {
            throw new IllegalStateException(
                    "El nombre corresponde a un rol esencial "
                            + "del sistema y no puede reutilizarse."
            );
        }

        validarNombreDuplicado(nombreValidado, null);

        Rol rol = new Rol();

        rol.setNombre(nombreValidado);
        rol.setDescripcion(descripcionValidada);
        rol.setActivo(true);

        Rol guardado = rolRepository.saveAndFlush(rol);

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "rol",
                        String.valueOf(guardado.getId()),
                        AccionAuditoria.CREAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        null,
                        snapshot(guardado),
                        null
                )
        );

        return guardado;
    }

    // =====================================================
    // ACTUALIZAR ROL
    // =====================================================

    @Transactional
    public Rol actualizar(
            Integer id,
            String nombre,
            String descripcion
    ) {

        permisoCatalogoService.exigirPermiso("role/update");

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (id == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el rol que desea actualizar."
            );
        }

        Rol rol = rolRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El rol solicitado no existe."
                        )
                );

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new IllegalStateException(
                    "No se puede editar un rol inactivo."
            );
        }

        String nombreValidado = validarNombre(nombre);

        String descripcionValidada =
                validarDescripcion(descripcion);

        Map<String, Object> anterior = snapshot(rol);

        boolean cambioNombre =
                !rol.getNombre().equals(nombreValidado);

        if (cambioNombre) {

            // Impedir renombrar roles esenciales.

            if (esRolProtegido(rol.getNombre())) {
                throw new IllegalStateException(
                        "No se puede cambiar el nombre "
                                + "de un rol esencial del sistema."
                );
            }

            // Impedir que otro rol adopte un nombre
            // que Spring Security reconoce como esencial.

            if (esRolProtegido(nombreValidado)) {
                throw new IllegalStateException(
                        "No se puede utilizar el nombre "
                                + "de un rol esencial."
                );
            }

            // También protege usuarios inactivos.

            if (usuarioRepository.existsByRol_Id(id)) {
                throw new IllegalStateException(
                        "No se puede cambiar el nombre "
                                + "porque el rol tiene usuarios asignados."
                );
            }

            validarNombreDuplicado(nombreValidado, id);

            rol.setNombre(nombreValidado);
        }

        rol.setDescripcion(descripcionValidada);

        Rol actualizado = rolRepository.saveAndFlush(rol);

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "rol",
                        String.valueOf(actualizado.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        anterior,
                        snapshot(actualizado),
                        null
                )
        );

        return actualizado;
    }

    // =====================================================
    // DESACTIVAR ROL
    // =====================================================

    @Transactional
    public void desactivar(Integer id) {

        permisoCatalogoService.exigirPermiso("role/delete");

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (id == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el rol que desea desactivar."
            );
        }

        Rol rol = rolRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El rol solicitado no existe."
                        )
                );

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new IllegalStateException(
                    "El rol ya se encuentra inactivo."
            );
        }

        // Protección especial de los ocho roles principales.

        if (esRolProtegido(rol.getNombre())) {
            throw new IllegalStateException(
                    "No se puede desactivar un rol esencial "
                            + "del sistema."
            );
        }

        // Incluye usuarios activos e inactivos.

        if (usuarioRepository.existsByRol_Id(id)) {
            throw new IllegalStateException(
                    "No se puede desactivar el rol porque "
                            + "tiene usuarios asignados."
            );
        }

        Map<String, Object> anterior = snapshot(rol);

        // Eliminación lógica: nunca DELETE físico.

        rol.setActivo(false);

        Rol actualizado = rolRepository.saveAndFlush(rol);

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "rol",
                        String.valueOf(actualizado.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        anterior,
                        Map.of(
                                "nombre", actualizado.getNombre(),
                                "activo", false,
                                "operacion", "DESACTIVACION_LOGICA"
                        ),
                        null
                )
        );
    }

    // =====================================================
    // VALIDACIÓN DE NOMBRE
    // =====================================================

    private String validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        String limpio =
                nombre.trim().replaceAll("\\s+", " ");

        if (limpio.length() > 200) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar 200 caracteres."
            );
        }

        if (normalizarAutoridad(limpio).isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre debe contener letras o números válidos."
            );
        }

        return limpio;
    }

    // =====================================================
    // VALIDACIÓN DE DESCRIPCIÓN
    // =====================================================

    private String validarDescripcion(String descripcion) {

        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }

        String limpio = descripcion.trim();

        if (limpio.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar 500 caracteres."
            );
        }

        return limpio;
    }

    // =====================================================
    // EVITAR DUPLICADOS Y COLISIONES DE AUTORIDAD
    // =====================================================

    private void validarNombreDuplicado(
            String nombre,
            Integer idExcluir
    ) {

        boolean existe;

        if (idExcluir == null) {

            existe = rolRepository
                    .existsByNombreIgnoreCaseAndActivoTrue(nombre);

        } else {

            existe = rolRepository
                    .existsByNombreIgnoreCaseAndActivoTrueAndIdNot(
                            nombre,
                            idExcluir
                    );
        }

        if (existe) {
            throw new IllegalStateException(
                    "Ya existe un registro con el nombre "
                            + nombre + "."
            );
        }

        // El JWT normaliza tildes, espacios y signos.
        // Evitamos que dos roles activos terminen generando
        // la misma autoridad ROLE_*.

        String autoridadNueva = normalizarAutoridad(nombre);

        for (Rol existente : rolRepository.findAll()) {

            if (idExcluir != null
                    && idExcluir.equals(existente.getId())) {
                continue;
            }

            if (!Boolean.TRUE.equals(existente.getActivo())) {
                continue;
            }

            if (autoridadNueva.equals(
                    normalizarAutoridad(existente.getNombre())
            )) {

                throw new IllegalStateException(
                        "Ya existe un rol activo con "
                                + "un nombre equivalente."
                );
            }
        }
    }

    // =====================================================
    // PROTEGER ROLES ESENCIALES
    // =====================================================

    private boolean esRolProtegido(String nombre) {

        return ROLES_PROTEGIDOS.contains(
                normalizarAutoridad(nombre)
        );
    }

    // =====================================================
    // NORMALIZACIÓN IGUAL AL CONVERSOR JWT
    // =====================================================

    private String normalizarAutoridad(String nombre) {

        if (nombre == null) {
            return "";
        }

        String sinTildes = Normalizer.normalize(
                nombre.trim(),
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return sinTildes
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    // =====================================================
    // SNAPSHOT PARA AUDITORÍA
    // =====================================================

    private Map<String, Object> snapshot(Rol rol) {

        return Map.of(
                "nombre", rol.getNombre(),
                "descripcion",
                rol.getDescripcion() == null
                        ? ""
                        : rol.getDescripcion(),
                "activo", Boolean.TRUE.equals(rol.getActivo())
        );
    }
}
