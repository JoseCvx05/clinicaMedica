package com.proyecto.clinicamedica.service;

/**
 * =========================================================
 * SERVICIO: HASH / HMAC
 * =========================================================
 *
 * Define el contrato para generar representaciones
 * criptográficas deterministas de datos sensibles.
 *
 * En este proyecto se utilizará principalmente para:
 *
 * - DPI
 * - NIT
 *
 * IMPORTANTE:
 *
 * No se utilizará un SHA-256 simple.
 *
 * Se utilizará HMAC-SHA-256, ya que incorpora una
 * clave secreta adicional y permite realizar búsquedas
 * deterministas sin almacenar el dato original.
 *
 * Esta interfaz permite aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Inversión de dependencias (SOLID)
 * =========================================================
 */
public interface HashService {

    /**
     * Genera un HMAC determinista del valor recibido.
     *
     * El mismo valor, utilizando la misma clave secreta,
     * producirá siempre el mismo resultado.
     *
     * @param valor dato normalizado que será protegido
     * @return HMAC hexadecimal
     */
    String generarHash(String valor);


    /**
     * Compara un valor original contra un hash/HMAC
     * previamente generado.
     *
     * @param valor valor original
     * @param hashEsperado hash esperado
     * @return true si corresponden
     */
    boolean coincide(String valor, String hashEsperado);
}