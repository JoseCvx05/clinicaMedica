
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.InventarioFarmacia;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import com.proyecto.clinicamedica.dto.InventarioFarmaciaResumenMensualDTO;
import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;

import java.util.Map;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE INVENTARIO DE MEDICAMENTOS
 * =========================================================
 *
 * Catálogo 8 de 9.
 *
 * Responsabilidades:
 *
 * - Consultar inventarios registrados.
 * - Buscar medicamentos por nombre.
 * - Filtrar inventarios por sucursal.
 * - Aplicar paginación.
 * - Verificar permisos administrativos.
 * - Crear inventarios de forma controlada.
 * - Validar medicamentos y sucursales activos.
 * - Evitar inventarios duplicados.
 * - Validar cantidades iniciales.
 * - Registrar auditoría.
 *
 * IMPORTANTE:
 *
 * - No altera despachos históricos del CU-11.
 * - No realiza descuentos de medicamentos.
 * - No elimina registros existentes.
 * - La edición y desactivación se implementarán
 *   posteriormente con sus respectivas reglas.
 * - Los movimientos posteriores de existencias
 *   requerirán registro y auditoría propios.
 *
 * =========================================================
 */
@Service
public class InventarioFarmaciaMantenimientoService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final InventarioFarmaciaRepository
            inventarioRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final SucursalRepository
            sucursalRepository;

    private final PermisoCatalogoService
            permisoCatalogoService;

    private final UsuarioActualService
            usuarioActualService;

    private final AuditoriaService
            auditoriaService;



    // =====================================================
    // CU-14 - REPOSITORIO DE MOVIMIENTOS
    // =====================================================

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================


    public InventarioFarmaciaMantenimientoService(
            InventarioFarmaciaRepository inventarioRepository,
            MedicamentoRepository medicamentoRepository,
            SucursalRepository sucursalRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService,
            MovimientoInventarioFarmaciaRepository movimientoRepository
    ) {

        this.inventarioRepository = inventarioRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.sucursalRepository = sucursalRepository;
        this.permisoCatalogoService = permisoCatalogoService;
        this.usuarioActualService = usuarioActualService;
        this.auditoriaService = auditoriaService;
        this.movimientoRepository = movimientoRepository;
    }


    // =====================================================
    // CU-14 - LISTAR INVENTARIO ADMINISTRATIVO
    // =====================================================
    //
    // Permite:
    //
    // - Consultar inventarios de todas las sucursales.
    // - Filtrar por una sucursal específica.
    // - Buscar medicamentos por nombre.
    // - Paginar resultados.
    //
    // No modifica las existencias.
    //
    // =====================================================

    @Transactional(readOnly = true)
    public Page<InventarioFarmacia> listar(
            String busqueda,
            Integer idSucursal,
            Pageable pageable
    ) {

        // -------------------------------------------------
        // VALIDAR PERMISO ADMINISTRATIVO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );


        // -------------------------------------------------
        // VALIDAR PAGINACIÓN
        // -------------------------------------------------

        if (pageable == null) {

            throw new IllegalArgumentException(
                    "La paginación del inventario es obligatoria."
            );
        }


        // -------------------------------------------------
        // NORMALIZAR BÚSQUEDA
        // -------------------------------------------------

        String texto = busqueda == null
                ? ""
                : busqueda.trim();


        // -------------------------------------------------
        // FILTRAR POR SUCURSAL
        // -------------------------------------------------

        if (idSucursal != null) {

            if (idSucursal <= 0) {

                throw new IllegalArgumentException(
                        "La sucursal seleccionada no es válida."
                );
            }

            return inventarioRepository
                    .findBySucursal_IdAndMedicamento_NombreContainingIgnoreCase(
                            idSucursal,
                            texto,
                            pageable
                    );
        }


        // -------------------------------------------------
        // CONSULTA GENERAL
        // -------------------------------------------------

        return inventarioRepository
                .findByMedicamento_NombreContainingIgnoreCase(
                        texto,
                        pageable
                );
    }


    // =====================================================
    // CU-14 - CREAR INVENTARIO DE MEDICAMENTOS
    // =====================================================
    //
    // Reglas:
    //
    // 1. Exigir permiso de creación.
    // 2. Validar identificadores.
    // 3. Validar cantidad inicial.
    // 4. Bloquear la sucursal.
    // 5. Verificar que la sucursal esté activa.
    // 6. Verificar que el medicamento esté activo.
    // 7. Impedir inventarios duplicados.
    // 8. Crear inventario activo.
    // 9. Registrar auditoría.
    //
    // Toda la operación utiliza una sola transacción.
    //
    // =====================================================

    @Transactional
    public InventarioFarmacia crear(
            Integer idMedicamento,
            Integer idSucursal,
            Integer cantidadInicial
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/create"
        );


        // -------------------------------------------------
        // 2. OBTENER ADMINISTRADOR AUTENTICADO
        // -------------------------------------------------

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();


        // -------------------------------------------------
        // 3. VALIDAR IDENTIFICADORES
        // -------------------------------------------------

        if (idMedicamento == null
                || idMedicamento <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento válido."
            );
        }

        if (idSucursal == null
                || idSucursal <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal válida."
            );
        }


        // -------------------------------------------------
        // 4. VALIDAR CANTIDAD INICIAL
        // -------------------------------------------------

        if (cantidadInicial == null) {

            throw new IllegalArgumentException(
                    "La cantidad inicial es obligatoria."
            );
        }

        if (cantidadInicial < 0) {

            throw new IllegalArgumentException(
                    "La cantidad inicial no puede ser negativa."
            );
        }


        // -------------------------------------------------
        // 5. BLOQUEAR SUCURSAL
        // -------------------------------------------------
        //
        // Serializa las creaciones administrativas de
        // inventarios en la misma sucursal cuando todas
        // utilizan este servicio.
        //
        // También coordina esta operación con los
        // bloqueos de desactivación de sucursales.
        //
        // -------------------------------------------------

        Sucursal sucursal =
                sucursalRepository
                        .buscarPorIdConBloqueo(idSucursal)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "La sucursal seleccionada no existe."
                                )
                        );


        // -------------------------------------------------
        // 6. VALIDAR SUCURSAL ACTIVA
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalStateException(
                    "No se puede registrar inventario "
                            + "en una sucursal inactiva."
            );
        }


        // -------------------------------------------------
        // 7. VALIDAR MEDICAMENTO ACTIVO
        // -------------------------------------------------

        Medicamento medicamento =
                medicamentoRepository
                        .findByIdAndActivoTrue(idMedicamento)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El medicamento no existe "
                                                + "o se encuentra inactivo."
                                )
                        );


        // -------------------------------------------------
        // 8. VERIFICAR INVENTARIO DUPLICADO
        // -------------------------------------------------
        //
        // Cada medicamento debe tener como máximo
        // un registro de inventario por sucursal.
        //
        // También considera inventarios inactivos
        // para conservar su historial.
        //
        // -------------------------------------------------

        boolean existeInventario =
                inventarioRepository
                        .existsByMedicamento_IdAndSucursal_Id(
                                idMedicamento,
                                idSucursal
                        );

        if (existeInventario) {

            throw new IllegalStateException(
                    "Ya existe un inventario registrado "
                            + "para este medicamento "
                            + "en la sucursal seleccionada."
            );
        }


        // -------------------------------------------------
        // 9. CREAR INVENTARIO
        // -------------------------------------------------

        InventarioFarmacia inventario =
                new InventarioFarmacia(
                        medicamento,
                        sucursal,
                        cantidadInicial
                );

        inventario.setActivo(true);


        // -------------------------------------------------
        // 10. GUARDAR EN POSTGRESQL
        // -------------------------------------------------

        inventario =
                inventarioRepository.saveAndFlush(
                        inventario
                );


        // =====================================================
        // CU-14 - REGISTRAR MOVIMIENTO INICIAL
        // =====================================================
        //
        // Registra las existencias iniciales.
        //
        // - Stock anterior: 0.
        // - Stock nuevo: cantidad inicial.
        // - Usuario: administrador autenticado.
        // - Origen: creación del inventario.
        //
        // El movimiento utiliza la misma transacción
        // que la creación y su auditoría.
        //
        // =====================================================

        MovimientoInventarioFarmacia movimientoInicial =
                new MovimientoInventarioFarmacia(
                        inventario,
                        administrador,
                        MovimientoInventarioFarmacia.TipoMovimiento.INICIAL,
                        cantidadInicial,
                        0,
                        inventario.getCantidadDisponible(),
                        "CU14_CREACION_INVENTARIO",
                        String.valueOf(inventario.getId()),
                        "Registro de existencias iniciales."
                );

        movimientoRepository.saveAndFlush(
                movimientoInicial
        );



        // -------------------------------------------------
        // 11. REGISTRAR AUDITORÍA
        // -------------------------------------------------
        //
        // Conserva:
        //
        // - Inventario creado.
        // - Medicamento.
        // - Sucursal.
        // - Existencias iniciales.
        // - Estado activo.
        // - Usuario administrador.
        //
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "inventario_farmacia",
                        String.valueOf(inventario.getId()),
                        AccionAuditoria.CREAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        null,
                        Map.of(
                                "idMedicamento",
                                idMedicamento,
                                "idSucursal",
                                idSucursal,
                                "cantidadInicial",
                                cantidadInicial,
                                "cantidadDisponible",
                                inventario.getCantidadDisponible(),
                                "activo",
                                true,
                                "operacion",
                                "CREACION_INVENTARIO"
                        ),
                        null
                )
        );


        // -------------------------------------------------
        // 12. RESULTADO
        // -------------------------------------------------
        //
        // Si falla la auditoría o cualquier validación,
        // la transacción deberá revertir la creación.
        //
        // -------------------------------------------------

        return inventario;
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // REGISTRAR ENTRADA DE MEDICAMENTOS
    // =====================================================
    //
    // - Requiere permiso administrativo.
    // - Valida cantidad positiva.
    // - Bloquea el inventario durante la operación.
    // - Verifica inventario, medicamento y sucursal.
    // - Agrega existencias de manera segura.
    // - Registra el movimiento de entrada.
    // - Registra auditoría.
    //
    // Todo se realiza dentro de una misma transacción.
    //
    // =====================================================

    @Transactional
    public InventarioFarmacia registrarEntrada(
            Integer idInventario,
            Integer cantidad,
            String observaciones
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );

        // -------------------------------------------------
        // 2. VALIDAR IDENTIFICADOR Y CANTIDAD
        // -------------------------------------------------

        if (idInventario == null || idInventario <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }

        if (cantidad == null || cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad de entrada debe ser mayor a cero."
            );
        }

        // -------------------------------------------------
        // 3. VALIDAR OBSERVACIONES
        // -------------------------------------------------

        String observacionesLimpias =
                observaciones == null
                        ? null
                        : observaciones.trim();

        if (observacionesLimpias != null
                && observacionesLimpias.length() > 500) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar "
                            + "los 500 caracteres."
            );
        }

        // -------------------------------------------------
        // 4. OBTENER ADMINISTRADOR ACTUAL
        // -------------------------------------------------

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // -------------------------------------------------
        // 5. BLOQUEAR INVENTARIO
        // -------------------------------------------------

        InventarioFarmacia inventario =
                inventarioRepository
                        .bloquearPorId(idInventario)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El inventario seleccionado no existe."
                                )
                        );

        // -------------------------------------------------
        // 6. VALIDAR INVENTARIO ACTIVO
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(inventario.getActivo())) {

            throw new IllegalStateException(
                    "No se pueden ingresar medicamentos "
                            + "a un inventario inactivo."
            );
        }

        // -------------------------------------------------
        // 7. VALIDAR SUCURSAL ACTIVA
        // -------------------------------------------------

        if (inventario.getSucursal() == null
                || !Boolean.TRUE.equals(
                inventario.getSucursal().getActivo()
        )) {

            throw new IllegalStateException(
                    "No se pueden ingresar medicamentos "
                            + "a una sucursal inactiva."
            );
        }

        // -------------------------------------------------
        // 8. VALIDAR MEDICAMENTO ACTIVO
        // -------------------------------------------------

        if (inventario.getMedicamento() == null
                || !inventario.getMedicamento().estaActivo()) {

            throw new IllegalStateException(
                    "No se pueden ingresar existencias "
                            + "de un medicamento inactivo."
            );
        }

        // -------------------------------------------------
        // 9. CONSERVAR STOCK ANTERIOR
        // -------------------------------------------------

        Integer stockAnterior =
                inventario.getCantidadDisponible();

        // -------------------------------------------------
        // 10. AGREGAR EXISTENCIAS
        // -------------------------------------------------
        //
        // Reutilizamos el método de la entidad.
        // Math.addExact evita desbordamientos.
        //
        // -------------------------------------------------

        inventario.agregar(cantidad);

        // -------------------------------------------------
        // 11. GUARDAR INVENTARIO
        // -------------------------------------------------

        inventarioRepository.saveAndFlush(inventario);

        Integer stockNuevo =
                inventario.getCantidadDisponible();

        // -------------------------------------------------
        // 12. REGISTRAR MOVIMIENTO DE ENTRADA
        // -------------------------------------------------

        MovimientoInventarioFarmacia movimiento =
                new MovimientoInventarioFarmacia(
                        inventario,
                        administrador,
                        MovimientoInventarioFarmacia.TipoMovimiento.ENTRADA,
                        cantidad,
                        stockAnterior,
                        stockNuevo,
                        "CU14_ENTRADA_INVENTARIO",
                        String.valueOf(inventario.getId()),
                        observacionesLimpias
                );

        movimientoRepository.saveAndFlush(movimiento);

        // -------------------------------------------------
        // 13. REGISTRAR AUDITORÍA
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "inventario_farmacia",
                        String.valueOf(inventario.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),

                        Map.of(
                                "cantidadDisponible", stockAnterior,
                                "activo", true
                        ),

                        Map.of(
                                "cantidadDisponible", stockNuevo,
                                "activo", true,
                                "cantidadIngresada", cantidad,
                                "operacion", "ENTRADA_INVENTARIO",
                                "idMovimiento", movimiento.getId()
                        ),

                        null
                )
        );

        // -------------------------------------------------
        // 14. RESULTADO
        // -------------------------------------------------

        return inventario;
    }

    // =====================================================
    // CU-14 - CATÁLOGO 8
    // AJUSTE CONTROLADO DE EXISTENCIAS
    // =====================================================
    //
    // Permite corregir las existencias de un inventario.
    //
    // SEGURIDAD:
    //
    // - Requiere permiso medicine-inventory/update.
    // - Valida inventario, medicamento y sucursal activos.
    // - Utiliza bloqueo PESSIMISTIC_WRITE.
    // - No permite existencias negativas.
    // - Requiere motivo obligatorio.
    // - Registra movimiento AJUSTE.
    // - Registra auditoría.
    //
    // Todo ocurre dentro de una misma transacción.
    //
    // =====================================================

    @Transactional
    public InventarioFarmacia ajustarExistencias(
            Integer idInventario,
            Integer nuevoStock,
            String motivo
    ) {

        // -------------------------------------------------
        // 1. VALIDAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/update"
        );


        // -------------------------------------------------
        // 2. VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idInventario == null || idInventario <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }


        // -------------------------------------------------
        // 3. VALIDAR NUEVO STOCK
        // -------------------------------------------------

        if (nuevoStock == null || nuevoStock < 0) {

            throw new IllegalArgumentException(
                    "Las nuevas existencias no pueden ser negativas."
            );
        }


        // -------------------------------------------------
        // 4. VALIDAR MOTIVO OBLIGATORIO
        // -------------------------------------------------

        String motivoLimpio =
                motivo == null ? "" : motivo.trim();

        if (motivoLimpio.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el motivo del ajuste."
            );
        }

        if (motivoLimpio.length() > 500) {

            throw new IllegalArgumentException(
                    "El motivo no puede superar los 500 caracteres."
            );
        }


        // -------------------------------------------------
        // 5. OBTENER ADMINISTRADOR
        // -------------------------------------------------

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();


        // -------------------------------------------------
        // 6. BLOQUEAR INVENTARIO
        // -------------------------------------------------

        InventarioFarmacia inventario =
                inventarioRepository
                        .bloquearPorId(idInventario)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El inventario seleccionado no existe."
                                )
                        );


        // -------------------------------------------------
        // 7. VALIDAR INVENTARIO ACTIVO
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(inventario.getActivo())) {

            throw new IllegalStateException(
                    "No se pueden ajustar existencias "
                            + "de un inventario inactivo."
            );
        }


        // -------------------------------------------------
        // 8. VALIDAR SUCURSAL ACTIVA
        // -------------------------------------------------

        if (inventario.getSucursal() == null
                || !Boolean.TRUE.equals(
                inventario.getSucursal().getActivo()
        )) {

            throw new IllegalStateException(
                    "La sucursal del inventario está inactiva."
            );
        }


        // -------------------------------------------------
        // 9. VALIDAR MEDICAMENTO ACTIVO
        // -------------------------------------------------

        if (inventario.getMedicamento() == null
                || !inventario.getMedicamento().estaActivo()) {

            throw new IllegalStateException(
                    "El medicamento del inventario está inactivo."
            );
        }


        // -------------------------------------------------
        // 10. CONSERVAR EXISTENCIAS ANTERIORES
        // -------------------------------------------------

        Integer stockAnterior =
                inventario.getCantidadDisponible();


        // -------------------------------------------------
        // 11. IMPEDIR AJUSTES SIN CAMBIOS
        // -------------------------------------------------

        if (stockAnterior.equals(nuevoStock)) {

            throw new IllegalStateException(
                    "Las nuevas existencias deben ser "
                            + "diferentes de las actuales."
            );
        }


        // -------------------------------------------------
        // 12. CALCULAR DIFERENCIA
        // -------------------------------------------------

        int diferencia =
                nuevoStock > stockAnterior
                        ? nuevoStock - stockAnterior
                        : stockAnterior - nuevoStock;


        // -------------------------------------------------
        // 13. APLICAR AJUSTE CONTROLADO
        // -------------------------------------------------

        if (nuevoStock > stockAnterior) {

            inventario.agregar(diferencia);

        } else {

            inventario.descontar(diferencia);
        }


        // -------------------------------------------------
        // 14. GUARDAR EXISTENCIAS
        // -------------------------------------------------

        inventarioRepository.saveAndFlush(inventario);


        // -------------------------------------------------
        // 15. REGISTRAR MOVIMIENTO AJUSTE
        // -------------------------------------------------

        MovimientoInventarioFarmacia movimiento =
                new MovimientoInventarioFarmacia(
                        inventario,
                        administrador,
                        MovimientoInventarioFarmacia.TipoMovimiento.AJUSTE,
                        diferencia,
                        stockAnterior,
                        inventario.getCantidadDisponible(),
                        "CU14_AJUSTE_INVENTARIO",
                        String.valueOf(inventario.getId()),
                        motivoLimpio
                );

        movimientoRepository.saveAndFlush(movimiento);


        // -------------------------------------------------
        // 16. REGISTRAR AUDITORÍA
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "inventario_farmacia",
                        String.valueOf(inventario.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),

                        Map.of(
                                "cantidadDisponible",
                                stockAnterior,
                                "activo",
                                true
                        ),

                        Map.of(
                                "cantidadDisponible",
                                inventario.getCantidadDisponible(),
                                "activo",
                                true,
                                "cantidadAjustada",
                                diferencia,
                                "operacion",
                                "AJUSTE_INVENTARIO",
                                "idMovimiento",
                                movimiento.getId(),
                                "motivo",
                                motivoLimpio
                        ),

                        null
                )
        );


        // -------------------------------------------------
        // 17. RESULTADO
        // -------------------------------------------------

        return inventario;
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // DESACTIVACIÓN LÓGICA DE INVENTARIO
    // =====================================================
    //
    // REGLAS:
    //
    // - Requiere permiso medicine-inventory/delete.
    // - Bloquea el inventario contra cambios simultáneos.
    // - No permite desactivar inventarios inactivos.
    // - No permite desactivar inventarios con existencias.
    // - Conserva los registros históricos.
    // - Registra la operación en auditoría.
    //
    // IMPORTANTE:
    //
    // No elimina físicamente ningún registro.
    // No modifica cantidades disponibles.
    // No altera despachos anteriores del CU-11.
    //
    // =====================================================

    @Transactional
    public InventarioFarmacia desactivar(
            Integer idInventario
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/delete"
        );


        // -------------------------------------------------
        // 2. VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idInventario == null || idInventario <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }


        // -------------------------------------------------
        // 3. OBTENER ADMINISTRADOR ACTUAL
        // -------------------------------------------------

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();


        // -------------------------------------------------
        // 4. BLOQUEAR INVENTARIO
        // -------------------------------------------------

        InventarioFarmacia inventario =
                inventarioRepository
                        .bloquearPorId(idInventario)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "El inventario seleccionado no existe."
                                )
                        );


        // -------------------------------------------------
        // 5. VALIDAR ESTADO ACTUAL
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(inventario.getActivo())) {

            throw new IllegalStateException(
                    "El inventario ya se encuentra inactivo."
            );
        }


        // -------------------------------------------------
        // 6. VERIFICAR EXISTENCIAS
        // -------------------------------------------------

        Integer existencias =
                inventario.getCantidadDisponible();

        if (existencias == null || existencias < 0) {

            throw new IllegalStateException(
                    "El inventario presenta existencias inválidas."
            );
        }

        if (existencias > 0) {

            throw new IllegalStateException(
                    "No se puede desactivar el inventario "
                            + "porque todavía tiene "
                            + existencias
                            + " unidades disponibles."
            );
        }


        // -------------------------------------------------
        // 7. DESACTIVACIÓN LÓGICA
        // -------------------------------------------------

        inventario.setActivo(false);


        // -------------------------------------------------
        // 8. GUARDAR CAMBIO DE ESTADO
        // -------------------------------------------------

        inventarioRepository.saveAndFlush(inventario);


        // -------------------------------------------------
        // 9. REGISTRAR AUDITORÍA
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "inventario_farmacia",
                        String.valueOf(inventario.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),

                        Map.of(
                                "activo", true,
                                "cantidadDisponible", existencias
                        ),

                        Map.of(
                                "activo", false,
                                "cantidadDisponible", existencias,
                                "operacion", "DESACTIVACION_LOGICA",
                                "idMedicamento",
                                inventario.getMedicamento().getId(),
                                "idSucursal",
                                inventario.getSucursal().getId()
                        ),

                        null
                )
        );


        // -------------------------------------------------
        // 10. RESULTADO
        // -------------------------------------------------

        return inventario;
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // CONSULTAR HISTORIAL DE MOVIMIENTOS
    // =====================================================
    //
    // Permite consultar:
    //
    // - Movimientos iniciales.
    // - Entradas de medicamentos.
    // - Ajustes de existencias.
    // - Salidas registradas.
    //
    // SEGURIDAD:
    //
    // - Requiere permiso medicine-inventory/read.
    // - Valida que el inventario exista.
    // - Permite consultar registros históricos.
    // - Aplica paginación.
    //
    // IMPORTANTE:
    //
    // - No modifica existencias.
    // - No elimina movimientos históricos.
    // - No afecta los despachos del CU-11.
    //
    // =====================================================

    @Transactional(readOnly = true)
    public Page<MovimientoInventarioFarmacia> consultarHistorial(
            Integer idInventario,
            Pageable pageable
    ) {

        // -------------------------------------------------
        // 1. VERIFICAR PERMISO ADMINISTRATIVO
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );

        // -------------------------------------------------
        // 2. VALIDAR IDENTIFICADOR
        // -------------------------------------------------

        if (idInventario == null || idInventario <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }

        // -------------------------------------------------
        // 3. VALIDAR PAGINACIÓN
        // -------------------------------------------------

        if (pageable == null) {

            throw new IllegalArgumentException(
                    "La paginación del historial es obligatoria."
            );
        }

        // -------------------------------------------------
        // 4. VERIFICAR EXISTENCIA DEL INVENTARIO
        // -------------------------------------------------

        if (!inventarioRepository.existsById(idInventario)) {

            throw new IllegalStateException(
                    "El inventario seleccionado no existe."
            );
        }

        // -------------------------------------------------
        // 5. CONSULTAR MOVIMIENTOS
        // -------------------------------------------------

        return movimientoRepository
                .findByInventario_IdOrderByFechaHoraDescIdDesc(
                        idInventario,
                        pageable
                );
    }


    // =====================================================
    // CU-14 - CATÁLOGO 8
    // RESUMEN MENSUAL DE INVENTARIO
    // =====================================================
    //
    // Calcula:
    //
    // - Existencias iniciales del período.
    // - Altas iniciales.
    // - Entradas de medicamentos.
    // - Salidas por despachos.
    // - Ajustes positivos.
    // - Ajustes negativos.
    // - Existencias finales.
    // - Total de movimientos registrados.
    //
    // SEGURIDAD:
    // - Requiere medicine-inventory/read.
    // - No modifica las existencias.
    // - No modifica movimientos históricos.
    //
    // =====================================================

    @Transactional(readOnly = true)
    public InventarioFarmaciaResumenMensualDTO obtenerResumenMensual(
            Integer idInventario,
            YearMonth periodo
    ) {

        // -------------------------------------------------
        // 1. VALIDAR PERMISOS
        // -------------------------------------------------

        permisoCatalogoService.exigirPermiso(
                "medicine-inventory/read"
        );

        // -------------------------------------------------
        // 2. VALIDAR PARÁMETROS
        // -------------------------------------------------

        if (idInventario == null || idInventario <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }

        if (periodo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un mes válido."
            );
        }

        // -------------------------------------------------
        // 3. BUSCAR INVENTARIO
        // -------------------------------------------------

        InventarioFarmacia inventario =
                inventarioRepository.findById(idInventario)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "El inventario no existe."
                                )
                        );

        // -------------------------------------------------
        // 4. DETERMINAR PERÍODO
        // ZONA HORARIA DE GUATEMALA
        // -------------------------------------------------

        ZoneId zona = ZoneId.of("America/Guatemala");

        OffsetDateTime desde =
                periodo.atDay(1)
                        .atStartOfDay(zona)
                        .toOffsetDateTime();

        OffsetDateTime hasta =
                periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay(zona)
                        .toOffsetDateTime();

        // -------------------------------------------------
        // 5. CONSULTAR ÚLTIMO MOVIMIENTO ANTERIOR
        // -------------------------------------------------

        MovimientoInventarioFarmacia anterior =
                movimientoRepository
                        .findFirstByInventario_IdAndFechaHoraLessThanOrderByFechaHoraDescIdDesc(
                                idInventario,
                                desde
                        )
                        .orElse(null);

        // -------------------------------------------------
        // 6. CONSULTAR MOVIMIENTOS DEL MES
        // -------------------------------------------------

        List<MovimientoInventarioFarmacia> movimientos =
                movimientoRepository.buscarPorInventarioYPeriodo(
                        idInventario,
                        desde,
                        hasta
                );

        // -------------------------------------------------
        // 7. VERIFICAR DISPONIBILIDAD DE HISTORIAL
        // -------------------------------------------------

        if (anterior == null && movimientos.isEmpty()) {
            throw new IllegalStateException(
                    "No existe historial suficiente para calcular "
                            + "las existencias del período seleccionado."
            );
        }

        // -------------------------------------------------
        // 8. CALCULAR STOCK INICIAL
        // -------------------------------------------------

        int stockInicial;

        if (anterior != null) {

            stockInicial = anterior.getStockNuevo();

        } else {

            stockInicial =
                    movimientos.get(0).getStockAnterior();
        }

        // -------------------------------------------------
        // 9. INICIALIZAR ACUMULADORES
        // -------------------------------------------------

        int altasIniciales = 0;
        int entradas = 0;
        int salidas = 0;

        int ajustesPositivos = 0;
        int ajustesNegativos = 0;

        int stockEnCurso = stockInicial;

        // -------------------------------------------------
        // 10. PROCESAR MOVIMIENTOS DEL MES
        // -------------------------------------------------

        for (MovimientoInventarioFarmacia movimiento
                : movimientos) {

            // Validar continuidad del historial.

            if (movimiento.getStockAnterior()
                    != stockEnCurso) {

                throw new IllegalStateException(
                        "Se detectó una inconsistencia en "
                                + "el historial del inventario."
                );
            }

            int cantidad = movimiento.getCantidad();

            switch (movimiento.getTipoMovimiento()) {

                case INICIAL:

                    altasIniciales = Math.addExact(
                            altasIniciales,
                            cantidad
                    );

                    break;

                case ENTRADA:

                    entradas = Math.addExact(
                            entradas,
                            cantidad
                    );

                    break;

                case SALIDA:

                    salidas = Math.addExact(
                            salidas,
                            cantidad
                    );

                    break;

                case AJUSTE:

                    if (movimiento.getStockNuevo()
                            > movimiento.getStockAnterior()) {

                        ajustesPositivos = Math.addExact(
                                ajustesPositivos,
                                cantidad
                        );

                    } else {

                        ajustesNegativos = Math.addExact(
                                ajustesNegativos,
                                cantidad
                        );
                    }

                    break;

                default:

                    throw new IllegalStateException(
                            "Tipo de movimiento no reconocido."
                    );
            }

            stockEnCurso = movimiento.getStockNuevo();
        }

        // -------------------------------------------------
        // 11. CALCULAR EXISTENCIAS FINALES
        // -------------------------------------------------

        int stockFinal = stockEnCurso;

        // -------------------------------------------------
        // 12. VALIDAR RESULTADO CONTABLE
        // -------------------------------------------------

        long stockCalculado =
                (long) stockInicial
                        + altasIniciales
                        + entradas
                        + ajustesPositivos
                        - salidas
                        - ajustesNegativos;

        if (stockCalculado != stockFinal) {

            throw new IllegalStateException(
                    "El resumen mensual presenta diferencias "
                            + "entre movimientos y existencias."
            );
        }

        // -------------------------------------------------
        // 13. CONSTRUIR DTO
        // -------------------------------------------------

        return new InventarioFarmaciaResumenMensualDTO(

                inventario.getId(),

                inventario.getMedicamento().getNombre(),

                inventario.getSucursal().getNombre(),

                periodo,

                stockInicial,

                altasIniciales,

                entradas,

                salidas,

                ajustesPositivos,

                ajustesNegativos,

                stockFinal,

                movimientos.size()
        );
    }


}
