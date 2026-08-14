package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

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
 * SERVICIO: JWT
 * =========================================================
 *
 * Genera tokens JWT para usuarios que previamente
 * fueron autenticados correctamente.
 *
 * Este servicio NO realiza:
 *
 * - Validación de contraseña.
 * - Control de intentos fallidos.
 * - Bloqueo temporal.
 * - Validación de rol para login.
 *
 * Esas responsabilidades pertenecen al servicio
 * de autenticación.
 *
 * El JWT contiene únicamente:
 *
 * - sub       -> nombre de usuario
 * - usuarioId -> ID interno
 * - rol       -> rol del usuario
 * - iat       -> fecha de emisión
 * - exp       -> fecha de expiración
 *
 * Nunca incluye:
 *
 * - contraseña
 * - hash de contraseña
 * - DPI
 * - dpiHash
 * - dpiCifrado
 * - NIT
 * - nitHash
 * - nitCifrado
 *
 * La firma criptográfica se realiza mediante
 * JwtEncoder configurado en JwtConfig.
 *
 * =========================================================
 */
@Service
public class JwtService {


    // =====================================================
    // DEPENDENCIAS / CONFIGURACIÓN
    // =====================================================

    private final JwtEncoder jwtEncoder;

    private final Duration duracionToken;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public JwtService(
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


        this.jwtEncoder =
                jwtEncoder;


        this.duracionToken =
                Duration.ofMinutes(
                        expiracionMinutos
                );
    }


    // =====================================================
    // GENERAR TOKEN
    // =====================================================

    public String generarToken(
            Usuario usuario
    ) {

        // =================================================
        // VALIDAR USUARIO
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
                || usuario
                .getNombreUsuario()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "El usuario no posee un nombre de usuario válido."
            );
        }


        if (usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || usuario
                .getRol()
                .getNombre()
                .isBlank()) {

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
        // CLAIMS
        // =================================================

        JwtClaimsSet claims =
                JwtClaimsSet
                        .builder()

                        // =================================
                        // IDENTIDAD PRINCIPAL
                        // =================================

                        .subject(
                                usuario.getNombreUsuario()
                        )


                        // =================================
                        // FECHA DE EMISIÓN
                        // =================================

                        .issuedAt(
                                emitidoEn
                        )


                        // =================================
                        // FECHA DE EXPIRACIÓN
                        // =================================

                        .expiresAt(
                                expiraEn
                        )


                        // =================================
                        // ID INTERNO
                        // =================================

                        .claim(
                                "usuarioId",
                                usuario.getId()
                        )


                        // =================================
                        // ROL
                        // =================================

                        .claim(
                                "rol",
                                usuario
                                        .getRol()
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
        // DEVOLVER TOKEN
        // =================================================

        return jwt.getTokenValue();
    }


    // =====================================================
    // DURACIÓN DEL TOKEN
    // =====================================================

    public Duration obtenerDuracionToken() {

        return duracionToken;
    }
}