package com.proyecto.clinicamedica.validator.registro;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

/**
 * =========================================================
 * CONTRATO DE VALIDACIÓN - REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Cada implementación valida una responsabilidad
 * específica del formulario.
 *
 * Ejemplos:
 *
 * - Datos personales.
 * - Credenciales.
 * - Duplicados.
 *
 * El Service trabajará contra esta interfaz y no contra
 * clases concretas, aplicando polimorfismo y DIP.
 * =========================================================
 */
public interface ValidadorRegistroExterno {

    /**
     * Define el orden en que debe ejecutarse
     * este validador.
     *
     * Menor número = se ejecuta primero.
     */
    int orden();


    /**
     * Ejecuta sus validaciones sobre el formulario.
     *
     * Cada implementación agrega sus errores al mismo
     * ResultadoValidacionRegistroExterno.
     */
    void validar(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    );
}