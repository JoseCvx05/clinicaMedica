package com.proyecto.clinicamedica.dto;

/**
 * =========================================================
 * DTO: BÚSQUEDA DE USUARIOS
 * =========================================================
 *
 * Representa los criterios utilizados en el listado
 * de usuarios del CU-01.
 *
 * Permite manejar:
 *
 * - Tipo de filtro.
 * - Texto de búsqueda.
 * - Rol.
 * - Sucursal.
 * - Estado.
 * - Página actual.
 * - Cantidad de registros por página.
 * - Ordenamiento.
 *
 * Este DTO NO se persiste en la base de datos.
 * =========================================================
 */
public class UsuarioBusquedaDTO {

    // =====================================================
    // FILTRO PRINCIPAL
    // =====================================================

    /**
     * Criterio seleccionado por el administrador.
     *
     * Valores permitidos:
     *
     * ID
     * NOMBRE
     * CORREO
     * ROL
     * USUARIO
     * DPI
     * NIT
     * SUCURSAL
     */
    private TipoFiltroUsuario tipoFiltro;

    /**
     * Texto ingresado en el campo:
     *
     * Buscar...
     *
     * RN-CU01-01:
     * máximo 25 caracteres.
     */
    private String criterio;


    // =====================================================
    // FILTROS COMPLEMENTARIOS
    // =====================================================

    private Integer idRol;

    private Integer idSucursal;

    private Boolean activo;


    // =====================================================
    // PAGINACIÓN
    // =====================================================

    /**
     * Spring Data utiliza páginas iniciando desde 0.
     *
     * Página visual 1
     *      ↓
     * pagina = 0
     */
    private int pagina = 0;

    /**
     * RN-CU01-02 define 20 registros por página.
     *
     * También permitiremos:
     *
     * 10, 20, 25 y 50.
     */
    private int tamanio = 20;


    // =====================================================
    // ORDENAMIENTO
    // =====================================================

    /**
     * Campo de ordenamiento predeterminado.
     */
    private String ordenarPor =
            "nombreUsuario";

    /**
     * ASC o DESC.
     */
    private String direccion =
            "ASC";


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioBusquedaDTO() {
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public TipoFiltroUsuario getTipoFiltro() {

        return tipoFiltro;
    }


    public void setTipoFiltro(
            TipoFiltroUsuario tipoFiltro
    ) {

        this.tipoFiltro =
                tipoFiltro;
    }


    public String getCriterio() {

        return criterio;
    }


    public void setCriterio(
            String criterio
    ) {

        this.criterio =
                criterio;
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


    public Boolean getActivo() {

        return activo;
    }


    public void setActivo(
            Boolean activo
    ) {

        this.activo =
                activo;
    }


    public int getPagina() {

        return pagina;
    }


    public void setPagina(
            int pagina
    ) {

        this.pagina =
                pagina;
    }


    public int getTamanio() {

        return tamanio;
    }


    public void setTamanio(
            int tamanio
    ) {

        this.tamanio =
                tamanio;
    }


    public String getOrdenarPor() {

        return ordenarPor;
    }


    public void setOrdenarPor(
            String ordenarPor
    ) {

        this.ordenarPor =
                ordenarPor;
    }


    public String getDireccion() {

        return direccion;
    }


    public void setDireccion(
            String direccion
    ) {

        this.direccion =
                direccion;
    }
}