package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * =========================================================
 * DTO: FORMULARIO DE CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Transporta únicamente los datos que el Médico puede
 * capturar/modificar desde el formulario clínico.
 *
 * IMPORTANTE:
 *
 * NO recibe:
 *
 * - idMedico
 * - estado de la Cita
 * - versión
 * - firma digital
 * - creadoPor
 * - modificadoPor
 *
 * Esos valores se obtienen/controlan en backend.
 *
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsultaMedicaFormulario {


    // =====================================================
    // CITA
    // =====================================================

    /**
     * Identifica la cita cuya consulta se está editando.
     *
     * El Service siempre volverá a comprobar:
     *
     * - que exista;
     * - que pertenezca al Médico autenticado;
     * - que esté en el estado permitido.
     */
    private Integer idCita;


    // =====================================================
    // MOTIVO DE VISITA
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * PostgreSQL:
     * VARCHAR(2000)
     *
     * La obligatoriedad real se valida en Service.
     */
    @Size(
            max = 2000,
            message = "El motivo de visita no puede exceder los 2000 caracteres."
    )
    private String motivoVisita;


    // =====================================================
    // HALLAZGOS CLÍNICOS
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * Obligatorio al finalizar.
     *
     * Puede estar incompleto mientras la consulta
     * permanece En curso.
     */
    private String hallazgosClinicos;


    // =====================================================
    // CIE-10
    // =====================================================

    /**
     * RN-CU08-01.
     *
     * Opcional.
     *
     * Si contiene valor, el Service verificará que:
     *
     * - exista;
     * - esté activo.
     */
    private Integer idCie10;


    // =====================================================
    // DIAGNÓSTICO
    // =====================================================

    /**
     * RN-CU08-01.
     *
     * Al finalizar:
     *
     * - obligatorio;
     * - mínimo 10 caracteres;
     * - máximo 5000 caracteres.
     *
     * No usamos @Size(min = 10) aquí porque durante
     * una consulta En curso el diagnóstico puede todavía
     * estar vacío.
     */
    @Size(
            max = 5000,
            message = "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres."
    )
    private String diagnostico;
    


    // =====================================================
    // PLAN DE TRATAMIENTO
    // =====================================================

    /**
     * RN-CU08-02.
     *
     * Obligatorio para finalizar.
     */
    private String planTratamiento;


    // =====================================================
    // NOTAS ADICIONALES
    // =====================================================

    /**
     * Campo opcional.
     */
    private String notasAdicionales;
}