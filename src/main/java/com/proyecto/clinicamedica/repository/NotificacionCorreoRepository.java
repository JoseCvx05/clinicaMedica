package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.NotificacionCorreo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: NOTIFICACIONES DE CORREO
 * =========================================================
 *
 * Permite persistir notificaciones y recuperar correos
 * pendientes/fallidos para reintentos automáticos.
 *
 * CU-09:
 *
 * Permite consultar la última notificación enviada para
 * un resultado de laboratorio.
 *
 * Esto permite diferenciar:
 *
 * - Primera publicación.
 * - Republicación después de una corrección RNF-024.
 *
 * =========================================================
 */
@Repository
public interface NotificacionCorreoRepository
        extends JpaRepository<NotificacionCorreo, Long> {


    // =====================================================
    // REINTENTOS
    // =====================================================

    List<NotificacionCorreo>
    findTop20ByEstadoEnvioInAndIntentosEnvioLessThanOrderByFechaCreacionAsc(
            Collection<String> estados,
            Short maximoIntentos
    );


    // =====================================================
    // EXISTENCIA GENERAL
    // =====================================================
    //
    // Se conserva porque otros módulos pueden utilizar
    // esta validación de idempotencia.
    //
    // =====================================================

    boolean existsByTipoNotificacionAndTablaReferenciaAndIdReferencia(
            String tipoNotificacion,
            String tablaReferencia,
            Integer idReferencia
    );


    // =====================================================
    // CU-09 / RNF-024
    // ÚLTIMA NOTIFICACIÓN DE UN RESULTADO
    // =====================================================
    //
    // IMPORTANTE:
    //
    // Un mismo ResultadoLaboratorio puede publicarse más
    // de una vez solamente cuando pasó previamente por
    // una corrección autorizada.
    //
    // Ejemplo:
    //
    // Resultado #3
    //
    // Publicación original
    //      -> notificación A
    //
    // Corrección autorizada
    //      -> nueva validación
    //      -> nueva publicación
    //      -> notificación B
    //
    // Este método permite obtener la notificación más
    // reciente y compararla posteriormente contra
    // fechaHoraPublicacion.
    //
    // =====================================================

    Optional<NotificacionCorreo>
    findTopByTipoNotificacionAndTablaReferenciaAndIdReferenciaOrderByFechaCreacionDesc(
            String tipoNotificacion,
            String tablaReferencia,
            Integer idReferencia
    );
}