package com.proyecto.clinicamedica.service.disponibilidad;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * CONTRATO: FUENTE DE OCUPACIÓN DE HORARIO
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Cada implementación representa una razón diferente
 * por la cual un horario puede no estar disponible.
 *
 * Ejemplos:
 *
 * - cita existente;
 * - evento de agenda;
 * - reserva temporal.
 *
 * =========================================================
 */
public interface FuenteOcupacionHorario {


    /**
     * Determina si existe un conflicto con el intervalo
     * solicitado.
     */
    boolean estaOcupado(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    );
}