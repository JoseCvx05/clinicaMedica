
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * =========================================================
 * SERVICE: SEGURIDAD DE FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Centraliza las validaciones de seguridad
 * que deben realizarse en el backend.
 *
 * Valida:
 *
 * - Usuario autenticado.
 * - Usuario activo.
 * - Rol Farmaceutico.
 * - Rol activo.
 * - Sucursal asignada y activa.
 * - Acceso a recetas de la misma sucursal.
 *
 * IMPORTANTE:
 *
 * No se confia en el ID de sucursal enviado
 * desde HTML, JavaScript o una URL.
 *
 * La sucursal autorizada siempre se obtiene
 * del usuario autenticado.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class FarmaciaSeguridadService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioActualService usuarioActualService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaSeguridadService(
            UsuarioActualService usuarioActualService
    ) {

        this.usuarioActualService = usuarioActualService;
    }


    // =====================================================
    // OBTENER FARMACEUTICO AUTORIZADO
    // =====================================================

    public Usuario obtenerFarmaceuticoAutorizado() {

        // -------------------------------------------------
        // USUARIO AUTENTICADO
        // -------------------------------------------------

        Usuario usuario =
                usuarioActualService.obtenerUsuarioActual();


        // -------------------------------------------------
        // VERIFICAR USUARIO
        // -------------------------------------------------

        if (usuario == null
                || usuario.getId() == null
                || !Boolean.TRUE.equals(
                usuario.getActivo()
        )) {

            throw new AccessDeniedException(
                    "El usuario no se encuentra autorizado para Farmacia."
            );
        }


        // -------------------------------------------------
        // VERIFICAR ROL
        // -------------------------------------------------

        Rol rol = usuario.getRol();

        if (rol == null
                || !Boolean.TRUE.equals(
                rol.getActivo()
        )) {

            throw new AccessDeniedException(
                    "El usuario no tiene un rol activo autorizado."
            );
        }


        // -------------------------------------------------
        // VALIDAR ROL FARMACEUTICO
        // -------------------------------------------------

        String nombreRol =
                normalizarNombreRol(
                        rol.getNombre()
                );

        if (!"FARMACEUTICO".equals(nombreRol)) {

            throw new AccessDeniedException(
                    "Solo el personal de Farmacia puede realizar esta operación."
            );
        }


        // -------------------------------------------------
        // VERIFICAR SUCURSAL
        // -------------------------------------------------

        Sucursal sucursal =
                usuario.getSucursal();

        if (sucursal == null
                || sucursal.getId() == null
                || !Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new AccessDeniedException(
                    "El farmacéutico debe pertenecer a una sucursal activa."
            );
        }


        return usuario;
    }


    // =====================================================
    // OBTENER ID DE SUCURSAL AUTORIZADA
    // =====================================================

    public Integer obtenerIdSucursalAutorizada() {

        Usuario farmaceutico =
                obtenerFarmaceuticoAutorizado();

        return farmaceutico
                .getSucursal()
                .getId();
    }


    // =====================================================
    // VALIDAR ACCESO A UNA RECETA MEDICA
    // =====================================================

    public void validarAccesoReceta(
            RecetaMedica receta
    ) {

        // -------------------------------------------------
        // OBTENER SUCURSAL DEL FARMACEUTICO
        // -------------------------------------------------

        Integer idSucursalAutorizada =
                obtenerIdSucursalAutorizada();


        // -------------------------------------------------
        // RECETA Y CITA VALIDAS
        // -------------------------------------------------

        if (receta == null
                || receta.getId() == null) {

            throw new IllegalArgumentException(
                    "La receta médica seleccionada no es válida."
            );
        }

        Cita cita = receta.getCita();

        if (cita == null
                || cita.getId() == null
                || cita.getSucursal() == null
                || cita.getSucursal().getId() == null) {

            throw new IllegalStateException(
                    "La receta no tiene una cita y sucursal válidas."
            );
        }


        // -------------------------------------------------
        // MISMA SUCURSAL
        // -------------------------------------------------

        Integer idSucursalReceta =
                cita.getSucursal().getId();

        if (!Objects.equals(
                idSucursalAutorizada,
                idSucursalReceta
        )) {

            throw new AccessDeniedException(
                    "No tiene autorización para consultar o despachar esta receta."
            );
        }
    }


    // =====================================================
    // NORMALIZAR NOMBRE DEL ROL
    // =====================================================
    //
    // Permite reconocer:
    //
    // Farmacéutico
    // Farmaceutico
    // FARMACEUTICO
    //
    // Sin cambiar los registros existentes
    // del catálogo de roles.
    //
    // =====================================================

    private String normalizarNombreRol(
            String nombreRol
    ) {

        if (nombreRol == null
                || nombreRol.isBlank()) {

            return "";
        }

        String normalizado =
                Normalizer.normalize(
                        nombreRol.trim(),
                        Normalizer.Form.NFD
                );

        normalizado =
                normalizado.replaceAll(
                        "\\p{M}+",
                        ""
                );

        return normalizado.toUpperCase(
                Locale.ROOT
        );
    }
}
