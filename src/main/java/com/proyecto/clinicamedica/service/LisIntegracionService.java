package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.ResultadoLisDTO;
import com.proyecto.clinicamedica.entity.RegistroMensajeLis;
import com.proyecto.clinicamedica.integration.lis.LisGateway;
import com.proyecto.clinicamedica.repository.RegistroMensajeLisRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * =========================================================
 * SERVICE: INTEGRACIÓN CON LIS
 * =========================================================
 *
 * RNF-030
 *
 * Punto central de comunicación entre CU-09 y una futura
 * integración con analizadores de laboratorio LIS.
 *
 * Responsabilidades:
 *
 * - Consultar disponibilidad del LIS.
 * - Obtener resultados externos.
 * - Validar cada mensaje.
 * - Registrar cada mensaje recibido.
 * - Evitar procesar dos veces un mensaje ya procesado.
 * - Permitir reintentar mensajes RECIBIDOS o FALLIDOS.
 * - Confirmar el procesamiento al sistema externo.
 *
 * IMPORTANTE:
 *
 * Este servicio NO publica resultados automáticamente.
 *
 * La validación y publicación clínica continúa siendo
 * responsabilidad de CU-09.
 *
 * =========================================================
 */
@Service
public class LisIntegracionService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final LisGateway lisGateway;

    private final RegistroMensajeLisRepository
            registroMensajeLisRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LisIntegracionService(

            LisGateway lisGateway,

            RegistroMensajeLisRepository
                    registroMensajeLisRepository
    ) {

        this.lisGateway =
                lisGateway;

        this.registroMensajeLisRepository =
                registroMensajeLisRepository;
    }


    // =====================================================
    // ESTADO DE LA INTEGRACIÓN
    // =====================================================

    public EstadoLis obtenerEstado() {

        return new EstadoLis(

                obtenerNombreIntegracion(),

                estaDisponible()
        );
    }


    // =====================================================
    // DISPONIBILIDAD
    // =====================================================

    public boolean estaDisponible() {

        return lisGateway
                .estaDisponible();
    }


    // =====================================================
    // NOMBRE DE LA INTEGRACIÓN
    // =====================================================

    public String obtenerNombreIntegracion() {

        String nombre =
                lisGateway
                        .obtenerNombreIntegracion();


        if (nombre == null
                || nombre.isBlank()) {

            return "LIS_NO_IDENTIFICADO";
        }


        return nombre.trim();
    }


    // =====================================================
    // OBTENER RESULTADOS PENDIENTES
    // =====================================================
    //
    // IMPORTANTE:
    //
    // Todo mensaje válido queda registrado en PostgreSQL
    // ANTES de entregarse para procesamiento clínico.
    //
    // =====================================================

    public List<ResultadoLisDTO>
    obtenerResultadosPendientes() {

        // =============================================
        // NO EXISTE LIS ACTIVO
        // =============================================

        if (!lisGateway.estaDisponible()) {

            return List.of();
        }


        // =============================================
        // CONSULTAR SISTEMA EXTERNO
        // =============================================

        List<ResultadoLisDTO> recibidos =
                lisGateway
                        .obtenerResultadosPendientes();


        if (recibidos == null
                || recibidos.isEmpty()) {

            return List.of();
        }


        List<ResultadoLisDTO> pendientes =
                new ArrayList<>();


        // =============================================
        // REVISAR CADA MENSAJE
        // =============================================

        for (ResultadoLisDTO resultado : recibidos) {

            if (resultado == null) {

                throw new IllegalStateException(
                        "La integración LIS recibió "
                                + "un resultado vacío."
                );
            }


            // =========================================
            // VALIDAR FORMATO INTERNO
            // =========================================

            resultado.validar();


            String idMensaje =
                    resultado
                            .idMensajeLis()
                            .trim();


            // =========================================
            // BUSCAR REGISTRO PREVIO
            // =========================================

            RegistroMensajeLis existente =
                    registroMensajeLisRepository
                            .findByIdMensajeLis(
                                    idMensaje
                            )
                            .orElse(
                                    null
                            );


            // =========================================
            // MENSAJE NUEVO
            // =========================================

            if (existente == null) {

                RegistroMensajeLis nuevo =
                        new RegistroMensajeLis(

                                idMensaje,

                                resultado
                                        .identificadorAnalizador(),

                                resultado
                                        .numeroOrden(),

                                resultado
                                        .codigoExamen()
                        );


                registroMensajeLisRepository
                        .saveAndFlush(
                                nuevo
                        );


                /*
                 * Solo después de persistir el mensaje
                 * lo entregamos para procesamiento.
                 */
                pendientes.add(
                        resultado
                );


                continue;
            }


            // =========================================
            // YA FUE PROCESADO
            // =========================================
            //
            // No debe volver a ingresar al flujo clínico.
            //
            // =========================================

            if (existente.getEstado()
                    == RegistroMensajeLis
                    .EstadoMensajeLis
                    .PROCESADO) {

                /*
                 * Si el sistema externo volvió a entregar
                 * un mensaje que nosotros ya procesamos,
                 * intentamos confirmar nuevamente.
                 *
                 * Esto ayuda si la confirmación anterior
                 * no llegó al LIS.
                 */
                lisGateway
                        .confirmarProcesamiento(
                                idMensaje
                        );


                continue;
            }


            // =========================================
            // RECIBIDO O FALLIDO
            // =========================================
            //
            // Puede reintentarse.
            //
            // Esto evita perder un resultado si la
            // aplicación se detuvo después de registrar
            // el mensaje pero antes de procesarlo.
            //
            // =========================================

            pendientes.add(
                    resultado
            );
        }


        return List.copyOf(
                pendientes
        );
    }


    // =====================================================
    // CONFIRMAR PROCESAMIENTO
    // =====================================================

    public void confirmarProcesamiento(
            String idMensajeLis
    ) {

        String idLimpio =
                limpiarIdMensaje(
                        idMensajeLis
                );


        // =============================================
        // REGISTRO LOCAL OBLIGATORIO
        // =============================================

        RegistroMensajeLis registro =
                registroMensajeLisRepository
                        .findByIdMensajeLis(
                                idLimpio
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No se puede confirmar un mensaje LIS "
                                                        + "que no ha sido registrado."
                                        )
                        );


        // =============================================
        // MARCAR LOCALMENTE COMO PROCESADO
        // =============================================

        if (registro.getEstado()
                != RegistroMensajeLis
                .EstadoMensajeLis
                .PROCESADO) {

            registro
                    .marcarProcesado();


            registroMensajeLisRepository
                    .saveAndFlush(
                            registro
                    );
        }


        // =============================================
        // CONFIRMAR AL LIS EXTERNO
        // =============================================

        if (lisGateway.estaDisponible()) {

            lisGateway
                    .confirmarProcesamiento(
                            idLimpio
                    );
        }
    }


    // =====================================================
    // MARCAR PROCESAMIENTO FALLIDO
    // =====================================================
    //
    // Permite conservar el error para auditoría y
    // reintentar posteriormente el mismo mensaje.
    //
    // =====================================================

    public void marcarFallido(

            String idMensajeLis,

            String error
    ) {

        String idLimpio =
                limpiarIdMensaje(
                        idMensajeLis
                );


        RegistroMensajeLis registro =
                registroMensajeLisRepository
                        .findByIdMensajeLis(
                                idLimpio
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "El mensaje LIS que se intenta marcar "
                                                        + "como fallido no está registrado."
                                        )
                        );


        registro
                .marcarFallido(
                        error
                );


        registroMensajeLisRepository
                .saveAndFlush(
                        registro
                );
    }


    // =====================================================
    // VERIFICAR SI YA FUE PROCESADO
    // =====================================================

    public boolean yaFueProcesado(
            String idMensajeLis
    ) {

        String idLimpio =
                limpiarIdMensaje(
                        idMensajeLis
                );


        RegistroMensajeLis registro =
                registroMensajeLisRepository
                        .findByIdMensajeLis(
                                idLimpio
                        )
                        .orElse(
                                null
                        );


        return registro != null
                && registro.getEstado()
                == RegistroMensajeLis
                .EstadoMensajeLis
                .PROCESADO;
    }


    // =====================================================
    // VALIDAR IDENTIFICADOR LIS
    // =====================================================

    private String limpiarIdMensaje(
            String idMensajeLis
    ) {

        if (idMensajeLis == null
                || idMensajeLis.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "El identificador del mensaje LIS "
                            + "es obligatorio."
            );
        }


        String limpio =
                idMensajeLis.trim();


        if (limpio.length() > 150) {

            throw new IllegalArgumentException(
                    "El identificador del mensaje LIS "
                            + "no puede superar los 150 caracteres."
            );
        }


        return limpio;
    }


    // =====================================================
    // ESTADO LIS
    // =====================================================

    public record EstadoLis(

            String nombreIntegracion,

            boolean disponible

    ) {
    }
}