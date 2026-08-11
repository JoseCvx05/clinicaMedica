package com.proyecto.clinicamedica.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================
 * REGISTRO DE AUDITORÍA
 * =========================================================
 *
 * DTO interno utilizado para transportar la información
 * necesaria para crear un registro en:
 *
 * bitacora_auditoria
 *
 * Este objeto es inmutable.
 *
 * Puede utilizarse para CU-01 y posteriormente para
 * otros módulos del sistema.
 *
 * IMPORTANTE:
 *
 * Los mapas de valores anteriores/nuevos NUNCA deben
 * contener información sensible como:
 *
 * - contrasenaHash
 * - dpiCifrado
 * - dpiHash
 * - nitCifrado
 * - nitHash
 * - JWT
 * =========================================================
 */
public record RegistroAuditoria(

        /**
         * Tabla sobre la cual se realizó la operación.
         *
         * Ejemplo:
         * usuario
         */
        String tablaAfectada,


        /**
         * Identificador del registro afectado.
         *
         * Se almacena como String porque la bitácora
         * es genérica para diferentes tablas.
         */
        String idRegistroAfectado,


        /**
         * Acción realizada.
         *
         * Ejemplos:
         *
         * CREAR
         * ACTUALIZAR
         * ELIMINAR
         */
        AccionAuditoria accion,


        /**
         * ID del usuario que ejecutó la operación.
         */
        Integer idUsuario,


        /**
         * Snapshot del nombre del usuario ejecutor.
         */
        String nombreUsuario,


        /**
         * Valores existentes antes de la operación.
         *
         * En creación normalmente será null.
         */
        Map<String, Object> valoresAnteriores,


        /**
         * Valores existentes después de la operación.
         *
         * En eliminación lógica contendrá el nuevo estado.
         */
        Map<String, Object> valoresNuevos,


        /**
         * Dirección IP desde donde se realizó
         * la operación.
         */
        String direccionIp

) {

    /**
     * Constructor compacto.
     *
     * Creamos copias no modificables de los mapas
     * recibidos para preservar la inmutabilidad real
     * del DTO.
     */
    public RegistroAuditoria {

        valoresAnteriores =
                copiarMapa(
                        valoresAnteriores
                );

        valoresNuevos =
                copiarMapa(
                        valoresNuevos
                );
    }


    /**
     * Crea una copia inmutable del mapa.
     *
     * Utilizamos LinkedHashMap en lugar de Map.copyOf()
     * porque los snapshots pueden contener campos
     * opcionales cuyo valor sea null.
     */
    private static Map<String, Object> copiarMapa(
            Map<String, Object> mapa
    ) {

        if (mapa == null) {
            return null;
        }


        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        mapa
                )
        );
    }
}