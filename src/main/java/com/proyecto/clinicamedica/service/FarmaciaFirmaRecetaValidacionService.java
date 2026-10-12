
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
import com.proyecto.clinicamedica.entity.RecetaMedica;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * SERVICE: VALIDACION DE FIRMA DIGITAL EN FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * RNF-031 - Integridad de documentos clinicos.
 *
 * Verifica:
 *
 * - Que la receta tenga una firma registrada.
 * - Que los detalles pertenezcan a la receta.
 * - Que el contenido coincida con su hash SHA-256.
 * - Que la firma del documento sea valida.
 *
 * IMPORTANTE:
 *
 * No modifica recetas.
 * No crea firmas nuevas.
 * No modifica inventario.
 *
 * =========================================================
 */
@Service
public class FarmaciaFirmaRecetaValidacionService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final FirmaDigitalMedicaService
            firmaDigitalMedicaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaFirmaRecetaValidacionService(
            FirmaDigitalMedicaService firmaDigitalMedicaService
    ) {

        this.firmaDigitalMedicaService =
                firmaDigitalMedicaService;
    }


    // =====================================================
    // VALIDAR FIRMA DE RECETA
    // =====================================================

    public void validar(
            RecetaMedica receta,
            List<DetalleRecetaMedica> detalles
    ) {

        // -------------------------------------------------
        // RECETA VALIDA
        // -------------------------------------------------

        if (receta == null || receta.getId() == null) {

            throw new IllegalStateException(
                    "La receta médica no es válida."
            );
        }


        // -------------------------------------------------
        // FIRMA OBLIGATORIA
        // -------------------------------------------------

        String hashRegistrado =
                receta.getFirmaDigitalHash();

        if (hashRegistrado == null
                || !hashRegistrado.matches("[0-9a-fA-F]{64}")) {

            throw new IllegalStateException(
                    "La receta médica no posee una firma digital válida."
            );
        }


        // -------------------------------------------------
        // DETALLES OBLIGATORIOS
        // -------------------------------------------------

        if (detalles == null || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La receta no contiene medicamentos para verificar."
            );
        }


        // -------------------------------------------------
        // COMPROBAR QUE TODOS PERTENECEN A LA RECETA
        // -------------------------------------------------

        for (DetalleRecetaMedica detalle : detalles) {

            if (detalle == null
                    || detalle.getId() == null
                    || detalle.getRecetaMedica() == null
                    || !Objects.equals(
                    receta.getId(),
                    detalle.getRecetaMedica().getId()
            )) {

                throw new IllegalStateException(
                        "Los medicamentos no corresponden "
                                + "a la receta médica firmada."
                );
            }
        }


        // -------------------------------------------------
        // RECONSTRUIR EL CONTENIDO FIRMADO
        // -------------------------------------------------

        String contenidoCanonico =
                construirContenidoCanonico(
                        receta,
                        detalles
                );


        // -------------------------------------------------
        // COMPROBAR EL HASH GUARDADO EN LA RECETA
        // -------------------------------------------------

        String hashCalculado =
                calcularSha256(contenidoCanonico);

        if (!hashCalculado.equalsIgnoreCase(hashRegistrado)) {

            throw new IllegalStateException(
                    "El contenido de la receta médica "
                            + "no coincide con su firma digital."
            );
        }


        // -------------------------------------------------
        // VERIFICAR CON EL SERVICIO DEL CU-08
        // -------------------------------------------------

        boolean firmaValida =
                firmaDigitalMedicaService.verificarDocumento(

                        FirmaDocumentoClinico
                                .TipoDocumentoClinico
                                .RECETA_MEDICA,

                        receta.getId().longValue(),

                        contenidoCanonico
                );


        if (!firmaValida) {

            throw new IllegalStateException(
                    "La firma digital de la receta médica "
                            + "no pudo ser verificada."
            );
        }
    }


    // =====================================================
    // RECONSTRUIR CONTENIDO CANONICO
    // =====================================================
    //
    // Utilizamos el mismo formato de contenido
    // que se generó al crear la receta en CU-08.
    //
    // IMPORTANTE:
    //
    // Si posteriormente cambia el formato de firma
    // del CU-08, deberá actualizarse conjuntamente
    // esta reconstrucción.
    //
    // =====================================================

    private String construirContenidoCanonico(
            RecetaMedica receta,
            List<DetalleRecetaMedica> detalles
    ) {

        List<DetalleRecetaMedica> ordenados =
                detalles.stream()
                        .sorted(
                                Comparator.comparing(
                                        DetalleRecetaMedica::getId
                                )
                        )
                        .toList();


        StringBuilder contenido =
                new StringBuilder();


        contenido.append("TIPO=RECETA_MEDICA\n")
                .append("ID_RECETA=")
                .append(receta.getId())
                .append('\n')

                .append("ID_CITA=")
                .append(receta.getCita().getId())
                .append('\n')

                .append("ID_CONSULTA=")
                .append(receta.getConsultaMedica().getId())
                .append('\n')

                .append("ID_MEDICO=")
                .append(receta.getMedico().getId())
                .append('\n');


        if (receta.getFechaEmision() != null) {

            contenido.append("FECHA_EMISION_EPOCH_MS=")
                    .append(
                            receta.getFechaEmision()
                                    .toInstant()
                                    .toEpochMilli()
                    )
                    .append('\n');
        }


        contenido.append("CANTIDAD_MEDICAMENTOS=")
                .append(ordenados.size());


        int numero = 1;


        for (DetalleRecetaMedica detalle : ordenados) {

            String prefijo =
                    "\nITEM_" + numero + "_";


            contenido.append(prefijo)
                    .append("ID_DETALLE=")
                    .append(detalle.getId())

                    .append(prefijo)
                    .append("ID_MEDICAMENTO=")
                    .append(detalle.getMedicamento().getId())

                    .append(prefijo)
                    .append("NOMBRE=")
                    .append(valorCanonico(
                            detalle.getNombreMedicamento()
                    ))

                    .append(prefijo)
                    .append("CANTIDAD=")
                    .append(detalle.getCantidad())

                    .append(prefijo)
                    .append("DOSIS=")
                    .append(valorCanonico(
                            detalle.getDosis()
                    ))

                    .append(prefijo)
                    .append("FRECUENCIA=")
                    .append(valorCanonico(
                            detalle.getFrecuencia()
                    ))

                    .append(prefijo)
                    .append("DURACION=")
                    .append(valorCanonico(
                            detalle.getDuracion()
                    ))

                    .append(prefijo)
                    .append("INDICACIONES=")
                    .append(valorCanonico(
                            detalle.getIndicacionesEspeciales()
                    ))

                    .append(prefijo)
                    .append("PRECIO_UNITARIO=")
                    .append(valorPrecio(
                            detalle.getPrecioUnitario()
                    ))

                    .append(prefijo)
                    .append("UNIDAD=")
                    .append(valorCanonico(
                            detalle.getUnidad()
                    ));


            numero++;
        }


        return contenido.toString();
    }


    // =====================================================
    // NORMALIZAR TEXTOS
    // =====================================================

    private String valorCanonico(String valor) {

        if (valor == null) {
            return "";
        }

        return valor.trim()
                .replace("\\", "\\\\")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n")
                .replace("=", "\\=");
    }


    // =====================================================
    // NORMALIZAR PRECIOS
    // =====================================================

    private String valorPrecio(BigDecimal valor) {

        if (valor == null) {
            return "";
        }

        return valor.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString();
    }


    // =====================================================
    // CALCULAR SHA-256
    // =====================================================

    private String calcularSha256(String contenido) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] resultado =
                    digest.digest(
                            contenido.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(resultado);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "No fue posible verificar "
                            + "el hash SHA-256 de la receta.",
                    ex
            );
        }
    }
}
