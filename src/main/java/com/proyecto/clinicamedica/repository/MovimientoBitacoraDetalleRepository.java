
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.MovimientoBitacoraDetalle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * =========================================================
 * CU-15 - REPOSITORY: DETALLE DE BITÁCORA
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Guardar los datos complementarios del movimiento.
 * - Consultar el detalle mediante su identificador.
 * - Consultar el estado activo/inactivo.
 * - Actualizar el estado de un movimiento.
 *
 * IMPORTANTE:
 *
 * - No modifica directamente el stock.
 * - No elimina movimientos históricos.
 * - No realiza despachos.
 * - No controla permisos de usuarios.
 *
 * Las operaciones y validaciones se realizarán
 * desde el Service correspondiente.
 *
 * =========================================================
 */
@Repository
public interface MovimientoBitacoraDetalleRepository
        extends JpaRepository<MovimientoBitacoraDetalle, Long> {

}
