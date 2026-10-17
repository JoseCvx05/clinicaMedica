
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Laboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.ExamenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.LaboratorioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * =========================================================
 * CU-14 - SERVICIO DE MANTENIMIENTO DE LABORATORIOS
 * =========================================================
 *
 * Catálogo 4 - Laboratorios.
 *
 * Responsabilidades:
 *
 * - Crear laboratorios.
 * - Actualizar nombre y descripción.
 * - Validar datos obligatorios.
 * - Validar nombres duplicados activos.
 * - Desactivar laboratorios mediante eliminación lógica.
 * - Proteger laboratorios con exámenes relacionados.
 * - Registrar auditoría de modificaciones.
 * - Verificar permisos administrativos.
 *
 * SEGURIDAD:
 *
 * - laboratory/create
 * - laboratory/update
 * - laboratory/delete
 *
 * IMPORTANTE:
 *
 * - No elimina registros físicamente.
 * - No modifica exámenes relacionados.
 * - No modifica órdenes de laboratorio.
 * - No modifica citas ni consultas médicas.
 * - Conserva los identificadores existentes.
 *
 * Las operaciones de modificación se ejecutan
 * dentro de transacciones de PostgreSQL.
 *
 * =========================================================
 */
@Service
public class LaboratorioMantenimientoService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int MAX_NOMBRE = 255;

    private static final int MAX_DESCRIPCION = 255;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final LaboratorioRepository laboratorioRepository;

    private final ExamenLaboratorioRepository examenLaboratorioRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LaboratorioMantenimientoService(
            LaboratorioRepository laboratorioRepository,
            ExamenLaboratorioRepository examenLaboratorioRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService
    ) {

        this.laboratorioRepository = laboratorioRepository;

        this.examenLaboratorioRepository = examenLaboratorioRepository;

        this.permisoCatalogoService = permisoCatalogoService;

        this.usuarioActualService = usuarioActualService;

        this.auditoriaService = auditoriaService;
    }

    // =====================================================
    // CU-14 - CREAR LABORATORIO
    // =====================================================

    @Transactional
    public Laboratorio crear(
            String nombre,
            String descripcion
    ) {

        // =================================================
        // VALIDAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/create"
        );

        // =================================================
        // VALIDAR DATOS
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarDescripcion(descripcion);

        // =================================================
        // PREVENIR DUPLICADOS ACTIVOS
        // =================================================

        if (laboratorioRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombreLimpio
                )) {

            throw new IllegalStateException(
                    "Ya existe un laboratorio activo con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // CONSTRUIR LABORATORIO
        // =================================================

        Laboratorio laboratorio =
                new Laboratorio();

        laboratorio.setNombre(nombreLimpio);

        laboratorio.setDescripcion(descripcionLimpia);

        laboratorio.setActivo(true);

        // =================================================
        // GUARDAR EN POSTGRESQL
        // =================================================

        Laboratorio guardado =
                laboratorioRepository.saveAndFlush(
                        laboratorio
                );

        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoria(
                guardado,
                AccionAuditoria.CREAR,
                null,
                crearSnapshot(guardado)
        );

        return guardado;
    }

    // =====================================================
    // CU-14 - ACTUALIZAR LABORATORIO
    // =====================================================

    @Transactional
    public Laboratorio actualizar(
            Integer id,
            String nombre,
            String descripcion
    ) {

        // =================================================
        // VALIDAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/update"
        );

        // =================================================
        // VALIDAR DATOS
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarDescripcion(descripcion);

        // =================================================
        // BUSCAR LABORATORIO CON BLOQUEO
        // =================================================

        Laboratorio laboratorio =
                buscarConBloqueo(id);

        // =================================================
        // VALIDAR DUPLICADOS
        // =================================================

        String nombreActual =
                laboratorio.getNombre();

        boolean cambioNombre =
                !normalizarComparacion(nombreActual)
                        .equals(
                                normalizarComparacion(nombreLimpio)
                        );

        if (laboratorio.isActivo()
                && cambioNombre
                && laboratorioRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombreLimpio
                )) {

            throw new IllegalStateException(
                    "Ya existe un laboratorio activo con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // CAPTURAR VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(laboratorio);

        // =================================================
        // ACTUALIZAR CAMPOS PERMITIDOS
        // =================================================

        laboratorio.setNombre(nombreLimpio);

        laboratorio.setDescripcion(descripcionLimpia);

        // IMPORTANTE:
        //
        // No modificar el campo activo.
        // No modificar el identificador.
        // No modificar exámenes relacionados.

        // =================================================
        // GUARDAR
        // =================================================

        Laboratorio actualizado =
                laboratorioRepository.saveAndFlush(
                        laboratorio
                );

        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoria(
                actualizado,
                AccionAuditoria.ACTUALIZAR,
                valoresAnteriores,
                crearSnapshot(actualizado)
        );

        return actualizado;
    }

    // =====================================================
    // CU-14 - DESACTIVAR LABORATORIO
    // =====================================================
    //
    // Eliminación lógica.
    //
    // Antes de desactivar:
    //
    // - Verificar permisos.
    // - Validar identificador.
    // - Bloquear el laboratorio.
    // - Verificar que esté activo.
    // - Proteger exámenes relacionados.
    // - Registrar auditoría.
    //
    // =====================================================

    @Transactional
    public void desactivar(
            Integer id
    ) {

        // =================================================
        // VALIDAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "laboratory/delete"
        );

        // =================================================
        // OBTENER REGISTRO CON BLOQUEO
        // =================================================

        Laboratorio laboratorio =
                buscarConBloqueo(id);

        // =================================================
        // VALIDAR ESTADO
        // =================================================

        if (!laboratorio.isActivo()) {

            throw new IllegalStateException(
                    "El laboratorio ya se encuentra inactivo."
            );
        }

        // =================================================
        // PROTEGER EXÁMENES ACTIVOS
        // =================================================

        if (examenLaboratorioRepository
                .existsByLaboratorio_IdAndActivoTrue(id)) {

            throw new IllegalStateException(
                    "No se puede desactivar el laboratorio "
                            + "porque tiene exámenes activos asociados."
            );
        }

        // =================================================
        // PROTEGER RELACIONES HISTÓRICAS
        // =================================================
        //
        // Por seguridad, también se impide desactivar
        // laboratorios con exámenes históricos relacionados.
        //
        // Esto evita afectar operaciones posteriores
        // sobre esos exámenes y preserva el CU-08 / CU-09.
        //
        // =================================================

        if (examenLaboratorioRepository
                .existsByLaboratorio_Id(id)) {

            throw new IllegalStateException(
                    "No se puede desactivar el laboratorio "
                            + "porque tiene exámenes históricos asociados."
            );
        }

        // =================================================
        // CAPTURAR VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(laboratorio);

        // =================================================
        // DESACTIVACIÓN LÓGICA
        // =================================================

        laboratorio.setActivo(false);

        laboratorioRepository.saveAndFlush(
                laboratorio
        );

        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoria(
                laboratorio,
                AccionAuditoria.ELIMINAR,
                valoresAnteriores,
                crearSnapshot(laboratorio)
        );
    }

    // =====================================================
    // BUSCAR CON BLOQUEO PESIMISTA
    // =====================================================

    private Laboratorio buscarConBloqueo(
            Integer id
    ) {

        validarId(id);

        Laboratorio laboratorio =
                entityManager.find(
                        Laboratorio.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (laboratorio == null) {

            throw new IllegalArgumentException(
                    "El laboratorio solicitado no existe."
            );
        }

        return laboratorio;
    }

    // =====================================================
    // VALIDAR IDENTIFICADOR
    // =====================================================

    private void validarId(
            Integer id
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del laboratorio no es válido."
            );
        }
    }

    // =====================================================
    // VALIDAR NOMBRE
    // =====================================================

    private String validarNombre(
            String nombre
    ) {

        if (nombre == null || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del laboratorio es obligatorio."
            );
        }

        String limpio =
                nombre.trim()
                        .replaceAll("\\s+", " ");

        if (limpio.length() > MAX_NOMBRE) {

            throw new IllegalArgumentException(
                    "El nombre del laboratorio no puede superar "
                            + MAX_NOMBRE + " caracteres."
            );
        }

        return limpio;
    }

    // =====================================================
    // VALIDAR DESCRIPCIÓN
    // =====================================================

    private String validarDescripcion(
            String descripcion
    ) {

        if (descripcion == null || descripcion.isBlank()) {

            return null;
        }

        String limpia =
                descripcion.trim();

        if (limpia.length() > MAX_DESCRIPCION) {

            throw new IllegalArgumentException(
                    "La descripción del laboratorio no puede superar "
                            + MAX_DESCRIPCION + " caracteres."
            );
        }

        return limpia;
    }

    // =====================================================
    // NORMALIZAR COMPARACIÓN DE NOMBRES
    // =====================================================

    private String normalizarComparacion(
            String nombre
    ) {

        if (nombre == null) {

            return "";
        }

        return nombre
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    // =====================================================
    // CONSTRUIR SNAPSHOT DE AUDITORÍA
    // =====================================================

    private Map<String, Object> crearSnapshot(
            Laboratorio laboratorio
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "id",
                laboratorio.getId()
        );

        snapshot.put(
                "nombre",
                laboratorio.getNombre()
        );

        snapshot.put(
                "descripcion",
                laboratorio.getDescripcion()
        );

        snapshot.put(
                "activo",
                laboratorio.isActivo()
        );

        return snapshot;
    }

    // =====================================================
    // REGISTRAR AUDITORÍA
    // =====================================================

    private void registrarAuditoria(
            Laboratorio laboratorio,
            AccionAuditoria accion,
            Map<String, Object> valoresAnteriores,
            Map<String, Object> valoresNuevos
    ) {

        Usuario usuario =
                usuarioActualService.obtenerUsuarioActual();

        if (usuario == null || usuario.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al usuario ejecutor."
            );
        }

        RegistroAuditoria registro =
                new RegistroAuditoria(
                        "laboratorio",
                        String.valueOf(laboratorio.getId()),
                        accion,
                        usuario.getId(),
                        usuario.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                );

        auditoriaService.registrar(registro);
    }
}
