package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.TipoFiltroUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.dto.UsuarioListadoDTO;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;
import com.proyecto.clinicamedica.service.UsuarioMantenimientoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

/**
 * =========================================================
 * CONTROLLER: LISTADO DE USUARIOS
 * =========================================================
 *
 * CU-01
 *
 * Responsabilidad:
 *
 * - Mostrar usuarios.
 * - Recibir filtros.
 * - Paginación.
 * - Ordenamiento.
 *
 * =========================================================
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioListadoController {

    private static final Set<String> ROLES_INTERNOS =
            Set.of(
                    "Médico",
                    "Enfermero",
                    "Recepcionista",
                    "Cajero",
                    "Laboratorista",
                    "Farmacéutico",
                    "Administrador"
            );

    private final UsuarioMantenimientoService
            usuarioMantenimientoService;

    private final RolService
            rolService;

    private final SucursalService
            sucursalService;

    public UsuarioListadoController(
            UsuarioMantenimientoService usuarioMantenimientoService,
            RolService rolService,
            SucursalService sucursalService
    ) {

        this.usuarioMantenimientoService =
                usuarioMantenimientoService;

        this.rolService =
                rolService;

        this.sucursalService =
                sucursalService;
    }

    // =====================================================
    // ROLES INTERNOS
    // =====================================================

    private List<Rol> obtenerRolesInternos() {

        return rolService
                .listarActivos()
                .stream()
                .filter(
                        rol ->
                                rol != null
                                        && rol.getNombre() != null
                                        && ROLES_INTERNOS
                                        .stream()
                                        .anyMatch(
                                                permitido ->
                                                        permitido.equalsIgnoreCase(
                                                                rol.getNombre()
                                                        )
                                        )
                )
                .toList();
    }


    // =====================================================
    // TAMAÑO DE PÁGINA SEGURO
    // =====================================================

    private int obtenerTamanioSeguro(
            int tamanio
    ) {

        return switch (tamanio) {

            case 10, 20, 25, 50 ->
                    tamanio;

            default ->
                    20;
        };
    }
}