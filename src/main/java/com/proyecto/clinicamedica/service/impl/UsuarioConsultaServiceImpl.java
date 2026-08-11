package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.dto.TipoFiltroUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;

import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.service.CifradoService;
import com.proyecto.clinicamedica.service.HashService;
import com.proyecto.clinicamedica.service.UsuarioConsultaService;

import com.proyecto.clinicamedica.specification.UsuarioSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;


/**
 * =========================================================
 * IMPLEMENTACIÓN: CONSULTA DE USUARIOS
 * =========================================================
 *
 * Responsabilidad única:
 *
 * - Listar usuarios.
 * - Buscar usuarios.
 * - Aplicar filtros.
 * - Aplicar paginación.
 * - Aplicar ordenamiento.
 * - Generar HMAC para búsquedas DPI/NIT.
 * - Obtener usuario para edición.
 * - Descifrar únicamente los datos necesarios para
 *   las vistas administrativas autorizadas.
 *
 * No crea usuarios.
 * No actualiza usuarios.
 * No elimina usuarios.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class UsuarioConsultaServiceImpl
        implements UsuarioConsultaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int
            LONGITUD_MAXIMA_BUSQUEDA = 25;


    private static final Set<Integer>
            TAMANIOS_PERMITIDOS =
            Set.of(
                    10,
                    20,
                    25,
                    50
            );


    private static final Set<String>
            CAMPOS_ORDENAMIENTO_PERMITIDOS =
            Set.of(
                    "nombreUsuario",
                    "nombreCompleto"
            );


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository
            usuarioRepository;

    private final HashService
            hashService;

    private final CifradoService
            cifradoService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioConsultaServiceImpl(
            UsuarioRepository usuarioRepository,
            HashService hashService,
            CifradoService cifradoService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.hashService =
                hashService;

        this.cifradoService =
                cifradoService;
    }


    // =====================================================
    // LISTAR / BUSCAR USUARIOS
    // =====================================================

    @Override
    public Page<UsuarioListadoDTO> listarUsuarios(
            UsuarioBusquedaDTO busqueda
    ) {

        UsuarioBusquedaDTO criterios =
                busqueda == null
                        ? new UsuarioBusquedaDTO()
                        : busqueda;


        // =================================================
        // VALIDAR BÚSQUEDA
        // =================================================

        validarCriterioBusqueda(
                criterios
        );


        // =================================================
        // NORMALIZAR
        // =================================================

        normalizarBusqueda(
                criterios
        );


        // =================================================
        // PAGINACIÓN Y ORDENAMIENTO
        // =================================================

        Pageable pageable =
                construirPageable(
                        criterios
                );


        // =================================================
        // DPI / NIT PROTEGIDOS
        // =================================================

        String criterioProtegido =
                generarCriterioProtegido(
                        criterios
                );


        // =================================================
        // SPECIFICATION
        // =================================================

        Specification<Usuario> specification =
                UsuarioSpecification
                        .conFiltros(
                                criterios,
                                criterioProtegido
                        );


        // =================================================
        // CONSULTAR POSTGRESQL
        // =================================================

        Page<Usuario> paginaUsuarios =
                usuarioRepository
                        .findAll(
                                specification,
                                pageable
                        );


        // =================================================
        // ENTITY -> DTO
        // =================================================

        return paginaUsuarios.map(
                this::convertirAListadoDTO
        );
    }


    // =====================================================
    // FA04 - OBTENER USUARIO PARA EDITAR
    // =====================================================

    @Override
    public UsuarioFormularioDTO obtenerUsuarioParaEditar(
            Integer id
    ) {

        // =================================================
        // VALIDAR ID
        // =================================================

        if (id == null) {

            throw new IllegalArgumentException(
                    "El usuario seleccionado no es válido."
            );
        }


        // =================================================
        // BUSCAR USUARIO
        // =================================================

        Usuario usuario =
                usuarioRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El usuario seleccionado no existe."
                                        )
                        );


        // =================================================
        // CONSTRUIR DTO
        // =================================================

        UsuarioFormularioDTO formulario =
                new UsuarioFormularioDTO();


        formulario.setId(
                usuario.getId()
        );


        formulario.setNombreCompleto(
                usuario.getNombreCompleto()
        );


        formulario.setCorreoElectronico(
                usuario.getCorreoElectronico()
        );


        formulario.setNombreUsuario(
                usuario.getNombreUsuario()
        );


        // =================================================
        // CONTRASEÑA
        // =================================================
        //
        // Nunca enviamos el BCrypt al navegador.
        // =================================================

        formulario.setContrasena(
                null
        );


        // =================================================
        // DPI
        // =================================================

        formulario.setDpi(
                descifrarDatoOpcional(
                        usuario.getDpiCifrado()
                )
        );


        // =================================================
        // TELÉFONO
        // =================================================

        formulario.setTelefono(
                usuario.getTelefono()
        );


        // =================================================
        // NIT
        // =================================================

        formulario.setNit(
                descifrarDatoOpcional(
                        usuario.getNitCifrado()
                )
        );


        // =================================================
        // NÚMERO DE SEGURO
        // =================================================

        formulario.setNumeroSeguro(
                usuario.getNumeroSeguro()
        );


        // =================================================
        // ROL
        // =================================================

        formulario.setIdRol(
                usuario.getRol() != null
                        ? usuario.getRol().getId()
                        : null
        );


        // =================================================
        // SUCURSAL
        // =================================================

        formulario.setIdSucursal(
                usuario.getSucursal() != null
                        ? usuario.getSucursal().getId()
                        : null
        );


        // =================================================
        // ESPECIALIDAD
        // =================================================

        formulario.setIdEspecialidad(
                usuario.getEspecialidad() != null
                        ? usuario.getEspecialidad().getId()
                        : null
        );


        // =================================================
        // ESTADO
        // =================================================

        formulario.setActivo(
                Boolean.TRUE.equals(
                        usuario.getActivo()
                )
        );


        return formulario;
    }


    // =====================================================
    // VALIDAR CRITERIO
    // =====================================================

    private void validarCriterioBusqueda(
            UsuarioBusquedaDTO busqueda
    ) {

        String criterio =
                busqueda.getCriterio();


        if (criterio != null
                && criterio.length()
                > LONGITUD_MAXIMA_BUSQUEDA) {

            throw new IllegalArgumentException(
                    "El campo de búsqueda no puede exceder "
                            + "los 25 caracteres."
            );
        }
    }


    // =====================================================
    // NORMALIZAR BÚSQUEDA
    // =====================================================

    private void normalizarBusqueda(
            UsuarioBusquedaDTO busqueda
    ) {

        if (busqueda.getCriterio() != null) {

            String criterio =
                    busqueda
                            .getCriterio()
                            .trim();


            busqueda.setCriterio(
                    criterio
            );
        }


        if (busqueda.getCriterio() == null
                || busqueda
                .getCriterio()
                .isBlank()) {

            busqueda.setCriterio(
                    null
            );

            return;
        }


        /*
         * Si escribieron criterio pero no seleccionaron
         * tipo de búsqueda, utilizamos Usuario.
         */
        if (busqueda.getTipoFiltro() == null) {

            busqueda.setTipoFiltro(
                    TipoFiltroUsuario.USUARIO
            );
        }
    }


    // =====================================================
    // CONSTRUIR PAGEABLE
    // =====================================================

    private Pageable construirPageable(
            UsuarioBusquedaDTO busqueda
    ) {

        int pagina =
                Math.max(
                        busqueda.getPagina(),
                        0
                );


        int tamanio =
                TAMANIOS_PERMITIDOS.contains(
                        busqueda.getTamanio()
                )
                        ? busqueda.getTamanio()
                        : 20;


        String ordenarPor =
                busqueda.getOrdenarPor();


        if (ordenarPor == null
                || !CAMPOS_ORDENAMIENTO_PERMITIDOS
                .contains(
                        ordenarPor
                )) {

            ordenarPor =
                    "nombreUsuario";
        }


        Sort.Direction direccion =
                obtenerDireccion(
                        busqueda.getDireccion()
                );


        return PageRequest.of(
                pagina,
                tamanio,
                Sort.by(
                        direccion,
                        ordenarPor
                )
        );
    }


    // =====================================================
    // DIRECCIÓN DEL ORDENAMIENTO
    // =====================================================

    private Sort.Direction obtenerDireccion(
            String direccion
    ) {

        if (direccion == null) {

            return Sort.Direction.ASC;
        }


        return "DESC".equalsIgnoreCase(
                direccion.trim()
        )
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }


    // =====================================================
    // GENERAR CRITERIO PROTEGIDO
    // =====================================================

    private String generarCriterioProtegido(
            UsuarioBusquedaDTO busqueda
    ) {

        if (busqueda.getTipoFiltro() == null
                || busqueda.getCriterio() == null
                || busqueda
                .getCriterio()
                .isBlank()) {

            return null;
        }


        // =================================================
        // DPI
        // =================================================

        if (busqueda.getTipoFiltro()
                == TipoFiltroUsuario.DPI) {

            return hashService.generarHash(
                    busqueda
                            .getCriterio()
                            .trim()
            );
        }


        // =================================================
        // NIT
        // =================================================

        if (busqueda.getTipoFiltro()
                == TipoFiltroUsuario.NIT) {

            return hashService.generarHash(
                    busqueda
                            .getCriterio()
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );
        }


        return null;
    }


    // =====================================================
    // ENTITY -> DTO DE LISTADO
    // =====================================================

    private UsuarioListadoDTO convertirAListadoDTO(
            Usuario usuario
    ) {

        String nombreRol =
                usuario.getRol() == null
                        ? null
                        : usuario
                        .getRol()
                        .getNombre();


        String nombreSucursal =
                usuario.getSucursal() == null
                        ? null
                        : usuario
                        .getSucursal()
                        .getNombre();


        String nit =
                descifrarDatoOpcional(
                        usuario.getNitCifrado()
                );


        return new UsuarioListadoDTO(
                usuario.getId(),

                usuario.getNombreUsuario(),

                usuario.getNombreCompleto(),

                usuario.getCorreoElectronico(),

                nombreRol,

                nit,

                Boolean.TRUE.equals(
                        usuario.getActivo()
                ),

                nombreSucursal
        );
    }


    // =====================================================
    // DESCIFRAR DATO OPCIONAL
    // =====================================================

    private String descifrarDatoOpcional(
            String valorCifrado
    ) {

        if (valorCifrado == null
                || valorCifrado.isBlank()) {

            return null;
        }


        return cifradoService.descifrar(
                valorCifrado
        );
    }
}