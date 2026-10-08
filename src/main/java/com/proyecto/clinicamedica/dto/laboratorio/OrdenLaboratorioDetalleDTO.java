package com.proyecto.clinicamedica.dto.laboratorio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;


/**
 * =========================================================
 * DTO: DETALLE DE ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Datos que necesita la pantalla de detalle:
 *
 * - Número de orden.
 * - Paciente.
 * - Médico.
 * - Estado.
 * - Tipo de orden.
 * - Observaciones.
 * - Fecha de creación.
 * - Exámenes solicitados.
 * - Precio individual.
 * - Monto total.
 *
 * =========================================================
 */
public record OrdenLaboratorioDetalleDTO(

        Integer idOrden,

        String numeroOrden,

        String nombrePaciente,

        String nombreMedico,

        String estado,

        boolean ordenExterna,

        String observaciones,

        OffsetDateTime fechaHoraCreacion,

        BigDecimal montoTotal,

        List<ExamenOrdenDTO> examenes
) {


    /**
     * =====================================================
     * EXAMEN DE LA ORDEN
     * =====================================================
     *
     * Representa cada examen solicitado por el Médico.
     *
     * El precio corresponde al snapshot guardado
     * en detalle_orden_laboratorio.
     *
     * =====================================================
     */
    public record ExamenOrdenDTO(

            Integer idDetalle,

            String nombreExamen,

            BigDecimal precioExamen,

            String unidad,

            String rangoReferencia
    ) {
    }
}