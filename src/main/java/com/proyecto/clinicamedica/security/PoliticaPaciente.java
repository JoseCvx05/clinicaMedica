package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * STRATEGY: POLÍTICA DE AUTENTICACIÓN DEL PACIENTE
 * =========================================================
 *
 * Implementa las reglas específicas del portal
 * utilizado por pacientes.
 *
 * La lógica común de autenticación:
 *
 * - búsqueda del usuario
 * - BCrypt
 * - intentos fallidos
 * - bloqueo temporal
 * - restablecimiento de intentos
 *
 * permanecerá en AutenticacionServiceImpl.
 *
 * Esta clase solamente define lo que cambia según
 * el tipo de portal.
 * =========================================================
 */
@Component
public class PoliticaPaciente
        implements PoliticaAutenticacion {

    private static final String ROL_PACIENTE =
            "Paciente";


    // =====================================================
    // TIPO DE ACCESO
    // =====================================================

    @Override
    public TipoAcceso getTipoAcceso() {

        return TipoAcceso.PACIENTE;
    }


    // =====================================================
    // VALIDAR TIPO DE USUARIO
    // =====================================================

    /**
     * El portal de pacientes solamente permite
     * usuarios cuyo rol activo sea Paciente.
     */
    @Override
    public boolean permiteAcceso(
            Usuario usuario
    ) {

        if (usuario == null) {
            return false;
        }


        if (usuario.getRol() == null) {
            return false;
        }


        String nombreRol =
                usuario.getRol()
                        .getNombre();


        if (nombreRol == null
                || nombreRol.isBlank()) {

            return false;
        }


        return ROL_PACIENTE.equalsIgnoreCase(
                nombreRol.trim()
        );
    }


    // =====================================================
    // FA06 - CREDENCIALES INCORRECTAS
    // =====================================================

    @Override
    public String getMensajeCredencialesIncorrectas(
            int intentosRestantes
    ) {

        return "Usuario o contraseña incorrectos. "
                + "Intentos restantes: "
                + intentosRestantes
                + ".";
    }


    // =====================================================
    // FA07 - CUENTA BLOQUEADA
    // =====================================================

    @Override
    public String getMensajeCuentaBloqueada() {

        return "Cuenta bloqueada temporalmente. "
                + "Intente de nuevo en 15 minutos.";
    }


    // =====================================================
    // FA09 - ROL NO AUTORIZADO
    // =====================================================

    @Override
    public String getMensajeRolNoAutorizado() {

        return "Este acceso es exclusivo para pacientes. "
                + "Si es personal del hospital, "
                + "use el panel administrativo.";
    }


    // =====================================================
    // AUTENTICACIÓN EXITOSA
    // =====================================================

    @Override
    public String getMensajeAutenticacionExitosa() {

        return "Inicio de sesión exitoso.";
    }


    // =====================================================
    // REDIRECCIÓN
    // =====================================================

    @Override
    public String getRedireccionExitosa(
            Usuario usuario
    ) {

        return "/paciente/dashboard";
    }
}