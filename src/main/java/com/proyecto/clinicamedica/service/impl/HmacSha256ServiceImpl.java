package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.service.HashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * =========================================================
 * IMPLEMENTACIÓN: HMAC-SHA-256
 * =========================================================
 *
 * Genera un HMAC-SHA-256 determinista para datos sensibles.
 *
 * Se utilizará principalmente para:
 *
 * - DPI
 * - NIT
 *
 * IMPORTANTE:
 *
 * La clave secreta NO está escrita en esta clase.
 * Se obtiene desde una variable de entorno/configuración.
 *
 * Esto permite realizar búsquedas por DPI/NIT sin almacenar
 * esos datos en texto plano.
 *
 * Implementa HashService, permitiendo aplicar:
 *
 * - Abstracción
 * - Polimorfismo
 * - Inversión de dependencias
 * =========================================================
 */
@Service
public class HmacSha256ServiceImpl implements HashService {

    private static final String ALGORITMO = "HmacSHA256";

    /**
     * Clave utilizada por HMAC.
     *
     * Se construye una sola vez al iniciar el servicio.
     */
    private final SecretKeySpec claveSecreta;


    /**
     * Spring obtiene la clave desde:
     *
     * app.security.hmac-secret
     *
     * La clave será almacenada como Base64.
     */
    public HmacSha256ServiceImpl(
            @Value("${app.security.hmac-secret}")
            String claveBase64
    ) {

        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new IllegalStateException(
                    "La clave HMAC no está configurada."
            );
        }

        try {

            byte[] claveBytes =
                    Base64.getDecoder()
                            .decode(claveBase64.trim());

            /*
             * Exigimos al menos 32 bytes = 256 bits.
             */
            if (claveBytes.length < 32) {
                throw new IllegalStateException(
                        "La clave HMAC debe contener al menos 256 bits."
                );
            }

            this.claveSecreta =
                    new SecretKeySpec(
                            claveBytes,
                            ALGORITMO
                    );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "La clave HMAC debe estar codificada correctamente en Base64.",
                    exception
            );
        }
    }


    /**
     * Genera un HMAC-SHA-256 hexadecimal.
     *
     * 32 bytes de SHA-256 producen:
     *
     * 64 caracteres hexadecimales.
     *
     * Esto coincide con dpi_hash y nit_hash
     * definidos como VARCHAR(64).
     */
    @Override
    public String generarHash(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "El valor para generar el HMAC no puede estar vacío."
            );
        }

        try {

            Mac mac = Mac.getInstance(ALGORITMO);

            mac.init(claveSecreta);

            byte[] resultado =
                    mac.doFinal(
                            valor.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat
                    .of()
                    .formatHex(resultado);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "El algoritmo HmacSHA256 no está disponible.",
                    exception
            );

        } catch (java.security.InvalidKeyException exception) {

            throw new IllegalStateException(
                    "La clave configurada para HMAC no es válida.",
                    exception
            );
        }
    }


    /**
     * Verifica si un valor corresponde al HMAC almacenado.
     *
     * MessageDigest.isEqual realiza una comparación
     * apropiada para valores criptográficos.
     */
    @Override
    public boolean coincide(
            String valor,
            String hashEsperado
    ) {

        if (valor == null
                || valor.isBlank()
                || hashEsperado == null
                || hashEsperado.isBlank()) {

            return false;
        }

        try {

            String hashCalculado =
                    generarHash(valor);

            byte[] calculado =
                    HexFormat.of()
                            .parseHex(hashCalculado);

            byte[] esperado =
                    HexFormat.of()
                            .parseHex(hashEsperado.trim());

            return MessageDigest.isEqual(
                    calculado,
                    esperado
            );

        } catch (IllegalArgumentException exception) {

            return false;
        }
    }
}