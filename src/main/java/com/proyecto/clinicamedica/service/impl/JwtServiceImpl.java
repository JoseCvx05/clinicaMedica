package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO JWT
 * =========================================================
 *
 * Genera tokens JWT para usuarios que ya fueron
 * autenticados correctamente.
 *
 * El token contiene únicamente:
 *
 * - sub       -> nombre de usuario
 * - usuarioId -> ID interno
 * - rol       -> rol del usuario
 * - iat       -> fecha de emisión
 * - exp       -> fecha de expiración
 *
 * NO incluye información sensible como:
 *
 * - Contraseña
 * - Hash de contraseña
 * - DPI
 * - DPI hash
 * - DPI cifrado
 * - NIT
 * - Teléfono
 *
 * La firma criptográfica es realizada por JwtEncoder,
 * configurado en JwtConfig.
 * =========================================================
 */
@Service
public class JwtServiceImpl
        implements JwtService {

    private final JwtEncoder jwtEncoder;

    private final Duration duracionToken;


    /**
     * Inyección mediante constructor.
     *
     * La duración se obtiene desde:
     *
     * app.security.jwt-expiration-minutes
     */
    public JwtServiceImpl(
            JwtEncoder jwtEncoder,

            @Value(
                    "${app.security.jwt-expiration-minutes:30}"
            )
            long expiracionMinutos
    ) {

        if (expiracionMinutos <= 0) {

            throw new IllegalStateException(
                    "La duración del JWT debe ser mayor que cero."
            );
        }

        this.jwtEncoder = jwtEncoder;

        this.duracionToken =
                Duration.ofMinutes(
                        expiracionMinutos
                );
    }


    // =====================================================
    // GENERAR TOKEN
    // =====================================================

    @Override
    public String generarToken(
            Usuario usuario
    ) {

        // =================================================
        // VALIDACIONES DEFENSIVAS
        // =================================================

        if (usuario == null) {

            throw new IllegalArgumentException(
                    "No se puede generar un JWT para un usuario nulo."
            );
        }


        if (usuario.getId() == null) {

            throw new IllegalArgumentException(
                    "El usuario debe estar persistido antes de generar el JWT."
            );
        }


        if (usuario.getNombreUsuario() == null
                || usuario.getNombreUsuario().isBlank()) {

            throw new IllegalArgumentException(
                    "El usuario no posee un nombre de usuario válido."
            );
        }


        if (usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || usuario.getRol().getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El usuario no posee un rol válido."
            );
        }


        // =================================================
        // FECHAS DEL TOKEN
        // =================================================

        Instant emitidoEn =
                Instant.now();

        Instant expiraEn =
                emitidoEn.plus(
                        duracionToken
                );


        // =================================================
        // CLAIMS JWT
        // =================================================

        JwtClaimsSet claims =
                JwtClaimsSet.builder()

                        /*
                         * SUB:
                         *
                         * Identidad principal del token.
                         */
                        .subject(
                                usuario.getNombreUsuario()
                        )

                        /*
                         * IAT:
                         *
                         * Momento en el que fue emitido.
                         */
                        .issuedAt(
                                emitidoEn
                        )

                        /*
                         * EXP:
                         *
                         * Momento en el que deja de ser válido.
                         */
                        .expiresAt(
                                expiraEn
                        )

                        /*
                         * ID interno del usuario.
                         */
                        .claim(
                                "usuarioId",
                                usuario.getId()
                        )

                        /*
                         * Rol necesario posteriormente
                         * para autorización.
                         */
                        .claim(
                                "rol",
                                usuario.getRol()
                                        .getNombre()
                        )

                        .build();


        // =================================================
        // FIRMAR JWT
        // =================================================

        Jwt jwt =
                jwtEncoder.encode(
                        JwtEncoderParameters.from(
                                claims
                        )
                );


        // =================================================
        // DEVOLVER TOKEN COMPACTO
        // =================================================

        return jwt.getTokenValue();
    }


    // =====================================================
    // DURACIÓN DEL TOKEN
    // =====================================================

    @Override
    public Duration obtenerDuracionToken() {

        return duracionToken;
    }
}