
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * =========================================================
 * CU-15 - SERVICE: CATÁLOGOS DE BITÁCORA
 * =========================================================
 *
 * Responsabilidades:
 *
 * FORMULARIO NUEVO MOVIMIENTO:
 *
 * - Obtener medicamentos activos.
 * - Obtener sucursales autorizadas.
 *
 * FILTROS DEL HISTORIAL:
 *
 * - Obtener medicamentos con movimientos registrados.
 * - Obtener sucursales con movimientos registrados.
 * - Obtener usuarios responsables de movimientos.
 * - Incluir registros históricos aunque sus catálogos
 *   estén actualmente inactivos.
 *
 * SEGURIDAD:
 *
 * ADMINISTRADOR:
 * - Puede consultar movimientos de todas las sucursales.
 *
 * FARMACÉUTICO:
 * - Únicamente puede consultar su sucursal asignada.
 *
 * IMPORTANTE:
 *
 * - No modifica medicamentos.
 * - No modifica sucursales.
 * - No modifica usuarios.
 * - No modifica inventarios.
 * - No registra movimientos.
 * - No afecta al CU-11 ni al CU-14.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class MovimientoBitacoraCatalogoService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MedicamentoRepository
            medicamentoRepository;

    private final SucursalRepository
            sucursalRepository;

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraCatalogoService(
            MedicamentoRepository medicamentoRepository,
            SucursalRepository sucursalRepository,
            MovimientoInventarioFarmaciaRepository movimientoRepository,
            MovimientoBitacoraSeguridadService seguridadService
    ) {

        this.medicamentoRepository = medicamentoRepository;

        this.sucursalRepository = sucursalRepository;

        this.movimientoRepository = movimientoRepository;

        this.seguridadService = seguridadService;
    }

    // =====================================================
    // CU-15 - LISTAR MEDICAMENTOS ACTIVOS
    // =====================================================
    //
    // Utilizado por el formulario Nuevo Movimiento.
    //
    // Solo permite seleccionar medicamentos activos.
    // =====================================================

    public List<Medicamento> listarMedicamentosActivos() {

        seguridadService.obtenerUsuarioAutorizado();

        return medicamentoRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    // =====================================================
    // CU-15 - LISTAR SUCURSALES AUTORIZADAS
    // =====================================================
    //
    // Utilizado por el formulario Nuevo Movimiento.
    // =====================================================

    public List<Sucursal> listarSucursalesAutorizadas() {

        Integer idSucursalAutorizada =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        // =================================================
        // ADMINISTRADOR
        // =================================================

        if (idSucursalAutorizada == null) {

            return sucursalRepository
                    .findByActivoTrueOrderByNombreAsc();
        }

        // =================================================
        // FARMACÉUTICO
        // =================================================

        Sucursal sucursal =
                sucursalRepository
                        .findById(idSucursalAutorizada)
                        .filter(
                                s -> Boolean.TRUE.equals(
                                        s.getActivo()
                                )
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe una sucursal activa "
                                                + "asignada al usuario."
                                )
                        );

        return List.of(sucursal);
    }

    // =====================================================
    // CU-15 - FILTRO: MEDICAMENTOS DEL HISTORIAL
    // =====================================================
    //
    // Incluye medicamentos activos e inactivos
    // que tengan movimientos registrados.
    //
    // Farmacéutico: solo su sucursal.
    // Administrador: todas las sucursales.
    // =====================================================

    public List<Medicamento>
    listarMedicamentosConMovimientos() {

        Integer idSucursalAutorizada =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        return movimientoRepository
                .listarMedicamentosConMovimientosCu15(
                        idSucursalAutorizada
                );
    }

    // =====================================================
    // CU-15 - FILTRO: SUCURSALES DEL HISTORIAL
    // =====================================================
    //
    // Incluye las sucursales que tengan
    // movimientos registrados.
    //
    // La seguridad se aplica desde el Service.
    // =====================================================

    public List<Sucursal>
    listarSucursalesConMovimientos() {

        Integer idSucursalAutorizada =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        return movimientoRepository
                .listarSucursalesConMovimientosCu15(
                        idSucursalAutorizada
                );
    }

    // =====================================================
    // CU-15 - FILTRO: USUARIOS RESPONSABLES
    // =====================================================
    //
    // Incluye usuarios activos e inactivos
    // que hayan registrado movimientos.
    //
    // Farmacéutico: solo usuarios con movimientos
    // dentro de su sucursal autorizada.
    //
    // Administrador: todas las sucursales.
    // =====================================================

    public List<Usuario>
    listarUsuariosConMovimientos() {

        Integer idSucursalAutorizada =
                seguridadService.obtenerSucursalConsulta(
                        null
                );

        return movimientoRepository
                .listarUsuariosConMovimientosCu15(
                        idSucursalAutorizada
                );
    }
}
