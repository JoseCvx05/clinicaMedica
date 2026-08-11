package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.RegistroAuditoria;
import com.proyecto.clinicamedica.entity.BitacoraAuditoria;
import com.proyecto.clinicamedica.repository.BitacoraAuditoriaRepository;
import com.proyecto.clinicamedica.service.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * IMPLEMENTACIÓN DEL SERVICIO DE AUDITORÍA
 * =========================================================
 *
 * Convierte un RegistroAuditoria en una entidad
 * BitacoraAuditoria y la persiste en PostgreSQL.
 *
 * Para CU-01 registrará:
 *
 * - Crear usuario.
 * - Actualizar usuario.
 * - Eliminar usuario mediante borrado lógico.
 *
 * La auditoría solamente admite creación de registros.
 *
 * La inmutabilidad se refuerza mediante:
 *
 * - @Immutable en BitacoraAuditoria.
 * - Trigger PostgreSQL.
 *
 * IMPORTANTE:
 *
 * Nunca se permite incluir información sensible en
 * valoresAnteriores o valoresNuevos.
 * =========================================================
 */
@Service
public class AuditoriaServiceImpl
        implements AuditoriaService {

    private final BitacoraAuditoriaRepository
            bitacoraAuditoriaRepository;


    /**
     * Nombres de propiedades que nunca deben aparecer
     * en los snapshots de auditoría.
     *
     * Se comparan normalizados en minúsculas.
     */
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


    public AuditoriaServiceImpl(
            BitacoraAuditoriaRepository
                    bitacoraAuditoriaRepository
    ) {

        this.bitacoraAuditoriaRepository =
                bitacoraAuditoriaRepository;
    }


    // =====================================================
    // REGISTRAR
    // =====================================================

    @Override
    @Transactional
    public void registrar(
            RegistroAuditoria registro
    ) {

        // =================================================
        // VALIDACIÓN DEL REGISTRO
        // =================================================

        validarRegistro(
                registro
        );


        // =================================================
        // VALIDACIÓN DE SNAPSHOTS
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
                registro.tablaAfectada()
                        .trim()
        );


        if (registro.idRegistroAfectado() != null) {

            auditoria.setIdRegistroAfectado(
                    registro.idRegistroAfectado()
                            .trim()
            );
        }


        auditoria.setAccion(
                registro.accion()
                        .getValorBaseDatos()
        );


        auditoria.setIdUsuario(
                registro.idUsuario()
        );


        if (registro.nombreUsuario() != null) {

            auditoria.setNombreUsuario(
                    registro.nombreUsuario()
                            .trim()
            );
        }


        auditoria.setValoresAnteriores(
                registro.valoresAnteriores()
        );


        auditoria.setValoresNuevos(
                registro.valoresNuevos()
        );


        if (registro.direccionIp() != null) {

            auditoria.setDireccionIp(
                    registro.direccionIp()
                            .trim()
            );
        }


        // =================================================
        // INSERT
        // =================================================

        bitacoraAuditoriaRepository.save(
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


        if (registro.tablaAfectada() == null
                || registro.tablaAfectada().isBlank()) {

            throw new IllegalArgumentException(
                    "La tabla afectada es obligatoria para registrar la auditoría."
            );
        }


        if (registro.tablaAfectada()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "La tabla afectada no puede exceder los 100 caracteres."
            );
        }


        if (registro.idRegistroAfectado() != null
                && registro.idRegistroAfectado()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "El identificador del registro afectado no puede exceder los 50 caracteres."
            );
        }


        if (registro.accion() == null) {

            throw new IllegalArgumentException(
                    "La acción de auditoría es obligatoria."
            );
        }


        if (registro.nombreUsuario() != null
                && registro.nombreUsuario()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre del usuario ejecutor no puede exceder los 100 caracteres."
            );
        }


        if (registro.direccionIp() != null
                && registro.direccionIp()
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
     * Impide que información sensible termine almacenada
     * accidentalmente en los campos JSONB de la bitácora.
     */
    private void validarSnapshotSeguro(
            Map<String, Object> snapshot
    ) {

        if (snapshot == null
                || snapshot.isEmpty()) {

            return;
        }


        for (String clave : snapshot.keySet()) {

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