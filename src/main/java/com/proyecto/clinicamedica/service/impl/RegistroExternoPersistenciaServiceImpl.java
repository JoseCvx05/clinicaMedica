package com.proyecto.clinicamedica.service.impl;

import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.event.UsuarioExternoRegistradoEvent;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import com.proyecto.clinicamedica.service.RegistroExternoPersistenciaService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * =========================================================
 * PERSISTENCIA: REGISTRO DE USUARIO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad exclusiva:
 *
 * - Guardar al paciente.
 * - Forzar el INSERT con saveAndFlush().
 * - Publicar el evento de registro exitoso.
 *
 * Utiliza una transacción independiente para que cualquier
 * conflicto de integridad pueda ser capturado por el
 * servicio coordinador sin dejar una transacción dañada.
 * =========================================================
 */
@Service
public class RegistroExternoPersistenciaServiceImpl
        implements RegistroExternoPersistenciaService {


    private final UsuarioRepository usuarioRepository;

    private final ApplicationEventPublisher eventPublisher;


    public RegistroExternoPersistenciaServiceImpl(
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

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public Usuario guardar(
            Usuario usuario
    ) {

        Usuario usuarioGuardado =
                usuarioRepository.saveAndFlush(
                        usuario
                );


        // =================================================
        // EVENTO CU-02
        // =================================================

        eventPublisher.publishEvent(
                new UsuarioExternoRegistradoEvent(
                        usuarioGuardado.getId(),
                        usuarioGuardado.getNombreCompleto(),
                        usuarioGuardado.getCorreoElectronico()
                )
        );


        return usuarioGuardado;
    }
}