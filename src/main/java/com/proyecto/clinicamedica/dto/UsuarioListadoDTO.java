package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * DTO: USUARIO PARA LISTADO
 * =========================================================
 *
 * Representa una fila de la tabla del CU-01.
 *
 * Este DTO contiene únicamente la información que la
 * interfaz necesita mostrar.
 *
 * NO expone:
 *
 * - Contraseña.
 * - Hash de contraseña.
 * - DPI cifrado.
 * - Hash de DPI.
 * - NIT cifrado.
 * - Hash de NIT.
 * - Datos de bloqueo.
 *
 * =========================================================
 */
public record UsuarioListadoDTO(

        /**
         * Identificador del usuario.
         */
        Integer id,


        /**
         * Nombre utilizado para iniciar sesión.
         */
        String nombreUsuario,


        /**
         * Nombre completo del usuario.
         */
        String nombreCompleto,


        /**
         * Correo electrónico registrado.
         */
        String correoElectronico,


        /**
         * Nombre del rol asignado.
         *
         * Ejemplo:
         *
         * Administrador
         * Médico
         * Enfermero
         */
        String rol,


        /**
         * NIT que podrá mostrarse en el listado.
         *
         * IMPORTANTE:
         *
         * Este valor se obtendrá mediante la capa de
         * servicio. Nunca se leerá directamente desde
         * nit_cifrado en el controlador.
         */
        String nit,


        /**
         * Estado lógico del usuario.
         *
         * true  = Activo
         * false = Inactivo
         */
        boolean activo,


        /**
         * Nombre de la sucursal asignada.
         *
         * Puede ser null para registros donde la
         * sucursal no esté asignada.
         */
        String sucursal

) {

    // =====================================================
    // ESTADO PARA LA INTERFAZ
    // =====================================================

    /**
     * Devuelve el estado preparado para mostrarlo
     * directamente en la tabla.
     *
     * @return Activo o Inactivo
     */
    public String estado() {

        return activo
                ? "Activo"
                : "Inactivo";
    }
}