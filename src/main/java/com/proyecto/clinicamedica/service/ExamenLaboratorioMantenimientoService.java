
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.ExamenLaboratorio;
import com.proyecto.clinicamedica.entity.Laboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.ExamenLaboratorioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE EXÁMENES DE LABORATORIO
 * =========================================================
 *
 * Catálogo 5 de 9.
 *
 * Responsabilidades:
 *
 * - Crear exámenes de laboratorio.
 * - Editar exámenes existentes.
 * - Actualizar precios del catálogo.
 * - Validar nombres duplicados.
 * - Validar laboratorio activo.
 * - Proteger exámenes utilizados en órdenes.
 * - Desactivar registros mediante eliminación lógica.
 * - Registrar auditoría.
 * - Verificar permisos por operación.
 *
 * SEGURIDAD:
 *
 * - lab-exam/create
 * - lab-exam/update
 * - lab-exam/delete
 *
 * PROTECCIÓN:
 *
 * - No elimina exámenes físicamente.
 * - No modifica órdenes históricas.
 * - No modifica detalles de órdenes.
 * - No altera precios históricos.
 * - No altera firmas digitales existentes.
 *
 * =========================================================
 */
@Service
public class ExamenLaboratorioMantenimientoService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int MAX_NOMBRE = 200;

    private static final int MAX_DESCRIPCION = 500;

    private static final int MAX_RANGO = 255;

    /*
     * El detalle histórico de una orden admite
     * unidades de hasta 100 caracteres.
     */
    private static final int MAX_UNIDAD = 100;

    /*
     * Precio 10,2:
     *
     * Máximo permitido: 99,999,999.99
     */
    private static final BigDecimal PRECIO_MAXIMO =
            new BigDecimal("99999999.99");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ExamenLaboratorioRepository
            examenLaboratorioRepository;

    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;

    private final PermisoCatalogoService
            permisoCatalogoService;

    private final UsuarioActualService
            usuarioActualService;

    private final AuditoriaService
            auditoriaService;

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ExamenLaboratorioMantenimientoService(

            ExamenLaboratorioRepository examenLaboratorioRepository,

            DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository,

            PermisoCatalogoService permisoCatalogoService,

            UsuarioActualService usuarioActualService,

            AuditoriaService auditoriaService
    ) {

        this.examenLaboratorioRepository =
                examenLaboratorioRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.permisoCatalogoService =
                permisoCatalogoService;

        this.usuarioActualService =
                usuarioActualService;

        this.auditoriaService =
                auditoriaService;
    }

    // =====================================================
    // CU-14 - CREAR EXAMEN
    // =====================================================

    @Transactional
    public ExamenLaboratorio crear(

            String nombre,

            String descripcion,

            BigDecimal precio,

            String rangoReferencia,

            String unidad,

            Integer idLaboratorio
    ) {

        // =================================================
        // VALIDAR PERMISOS
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/create"
        );

        // =================================================
        // VALIDAR CAMPOS
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarTextoOpcional(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        BigDecimal precioValidado =
                validarPrecio(precio);

        String rangoLimpio =
                validarTextoOpcional(
                        rangoReferencia,
                        MAX_RANGO,
                        "El rango de referencia"
                );

        String unidadLimpia =
                validarTextoOpcional(
                        unidad,
                        MAX_UNIDAD,
                        "La unidad"
                );

        // =================================================
        // VALIDAR LABORATORIO
        // =================================================

        Laboratorio laboratorio =
                buscarLaboratorioActivoConBloqueo(
                        idLaboratorio
                );

        // =================================================
        // VALIDAR NOMBRE DUPLICADO
        // =================================================

        if (existeNombreActivo(
                nombreLimpio,
                null
        )) {

            throw new IllegalStateException(
                    "Ya existe un examen de laboratorio activo "
                            + "con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // CONSTRUIR EXAMEN
        // =================================================

        ExamenLaboratorio examen =
                new ExamenLaboratorio();

        examen.setNombre(nombreLimpio);

        examen.setDescripcion(descripcionLimpia);

        examen.setPrecio(precioValidado);

        examen.setRangoReferencia(rangoLimpio);

        examen.setUnidad(unidadLimpia);

        examen.setLaboratorio(laboratorio);

        examen.setActivo(true);

        // =================================================
        // VALIDACIÓN DE DOMINIO
        // =================================================

        examen.validar();

        // =================================================
        // GUARDAR EN POSTGRESQL
        // =================================================

        ExamenLaboratorio guardado =
                examenLaboratorioRepository
                        .saveAndFlush(examen);

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
    // CU-14 - ACTUALIZAR EXAMEN
    // =====================================================

    @Transactional
    public ExamenLaboratorio actualizar(

            Integer id,

            String nombre,

            String descripcion,

            BigDecimal precio,

            String rangoReferencia,

            String unidad,

            Integer idLaboratorio
    ) {

        // =================================================
        // VALIDAR PERMISOS
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/update"
        );

        // =================================================
        // VALIDAR CAMPOS
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarTextoOpcional(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        BigDecimal precioValidado =
                validarPrecio(precio);

        String rangoLimpio =
                validarTextoOpcional(
                        rangoReferencia,
                        MAX_RANGO,
                        "El rango de referencia"
                );

        String unidadLimpia =
                validarTextoOpcional(
                        unidad,
                        MAX_UNIDAD,
                        "La unidad"
                );

        // =================================================
        // BUSCAR EXAMEN CON BLOQUEO
        // =================================================

        ExamenLaboratorio examen =
                buscarExamenConBloqueo(id);

        // =================================================
        // VALIDAR LABORATORIO DESTINO
        // =================================================

        Laboratorio laboratorio =
                buscarLaboratorioActivoConBloqueo(
                        idLaboratorio
                );

        // =================================================
        // PROTEGER CAMBIO DE LABORATORIO
        // =================================================
        //
        // Si el examen ya aparece en órdenes históricas,
        // no permitimos trasladarlo a otro laboratorio.
        //
        // Sí permitimos actualizar:
        //
        // - Nombre.
        // - Descripción.
        // - Precio.
        // - Rango.
        // - Unidad.
        //
        // Los detalles históricos conservan sus
        // propios snapshots.
        //
        // =================================================

        Integer idLaboratorioAnterior =
                examen.getLaboratorio() == null
                        ? null
                        : examen.getLaboratorio().getId();

        boolean cambioLaboratorio =
                !idLaboratorio.equals(
                        idLaboratorioAnterior
                );

        if (cambioLaboratorio
                && detalleOrdenLaboratorioRepository
                .existsByExamenLaboratorio_Id(id)) {

            throw new IllegalStateException(
                    "No se puede cambiar el laboratorio "
                            + "de un examen utilizado en "
                            + "órdenes médicas existentes."
            );
        }

        // =================================================
        // VALIDAR DUPLICADOS
        // =================================================

        if (examen.isActivo()
                && existeNombreActivo(
                nombreLimpio,
                id
        )) {

            throw new IllegalStateException(
                    "Ya existe un examen de laboratorio activo "
                            + "con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(examen);

        // =================================================
        // ACTUALIZAR CAMPOS
        // =================================================

        examen.setNombre(nombreLimpio);

        examen.setDescripcion(descripcionLimpia);

        examen.setPrecio(precioValidado);

        examen.setRangoReferencia(rangoLimpio);

        examen.setUnidad(unidadLimpia);

        examen.setLaboratorio(laboratorio);

        /*
         * No modificar:
         *
         * - ID del examen.
         * - Estado activo/inactivo.
         * - Órdenes de laboratorio.
         * - Detalles históricos.
         */

        examen.validar();

        // =================================================
        // GUARDAR
        // =================================================

        ExamenLaboratorio actualizado =
                examenLaboratorioRepository
                        .saveAndFlush(examen);

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
    // CU-14 - DESACTIVAR EXAMEN
    // =====================================================

    @Transactional
    public void desactivar(
            Integer id
    ) {

        // =================================================
        // VALIDAR PERMISOS
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "lab-exam/delete"
        );

        // =================================================
        // BUSCAR EXAMEN CON BLOQUEO
        // =================================================

        ExamenLaboratorio examen =
                buscarExamenConBloqueo(id);

        // =================================================
        // VALIDAR ESTADO
        // =================================================

        if (!examen.isActivo()) {

            throw new IllegalStateException(
                    "El examen de laboratorio ya se "
                            + "encuentra inactivo."
            );
        }

        // =================================================
        // PROTEGER ÓRDENES EXISTENTES
        // =================================================
        //
        // Por seguridad, no permitimos desactivar
        // exámenes que aparezcan en órdenes registradas.
        //
        // Esta es una restricción conservadora para
        // proteger las referencias clínicas.
        //
        // =================================================

        if (detalleOrdenLaboratorioRepository
                .existsByExamenLaboratorio_Id(id)) {

            throw new IllegalStateException(
                    "No se puede desactivar el examen "
                            + "porque está asociado a "
                            + "órdenes de laboratorio existentes."
            );
        }

        // =================================================
        // VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(examen);

        // =================================================
        // DESACTIVACIÓN LÓGICA
        // =================================================

        examen.setActivo(false);

        examenLaboratorioRepository.saveAndFlush(
                examen
        );

        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoria(
                examen,
                AccionAuditoria.ELIMINAR,
                valoresAnteriores,
                crearSnapshot(examen)
        );
    }

    // =====================================================
    // BUSCAR EXAMEN CON BLOQUEO PESIMISTA
    // =====================================================

    private ExamenLaboratorio buscarExamenConBloqueo(
            Integer id
    ) {

        validarId(
                id,
                "examen de laboratorio"
        );

        ExamenLaboratorio examen =
                entityManager.find(
                        ExamenLaboratorio.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (examen == null) {

            throw new IllegalArgumentException(
                    "El examen de laboratorio solicitado no existe."
            );
        }

        return examen;
    }

    // =====================================================
    // BUSCAR LABORATORIO ACTIVO CON BLOQUEO
    // =====================================================

    private Laboratorio buscarLaboratorioActivoConBloqueo(
            Integer id
    ) {

        validarId(
                id,
                "laboratorio"
        );

        Laboratorio laboratorio =
                entityManager.find(
                        Laboratorio.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (laboratorio == null
                || !laboratorio.isActivo()) {

            throw new IllegalStateException(
                    "Debe seleccionar un laboratorio activo y válido."
            );
        }

        return laboratorio;
    }

    // =====================================================
    // VALIDAR IDENTIFICADOR
    // =====================================================

    private void validarId(
            Integer id,
            String nombreCampo
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador de "
                            + nombreCampo
                            + " no es válido."
            );
        }
    }

    // =====================================================
    // VALIDAR NOMBRE
    // =====================================================

    private String validarNombre(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        String limpio =
                nombre.trim()
                        .replaceAll("\\s+", " ");

        if (limpio.length() > MAX_NOMBRE) {

            throw new IllegalArgumentException(
                    "El nombre del examen no puede superar "
                            + MAX_NOMBRE
                            + " caracteres."
            );
        }

        return limpio;
    }

    // =====================================================
    // VALIDAR TEXTO OPCIONAL
    // =====================================================

    private String validarTextoOpcional(

            String valor,

            int longitudMaxima,

            String nombreCampo
    ) {

        if (valor == null || valor.isBlank()) {

            return null;
        }

        String limpio =
                valor.trim();

        if (limpio.length() > longitudMaxima) {

            throw new IllegalArgumentException(
                    nombreCampo
                            + " no puede superar los "
                            + longitudMaxima
                            + " caracteres."
            );
        }

        return limpio;
    }

    // =====================================================
    // VALIDAR PRECIO
    // =====================================================
    //
    // Regla CU-14:
    //
    // - Obligatorio.
    // - Mayor a cero.
    // - Precisión 10,2.
    // - No se redondean cantidades inválidas.
    //
    // =====================================================

    private BigDecimal validarPrecio(
            BigDecimal precio
    ) {

        if (precio == null
                || precio.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "El precio base debe ser mayor a 0."
            );
        }

        if (precio.compareTo(PRECIO_MAXIMO) > 0) {

            throw new IllegalArgumentException(
                    "El precio base supera el máximo "
                            + "permitido para una precisión 10,2."
            );
        }

        try {

            return precio.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

        } catch (ArithmeticException ex) {

            throw new IllegalArgumentException(
                    "El precio base debe tener "
                            + "como máximo 2 decimales."
            );
        }
    }

    // =====================================================
    // VALIDAR NOMBRE DUPLICADO ACTIVO
    // =====================================================
    //
    // La consulta es específica de PostgreSQL.
    //
    // Normaliza:
    //
    // - Mayúsculas y minúsculas.
    // - Espacios al inicio y al final.
    // - Espacios internos repetidos.
    //
    // En una edición se excluye el mismo ID.
    //
    // La restricción definitiva frente a operaciones
    // simultáneas será un índice único PostgreSQL,
    // que verificaremos en los próximos pasos.
    //
    // =====================================================

    private boolean existeNombreActivo(

            String nombre,

            Integer idExcluir
    ) {

        String sql = """
                SELECT e.id
                FROM public.examen_laboratorio e
                WHERE e.activo IS TRUE
                  AND LOWER(
                      REGEXP_REPLACE(
                          BTRIM(e.nombre),
                          '[[:space:]]+',
                          ' ',
                          'g'
                      )
                  ) = :nombre
                  AND e.id <> :idExcluir
                LIMIT 1
                """;

        Integer idExcluido =
                idExcluir == null ? -1 : idExcluir;

        String nombreNormalizado =
                nombre.trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);

        return !entityManager
                .createNativeQuery(sql)
                .setParameter(
                        "nombre",
                        nombreNormalizado
                )
                .setParameter(
                        "idExcluir",
                        idExcluido
                )
                .getResultList()
                .isEmpty();
    }

    // =====================================================
    // CONSTRUIR SNAPSHOT DE AUDITORÍA
    // =====================================================

    private Map<String, Object> crearSnapshot(
            ExamenLaboratorio examen
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "id",
                examen.getId()
        );

        snapshot.put(
                "nombre",
                examen.getNombre()
        );

        snapshot.put(
                "descripcion",
                examen.getDescripcion()
        );

        snapshot.put(
                "precio",
                examen.getPrecio()
        );

        snapshot.put(
                "rangoReferencia",
                examen.getRangoReferencia()
        );

        snapshot.put(
                "unidad",
                examen.getUnidad()
        );

        snapshot.put(
                "idLaboratorio",
                examen.getLaboratorio() == null
                        ? null
                        : examen.getLaboratorio().getId()
        );

        snapshot.put(
                "activo",
                examen.isActivo()
        );

        return snapshot;
    }

    // =====================================================
    // REGISTRAR AUDITORÍA
    // =====================================================

    private void registrarAuditoria(

            ExamenLaboratorio examen,

            AccionAuditoria accion,

            Map<String, Object> valoresAnteriores,

            Map<String, Object> valoresNuevos
    ) {

        Usuario usuario =
                usuarioActualService
                        .obtenerUsuarioActual();

        if (usuario == null
                || usuario.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar "
                            + "al usuario ejecutor."
            );
        }

        RegistroAuditoria registro =
                new RegistroAuditoria(

                        "examen_laboratorio",

                        String.valueOf(
                                examen.getId()
                        ),

                        accion,

                        usuario.getId(),

                        usuario.getNombreUsuario(),

                        valoresAnteriores,

                        valoresNuevos,

                        null
                );

        auditoriaService.registrar(
                registro
        );
    }
}
