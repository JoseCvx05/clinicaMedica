package com.proyecto.clinicamedica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * =========================================================
 * CONFIGURACIÓN JWT
 * =========================================================
 *
 * Centraliza la clave utilizada para:
 *
 * - Firmar JWT.
 * - Validar JWT.
 *
 * Utilizamos:
 *
 * HMAC-SHA-256 / HS256
 *
 * IMPORTANTE:
 *
 * JWT_SECRET debe ser independiente de:
 *
 * - AES_SECRET
 * - HMAC_SECRET
 *
 * Nunca debe almacenarse directamente en el código.
 * =========================================================
 */
@Configuration
public class JwtConfig {

    /**
     * Política del proyecto:
     *
     * mínimo 32 bytes = 256 bits.
     */
    private static final int LONGITUD_MINIMA_CLAVE =
            32;


    // =====================================================
    // SECRET KEY
    // =====================================================

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.security.jwt-secret}")
            String jwtSecretBase64
    ) {

        if (jwtSecretBase64 == null
                || jwtSecretBase64.isBlank()) {

            throw new IllegalStateException(
                    "JWT_SECRET no se encuentra configurado."
            );
        }


        byte[] claveBytes;

        try {

            claveBytes =
                    Base64.getDecoder()
                            .decode(
                                    jwtSecretBase64.trim()
                            );

        } catch (IllegalArgumentException e) {

            throw new IllegalStateException(
                    "JWT_SECRET debe estar codificado en Base64.",
                    e
            );
        }


        if (claveBytes.length
                < LONGITUD_MINIMA_CLAVE) {

            throw new IllegalStateException(
                    "JWT_SECRET debe contener al menos "
                            + LONGITUD_MINIMA_CLAVE
                            + " bytes."
            );
        }


        return new SecretKeySpec(
                claveBytes,
                "HmacSHA256"
        );
    }


    // =====================================================
    // JWT ENCODER
    // =====================================================

    /**
     * Se utiliza para firmar los JWT generados
     * después de una autenticación correcta.
     */
    @Bean
    public JwtEncoder jwtEncoder(
            SecretKey jwtSecretKey
    ) {

        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }


    // =====================================================
    // JWT DECODER
    // =====================================================

    /**
     * Spring Security utilizará este componente
     * posteriormente para verificar y decodificar
     * los JWT enviados por el navegador.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey
    ) {

        return NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}