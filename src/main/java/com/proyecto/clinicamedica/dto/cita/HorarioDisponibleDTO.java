package com.proyecto.clinicamedica.dto.cita;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * DTO: HORARIO DISPONIBLE
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Representa un espacio que puede seleccionar el paciente
 * en el Paso 4 del wizard.
 *
 * =========================================================
 */
public record HorarioDisponibleDTO(

        OffsetDateTime inicio,

        OffsetDateTime fin

) {
}