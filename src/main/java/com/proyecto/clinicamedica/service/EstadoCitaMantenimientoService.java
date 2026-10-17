
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.config.CacheConfig;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE ESTADOS DE CITA
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Crear estados de cita.
 * - Actualizar estados existentes.
 * - Desactivar estados mediante eliminación lógica.
 * - Validar campos y longitudes máximas.
 * - Evitar nombres activos duplicados.
 * - Proteger estados operativos del sistema.
 * - Conservar las citas históricas.
 * - Verificar permisos específicos.
 * - Registrar auditoría.
 * - Actualizar la caché después del commit.
 *
 * SEGURIDAD:
 *
 * - appointment-status/create
 * - appointment-status/update
 * - appointment-status/delete
 *
 * IMPORTANTE:
 *
 * Los estados críticos forman parte de los flujos
 * operativos de los CU anteriores.
 *
 * No se permite cambiar sus nombres ni desactivarlos.
 *
 * Se permite actualizar sus descripciones.
 *
 * Las eliminaciones son únicamente lógicas.
 *
 * No se modifica directamente ninguna cita.
 *
 * =========================================================
 */
@Service
public class EstadoCitaMantenimientoService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int MAX_NOMBRE = 50;

    private static final int MAX_DESCRIPCION = 200;

    // =====================================================
    // ESTADOS CRÍTICOS DEL SISTEMA
    // =====================================================
    //
    // Sus nombres son utilizados en diferentes módulos.
    //
    // No deben renombrarse ni desactivarse desde CU-14.
    //
    // =====================================================

    private static final Set<String> ESTADOS_PROTEGIDOS =
            Set.of(
                    "pendiente de pago",
                    "pagada",
                    "cancelada",
                    "confirmada",
                    "paciente presente",
                    "signos vitales",
                    "en espera",
                    "consulta médica",
                    "atención finalizada",
                    "no asistió"
            );

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final EstadoCitaRepository estadoCitaRepository;

    private final CitaRepository citaRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    private final CacheManager cacheManager;

    // =====================================================
    // ENTITY MANAGER
    // =====================================================
    //
    // Permite bloquear el registro que se modifica
    // mediante PESSIMISTIC_WRITE.
    //
    // =====================================================

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EstadoCitaMantenimientoService(
            EstadoCitaRepository estadoCitaRepository,
            CitaRepository citaRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService,
            CacheManager cacheManager
    ) {

        this.estadoCitaRepository = estadoCitaRepository;

        this.citaRepository = citaRepository;

        this.permisoCatalogoService = permisoCatalogoService;

        this.usuarioActualService = usuarioActualService;

        this.auditoriaService = auditoriaService;

        this.cacheManager = cacheManager;
    }

    // =====================================================
    // CU-14 - CREAR ESTADO DE CITA
    // =====================================================

    @Transactional
    public EstadoCita crear(
            String nombre,
            String descripcion
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/create"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR DATOS
        // =================================================

        String nombreNormalizado =
                validarNombre(nombre);

        String descripcionNormalizada =
                validarDescripcion(descripcion);

        // =================================================
        // PROTEGER NOMBRES RESERVADOS
        // =================================================
        //
        // Los nombres utilizados por los flujos del
        // sistema no deben recrearse manualmente.
        //
        // =================================================

        if (esEstadoProtegido(nombreNormalizado)) {

            throw new IllegalStateException(
                    "El nombre corresponde a un estado "
                            + "operativo reservado del sistema."
            );
        }

        // =================================================
        // VALIDAR DUPLICADOS ACTIVOS
        // =================================================

        if (estadoCitaRepository
                .findByNombreIgnoreCaseAndActivoTrue(
                        nombreNormalizado
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ya existe un estado de cita activo "
                            + "con el nombre "
                            + nombreNormalizado
                            + "."
            );
        }

        // =================================================
        // CONSTRUIR ESTADO
        // =================================================

        EstadoCita estado = new EstadoCita();

        estado.setNombre(nombreNormalizado);

        estado.setDescripcion(descripcionNormalizada);

        estado.setActivo(true);

        // =================================================
        // ORDEN DEL FLUJO
        // =================================================
        //
        // No asignamos automáticamente una posición.
        //
        // Los estados nuevos no se incorporan a las
        // transiciones operativas existentes.
        //
        // =================================================

        estado.setOrdenFlujo(null);

        // =================================================
        // GUARDAR
        // =================================================

        EstadoCita guardado =
                estadoCitaRepository.saveAndFlush(
                        estado
                );

        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "estado_cita",
                        String.valueOf(guardado.getId()),
                        AccionAuditoria.CREAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        null,
                        crearSnapshot(guardado),
                        null
                )
        );

        // =================================================
        // ACTUALIZAR CACHÉ DESPUÉS DEL COMMIT
        // =================================================

        programarInvalidacionCache();

        return guardado;
    }

    // =====================================================
    // CU-14 - ACTUALIZAR ESTADO DE CITA
    // =====================================================

    @Transactional
    public EstadoCita actualizar(
            Integer id,
            String nombre,
            String descripcion
    ) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/update"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        validarId(id);

        // =================================================
        // BLOQUEAR REGISTRO
        // =================================================

        EstadoCita estado =
                buscarConBloqueo(id);

        // =================================================
        // VALIDAR DATOS
        // =================================================

        String nombreNormalizado =
                validarNombre(nombre);

        String descripcionNormalizada =
                validarDescripcion(descripcion);

        // =================================================
        // PROTEGER ESTADOS CRÍTICOS
        // =================================================
        //
        // Si el estado es operativo, su nombre debe
        // permanecer exactamente como está.
        //
        // Su descripción sí puede actualizarse.
        //
        // =================================================

        boolean esCritico =
                esEstadoProtegido(estado.getNombre());

        boolean nombreModificado =
                !estado.getNombre().equals(nombreNormalizado);

        if (esCritico && nombreModificado) {

            throw new IllegalStateException(
                    "No se puede cambiar el nombre de "
                            + "un estado operativo del sistema."
            );
        }

        // =================================================
        // PROTEGER NOMBRES RESERVADOS
        // =================================================

        if (!esCritico
                && esEstadoProtegido(nombreNormalizado)) {

            throw new IllegalStateException(
                    "No puede utilizar el nombre de un "
                            + "estado operativo reservado."
            );
        }

        // =================================================
        // PROTEGER CITAS ASOCIADAS
        // =================================================
        //
        // No permitimos cambiar el nombre de un
        // estado utilizado por citas, incluso históricas.
        //
        // =================================================

        if (nombreModificado
                && citaRepository
                .existeCitaAsociadaAEstado(id)) {

            throw new IllegalStateException(
                    "No se puede modificar el nombre "
                            + "porque existen citas asociadas "
                            + "a este estado."
            );
        }

        // =================================================
        // VALIDAR DUPLICADOS ACTIVOS
        // =================================================

        if (Boolean.TRUE.equals(estado.getActivo())) {

            estadoCitaRepository
                    .findByNombreIgnoreCaseAndActivoTrue(
                            nombreNormalizado
                    )
                    .ifPresent(existente -> {

                        if (!existente.getId().equals(id)) {

                            throw new IllegalArgumentException(
                                    "Ya existe un estado de cita "
                                            + "activo con el nombre "
                                            + nombreNormalizado
                                            + "."
                            );
                        }
                    });
        }

        // =================================================
        // SNAPSHOT ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(estado);

        // =================================================
        // ACTUALIZAR CAMPOS PERMITIDOS
        // =================================================
        //
        // No modificamos:
        //
        // - ID.
        // - Estado activo.
        // - Orden del flujo.
        //
        // =================================================

        estado.setNombre(nombreNormalizado);

        estado.setDescripcion(descripcionNormalizada);

        // =================================================
        // GUARDAR
        // =================================================

        EstadoCita actualizado =
                estadoCitaRepository.saveAndFlush(
                        estado
                );

        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "estado_cita",
                        String.valueOf(actualizado.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        crearSnapshot(actualizado),
                        null
                )
        );

        programarInvalidacionCache();

        return actualizado;
    }

    // =====================================================
    // CU-14 - DESACTIVAR ESTADO DE CITA
    // =====================================================

    @Transactional
    public void desactivar(Integer id) {

        // =================================================
        // VERIFICAR PERMISO
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "appointment-status/delete"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR ID
        // =================================================

        validarId(id);

        // =================================================
        // BLOQUEAR REGISTRO
        // =================================================

        EstadoCita estado =
                buscarConBloqueo(id);

        // =================================================
        // VERIFICAR ESTADO ACTUAL
        // =================================================

        if (!Boolean.TRUE.equals(estado.getActivo())) {

            throw new IllegalStateException(
                    "El estado de cita ya se encuentra inactivo."
            );
        }

        // =================================================
        // PROTEGER ESTADOS OPERATIVOS
        // =================================================

        if (esEstadoProtegido(estado.getNombre())) {

            throw new IllegalStateException(
                    "No se puede desactivar este estado "
                            + "porque forma parte del flujo "
                            + "operativo de citas."
            );
        }

        // =================================================
        // PROTEGER CITAS HISTÓRICAS Y ACTIVAS
        // =================================================

        if (citaRepository
                .existeCitaAsociadaAEstado(id)) {

            throw new IllegalStateException(
                    "No se puede desactivar el estado "
                            + "porque existen citas asociadas."
            );
        }

        // =================================================
        // SNAPSHOT ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(estado);

        // =================================================
        // ELIMINACIÓN LÓGICA
        // =================================================
        //
        // No ejecutamos delete().
        //
        // El registro histórico permanece en PostgreSQL.
        //
        // =================================================

        estado.setActivo(false);

        EstadoCita guardado =
                estadoCitaRepository.saveAndFlush(
                        estado
                );

        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "estado_cita",
                        String.valueOf(guardado.getId()),
                        AccionAuditoria.ELIMINAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        crearSnapshot(guardado),
                        null
                )
        );

        programarInvalidacionCache();
    }

    // =====================================================
    // VALIDAR IDENTIFICADOR
    // =====================================================

    private void validarId(Integer id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un estado de cita válido."
            );
        }
    }

    // =====================================================
    // BUSCAR REGISTRO CON BLOQUEO
    // =====================================================

    private EstadoCita buscarConBloqueo(Integer id) {

        EstadoCita estado =
                entityManager.find(
                        EstadoCita.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (estado == null) {

            throw new IllegalArgumentException(
                    "El estado de cita seleccionado no existe."
            );
        }

        return estado;
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

        String normalizado =
                nombre.trim()
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

        if (descripcion == null || descripcion.isBlank()) {

            return null;
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

    // =====================================================
    // IDENTIFICAR ESTADOS PROTEGIDOS
    // =====================================================

    private boolean esEstadoProtegido(String nombre) {

        if (nombre == null) {

            return false;
        }

        String normalizado =
                nombre.trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);

        return ESTADOS_PROTEGIDOS.contains(
                normalizado
        );
    }

    // =====================================================
    // INVALIDAR CACHÉ DESPUÉS DEL COMMIT
    // =====================================================
    //
    // No se elimina la caché antes de confirmar
    // los cambios en PostgreSQL.
    //
    // Si la transacción falla, no se invalida.
    //
    // =====================================================

    private void programarInvalidacionCache() {

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {

                                    invalidarCache();
                                }
                            }
                    );

        } else {

            invalidarCache();
        }
    }

    // =====================================================
    // INVALIDAR CACHÉ DE ESTADOS
    // =====================================================

    private void invalidarCache() {

        Cache cache =
                cacheManager.getCache(
                        CacheConfig.ESTADOS_CITA
                );

        if (cache != null) {

            cache.clear();
        }
    }

    // =====================================================
    // SNAPSHOT PARA AUDITORÍA
    // =====================================================
    //
    // No contiene información sensible.
    //
    // LinkedHashMap permite valores null.
    //
    // =====================================================

    private Map<String, Object> crearSnapshot(
            EstadoCita estado
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "nombre",
                estado.getNombre()
        );

        snapshot.put(
                "descripcion",
                estado.getDescripcion()
        );

        snapshot.put(
                "ordenFlujo",
                estado.getOrdenFlujo()
        );

        snapshot.put(
                "activo",
                estado.getActivo()
        );

        return snapshot;
    }
}
