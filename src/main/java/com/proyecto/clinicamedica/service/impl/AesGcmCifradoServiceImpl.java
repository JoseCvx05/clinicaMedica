package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.service.CifradoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * =========================================================
 * IMPLEMENTACIÓN DE CIFRADO AES-256-GCM
 * =========================================================
 *
 * Protege información sensible reversible como:
 *
 * - DPI
 * - NIT
 *
 * Características:
 *
 * - AES de 256 bits.
 * - Modo GCM.
 * - IV aleatorio para cada cifrado.
 * - Autenticación integrada mediante GCM.
 *
 * IMPORTANTE:
 *
 * La clave NO se almacena en el código fuente.
 * Se obtiene desde configuración mediante AES_SECRET.
 *
 * Este servicio NO se utiliza para contraseñas.
 * =========================================================
 */
@Service
public class AesGcmCifradoServiceImpl implements CifradoService {

    private static final String ALGORITMO =
            "AES/GCM/NoPadding";

    /**
     * Tamaño recomendado del IV para GCM:
     * 12 bytes = 96 bits.
     */
    private static final int TAMANO_IV = 12;

    /**
     * Longitud de la etiqueta de autenticación:
     * 128 bits.
     */
    private static final int TAMANO_TAG = 128;

    /**
     * Prefijo de versión.
     *
     * Nos permitirá cambiar el mecanismo de cifrado
     * en el futuro sin confundir datos antiguos.
     */
    private static final String VERSION = "v1";

    private final SecretKeySpec claveSecreta;

    private final SecureRandom secureRandom =
            new SecureRandom();


    /**
     * Recibe una clave codificada en Base64.
     *
     * La clave debe contener exactamente:
     *
     * 32 bytes = 256 bits.
     */
    public AesGcmCifradoServiceImpl(
            @Value("${app.security.aes-secret}")
            String claveBase64
    ) {

        if (claveBase64 == null || claveBase64.isBlank()) {

            throw new IllegalStateException(
                    "La clave AES no está configurada."
            );
        }

        try {

            byte[] claveBytes =
                    Base64.getDecoder()
                            .decode(claveBase64.trim());

            if (claveBytes.length != 32) {

                throw new IllegalStateException(
                        "La clave AES debe contener exactamente 256 bits (32 bytes)."
                );
            }

            this.claveSecreta =
                    new SecretKeySpec(
                            claveBytes,
                            "AES"
                    );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "La clave AES debe estar correctamente codificada en Base64.",
                    exception
            );
        }
    }


    /**
     * Cifra un valor utilizando AES-256-GCM.
     *
     * El formato almacenado será:
     *
     * v1:BASE64(IV + DATOS_CIFRADOS)
     */
    @Override
    public String cifrar(String valor) {

        if (valor == null || valor.isBlank()) {

            throw new IllegalArgumentException(
                    "El valor que se desea cifrar no puede estar vacío."
            );
        }

        try {

            // =============================================
            // 1. Generar IV aleatorio
            // =============================================

            byte[] iv = new byte[TAMANO_IV];

            secureRandom.nextBytes(iv);


            // =============================================
            // 2. Configurar AES-GCM
            // =============================================

            Cipher cipher =
                    Cipher.getInstance(ALGORITMO);

            GCMParameterSpec gcmParameterSpec =
                    new GCMParameterSpec(
                            TAMANO_TAG,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    claveSecreta,
                    gcmParameterSpec
            );


            // =============================================
            // 3. Cifrar
            // =============================================

            byte[] datosCifrados =
                    cipher.doFinal(
                            valor.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            // =============================================
            // 4. Guardar IV + ciphertext
            // =============================================

            byte[] resultado =
                    new byte[
                            iv.length
                                    + datosCifrados.length
                            ];

            System.arraycopy(
                    iv,
                    0,
                    resultado,
                    0,
                    iv.length
            );

            System.arraycopy(
                    datosCifrados,
                    0,
                    resultado,
                    iv.length,
                    datosCifrados.length
            );


            // =============================================
            // 5. Codificar para almacenar en PostgreSQL
            // =============================================

            return VERSION
                    + ":"
                    + Base64.getEncoder()
                    .encodeToString(resultado);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "No fue posible cifrar la información sensible.",
                    exception
            );
        }
    }


    /**
     * Descifra un valor previamente generado
     * por este servicio.
     */
    @Override
    public String descifrar(String valorCifrado) {

        if (valorCifrado == null
                || valorCifrado.isBlank()) {

            throw new IllegalArgumentException(
                    "El valor cifrado no puede estar vacío."
            );
        }

        try {

            // =============================================
            // 1. Separar versión y contenido
            // =============================================

            String[] partes =
                    valorCifrado.split(":", 2);

            if (partes.length != 2) {

                throw new IllegalArgumentException(
                        "El formato del dato cifrado no es válido."
                );
            }

            if (!VERSION.equals(partes[0])) {

                throw new IllegalArgumentException(
                        "La versión del dato cifrado no es compatible."
                );
            }


            // =============================================
            // 2. Decodificar Base64
            // =============================================

            byte[] contenido =
                    Base64.getDecoder()
                            .decode(partes[1]);


            if (contenido.length <= TAMANO_IV) {

                throw new IllegalArgumentException(
                        "El contenido cifrado no es válido."
                );
            }


            // =============================================
            // 3. Extraer IV
            // =============================================

            byte[] iv =
                    new byte[TAMANO_IV];

            System.arraycopy(
                    contenido,
                    0,
                    iv,
                    0,
                    TAMANO_IV
            );


            // =============================================
            // 4. Extraer ciphertext
            // =============================================

            int longitudCifrado =
                    contenido.length - TAMANO_IV;

            byte[] datosCifrados =
                    new byte[longitudCifrado];

            System.arraycopy(
                    contenido,
                    TAMANO_IV,
                    datosCifrados,
                    0,
                    longitudCifrado
            );


            // =============================================
            // 5. Configurar descifrado AES-GCM
            // =============================================

            Cipher cipher =
                    Cipher.getInstance(ALGORITMO);

            GCMParameterSpec gcmParameterSpec =
                    new GCMParameterSpec(
                            TAMANO_TAG,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    claveSecreta,
                    gcmParameterSpec
            );


            // =============================================
            // 6. Descifrar
            // =============================================

            byte[] resultado =
                    cipher.doFinal(datosCifrados);

            return new String(
                    resultado,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "No fue posible descifrar la información sensible.",
                    exception
            );
        }
    }
}