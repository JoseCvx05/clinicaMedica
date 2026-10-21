
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.MovimientoBitacoraDetalle;
import com.proyecto.clinicamedica.entity.MovimientoInventarioFarmacia;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.MovimientoBitacoraDetalleRepository;
import com.proyecto.clinicamedica.repository.MovimientoInventarioFarmaciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * =========================================================
 * CU-15 - SERVICE: ESTADO DE MOVIMIENTOS
 * =========================================================
 *
 * Funcionalidad:
 *
 * - Activar movimiento.
 * - Desactivar movimiento.
 * - Validar rol y sucursal.
 * - Registrar auditoría.
 * - Conservar movimientos históricos.
 *
 * SEGURIDAD:
 *
 * - Administrador: sucursales autorizadas.
 * - Farmacéutico: únicamente su sucursal.
 *
 * IMPORTANTE:
 *
 * Desactivar un registro NO significa:
 *
 * - Eliminar el movimiento.
 * - Revertir las existencias.
 * - Modificar los despachos del CU-11.
 * - Modificar inventarios del CU-14.
 *
 * Únicamente cambia el estado lógico
 * del registro complementario del CU-15.
 *
 * =========================================================
 */
@Service
public class MovimientoBitacoraEstadoService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final MovimientoInventarioFarmaciaRepository
            movimientoRepository;

    private final MovimientoBitacoraDetalleRepository
            detalleRepository;

    private final MovimientoBitacoraSeguridadService
            seguridadService;

    private final AuditoriaService
            auditoriaService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MovimientoBitacoraEstadoService(
            MovimientoInventarioFarmaciaRepository movimientoRepository,
            MovimientoBitacoraDetalleRepository detalleRepository,
            MovimientoBitacoraSeguridadService seguridadService,
            AuditoriaService auditoriaService
    ) {

        this.movimientoRepository = movimientoRepository;

        this.detalleRepository = detalleRepository;

        this.seguridadService = seguridadService;

        this.auditoriaService = auditoriaService;
    }

    // =====================================================
    // RESULTADO DE LA OPERACIÓN
    // =====================================================

    public record ResultadoEstado(
            Long idMovimiento,
            Boolean activo,
            String mensaje
    ) {
    }

    // =====================================================
    // ACTIVAR / DESACTIVAR MOVIMIENTO
    // =====================================================

    @Transactional
    public ResultadoEstado cambiarEstado(
            Long idMovimiento,
            boolean activar
    ) {

        // =================================================
        // 1. VALIDAR ID
        // =================================================

        if (idMovimiento == null || idMovimiento <= 0) {

            throw new IllegalArgumentException(
                    "El movimiento seleccionado no es válido."
            );
        }

        // =================================================
        // 2. VALIDAR USUARIO AUTORIZADO
        // =================================================

        Usuario usuario =
                seguridadService.obtenerUsuarioAutorizado();

        // =================================================
        // 3. CONSULTAR MOVIMIENTO HISTÓRICO
        // =================================================

        MovimientoInventarioFarmacia movimiento =
                movimientoRepository.findById(
                        idMovimiento
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "El movimiento seleccionado no existe."
                        )
                );

        // =================================================
        // 4. VALIDAR SUCURSAL
        // =================================================
        //
        // Se toma de PostgreSQL.
        // Nunca se acepta una sucursal enviada
        // directamente desde el navegador.
        //
        // =================================================

        Integer idSucursal =
                movimiento.getInventario()
                        .getSucursal()
                        .getId();

        seguridadService.obtenerSucursalConsulta(
                idSucursal
        );

        // =================================================
        // 5. CONSULTAR DETALLE COMPLEMENTARIO
        // =================================================
        //
        // CU-15:
        // normalmente ya tiene detalle.
        //
        // CU-11 y CU-14:
        // pueden ser movimientos anteriores
        // que todavía no tienen detalle.
        //
        // =================================================

        Optional<MovimientoBitacoraDetalle> detalleExistente =
                detalleRepository.findById(
                        idMovimiento
                );

        // =================================================
        // 6. OBTENER ESTADO ANTERIOR
        // =================================================
        //
        // Los movimientos históricos que todavía
        // no tienen detalle se consideran activos.
        //
        // =================================================

        boolean estadoAnterior =
                detalleExistente
                        .map(MovimientoBitacoraDetalle::getActivo)
                        .orElse(true);

        // =================================================
        // 7. EVITAR CAMBIOS REPETIDOS
        // =================================================
        //
        // Utilizamos estado explícito.
        //
        // Así, si se envía dos veces la orden
        // de desactivar, no vuelve a activarse.
        //
        // =================================================

        if (estadoAnterior == activar) {

            String mensaje = activar
                    ? "El movimiento ya se encuentra activo."
                    : "El movimiento ya se encuentra inactivo.";

            return new ResultadoEstado(
                    idMovimiento,
                    activar,
                    mensaje
            );
        }

        // =================================================
        // 8. OBTENER O CREAR DETALLE
        // =================================================
        //
        // Si el movimiento es histórico,
        // se crea su detalle complementario.
        //
        // No se modifica el movimiento original.
        //
        // =================================================

        MovimientoBitacoraDetalle detalle =
                detalleExistente.orElseGet(
                        () -> MovimientoBitacoraDetalle
                                .paraHistorico(movimiento)
                );

        // =================================================
        // 9. CAMBIAR ESTADO LÓGICO
        // =================================================
        //
        // La entidad utiliza @Version.
        //
        // No se llama a:
        //
        // - inventario.agregar()
        // - inventario.descontar()
        //
        // =================================================

        detalle.cambiarEstado(
                activar
        );

        // =================================================
        // 10. GUARDAR CAMBIO
        // =================================================

        detalleRepository.saveAndFlush(
                detalle
        );

        // =================================================
        // 11. REGISTRAR AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "movimiento_inventario_bitacora_detalle",
                        String.valueOf(idMovimiento),
                        AccionAuditoria.ACTUALIZAR,
                        usuario.getId(),
                        usuario.getNombreUsuario(),
                        Map.of(
                                "activo",
                                estadoAnterior
                        ),
                        Map.of(
                                "activo",
                                activar
                        ),
                        null
                )
        );

        // =================================================
        // 12. MENSAJE DE CONFIRMACIÓN
        // =================================================

        String mensaje = activar
                ? "Movimiento activado correctamente."
                : "Movimiento desactivado correctamente.";

        // =================================================
        // 13. DEVOLVER RESULTADO
        // =================================================

        return new ResultadoEstado(
                idMovimiento,
                activar,
                mensaje
        );
    }
}
