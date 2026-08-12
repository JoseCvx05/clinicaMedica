package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.NotificacionCorreo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * =========================================================
 * REPOSITORY: NOTIFICACIONES DE CORREO
 * =========================================================
 *
 * Permite persistir notificaciones y recuperar correos
 * pendientes/fallidos para reintentos automáticos.
 * =========================================================
 */
@Repository
public interface NotificacionCorreoRepository
        extends JpaRepository<NotificacionCorreo, Long> {


    List<NotificacionCorreo>
    findTop20ByEstadoEnvioInAndIntentosEnvioLessThanOrderByFechaCreacionAsc(
            Collection<String> estados,
            Short maximoIntentos
    );
}