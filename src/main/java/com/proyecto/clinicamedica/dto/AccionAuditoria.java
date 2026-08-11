package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ACCIONES DE AUDITORÍA
 * =========================================================
 *
 * Representa las acciones permitidas actualmente por
 * la tabla bitacora_auditoria.
 *
 * Evita utilizar cadenas de texto dispersas como:
 *
 * "Crear"
 * "Actualizar"
 * "Eliminar"
 *
 * De esta forma reducimos errores de escritura y
 * centralizamos los valores aceptados por PostgreSQL.
 * =========================================================
 */
public enum AccionAuditoria {

    CREAR("Crear"),

    ACTUALIZAR("Actualizar"),

    ELIMINAR("Eliminar"),

    INICIO_SESION("Inicio Sesión"),

    CIERRE_SESION("Cierre Sesión"),

    REASIGNACION("Reasignación"),

    OTRO("Otro");


    /**
     * Valor exacto almacenado en PostgreSQL.
     */
    private final String valorBaseDatos;


    AccionAuditoria(
            String valorBaseDatos
    ) {

        this.valorBaseDatos =
                valorBaseDatos;
    }


    /**
     * Devuelve el texto exacto esperado por la
     * restricción CHECK de bitacora_auditoria.
     */
    public String getValorBaseDatos() {

        return valorBaseDatos;
    }
}