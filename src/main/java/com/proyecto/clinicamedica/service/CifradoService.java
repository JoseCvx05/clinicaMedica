package com.proyecto.clinicamedica.service;

/**
 * =========================================================
 * SERVICIO: CIFRADO
 * =========================================================
 *
 * Define el contrato para cifrar y descifrar
 * información sensible del sistema.
 *
 * Se utilizará principalmente para:
 *
 * - DPI
 * - NIT
 *
 * IMPORTANTE:
 *
 * Este servicio NO se utiliza para contraseñas.
 *
 * Las contraseñas son irreversibles y se manejan
 * mediante PasswordEncoder / BCrypt.
 *
 * Este servicio sí permite recuperar el valor original
 * únicamente cuando una operación autorizada lo necesite.
 *
 * Permite aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Inversión de dependencias (SOLID)
 * =========================================================
 */
public interface CifradoService {

    /**
     * Cifra un valor sensible.
     *
     * @param valor valor original
     * @return valor cifrado
     */
    String cifrar(String valor);


    /**
     * Descifra un valor previamente protegido.
     *
     * @param valorCifrado valor cifrado
     * @return valor original
     */
    String descifrar(String valorCifrado);
}