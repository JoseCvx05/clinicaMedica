
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.EspecialidadRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE ESPECIALIDADES
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Crear especialidades.
 * - Actualizar especialidades existentes.
 * - Desactivar especialidades mediante borrado lógico.
 * - Validar campos obligatorios.
 * - Validar longitudes máximas.
 * - Evitar nombres activos duplicados.
 * - Verificar permisos por operación.
 * - Proteger especialidades utilizadas por médicos.
 * - Proteger citas que todavía no están cerradas.
 * - Proteger asignaciones activas con sucursales.
 * - Registrar auditoría.
 *
 * IMPORTANTE:
 *
 * La eliminación es lógica.
 *
 * NO se eliminan físicamente registros.
 *
 * Este servicio conserva las referencias históricas
 * de especialidades utilizadas en otros módulos.
 *
 * =========================================================
 */
@Service
public class EspecialidadMantenimientoService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int MAX_NOMBRE = 200;

    private static final int MAX_DESCRIPCION = 500;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EspecialidadRepository especialidadRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    // =====================================================
    // CU-14 - DEPENDENCIAS PARA DESACTIVACIÓN SEGURA
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final CitaRepository citaRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EspecialidadMantenimientoService(
            EspecialidadRepository especialidadRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            SucursalEspecialidadRepository
                    sucursalEspecialidadRepository
    ) {

        this.especialidadRepository = especialidadRepository;

        this.permisoCatalogoService = permisoCatalogoService;

        this.usuarioActualService = usuarioActualService;

        this.auditoriaService = auditoriaService;

        this.usuarioRepository = usuarioRepository;

        this.citaRepository = citaRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;
    }

    // =====================================================
    // CU-14 - CREAR ESPECIALIDAD
    // =====================================================

    @Transactional
    public Especialidad crear(
            String nombre,
            String descripcion
    ) {

        // Verificar autorización específica
        permisoCatalogoService.exigirPermiso(
                "specialty/create"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // Normalizar y validar campos
        String nombreNormalizado = validarNombre(nombre);

        String descripcionNormalizada =
                validarDescripcion(descripcion);

        // Impedir dos especialidades activas
        // con el mismo nombre.
        if (especialidadRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombreNormalizado
                )) {

            throw new IllegalArgumentException(
                    "Ya existe un registro con el nombre "
                            + nombreNormalizado
                            + "."
            );
        }

        // Crear registro
        Especialidad especialidad = new Especialidad();

        especialidad.setNombre(nombreNormalizado);

        especialidad.setDescripcion(
                descripcionNormalizada
        );

        especialidad.setActivo(true);

        // Guardar y forzar validación de PostgreSQL
        Especialidad guardada =
                especialidadRepository.saveAndFlush(
                        especialidad
                );

        // Preparar auditoría
        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "nombre",
                guardada.getNombre()
        );

        valoresNuevos.put(
                "descripcion",
                guardada.getDescripcion()
        );

        valoresNuevos.put(
                "activo",
                guardada.getActivo()
        );

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "especialidad",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.CREAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        null,
                        valoresNuevos,
                        null
                )
        );

        return guardada;
    }

    // =====================================================
    // CU-14 - ACTUALIZAR ESPECIALIDAD
    // =====================================================

    @Transactional
    public Especialidad actualizar(
            Integer id,
            String nombre,
            String descripcion
    ) {

        // Verificar autorización específica
        permisoCatalogoService.exigirPermiso(
                "specialty/update"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad válida."
            );
        }

        // Buscar registro existente
        Especialidad especialidad =
                especialidadRepository.findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La especialidad seleccionada no existe."
                                )
                        );

        // Validar nuevos valores
        String nombreNormalizado = validarNombre(nombre);

        String descripcionNormalizada =
                validarDescripcion(descripcion);

        // Verificar duplicados cuando la especialidad
        // que estamos editando se encuentra activa.
        if (Boolean.TRUE.equals(especialidad.getActivo())) {

            especialidadRepository
                    .findByNombreIgnoreCaseAndActivoTrue(
                            nombreNormalizado
                    )
                    .ifPresent(existente -> {

                        if (!existente.getId().equals(id)) {

                            throw new IllegalArgumentException(
                                    "Ya existe un registro con el nombre "
                                            + nombreNormalizado
                                            + "."
                            );
                        }
                    });
        }

        // Guardar valores anteriores
        Map<String, Object> valoresAnteriores =
                new LinkedHashMap<>();

        valoresAnteriores.put(
                "nombre",
                especialidad.getNombre()
        );

        valoresAnteriores.put(
                "descripcion",
                especialidad.getDescripcion()
        );

        valoresAnteriores.put(
                "activo",
                especialidad.getActivo()
        );

        // Actualizar únicamente los campos editables.
        // El estado se manejará mediante operaciones
        // independientes y protegidas.
        especialidad.setNombre(nombreNormalizado);

        especialidad.setDescripcion(
                descripcionNormalizada
        );

        Especialidad guardada =
                especialidadRepository.saveAndFlush(
                        especialidad
                );

        // Guardar valores nuevos
        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "nombre",
                guardada.getNombre()
        );

        valoresNuevos.put(
                "descripcion",
                guardada.getDescripcion()
        );

        valoresNuevos.put(
                "activo",
                guardada.getActivo()
        );

        // Registrar auditoría
        auditoriaService.registrar(
                new RegistroAuditoria(
                        "especialidad",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                )
        );

        return guardada;
    }

    // =====================================================
    // CU-14 - DESACTIVAR ESPECIALIDAD
    // =====================================================
    //
    // Implementa eliminación lógica segura.
    //
    // REGLAS:
    //
    // 1. Solo usuarios autorizados pueden desactivar.
    // 2. La especialidad debe existir y estar activa.
    // 3. No debe tener médicos activos asignados.
    // 4. No debe tener citas sin cerrar.
    // 5. No debe tener asignaciones activas con sedes.
    // 6. No se eliminan registros físicamente.
    // 7. Toda desactivación se registra en auditoría.
    //
    // =====================================================

    @Transactional
    public void desactivar(Integer id) {

        // =================================================
        // VALIDAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "specialty/delete"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad válida."
            );
        }

        // =================================================
        // BUSCAR ESPECIALIDAD CON BLOQUEO
        // =================================================
        //
        // Bloqueamos el registro para impedir que dos
        // administradores lo desactiven simultáneamente.
        //
        // =================================================

        Especialidad especialidad =
                especialidadRepository
                        .buscarPorIdConBloqueo(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La especialidad seleccionada no existe."
                                )
                        );

        // =================================================
        // COMPROBAR ESTADO ACTUAL
        // =================================================

        if (!Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            throw new IllegalStateException(
                    "La especialidad ya se encuentra inactiva."
            );
        }

        // =================================================
        // VERIFICAR MÉDICOS ACTIVOS
        // =================================================
        //
        // No importa la sucursal.
        //
        // Se comprueban las denominaciones habituales
        // del rol para evitar diferencias por tildes.
        //
        // =================================================

        boolean tieneMedicosActivos =
                usuarioRepository
                        .existsByActivoTrueAndRol_NombreIgnoreCaseAndEspecialidad_Id(
                                "Médico",
                                id
                        );

        if (!tieneMedicosActivos) {

            tieneMedicosActivos =
                    usuarioRepository
                            .existsByActivoTrueAndRol_NombreIgnoreCaseAndEspecialidad_Id(
                                    "Medico",
                                    id
                            );
        }

        if (tieneMedicosActivos) {

            throw new IllegalStateException(
                    "No se puede desactivar la especialidad "
                            + "porque tiene médicos activos asignados."
            );
        }

        // =================================================
        // VERIFICAR CITAS NO CERRADAS
        // =================================================
        //
        // Comprueba citas de cualquier sucursal.
        //
        // No bloqueamos únicamente por citas históricas
        // que ya están finalizadas o canceladas.
        //
        // =================================================

        boolean tieneCitasNoCerradas =
                citaRepository
                        .existeCitaNoCerradaEnEspecialidad(id);

        if (tieneCitasNoCerradas) {

            throw new IllegalStateException(
                    "No se puede desactivar la especialidad "
                            + "porque tiene citas pendientes "
                            + "o en proceso de atención."
            );
        }

        // =================================================
        // VERIFICAR ASIGNACIONES ACTIVAS CON SUCURSALES
        // =================================================
        //
        // Antes de desactivar una especialidad global,
        // deben desactivarse sus relaciones activas
        // mediante el CU-13.
        //
        // =================================================

        boolean tieneSucursalesAsignadas =
                sucursalEspecialidadRepository
                        .existsByEspecialidad_IdAndActivoTrue(id);

        if (tieneSucursalesAsignadas) {

            throw new IllegalStateException(
                    "No se puede desactivar la especialidad "
                            + "porque tiene asignaciones activas "
                            + "con sucursales. "
                            + "Primero debe desactivar esas "
                            + "asignaciones en el CU-13."
            );
        }

        // =================================================
        // PREPARAR VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                new LinkedHashMap<>();

        valoresAnteriores.put(
                "nombre",
                especialidad.getNombre()
        );

        valoresAnteriores.put(
                "descripcion",
                especialidad.getDescripcion()
        );

        valoresAnteriores.put(
                "activo",
                especialidad.getActivo()
        );

        // =================================================
        // REALIZAR ELIMINACIÓN LÓGICA
        // =================================================
        //
        // NO se utiliza delete().
        //
        // El registro continúa existiendo en PostgreSQL
        // y conserva sus relaciones históricas.
        //
        // =================================================

        especialidad.setActivo(false);

        Especialidad guardada =
                especialidadRepository.saveAndFlush(
                        especialidad
                );

        // =================================================
        // PREPARAR VALORES NUEVOS
        // =================================================

        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "nombre",
                guardada.getNombre()
        );

        valoresNuevos.put(
                "descripcion",
                guardada.getDescripcion()
        );

        valoresNuevos.put(
                "activo",
                guardada.getActivo()
        );

        // =================================================
        // REGISTRAR AUDITORÍA
        // =================================================
        //
        // Se utiliza la acción ELIMINAR porque representa
        // la desactivación lógica del catálogo.
        //
        // Si falla la auditoría, la transacción debe
        // revertir también el cambio de estado.
        //
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "especialidad",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.ELIMINAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                )
        );
    }

    // =====================================================
    // VALIDAR NOMBRE
    // =====================================================

    private String validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        // Eliminar espacios sobrantes
        String normalizado = nombre
                .trim()
                .replaceAll("\\s+", " ");

        if (normalizado.length() > MAX_NOMBRE) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder los "
                            + MAX_NOMBRE
                            + " caracteres."
            );
        }

        return normalizado;
    }

    // =====================================================
    // VALIDAR DESCRIPCIÓN
    // =====================================================

    private String validarDescripcion(
            String descripcion
    ) {

        if (descripcion == null
                || descripcion.isBlank()) {

            throw new IllegalArgumentException(
                    "La descripción es obligatoria."
            );
        }

        String normalizada = descripcion.trim();

        if (normalizada.length() > MAX_DESCRIPCION) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder los "
                            + MAX_DESCRIPCION
                            + " caracteres."
            );
        }

        return normalizada;
    }
}
