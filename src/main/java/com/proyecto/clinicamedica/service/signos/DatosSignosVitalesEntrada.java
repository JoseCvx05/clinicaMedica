package com.proyecto.clinicamedica.service.signos;

import java.math.BigDecimal;


/**
 * =========================================================
 * CONTRATO DE ENTRADA: SIGNOS VITALES
 * =========================================================
 *
 * RNF-029.
 *
 * Representa los datos mínimos que necesita el núcleo
 * de CU-07 para registrar signos vitales.
 *
 * Actualmente puede ser implementado por:
 *
 * - Captura manual desde el formulario web.
 *
 * En el futuro podrá ser implementado por:
 *
 * - Adaptador de dispositivo IoT.
 * - Monitor médico.
 * - Equipo automático de signos vitales.
 *
 * El Service no depende de la tecnología que originó
 * las mediciones.
 *
 * =========================================================
 */
public interface DatosSignosVitalesEntrada {


    Integer getIdCita();


    Short getPresionSistolica();


    Short getPresionDiastolica();


    BigDecimal getTemperatura();


    BigDecimal getPeso();


    BigDecimal getTalla();


    Short getFrecuenciaCardiaca();


    Boolean getEsEmergencia();
}