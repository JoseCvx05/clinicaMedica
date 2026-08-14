package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.ResultadoAnalisisAntivirus;
import com.proyecto.clinicamedica.model.cita.EstadoAnalisisAntivirus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.charset.StandardCharsets;


/**
 * =========================================================
 * SERVICIO: ANTIVIRUS DE DOCUMENTOS
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Analiza los documentos mediante ClamAV antes
 * de permitir su almacenamiento.
 *
 * Utiliza el protocolo INSTREAM de clamd.
 *
 * El documento todavía NO se almacena físicamente
 * cuando se realiza este análisis.
 *
 * Estados posibles:
 *
 * - LIMPIO
 * - INFECTADO
 * - ERROR
 *
 * Se utiliza estrategia fail-closed:
 *
 * si el antivirus no puede verificar el archivo,
 * el documento NO se considera limpio.
 *
 * =========================================================
 */
@Service
public class AntivirusDocumentoService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int TAMANO_CHUNK =
            16 * 1024;


    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private final String host;

    private final int puerto;

    private final int timeoutConexionMs;

    private final int timeoutLecturaMs;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AntivirusDocumentoService(

            @Value("${antivirus.clamav.host:127.0.0.1}")
            String host,

            @Value("${antivirus.clamav.port:3310}")
            int puerto,

            @Value("${antivirus.clamav.timeout-conexion-ms:3000}")
            int timeoutConexionMs,

            @Value("${antivirus.clamav.timeout-lectura-ms:10000}")
            int timeoutLecturaMs
    ) {

        this.host =
                host;

        this.puerto =
                puerto;

        this.timeoutConexionMs =
                timeoutConexionMs;

        this.timeoutLecturaMs =
                timeoutLecturaMs;
    }


    // =====================================================
    // ANALIZAR DOCUMENTO
    // =====================================================

    public ResultadoAnalisisAntivirus analizar(
            MultipartFile archivo
    ) {

        // =================================================
        // DOCUMENTO OPCIONAL
        // =================================================

        if (archivo == null
                || archivo.isEmpty()) {

            return new ResultadoAnalisisAntivirus(
                    EstadoAnalisisAntivirus.LIMPIO,
                    "No se adjuntó documento."
            );
        }


        try (
                Socket socket =
                        new Socket()
        ) {

            // =================================================
            // CONECTAR CON CLAMD
            // =================================================

            socket.connect(
                    new InetSocketAddress(
                            host,
                            puerto
                    ),
                    timeoutConexionMs
            );


            socket.setSoTimeout(
                    timeoutLecturaMs
            );


            try (
                    DataOutputStream salida =
                            new DataOutputStream(
                                    socket.getOutputStream()
                            );

                    InputStream entradaArchivo =
                            archivo.getInputStream()
            ) {

                // =============================================
                // COMANDO INSTREAM
                // =============================================
                //
                // Se utiliza framing "z".
                //
                // El comando termina con byte NUL.
                // =============================================

                salida.write(
                        "zINSTREAM\0"
                                .getBytes(
                                        StandardCharsets.US_ASCII
                                )
                );


                // =============================================
                // ENVIAR ARCHIVO EN CHUNKS
                // =============================================

                byte[] buffer =
                        new byte[TAMANO_CHUNK];


                int leidos;


                while (
                        (leidos =
                                entradaArchivo.read(
                                        buffer
                                ))
                                != -1
                ) {

                    /*
                     * INSTREAM requiere enviar antes de cada
                     * bloque un entero de 4 bytes indicando
                     * la cantidad de bytes del bloque.
                     *
                     * DataOutputStream.writeInt() utiliza
                     * big-endian, como requiere el protocolo.
                     */
                    salida.writeInt(
                            leidos
                    );


                    salida.write(
                            buffer,
                            0,
                            leidos
                    );
                }


                // =============================================
                // CHUNK FINAL
                // =============================================
                //
                // Un entero 0 indica que terminó el archivo.
                // =============================================

                salida.writeInt(
                        0
                );


                salida.flush();


                // =============================================
                // LEER RESPUESTA DE CLAMD
                // =============================================

                String respuesta =
                        leerRespuesta(
                                socket.getInputStream()
                        );


                return interpretarRespuesta(
                        respuesta
                );
            }


        } catch (IOException ex) {

            /*
             * FAIL-CLOSED
             *
             * Si ClamAV no responde o no puede analizar
             * el archivo, nunca lo consideramos limpio.
             */
            return new ResultadoAnalisisAntivirus(
                    EstadoAnalisisAntivirus.ERROR,

                    "No fue posible comunicarse con "
                            + "el servicio antivirus."
            );
        }
    }


    // =====================================================
    // LEER RESPUESTA DE CLAMD
    // =====================================================

    private String leerRespuesta(
            InputStream entrada
    ) throws IOException {

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();


        int valor;


        while (
                (valor = entrada.read())
                        != -1
        ) {

            /*
             * Como utilizamos el framing "z",
             * la respuesta termina en byte NUL.
             */
            if (valor == 0) {

                break;
            }


            buffer.write(
                    valor
            );
        }


        return buffer.toString(
                StandardCharsets.UTF_8
        );
    }


    // =====================================================
    // INTERPRETAR RESPUESTA
    // =====================================================

    private ResultadoAnalisisAntivirus interpretarRespuesta(
            String respuesta
    ) {

        // =================================================
        // RESPUESTA VACÍA
        // =================================================

        if (respuesta == null
                || respuesta.isBlank()) {

            return error(
                    "El antivirus devolvió una respuesta vacía."
            );
        }


        String normalizada =
                respuesta.trim();


        // =================================================
        // DOCUMENTO LIMPIO
        // =================================================

        if (normalizada.endsWith(
                ": OK"
        )) {

            return new ResultadoAnalisisAntivirus(
                    EstadoAnalisisAntivirus.LIMPIO,
                    normalizada
            );
        }


        // =================================================
        // AMENAZA DETECTADA
        // =================================================

        if (normalizada.contains(
                " FOUND"
        )) {

            return new ResultadoAnalisisAntivirus(
                    EstadoAnalisisAntivirus.INFECTADO,
                    normalizada
            );
        }


        // =================================================
        // ERROR DEL MOTOR ANTIVIRUS
        // =================================================

        return error(
                normalizada
        );
    }


    // =====================================================
    // RESULTADO DE ERROR
    // =====================================================

    private ResultadoAnalisisAntivirus error(
            String detalle
    ) {

        return new ResultadoAnalisisAntivirus(
                EstadoAnalisisAntivirus.ERROR,
                detalle
        );
    }
}