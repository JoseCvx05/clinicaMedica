package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionDocumentoCita;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;


/**
 * =========================================================
 * SERVICIO: VALIDACIÓN DE DOCUMENTO DE CITA
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * El documento es opcional.
 *
 * Si el paciente adjunta uno debe:
 *
 * - No estar vacío.
 * - Tener extensión PDF.
 * - Pesar máximo 2 MB.
 * - Tener firma real %PDF-.
 * - Poder ser procesado por PDFBox.
 * - No estar protegido o encriptado.
 * - Tener al menos una página.
 * - Contener texto o imágenes.
 *
 * Un PDF completamente vacío será rechazado.
 *
 * =========================================================
 */
@Service
public class ValidacionDocumentoCitaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final long TAMANO_MAXIMO_BYTES =
            2L * 1024L * 1024L;


    private static final String MENSAJE_INVALIDO =
            "El documento debe ser un archivo PDF válido, "
                    + "no encriptado y con tamaño máximo de 2 MB.";


    // =====================================================
    // VALIDAR DOCUMENTO
    // =====================================================

    public ResultadoValidacionDocumentoCita validar(
            MultipartFile archivo
    ) {

        // =================================================
        // DOCUMENTO OPCIONAL
        // =================================================

        if (archivo == null) {

            return ResultadoValidacionDocumentoCita
                    .documentoValido();
        }


        /*
         * Si el usuario no seleccionó archivo,
         * Spring puede entregar un MultipartFile vacío
         * y sin nombre.
         */
        if (archivo.isEmpty()
                && (
                archivo.getOriginalFilename() == null
                        || archivo
                        .getOriginalFilename()
                        .isBlank()
        )) {

            return ResultadoValidacionDocumentoCita
                    .documentoValido();
        }


        // =================================================
        // ARCHIVO SELECCIONADO PERO VACÍO
        // =================================================

        if (archivo.isEmpty()) {

            return invalido();
        }


        // =================================================
        // TAMAÑO MÁXIMO - 2 MB
        // =================================================

        if (archivo.getSize()
                > TAMANO_MAXIMO_BYTES) {

            return invalido();
        }


        // =================================================
        // VALIDAR NOMBRE Y EXTENSIÓN
        // =================================================

        String nombre =
                archivo.getOriginalFilename();


        if (nombre == null
                || !nombre
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(".pdf")) {

            return invalido();
        }


        // =================================================
        // OBTENER CONTENIDO
        // =================================================

        final byte[] contenido;


        try {

            contenido =
                    archivo.getBytes();

        } catch (IOException ex) {

            return invalido();
        }


        // =================================================
        // VALIDAR FIRMA %PDF-
        // =================================================

        if (!tieneFirmaPdf(
                contenido
        )) {

            return invalido();
        }


        // =================================================
        // VALIDACIÓN REAL CON PDFBOX
        // =================================================

        try (
                PDDocument documento =
                        Loader.loadPDF(
                                contenido
                        )
        ) {

            // =============================================
            // DEBE TENER AL MENOS UNA PÁGINA
            // =============================================

            if (documento.getNumberOfPages() <= 0) {

                return invalido();
            }


            // =============================================
            // NO DEBE ESTAR ENCRIPTADO
            // =============================================

            if (documento.isEncrypted()) {

                return invalido();
            }


            // =============================================
            // DEBE TENER CONTENIDO
            // =============================================

            if (!tieneContenidoUtil(
                    documento
            )) {

                return invalido();
            }


            // =============================================
            // DOCUMENTO VÁLIDO
            // =============================================

            return ResultadoValidacionDocumentoCita
                    .documentoValido();


        } catch (InvalidPasswordException ex) {

            /*
             * PDF protegido con contraseña.
             */
            return invalido();


        } catch (IOException | RuntimeException ex) {

            /*
             * PDF corrupto, inválido o imposible
             * de procesar correctamente.
             */
            return invalido();
        }
    }


    // =====================================================
    // VALIDAR CONTENIDO DEL PDF
    // =====================================================

    /**
     * Se considera que el PDF contiene información útil
     * cuando tiene:
     *
     * - texto visible; o
     * - al menos una imagen.
     *
     * Esto permite aceptar documentos escaneados.
     */
    private boolean tieneContenidoUtil(
            PDDocument documento
    ) throws IOException {

        // =================================================
        // BUSCAR TEXTO
        // =================================================

        PDFTextStripper extractor =
                new PDFTextStripper();


        String texto =
                extractor.getText(
                        documento
                );


        if (texto != null
                && !texto.trim().isEmpty()) {

            return true;
        }


        // =================================================
        // BUSCAR IMÁGENES
        // =================================================

        for (PDPage pagina :
                documento.getPages()) {

            if (contieneImagen(
                    pagina.getResources()
            )) {

                return true;
            }
        }


        // =================================================
        // SIN TEXTO NI IMÁGENES
        // =================================================

        return false;
    }


    // =====================================================
    // BUSCAR IMÁGENES
    // =====================================================

    /**
     * Busca imágenes directamente en los recursos
     * de las páginas.
     *
     * También revisa Form XObject porque algunas imágenes
     * pueden encontrarse dentro de estos recursos internos.
     */
    private boolean contieneImagen(
            PDResources recursos
    ) throws IOException {

        if (recursos == null) {

            return false;
        }


        for (COSName nombre :
                recursos.getXObjectNames()) {

            PDXObject objeto =
                    recursos.getXObject(
                            nombre
                    );


            // =============================================
            // IMAGEN DIRECTA
            // =============================================

            if (objeto instanceof PDImageXObject) {

                return true;
            }


            // =============================================
            // FORMULARIO CON RECURSOS INTERNOS
            // =============================================

            if (objeto
                    instanceof PDFormXObject formulario) {

                if (contieneImagen(
                        formulario.getResources()
                )) {

                    return true;
                }
            }
        }


        return false;
    }


    // =====================================================
    // VALIDAR FIRMA %PDF-
    // =====================================================

    private boolean tieneFirmaPdf(
            byte[] contenido
    ) {

        if (contenido == null
                || contenido.length < 5) {

            return false;
        }


        return contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F'
                && contenido[4] == '-';
    }


    // =====================================================
    // RESULTADO INVÁLIDO
    // =====================================================

    private ResultadoValidacionDocumentoCita invalido() {

        return ResultadoValidacionDocumentoCita
                .documentoInvalido(
                        MENSAJE_INVALIDO
                );
    }
}