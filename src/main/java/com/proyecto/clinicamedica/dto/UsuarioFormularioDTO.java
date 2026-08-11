package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * DTO: FORMULARIO DE USUARIO
 * =========================================================
 *
 * Representa los datos utilizados en:
 *
 * - FA01: Crear Usuario.
 * - FA04: Editar Usuario.
 *
 * Este DTO NO se persiste directamente.
 *
 * La capa de servicio será responsable de:
 *
 * - Validar las reglas del CU-01.
 * - Cifrar DPI y NIT.
 * - Generar hashes de búsqueda.
 * - Cifrar la contraseña con BCrypt.
 * - Resolver Rol, Sucursal y Especialidad.
 * - Registrar auditoría.
 *
 * =========================================================
 */
public class UsuarioFormularioDTO {

    // =====================================================
    // IDENTIFICADOR
    // =====================================================
    //
    // null durante creación.
    //
    // tendrá valor durante edición.
    // =====================================================

    private Integer id;


    // =====================================================
    // DATOS PERSONALES
    // =====================================================

    /**
     * RN-CU01-04
     *
     * Obligatorio.
     * Entre 10 y 100 caracteres.
     */
    private String nombreCompleto;


    /**
     * Correo electrónico del usuario.
     */
    private String correoElectronico;


    /**
     * RN-CU01-07
     *
     * Opcional.
     *
     * Si se ingresa:
     * exactamente 13 dígitos numéricos.
     *
     * Este DTO recibe el DPI en texto claro solamente
     * durante la petición.
     *
     * Nunca será almacenado directamente así.
     */
    private String dpi;


    /**
     * RN-CU01-08
     *
     * Opcional.
     *
     * Si se ingresa:
     * exactamente 8 dígitos.
     */
    private String telefono;


    /**
     * RN-CU01-11
     *
     * Opcional.
     *
     * Si se ingresa:
     * entre 8 y 9 caracteres alfanuméricos.
     */
    private String nit;


    /**
     * RN-CU01-12
     *
     * Opcional.
     *
     * Si se ingresa:
     * entre 5 y 50 caracteres.
     */
    private String numeroSeguro;


    // =====================================================
    // CREDENCIALES
    // =====================================================

    /**
     * RN-CU01-05
     *
     * Obligatorio.
     *
     * Entre 8 y 9 caracteres.
     * Únicamente alfanuméricos.
     * Único dentro del sistema.
     */
    private String nombreUsuario;


    /**
     * Contraseña.
     *
     * CREACIÓN:
     * obligatoria.
     *
     * EDICIÓN:
     * podrá permanecer vacía para conservar
     * la contraseña existente.
     *
     * RNF-015:
     * las contraseñas temporales deben tener
     * mínimo 12 caracteres y combinación.
     */
    private String contrasena;


    // =====================================================
    // CATÁLOGOS
    // =====================================================

    /**
     * RN-CU01-03 / RN-CU01-09
     *
     * Rol obligatorio.
     */
    private Integer idRol;


    /**
     * RN-CU01-06 / RN-CU01-13
     *
     * Obligatoria durante creación.
     */
    private Integer idSucursal;


    /**
     * RN-CU01-14
     *
     * Visible únicamente cuando el rol
     * seleccionado sea Médico.
     */
    private Integer idEspecialidad;


    // =====================================================
    // ESTADO
    // =====================================================

    /**
     * RN-CU01-10
     *
     * Activo / Inactivo.
     *
     * En creación inicia como Activo.
     */
    private Boolean activo = true;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioFormularioDTO() {
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    public void setId(
            Integer id
    ) {

        this.id = id;
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


    public String getCorreoElectronico() {

        return correoElectronico;
    }


    public void setCorreoElectronico(
            String correoElectronico
    ) {

        this.correoElectronico =
                correoElectronico;
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


    public String getTelefono() {

        return telefono;
    }


    public void setTelefono(
            String telefono
    ) {

        this.telefono =
                telefono;
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


    public String getNumeroSeguro() {

        return numeroSeguro;
    }


    public void setNumeroSeguro(
            String numeroSeguro
    ) {

        this.numeroSeguro =
                numeroSeguro;
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


    public Integer getIdRol() {

        return idRol;
    }


    public void setIdRol(
            Integer idRol
    ) {

        this.idRol =
                idRol;
    }


    public Integer getIdSucursal() {

        return idSucursal;
    }


    public void setIdSucursal(
            Integer idSucursal
    ) {

        this.idSucursal =
                idSucursal;
    }


    public Integer getIdEspecialidad() {

        return idEspecialidad;
    }


    public void setIdEspecialidad(
            Integer idEspecialidad
    ) {

        this.idEspecialidad =
                idEspecialidad;
    }


    public Boolean getActivo() {

        return activo;
    }


    public void setActivo(
            Boolean activo
    ) {

        this.activo =
                activo;
    }
}