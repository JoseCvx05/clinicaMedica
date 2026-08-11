package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * ENUM: TIPO DE FILTRO DE USUARIO
 * =========================================================
 *
 * Representa los criterios permitidos para buscar
 * usuarios dentro del CU-01.
 *
 * Evita utilizar Strings libres dentro de la lógica
 * de negocio y del repositorio.
 * =========================================================
 */
public enum TipoFiltroUsuario {

    ID("ID"),

    NOMBRE("Nombre"),

    CORREO("Correo"),

    ROL("Rol"),

    USUARIO("Usuario"),

    DPI("DPI"),

    NIT("NIT"),

    SUCURSAL("Sucursal");


    // =====================================================
    // ETIQUETA PARA MOSTRAR EN LA INTERFAZ
    // =====================================================

    private final String etiqueta;


    TipoFiltroUsuario(
            String etiqueta
    ) {

        this.etiqueta =
                etiqueta;
    }


    public String getEtiqueta() {

        return etiqueta;
    }
}