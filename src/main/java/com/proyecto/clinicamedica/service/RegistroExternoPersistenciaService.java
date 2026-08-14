package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.event.UsuarioExternoRegistradoEvent;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.context.ApplicationEventPublisher;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * =========================================================
 * SERVICIO: PERSISTENCIA DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidades:
 *
 * - Persistir al nuevo paciente.
 * - Forzar el INSERT mediante saveAndFlush().
 * - Publicar el evento de registro exitoso.
 *
 * Utiliza una transacción independiente para que cualquier
 * conflicto de integridad proveniente de PostgreSQL pueda
 * ser capturado por el servicio coordinador sin dejar
 * dañada su transacción principal.
 *
 * =========================================================
 */
@Service
public class RegistroExternoPersistenciaService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final ApplicationEventPublisher eventPublisher;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RegistroExternoPersistenciaService(
            UsuarioRepository usuarioRepository,
            ApplicationEventPublisher eventPublisher
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.eventPublisher =
                eventPublisher;
    }


    // =====================================================
    // GUARDAR
    // =====================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public Usuario guardar(
            Usuario usuario
    ) {

        // =================================================
        // PERSISTIR Y FORZAR INSERT
        // =================================================
        //
        // saveAndFlush() fuerza a Hibernate a ejecutar
        // inmediatamente el INSERT.
        //
        // De esta forma PostgreSQL puede detectar aquí
        // posibles restricciones UNIQUE, FK, CHECK, etc.
        // =================================================

        Usuario usuarioGuardado =
                usuarioRepository
                        .saveAndFlush(
                                usuario
                        );


        // =================================================
        // PUBLICAR EVENTO CU-02
        // =================================================

        eventPublisher.publishEvent(
                new UsuarioExternoRegistradoEvent(
                        usuarioGuardado.getId(),
                        usuarioGuardado.getNombreCompleto(),
                        usuarioGuardado.getCorreoElectronico()
                )
        );


        // =================================================
        // RETORNAR USUARIO PERSISTIDO
        // =================================================

        return usuarioGuardado;
    }
}