package com.proyecto.clinicamedica.security;

import com.proyecto.clinicamedica.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * =========================================================
 * STRATEGY: POLÍTICA DE AUTENTICACIÓN INTERNA
 * =========================================================
 *
 * Define las reglas particulares del acceso utilizado
 * por el personal interno del hospital.
 *
 * Roles internos permitidos:
 *
 * - Médico
 * - Enfermero
 * - Recepcionista
 * - Cajero
 * - Laboratorista
 * - Farmacéutico
 * - Administrador
 *
 * IMPORTANTE:
 *
 * Poder autenticarse como usuario interno NO significa
 * tener permiso para utilizar todos los módulos.
 *
 * Spring Security realizará posteriormente la
 * autorización específica.
 *
 * Ejemplo:
 *
 * ROLE_ADMINISTRADOR
 *        ↓
 * /admin/usuarios/**
 *
 * =========================================================
 */
@Component
public class PoliticaInterna
        implements PoliticaAutenticacion {

    /**
     * Roles que pueden utilizar el acceso interno.
     *
     * Se utiliza Set porque:
     *
     * - No queremos duplicados.
     * - La consulta contains() es directa.
     * - Centralizamos los roles permitidos.
     */
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


    // =====================================================
    // TIPO DE ACCESO
    // =====================================================

    @Override
    public TipoAcceso getTipoAcceso() {

        return TipoAcceso.INTERNO;
    }


    // =====================================================
    // VALIDAR TIPO DE USUARIO
    // =====================================================

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


        String rolNormalizado =
                nombreRol.trim();


        return ROLES_INTERNOS
                .stream()
                .anyMatch(
                        rolPermitido ->
                                rolPermitido.equalsIgnoreCase(
                                        rolNormalizado
                                )
                );
    }


    // =====================================================
    // RN-GLOBAL-007
    // CREDENCIALES INCORRECTAS
    // =====================================================

    @Override
    public String getMensajeCredencialesIncorrectas(
            int intentosRestantes
    ) {

        return "Las credenciales ingresadas son incorrectas. "
                + "Tiene "
                + intentosRestantes
                + " intentos restantes antes del bloqueo temporal.";
    }


    // =====================================================
    // RN-GLOBAL-007
    // CUENTA BLOQUEADA
    // =====================================================

    @Override
    public String getMensajeCuentaBloqueada() {

        return "Su cuenta ha sido bloqueada temporalmente "
                + "por múltiples intentos fallidos. "
                + "Contacte al administrador del sistema.";
    }


    // =====================================================
    // ROL NO AUTORIZADO
    // =====================================================

    /**
     * Se utilizará, por ejemplo, si una cuenta Paciente
     * intenta utilizar el acceso del personal interno.
     */
    @Override
    public String getMensajeRolNoAutorizado() {

        return "Este acceso es exclusivo para personal interno "
                + "del hospital.";
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

        /*
         * Por ahora todos los usuarios internos ingresarán
         * a un punto común.
         *
         * Spring Security decidirá posteriormente qué
         * módulos puede utilizar cada rol.
         *
         * No estamos inventando un dashboard distinto para
         * cada rol sin que los casos de uso lo requieran.
         */
        return "/interno/dashboard";
    }
}