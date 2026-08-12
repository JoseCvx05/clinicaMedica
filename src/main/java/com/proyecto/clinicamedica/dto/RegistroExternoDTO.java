package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * DTO: REGISTRO DE USUARIO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Contiene exclusivamente los datos que el paciente
 * puede ingresar desde el formulario público.
 *
 * No contiene:
 *
 * - rol
 * - sucursal
 * - especialidad
 * - estado
 * - hashes
 * - datos cifrados
 *
 * Esos valores son responsabilidad del Service.
 * =========================================================
 */
public class RegistroExternoDTO {

    private String nombreCompleto;

    private String dpi;

    private String nit;

    private String telefono;

    private String numeroSeguro;

    private String correoElectronico;

    private String nombreUsuario;

    private String contrasena;


    public RegistroExternoDTO() {
    }


    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(
            String nombreCompleto
    ) {
        this.nombreCompleto =
                nombreCompleto;
    }


    public String getDpi() {
        return dpi;
    }

    public void setDpi(
            String dpi
    ) {
        this.dpi =
                dpi;
    }


    public String getNit() {
        return nit;
    }

    public void setNit(
            String nit
    ) {
        this.nit =
                nit;
    }


    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(
            String telefono
    ) {
        this.telefono =
                telefono;
    }


    public String getNumeroSeguro() {
        return numeroSeguro;
    }

    public void setNumeroSeguro(
            String numeroSeguro
    ) {
        this.numeroSeguro =
                numeroSeguro;
    }


    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(
            String correoElectronico
    ) {
        this.correoElectronico =
                correoElectronico;
    }


    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(
            String nombreUsuario
    ) {
        this.nombreUsuario =
                nombreUsuario;
    }


    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(
            String contrasena
    ) {
        this.contrasena =
                contrasena;
    }
}