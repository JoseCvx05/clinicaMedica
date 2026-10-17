
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.MedicamentoRepository;

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
 * CU-14 - MANTENIMIENTO DE MEDICAMENTOS
 * =========================================================
 *
 * Catálogo 6 de 9.
 *
 * FUNCIONALIDADES:
 *
 * - Crear medicamentos.
 * - Editar medicamentos.
 * - Actualizar precios.
 * - Administrar indicador de medicamento controlado.
 * - Configurar stock mínimo.
 * - Validar nombres duplicados.
 * - Desactivar mediante eliminación lógica.
 * - Proteger inventarios y recetas pendientes.
 * - Registrar auditoría.
 * - Verificar permisos granulares.
 *
 * PROTECCIÓN:
 *
 * - No elimina físicamente medicamentos.
 * - No modifica recetas médicas.
 * - No altera precios históricos.
 * - No descuenta ni actualiza inventarios.
 * - No modifica despachos anteriores.
 *
 * =========================================================
 */
@Service
public class MedicamentoMantenimientoService {

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final int MAX_NOMBRE = 200;

    private static final int MAX_DESCRIPCION = 500;

    private static final int MAX_UNIDAD = 50;

    private static final BigDecimal PRECIO_MAXIMO =
            new BigDecimal("99999999.99");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MedicamentoRepository
            medicamentoRepository;

    private final InventarioFarmaciaRepository
            inventarioFarmaciaRepository;

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

    public MedicamentoMantenimientoService(

            MedicamentoRepository medicamentoRepository,

            InventarioFarmaciaRepository inventarioFarmaciaRepository,

            PermisoCatalogoService permisoCatalogoService,

            UsuarioActualService usuarioActualService,

            AuditoriaService auditoriaService
    ) {

        this.medicamentoRepository =
                medicamentoRepository;

        this.inventarioFarmaciaRepository =
                inventarioFarmaciaRepository;

        this.permisoCatalogoService =
                permisoCatalogoService;

        this.usuarioActualService =
                usuarioActualService;

        this.auditoriaService =
                auditoriaService;
    }

    // =====================================================
    // CU-14 - CREAR MEDICAMENTO
    // =====================================================

    @Transactional
    public Medicamento crear(

            String nombre,

            String descripcion,

            BigDecimal precio,

            String unidad,

            Boolean esControlado,

            Integer stockMinimo
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/create"
        );

        // =================================================
        // VALIDACIONES
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarTextoObligatorio(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        BigDecimal precioValidado =
                validarPrecio(precio);

        String unidadLimpia =
                validarTextoObligatorio(
                        unidad,
                        MAX_UNIDAD,
                        "La unidad"
                );

        validarControlado(esControlado);

        validarStockMinimo(stockMinimo);

        // =================================================
        // VALIDAR DUPLICADOS ACTIVOS
        // =================================================

        if (existeNombreActivo(
                nombreLimpio,
                null
        )) {

            throw new IllegalStateException(
                    "Ya existe un medicamento activo con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // CONSTRUIR MEDICAMENTO
        // =================================================

        Medicamento medicamento =
                new Medicamento();

        medicamento.setNombre(nombreLimpio);

        medicamento.setDescripcion(descripcionLimpia);

        medicamento.setPrecio(precioValidado);

        medicamento.setUnidad(unidadLimpia);

        medicamento.setEsControlado(esControlado);

        medicamento.setStockMinimo(stockMinimo);

        medicamento.setActivo(true);

        // =================================================
        // VALIDACIÓN DE DOMINIO
        // =================================================

        medicamento.validar();

        // =================================================
        // GUARDAR
        // =================================================

        Medicamento guardado =
                medicamentoRepository.saveAndFlush(
                        medicamento
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
    // CU-14 - ACTUALIZAR MEDICAMENTO
    // =====================================================

    @Transactional
    public Medicamento actualizar(

            Integer id,

            String nombre,

            String descripcion,

            BigDecimal precio,

            String unidad,

            Boolean esControlado,

            Integer stockMinimo
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/update"
        );

        // =================================================
        // VALIDAR CAMPOS
        // =================================================

        String nombreLimpio =
                validarNombre(nombre);

        String descripcionLimpia =
                validarTextoObligatorio(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        BigDecimal precioValidado =
                validarPrecio(precio);

        String unidadLimpia =
                validarTextoObligatorio(
                        unidad,
                        MAX_UNIDAD,
                        "La unidad"
                );

        validarControlado(esControlado);

        validarStockMinimo(stockMinimo);

        // =================================================
        // BUSCAR CON BLOQUEO
        // =================================================

        Medicamento medicamento =
                buscarConBloqueo(id);

        // =================================================
        // VALIDAR DUPLICADOS
        // =================================================

        if (medicamento.estaActivo()
                && existeNombreActivo(
                nombreLimpio,
                id
        )) {

            throw new IllegalStateException(
                    "Ya existe un medicamento activo con el nombre "
                            + nombreLimpio + "."
            );
        }

        // =================================================
        // SNAPSHOT ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(medicamento);

        // =================================================
        // ACTUALIZAR DATOS
        // =================================================

        medicamento.setNombre(nombreLimpio);

        medicamento.setDescripcion(descripcionLimpia);

        medicamento.setPrecio(precioValidado);

        medicamento.setUnidad(unidadLimpia);

        medicamento.setEsControlado(esControlado);

        medicamento.setStockMinimo(stockMinimo);

        /*
         * No modificar:
         *
         * - ID.
         * - Estado activo/inactivo.
         * - Cantidades de inventario.
         * - Recetas.
         * - Despachos.
         * - Precios históricos.
         */

        medicamento.validar();

        // =================================================
        // GUARDAR
        // =================================================

        Medicamento actualizado =
                medicamentoRepository.saveAndFlush(
                        medicamento
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
    // CU-14 - DESACTIVAR MEDICAMENTO
    // =====================================================

    @Transactional
    public void desactivar(
            Integer id
    ) {

        // =================================================
        // SEGURIDAD
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "medicine/delete"
        );

        // =================================================
        // BLOQUEAR MEDICAMENTO
        // =================================================

        Medicamento medicamento =
                buscarConBloqueo(id);

        // =================================================
        // VALIDAR ESTADO
        // =================================================

        if (!medicamento.estaActivo()) {

            throw new IllegalStateException(
                    "El medicamento ya se encuentra inactivo."
            );
        }

        // =================================================
        // PROTEGER STOCK EN TODAS LAS SUCURSALES
        // =================================================

        boolean tieneExistencias =
                inventarioFarmaciaRepository
                        .existsByMedicamento_IdAndCantidadDisponibleGreaterThan(
                                id,
                                0
                        );

        if (tieneExistencias) {

            throw new IllegalStateException(
                    "No se puede desactivar el medicamento "
                            + "porque tiene existencias disponibles "
                            + "en una o más sucursales."
            );
        }

        // =================================================
        // PROTEGER RECETAS PENDIENTES
        // =================================================
        //
        // Se consideran:
        //
        // - Recetas activas.
        // - Cantidades prescritas.
        // - Entregas confirmadas.
        // - Entregas parciales.
        // - Sustituciones registradas.
        //
        // No se bloquea únicamente por tener
        // recetas históricas ya completadas.
        //
        // =================================================

        if (tieneRecetasPendientes(id)) {

            throw new IllegalStateException(
                    "No se puede desactivar el medicamento "
                            + "porque aparece en recetas activas "
                            + "con cantidades pendientes de entrega."
            );
        }

        // =================================================
        // SNAPSHOT ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(medicamento);

        // =================================================
        // ELIMINACIÓN LÓGICA
        // =================================================

        medicamento.setActivo(false);

        medicamentoRepository.saveAndFlush(
                medicamento
        );

        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoria(
                medicamento,
                AccionAuditoria.ELIMINAR,
                valoresAnteriores,
                crearSnapshot(medicamento)
        );
    }

    // =====================================================
    // BUSCAR MEDICAMENTO CON BLOQUEO
    // =====================================================

    private Medicamento buscarConBloqueo(
            Integer id
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del medicamento no es válido."
            );
        }

        Medicamento medicamento =
                entityManager.find(
                        Medicamento.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (medicamento == null) {

            throw new IllegalArgumentException(
                    "El medicamento solicitado no existe."
            );
        }

        return medicamento;
    }

    // =====================================================
    // VALIDAR NOMBRE
    // =====================================================

    private String validarNombre(
            String nombre
    ) {

        if (nombre == null || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        String limpio =
                nombre.trim()
                        .replaceAll("\\s+", " ");

        if (limpio.length() > MAX_NOMBRE) {

            throw new IllegalArgumentException(
                    "El nombre no puede superar los "
                            + MAX_NOMBRE
                            + " caracteres."
            );
        }

        return limpio;
    }

    // =====================================================
    // VALIDAR CAMPOS OBLIGATORIOS
    // =====================================================

    private String validarTextoObligatorio(

            String valor,

            int longitudMaxima,

            String campo
    ) {

        if (valor == null || valor.isBlank()) {

            throw new IllegalArgumentException(
                    campo + " es obligatoria."
            );
        }

        String limpio =
                valor.trim();

        if (limpio.length() > longitudMaxima) {

            throw new IllegalArgumentException(
                    campo
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

    private BigDecimal validarPrecio(
            BigDecimal precio
    ) {

        if (precio == null
                || precio.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "El precio debe ser mayor a 0."
            );
        }

        if (precio.compareTo(PRECIO_MAXIMO) > 0) {

            throw new IllegalArgumentException(
                    "El precio supera el máximo permitido."
            );
        }

        try {

            return precio.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

        } catch (ArithmeticException ex) {

            throw new IllegalArgumentException(
                    "El precio debe tener como máximo 2 decimales."
            );
        }
    }

    // =====================================================
    // VALIDAR INDICADOR CONTROLADO
    // =====================================================

    private void validarControlado(
            Boolean esControlado
    ) {

        if (esControlado == null) {

            throw new IllegalArgumentException(
                    "Debe indicar si el medicamento es controlado."
            );
        }
    }

    // =====================================================
    // VALIDAR STOCK MÍNIMO
    // =====================================================

    private void validarStockMinimo(
            Integer stockMinimo
    ) {

        if (stockMinimo != null && stockMinimo < 0) {

            throw new IllegalArgumentException(
                    "El stock mínimo no puede ser negativo."
            );
        }
    }

    // =====================================================
    // VERIFICAR NOMBRE ACTIVO DUPLICADO
    // =====================================================
    //
    // Normaliza mayúsculas, minúsculas y espacios.
    //
    // El índice único de PostgreSQL será la
    // protección definitiva para concurrencia.
    //
    // =====================================================

    private boolean existeNombreActivo(

            String nombre,

            Integer idExcluir
    ) {

        String sql = """
                SELECT m.id
                FROM public.medicamento m
                WHERE m.activo IS TRUE
                  AND LOWER(
                      REGEXP_REPLACE(
                          BTRIM(m.nombre),
                          '[[:space:]]+',
                          ' ',
                          'g'
                      )
                  ) = :nombre
                  AND m.id <> :idExcluir
                LIMIT 1
                """;

        String nombreNormalizado =
                nombre.trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);

        int idExcluido =
                idExcluir == null ? -1 : idExcluir;

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
    // VERIFICAR RECETAS ACTIVAS PENDIENTES
    // =====================================================
    //
    // Una línea está pendiente cuando:
    //
    // Cantidad prescrita > cantidad confirmada.
    //
    // Las sustituciones cuentan porque las entregas
    // se asocian al detalle original de la receta.
    //
    // No se modifican recetas ni despachos.
    //
    // =====================================================

    private boolean tieneRecetasPendientes(
            Integer idMedicamento
    ) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM public.detalle_receta_medica dr
                    JOIN public.receta_medica r
                      ON r.id = dr.id_receta_medica
                    WHERE dr.id_medicamento = :idMedicamento
                      AND r.activo IS TRUE
                      AND dr.cantidad > COALESCE(
                          (
                              SELECT SUM(dd.cantidad_entregada)
                              FROM public.detalle_despacho dd
                              JOIN public.despacho d
                                ON d.id = dd.id_despacho
                              WHERE dd.id_detalle_receta_medica = dr.id
                                AND d.estado = 'CONFIRMADO'
                          ),
                          0
                      )
                )
                """;

        Object resultado =
                entityManager
                        .createNativeQuery(sql)
                        .setParameter(
                                "idMedicamento",
                                idMedicamento
                        )
                        .getSingleResult();

        return Boolean.TRUE.equals(resultado);
    }

    // =====================================================
    // SNAPSHOT DE AUDITORÍA
    // =====================================================

    private Map<String, Object> crearSnapshot(
            Medicamento medicamento
    ) {

        Map<String, Object> datos =
                new LinkedHashMap<>();

        datos.put(
                "id",
                medicamento.getId()
        );

        datos.put(
                "nombre",
                medicamento.getNombre()
        );

        datos.put(
                "descripcion",
                medicamento.getDescripcion()
        );

        datos.put(
                "precio",
                medicamento.getPrecio()
        );

        datos.put(
                "unidad",
                medicamento.getUnidad()
        );

        datos.put(
                "esControlado",
                medicamento.getEsControlado()
        );

        datos.put(
                "stockMinimo",
                medicamento.getStockMinimo()
        );

        datos.put(
                "activo",
                medicamento.getActivo()
        );

        return datos;
    }

    // =====================================================
    // REGISTRAR AUDITORÍA
    // =====================================================

    private void registrarAuditoria(

            Medicamento medicamento,

            AccionAuditoria accion,

            Map<String, Object> valoresAnteriores,

            Map<String, Object> valoresNuevos
    ) {

        Usuario usuario =
                usuarioActualService.obtenerUsuarioActual();

        if (usuario == null
                || usuario.getId() == null) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario ejecutor."
            );
        }

        RegistroAuditoria registro =
                new RegistroAuditoria(

                        "medicamento",

                        String.valueOf(
                                medicamento.getId()
                        ),

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
