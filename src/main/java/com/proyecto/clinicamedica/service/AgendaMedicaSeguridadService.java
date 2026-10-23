
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================
 * CU-16 - SERVICE: SEGURIDAD DE AGENDA MÉDICA
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Identificar al usuario autenticado.
 * - Verificar que se encuentre activo.
 * - Exigir el rol Médico.
 * - Obtener el ID real del médico desde PostgreSQL.
 * - Centralizar la seguridad de eventos y tareas.
 *
 * UTILIZADO POR:
 *
 * - Calendario de Agenda Médica.
 * - Gestión de eventos.
 * - TaskPanel.
 *
 * SEGURIDAD:
 *
 * - No recibe el ID del médico desde el navegador.
 * - No permite utilizar la agenda con otro rol.
 * - Los Services deben verificar la propiedad
 *   de cada evento o tarea consultada.
 *
 * COMPATIBILIDAD:
 *
 * - Reutiliza UsuarioActualService.
 * - No modifica la seguridad del CU-08.
 * - No modifica usuarios ni citas.
 *
 * =========================================================
 */
@Service
public class AgendaMedicaSeguridadService {

    // =====================================================
    // CONSTANTE DEL ROL
    // =====================================================

    private static final String ROL_MEDICO = "Médico";

    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final UsuarioActualService usuarioActualService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AgendaMedicaSeguridadService(
            UsuarioActualService usuarioActualService
    ) {

        this.usuarioActualService = usuarioActualService;
    }

    // =====================================================
    // CU-16 - OBTENER MÉDICO AUTENTICADO
    // =====================================================

    @Transactional(readOnly = true)
    public Usuario obtenerMedicoActual() {

        // =============================================
        // 1. RECUPERAR USUARIO AUTENTICADO
        // =============================================

        Usuario usuario =
                usuarioActualService.obtenerUsuarioActual();

        // =============================================
        // 2. VALIDAR USUARIO ACTIVO
        // =============================================

        if (usuario == null
                || !Boolean.TRUE.equals(usuario.getActivo())) {

            throw new AccessDeniedException(
                    "No tiene permisos para acceder "
                            + "a la Agenda Médica."
            );
        }

        // =============================================
        // 3. VALIDAR ROL
        // =============================================

        if (usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || !ROL_MEDICO.equalsIgnoreCase(
                usuario.getRol()
                        .getNombre()
                        .trim()
        )) {

            throw new AccessDeniedException(
                    "La Agenda Médica está disponible "
                            + "únicamente para el rol Médico."
            );
        }

        // =============================================
        // 4. VALIDAR IDENTIFICADOR
        // =============================================

        if (usuario.getId() == null
                || usuario.getId() <= 0) {

            throw new AccessDeniedException(
                    "No fue posible identificar "
                            + "al médico autenticado."
            );
        }

        // =============================================
        // 5. DEVOLVER MÉDICO AUTORIZADO
        // =============================================

        return usuario;
    }

    // =====================================================
    // CU-16 - OBTENER ID DEL MÉDICO AUTENTICADO
    // =====================================================

    @Transactional(readOnly = true)
    public Integer obtenerIdMedicoActual() {

        return obtenerMedicoActual().getId();
    }
}
