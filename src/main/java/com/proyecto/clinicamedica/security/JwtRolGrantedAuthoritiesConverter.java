package com.proyecto.clinicamedica.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * =========================================================
 * CONVERSOR DE ROL JWT A AUTORIDAD SPRING SECURITY
 * =========================================================
 *
 * Convierte el claim:
 *
 * rol = "Paciente"
 *
 * en:
 *
 * ROLE_PACIENTE
 *
 * Esto permitirá utilizar posteriormente:
 *
 * hasRole("PACIENTE")
 *
 * También normaliza roles con tildes.
 *
 * Ejemplo:
 *
 * Médico
 *      ↓
 * MEDICO
 *      ↓
 * ROLE_MEDICO
 *
 * Farmacéutico
 *      ↓
 * FARMACEUTICO
 *      ↓
 * ROLE_FARMACEUTICO
 * =========================================================
 */
@Component
public class JwtRolGrantedAuthoritiesConverter
        implements Converter<
        Jwt,
        Collection<GrantedAuthority>
        > {

    private static final String CLAIM_ROL =
            "rol";

    private static final String PREFIJO_ROL =
            "ROLE_";


    // =====================================================
    // CONVERTIR
    // =====================================================

    @Override
    public Collection<GrantedAuthority> convert(
            Jwt jwt
    ) {

        if (jwt == null) {
            return List.of();
        }


        String rol =
                jwt.getClaimAsString(
                        CLAIM_ROL
                );


        if (rol == null
                || rol.isBlank()) {

            return List.of();
        }


        String rolNormalizado =
                normalizarRol(
                        rol
                );


        if (rolNormalizado.isBlank()) {
            return List.of();
        }


        GrantedAuthority autoridad =
                new SimpleGrantedAuthority(
                        PREFIJO_ROL
                                + rolNormalizado
                );


        return List.of(
                autoridad
        );
    }


    // =====================================================
    // NORMALIZAR NOMBRE DE ROL
    // =====================================================

    private String normalizarRol(
            String rol
    ) {

        String sinTildes =
                Normalizer.normalize(
                                rol.trim(),
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}",
                                ""
                        );


        return sinTildes
                .toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^A-Z0-9]+",
                        "_"
                )
                .replaceAll(
                        "^_+|_+$",
                        ""
                );
    }
}