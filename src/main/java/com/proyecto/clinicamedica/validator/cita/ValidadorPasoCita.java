package com.proyecto.clinicamedica.validator.cita;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionCita;
import com.proyecto.clinicamedica.model.cita.PasoCita;

/**
 * =========================================================
 * CONTRATO DE VALIDACIÓN - PASOS DEL WIZARD
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Cada implementación valida exclusivamente un paso
 * del asistente.
 *
 * Ejemplos:
 *
 * - Sucursal
 * - Especialidad
 * - Médico
 * - Fecha/Hora
 * - Confirmación
 *
 * El Service trabajará contra esta interfaz y no contra
 * implementaciones concretas.
 * =========================================================
 */
public interface ValidadorPasoCita {


    /**
     * Indica a qué paso pertenece este validador.
     */
    PasoCita paso();


    /**
     * Orden dentro del mismo paso.
     *
     * Nos servirá especialmente en Confirmación,
     * donde pueden existir varias validaciones:
     *
     * - motivo;
     * - reserva temporal;
     * - documento.
     */
    default int orden() {

        return 100;
    }


    /**
     * Ejecuta la validación correspondiente.
     */
    void validar(
            CitaWizardDTO formulario,
            ResultadoValidacionCita resultado
    );
}