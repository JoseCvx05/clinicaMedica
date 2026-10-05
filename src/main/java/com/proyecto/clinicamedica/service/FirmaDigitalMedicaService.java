package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico.TipoDocumentoClinico;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.FirmaDocumentoClinicoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;

import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * =========================================================
 * SERVICIO: FIRMA DIGITAL MÉDICA
 * =========================================================
 *
 * CU-08 - RNF-031
 *
 * Firma criptográficamente documentos clínicos.
 *
 * Utiliza:
 *
 * - SHA-256 para integridad.
 * - RSA 2048 bits.
 * - SHA256withRSA para firma criptográfica.
 * - Base64 para almacenamiento.
 *
 * IMPORTANTE:
 *
 * Este mecanismo representa una firma criptográfica
 * interna del sistema.
 *
 * No equivale por sí solo a un certificado emitido por
 * una autoridad certificadora externa.
 *
 * =========================================================
 */
@Service
public class FirmaDigitalMedicaService {


    // =====================================================
    // CONFIGURACIÓN CRIPTOGRÁFICA
    // =====================================================

    private static final String ALGORITMO_FIRMA =
            "SHA256withRSA";

    private static final String ALGORITMO_LLAVE =
            "RSA";

    private static final String ALGORITMO_HASH =
            "SHA-256";

    private static final int TAMANO_LLAVE_RSA =
            2048;

    private static final String ROL_MEDICO =
            "Médico";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FirmaDocumentoClinicoRepository
            firmaDocumentoClinicoRepository;

    private final AuditoriaService
            auditoriaService;


    // =====================================================
    // LLAVES DE LOS MÉDICOS
    // =====================================================
    //
    // Una llave independiente por Médico durante la
    // ejecución actual de la aplicación.
    //
    // La llave privada solamente existe en memoria.
    //
    // La llave pública queda almacenada junto a la firma
    // para poder verificar documentos incluso después
    // de reiniciar la aplicación.
    // =====================================================

    private final Map<Integer, KeyPair>
            llavesMedicos =
            new ConcurrentHashMap<>();


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FirmaDigitalMedicaService(

            FirmaDocumentoClinicoRepository
                    firmaDocumentoClinicoRepository,

            AuditoriaService
                    auditoriaService
    ) {

        this.firmaDocumentoClinicoRepository =
                firmaDocumentoClinicoRepository;

        this.auditoriaService =
                auditoriaService;
    }


    // =====================================================
    // FIRMAR DOCUMENTO
    // =====================================================

    /**
     * Firma un documento clínico.
     *
     * La operación es idempotente:
     *
     * - Si el mismo documento ya fue firmado y el contenido
     *   sigue siendo exactamente igual, retorna la firma.
     *
     * - Si el documento ya estaba firmado pero su contenido
     *   cambió, bloquea la operación.
     */
    @Transactional
    public FirmaDocumentoClinico firmarDocumento(

            Usuario medico,

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento,

            String contenidoCanonico
    ) {

        // =================================================
        // 1. VALIDAR MÉDICO
        // =================================================

        validarMedico(
                medico
        );


        // =================================================
        // 2. VALIDAR DOCUMENTO
        // =================================================

        validarDocumento(

                tipoDocumento,

                idDocumento,

                contenidoCanonico
        );


        // =================================================
        // 3. CALCULAR HASH
        // =================================================

        String hashContenido =
                calcularHashSha256(
                        contenidoCanonico
                );


        // =================================================
        // 4. COMPROBAR SI YA ESTÁ FIRMADO
        // =================================================

        Optional<FirmaDocumentoClinico> existente =
                firmaDocumentoClinicoRepository
                        .findByTipoDocumentoAndIdDocumento(

                                tipoDocumento,

                                idDocumento
                        );


        if (existente.isPresent()) {

            FirmaDocumentoClinico firmaExistente =
                    existente.get();


            // =============================================
            // MISMO DOCUMENTO
            // =============================================

            if (hashContenido.equalsIgnoreCase(
                    firmaExistente.getHashContenido()
            )) {

                boolean valida =
                        verificarFirmaInterna(

                                firmaExistente,

                                contenidoCanonico
                        );


                if (!valida) {

                    throw new IllegalStateException(
                            "La firma digital existente "
                                    + "no pudo ser verificada."
                    );
                }


                return firmaExistente;
            }


            // =============================================
            // DOCUMENTO ALTERADO
            // =============================================

            throw new IllegalStateException(
                    "El documento clínico ya fue firmado "
                            + "y su contenido actual no coincide "
                            + "con el contenido firmado."
            );
        }


        // =================================================
        // 5. OBTENER LLAVE DEL MÉDICO
        // =================================================

        KeyPair keyPair =
                obtenerLlaveMedico(
                        medico.getId()
                );


        // =================================================
        // 6. GENERAR FIRMA
        // =================================================

        String firmaBase64 =
                generarFirma(

                        contenidoCanonico,

                        keyPair
                );


        // =================================================
        // 7. LLAVE PÚBLICA
        // =================================================

        String llavePublicaBase64 =
                Base64.getEncoder()
                        .encodeToString(

                                keyPair.getPublic()
                                        .getEncoded()
                        );


        // =================================================
        // 8. CREAR REGISTRO
        // =================================================

        FirmaDocumentoClinico firma =
                new FirmaDocumentoClinico();


        firma.setMedico(
                medico
        );


        firma.setTipoDocumento(
                tipoDocumento
        );


        firma.setIdDocumento(
                idDocumento
        );


        firma.setHashContenido(
                hashContenido
        );


        firma.setFirmaBase64(
                firmaBase64
        );


        firma.setLlavePublicaBase64(
                llavePublicaBase64
        );


        firma.setAlgoritmo(
                ALGORITMO_FIRMA
        );


        // =================================================
        // 9. GUARDAR
        // =================================================

        FirmaDocumentoClinico guardada =
                firmaDocumentoClinicoRepository
                        .saveAndFlush(
                                firma
                        );


        if (guardada.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible registrar "
                            + "la firma digital del documento."
            );
        }


        // =================================================
        // 10. VERIFICAR INMEDIATAMENTE
        // =================================================

        boolean firmaValida =
                verificarFirmaInterna(

                        guardada,

                        contenidoCanonico
                );


        if (!firmaValida) {

            throw new IllegalStateException(
                    "La firma digital generada "
                            + "no pudo ser verificada."
            );
        }


        // =================================================
        // 11. AUDITORÍA
        // =================================================

        registrarAuditoria(
                guardada,
                medico
        );


        return guardada;
    }


    // =====================================================
    // VERIFICAR DOCUMENTO
    // =====================================================

    /**
     * Verifica:
     *
     * 1. Que el hash actual coincida.
     * 2. Que la firma RSA sea válida con la llave pública
     *    almacenada.
     */
    @Transactional(readOnly = true)
    public boolean verificarDocumento(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento,

            String contenidoCanonico
    ) {

        validarDocumento(

                tipoDocumento,

                idDocumento,

                contenidoCanonico
        );


        FirmaDocumentoClinico firma =
                firmaDocumentoClinicoRepository
                        .findByTipoDocumentoAndIdDocumento(

                                tipoDocumento,

                                idDocumento
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "El documento clínico "
                                                        + "no posee firma digital."
                                        )
                        );


        return verificarFirmaInterna(

                firma,

                contenidoCanonico
        );
    }


    // =====================================================
    // BUSCAR FIRMA
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<FirmaDocumentoClinico> buscarFirma(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento
    ) {

        if (tipoDocumento == null
                || idDocumento == null
                || idDocumento <= 0) {

            return Optional.empty();
        }


        return firmaDocumentoClinicoRepository
                .findByTipoDocumentoAndIdDocumento(

                        tipoDocumento,

                        idDocumento
                );
    }


    // =====================================================
    // OBTENER / GENERAR LLAVE DEL MÉDICO
    // =====================================================

    private KeyPair obtenerLlaveMedico(
            Integer idMedico
    ) {

        if (idMedico == null
                || idMedico <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del Médico "
                            + "no es válido."
            );
        }


        return llavesMedicos
                .computeIfAbsent(

                        idMedico,

                        id ->
                                generarKeyPair()
                );
    }


    // =====================================================
    // GENERAR PAR RSA
    // =====================================================

    private KeyPair generarKeyPair() {

        try {

            KeyPairGenerator generador =
                    KeyPairGenerator
                            .getInstance(
                                    ALGORITMO_LLAVE
                            );


            generador.initialize(
                    TAMANO_LLAVE_RSA
            );


            return generador
                    .generateKeyPair();


        } catch (Exception ex) {

            throw new IllegalStateException(
                    "No fue posible generar "
                            + "las llaves criptográficas del Médico.",
                    ex
            );
        }
    }


    // =====================================================
    // GENERAR FIRMA
    // =====================================================

    private String generarFirma(

            String contenidoCanonico,

            KeyPair keyPair
    ) {

        try {

            Signature firma =
                    Signature.getInstance(
                            ALGORITMO_FIRMA
                    );


            firma.initSign(
                    keyPair.getPrivate()
            );


            firma.update(

                    contenidoCanonico
                            .getBytes(
                                    StandardCharsets.UTF_8
                            )
            );


            byte[] firmaGenerada =
                    firma.sign();


            return Base64
                    .getEncoder()
                    .encodeToString(
                            firmaGenerada
                    );


        } catch (Exception ex) {

            throw new IllegalStateException(
                    "No fue posible generar "
                            + "la firma digital del documento.",
                    ex
            );
        }
    }


    // =====================================================
    // VERIFICACIÓN INTERNA
    // =====================================================

    private boolean verificarFirmaInterna(

            FirmaDocumentoClinico firma,

            String contenidoCanonico
    ) {

        try {

            if (firma == null
                    || contenidoCanonico == null) {

                return false;
            }


            // =============================================
            // 1. COMPROBAR HASH
            // =============================================

            String hashActual =
                    calcularHashSha256(
                            contenidoCanonico
                    );


            if (!hashActual.equalsIgnoreCase(
                    firma.getHashContenido()
            )) {

                return false;
            }


            // =============================================
            // 2. RECUPERAR LLAVE PÚBLICA
            // =============================================

            byte[] llaveBytes =
                    Base64.getDecoder()
                            .decode(
                                    firma.getLlavePublicaBase64()
                            );


            X509EncodedKeySpec especificacion =
                    new X509EncodedKeySpec(
                            llaveBytes
                    );


            KeyFactory fabrica =
                    KeyFactory.getInstance(
                            ALGORITMO_LLAVE
                    );


            PublicKey llavePublica =
                    fabrica.generatePublic(
                            especificacion
                    );


            // =============================================
            // 3. VERIFICAR FIRMA
            // =============================================

            Signature verificador =
                    Signature.getInstance(
                            firma.getAlgoritmo()
                    );


            verificador.initVerify(
                    llavePublica
            );


            verificador.update(

                    contenidoCanonico
                            .getBytes(
                                    StandardCharsets.UTF_8
                            )
            );


            byte[] firmaBytes =
                    Base64.getDecoder()
                            .decode(
                                    firma.getFirmaBase64()
                            );


            return verificador.verify(
                    firmaBytes
            );


        } catch (Exception ex) {

            return false;
        }
    }


    // =====================================================
    // SHA-256
    // =====================================================

    public String calcularHashSha256(
            String contenido
    ) {

        if (contenido == null
                || contenido.isBlank()) {

            throw new IllegalArgumentException(
                    "El contenido del documento "
                            + "es obligatorio."
            );
        }


        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            ALGORITMO_HASH
                    );


            byte[] hash =
                    digest.digest(

                            contenido.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            StringBuilder hexadecimal =
                    new StringBuilder();


            for (byte valor : hash) {

                hexadecimal.append(
                        String.format(
                                "%02x",
                                valor & 0xff
                        )
                );
            }


            return hexadecimal.toString();


        } catch (Exception ex) {

            throw new IllegalStateException(
                    "No fue posible calcular "
                            + "el hash SHA-256 del documento.",
                    ex
            );
        }
    }


    // =====================================================
    // VALIDAR MÉDICO
    // =====================================================

    private void validarMedico(
            Usuario medico
    ) {

        if (medico == null
                || medico.getId() == null
                || medico.getId() <= 0) {

            throw new IllegalArgumentException(
                    "El Médico responsable "
                            + "de la firma no es válido."
            );
        }


        if (!Boolean.TRUE.equals(
                medico.getActivo()
        )) {

            throw new IllegalStateException(
                    "El Médico responsable "
                            + "se encuentra inactivo."
            );
        }


        if (medico.getRol() == null
                || medico.getRol()
                .getNombre() == null

                || !ROL_MEDICO.equalsIgnoreCase(

                medico.getRol()
                        .getNombre()
                        .trim()
        )) {

            throw new IllegalStateException(
                    "El usuario indicado no posee "
                            + "el rol Médico."
            );
        }
    }


    // =====================================================
    // VALIDAR DOCUMENTO
    // =====================================================

    private void validarDocumento(

            TipoDocumentoClinico tipoDocumento,

            Long idDocumento,

            String contenidoCanonico
    ) {

        if (tipoDocumento == null) {

            throw new IllegalArgumentException(
                    "El tipo de documento clínico "
                            + "es obligatorio."
            );
        }


        if (idDocumento == null
                || idDocumento <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del documento "
                            + "no es válido."
            );
        }


        if (contenidoCanonico == null
                || contenidoCanonico.isBlank()) {

            throw new IllegalArgumentException(
                    "El contenido del documento clínico "
                            + "es obligatorio."
            );
        }
    }


    // =====================================================
    // AUDITORÍA
    // =====================================================

    private void registrarAuditoria(

            FirmaDocumentoClinico firma,

            Usuario medico
    ) {

        Map<String, Object> datos =
                new LinkedHashMap<>();


        datos.put(
                "tipoDocumento",
                firma.getTipoDocumento()
                        .name()
        );


        datos.put(
                "idDocumento",
                firma.getIdDocumento()
        );


        datos.put(
                "idMedico",
                medico.getId()
        );


        datos.put(
                "hashContenido",
                firma.getHashContenido()
        );


        datos.put(
                "algoritmo",
                firma.getAlgoritmo()
        );


        /*
         * No enviamos a la bitácora:
         *
         * - firmaBase64 completa;
         * - llave pública completa.
         *
         * La evidencia completa ya está almacenada
         * en firma_documento_clinico.
         */
        auditoriaService.registrar(
                new RegistroAuditoria(

                        "firma_documento_clinico",

                        String.valueOf(
                                firma.getId()
                        ),

                        AccionAuditoria.CREAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        null,

                        datos,

                        null
                )
        );
    }
}