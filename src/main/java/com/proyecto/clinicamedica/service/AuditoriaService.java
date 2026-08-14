package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.entity.BitacoraAuditoria;
import com.proyecto.clinicamedica.repository.BitacoraAuditoriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * =========================================================
 * SERVICIO: AUDITORÍA
 * =========================================================
 *
 * Registra las operaciones realizadas dentro del sistema
 * en la bitácora de auditoría.
 *
 * Responsabilidades:
 *
 * - Validar el registro recibido.
 * - Evitar almacenar información sensible.
 * - Convertir RegistroAuditoria en BitacoraAuditoria.
 * - Insertar el registro en PostgreSQL.
 *
 * IMPORTANTE:
 *
 * Este servicio únicamente CREA registros.
 *
 * No permite:
 *
 * - Actualizar auditorías.
 * - Eliminar auditorías.
 *
 * La inmutabilidad también debe mantenerse reforzada
 * mediante la entidad y PostgreSQL.
 *
 * =========================================================
 */
@Service
public class AuditoriaService {


    // =====================================================
    // CAMPOS SENSIBLES
    // =====================================================

    private static final Set<String> CAMPOS_SENSIBLES =
            Set.of(
                    "contrasena",
                    "contrasenahash",
                    "password",
                    "passwordhash",

                    "dpi",
                    "dpicifrado",
                    "dpihash",

                    "nit",
                    "nitcifrado",
                    "nithash",

                    "jwt",
                    "token",
                    "accesstoken",
                    "refreshtoken"
            );


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final BitacoraAuditoriaRepository
            bitacoraAuditoriaRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AuditoriaService(
            BitacoraAuditoriaRepository bitacoraAuditoriaRepository
    ) {

        this.bitacoraAuditoriaRepository =
                bitacoraAuditoriaRepository;
    }


    // =====================================================
    // REGISTRAR AUDITORÍA
    // =====================================================

    @Transactional
    public void registrar(
            RegistroAuditoria registro
    ) {

        // =================================================
        // VALIDAR REGISTRO
        // =================================================

        validarRegistro(
                registro
        );


        // =================================================
        // VALIDAR SNAPSHOTS
        // =================================================

        validarSnapshotSeguro(
                registro.valoresAnteriores()
        );


        validarSnapshotSeguro(
                registro.valoresNuevos()
        );


        // =================================================
        // CONSTRUIR ENTIDAD
        // =================================================

        BitacoraAuditoria auditoria =
                new BitacoraAuditoria();


        auditoria.setTablaAfectada(
                registro
                        .tablaAfectada()
                        .trim()
        );


        // =================================================
        // REGISTRO AFECTADO
        // =================================================

        if (registro.idRegistroAfectado() != null) {

            auditoria.setIdRegistroAfectado(
                    registro
                            .idRegistroAfectado()
                            .trim()
            );
        }


        // =================================================
        // ACCIÓN
        // =================================================

        auditoria.setAccion(
                registro
                        .accion()
                        .getValorBaseDatos()
        );


        // =================================================
        // USUARIO EJECUTOR
        // =================================================

        auditoria.setIdUsuario(
                registro.idUsuario()
        );


        if (registro.nombreUsuario() != null) {

            auditoria.setNombreUsuario(
                    registro
                            .nombreUsuario()
                            .trim()
            );
        }


        // =================================================
        // VALORES ANTERIORES / NUEVOS
        // =================================================

        auditoria.setValoresAnteriores(
                registro.valoresAnteriores()
        );


        auditoria.setValoresNuevos(
                registro.valoresNuevos()
        );


        // =================================================
        // DIRECCIÓN IP
        // =================================================

        if (registro.direccionIp() != null) {

            auditoria.setDireccionIp(
                    registro
                            .direccionIp()
                            .trim()
            );
        }


        // =================================================
        // INSERT EN POSTGRESQL
        // =================================================

        bitacoraAuditoriaRepository
                .save(
                        auditoria
                );
    }


    // =====================================================
    // VALIDAR REGISTRO
    // =====================================================

    private void validarRegistro(
            RegistroAuditoria registro
    ) {

        if (registro == null) {

            throw new IllegalArgumentException(
                    "El registro de auditoría no puede ser nulo."
            );
        }


        // =================================================
        // TABLA AFECTADA
        // =================================================

        if (registro.tablaAfectada() == null
                || registro
                .tablaAfectada()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "La tabla afectada es obligatoria para registrar la auditoría."
            );
        }


        if (registro
                .tablaAfectada()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "La tabla afectada no puede exceder los 100 caracteres."
            );
        }


        // =================================================
        // ID DEL REGISTRO
        // =================================================

        if (registro.idRegistroAfectado() != null
                && registro
                .idRegistroAfectado()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "El identificador del registro afectado no puede exceder los 50 caracteres."
            );
        }


        // =================================================
        // ACCIÓN
        // =================================================

        if (registro.accion() == null) {

            throw new IllegalArgumentException(
                    "La acción de auditoría es obligatoria."
            );
        }


        // =================================================
        // NOMBRE DEL USUARIO
        // =================================================

        if (registro.nombreUsuario() != null
                && registro
                .nombreUsuario()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre del usuario ejecutor no puede exceder los 100 caracteres."
            );
        }


        // =================================================
        // DIRECCIÓN IP
        // =================================================

        if (registro.direccionIp() != null
                && registro
                .direccionIp()
                .trim()
                .length() > 45) {

            throw new IllegalArgumentException(
                    "La dirección IP no puede exceder los 45 caracteres."
            );
        }
    }


    // =====================================================
    // VALIDAR SNAPSHOT SEGURO
    // =====================================================

    /**
     * Evita almacenar accidentalmente información
     * sensible dentro de los campos JSONB de auditoría.
     */
    private void validarSnapshotSeguro(
            Map<String, Object> snapshot
    ) {

        if (snapshot == null
                || snapshot.isEmpty()) {

            return;
        }


        for (String clave :
                snapshot.keySet()) {

            if (clave == null) {

                continue;
            }


            String claveNormalizada =
                    normalizarClave(
                            clave
                    );


            if (CAMPOS_SENSIBLES.contains(
                    claveNormalizada
            )) {

                throw new IllegalArgumentException(
                        "El campo "
                                + clave
                                + " contiene información sensible "
                                + "y no puede almacenarse en la auditoría."
                );
            }
        }
    }


    // =====================================================
    // NORMALIZAR CLAVE
    // =====================================================

    private String normalizarClave(
            String clave
    ) {

        return clave
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}