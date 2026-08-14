package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.CitaDocumentoAdjunto;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaDocumentoAdjuntoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.UUID;


/**
 * =========================================================
 * SERVICIO: DOCUMENTO DE CITA
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Responsabilidades:
 *
 * - Almacenar físicamente el PDF validado.
 * - Generar un nombre físico seguro.
 * - Evitar path traversal.
 * - Registrar metadata del documento.
 * - Asociarlo con la cita y usuario.
 * - Compensar eliminando el archivo físico cuando
 *   ocurre un error durante el almacenamiento.
 *
 * Este servicio supone que el documento YA pasó:
 *
 * - validación PDF;
 * - validación de tamaño;
 * - validación de cifrado;
 * - análisis antivirus.
 *
 * =========================================================
 */
@Service
public class DocumentoCitaService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaDocumentoAdjuntoRepository
            documentoRepository;

    private final Path directorioBase;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DocumentoCitaService(
            CitaDocumentoAdjuntoRepository documentoRepository,

            @Value("${cita.documentos.directorio:uploads/citas}")
            String directorio
    ) {

        this.documentoRepository =
                documentoRepository;

        this.directorioBase =
                Path.of(
                                directorio
                        )
                        .toAbsolutePath()
                        .normalize();
    }


    // =====================================================
    // GUARDAR DOCUMENTO
    // =====================================================

    @Transactional
    public CitaDocumentoAdjunto guardar(
            Cita cita,
            Usuario usuario,
            MultipartFile archivo
    ) {

        // =================================================
        // VALIDAR CITA
        // =================================================

        if (cita == null
                || cita.getId() == null) {

            throw new IllegalArgumentException(
                    "La cita debe existir antes de guardar el documento."
            );
        }


        // =================================================
        // VALIDAR USUARIO
        // =================================================

        if (usuario == null) {

            throw new IllegalArgumentException(
                    "El usuario que carga el documento es obligatorio."
            );
        }


        // =================================================
        // VALIDAR ARCHIVO
        // =================================================

        if (archivo == null
                || archivo.isEmpty()) {

            throw new IllegalArgumentException(
                    "El documento no contiene información."
            );
        }


        Path rutaGuardada =
                null;


        try {

            // =================================================
            // CREAR DIRECTORIO
            // =================================================

            Files.createDirectories(
                    directorioBase
            );


            // =================================================
            // GENERAR NOMBRE FÍSICO SEGURO
            // =================================================

            String nombreFisico =
                    UUID.randomUUID()
                            + ".pdf";


            Path rutaDestino =
                    directorioBase
                            .resolve(
                                    nombreFisico
                            )
                            .normalize();


            // =================================================
            // PROTECCIÓN CONTRA PATH TRAVERSAL
            // =================================================

            if (!rutaDestino.startsWith(
                    directorioBase
            )) {

                throw new IllegalStateException(
                        "La ruta de almacenamiento no es válida."
                );
            }


            // =================================================
            // GUARDAR ARCHIVO FÍSICO
            // =================================================

            Files.copy(
                    archivo.getInputStream(),
                    rutaDestino,
                    StandardCopyOption.REPLACE_EXISTING
            );


            rutaGuardada =
                    rutaDestino;


            // =================================================
            // CREAR METADATA
            // =================================================

            CitaDocumentoAdjunto documento =
                    new CitaDocumentoAdjunto();


            documento.setCita(
                    cita
            );


            /*
             * El nombre original solamente se conserva
             * como metadata.
             *
             * Nunca se utiliza como ruta física.
             */
            documento.setNombreArchivo(
                    limpiarNombreOriginal(
                            archivo.getOriginalFilename()
                    )
            );


            documento.setRutaArchivo(
                    rutaDestino.toString()
            );


            documento.setTipoMime(
                    "application/pdf"
            );


            documento.setTamanoBytes(
                    Math.toIntExact(
                            archivo.getSize()
                    )
            );


            documento.setEstadoValidacion(
                    "VALIDADO"
            );


            documento.setCreadoPor(
                    usuario
            );


            // =================================================
            // GUARDAR METADATA
            // =================================================

            return documentoRepository
                    .saveAndFlush(
                            documento
                    );


        } catch (IOException ex) {

            // =================================================
            // COMPENSACIÓN
            // =================================================

            eliminarSiExiste(
                    rutaGuardada
            );


            throw new IllegalStateException(
                    "No fue posible almacenar el documento de la cita.",
                    ex
            );


        } catch (RuntimeException ex) {

            /*
             * Si PostgreSQL rechaza la metadata después
             * de guardar el archivo físico, eliminamos
             * dicho archivo para evitar dejarlo huérfano.
             */

            eliminarSiExiste(
                    rutaGuardada
            );


            throw ex;
        }
    }


    // =====================================================
    // LIMPIAR NOMBRE ORIGINAL
    // =====================================================

    private String limpiarNombreOriginal(
            String nombre
    ) {

        if (nombre == null
                || nombre.isBlank()) {

            return "documento.pdf";
        }


        /*
         * Extraemos únicamente el nombre final.
         *
         * Ejemplo:
         *
         * C:/usuario/archivo.pdf
         *
         * termina como:
         *
         * archivo.pdf
         */
        String soloNombre =
                Path.of(
                                nombre
                        )
                        .getFileName()
                        .toString();

        // =================================================
        // LONGITUD MÁXIMA
        // =================================================

        if (soloNombre.length() > 255) {

            soloNombre =
                    soloNombre.substring(
                            0,
                            255
                    );
        }


        return soloNombre;
    }


    // =====================================================
    // COMPENSACIÓN DEL ARCHIVO FÍSICO
    // =====================================================

    private void eliminarSiExiste(
            Path ruta
    ) {

        if (ruta == null) {

            return;
        }


        try {

            Files.deleteIfExists(
                    ruta
            );

        } catch (IOException ignored) {

            /*
             * No reemplazamos el error original
             * del proceso de almacenamiento.
             */
        }
    }
}