package com.proyecto.clinicamedica.security;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * RESOLVER DE POLÍTICAS DE AUTENTICACIÓN
 * =========================================================
 *
 * Selecciona la estrategia de autenticación correspondiente
 * según el TipoAcceso.
 *
 * Ejemplos:
 *
 * PACIENTE
 *      ↓
 * PoliticaPaciente
 *
 * INTERNO
 *      ↓
 * PoliticaInterna
 *
 * Esto permite que AutenticacionServiceImpl dependa de
 * PoliticaAutenticacion y no de implementaciones concretas.
 *
 * Aplica:
 *
 * - Strategy
 * - Polimorfismo
 * - Open/Closed Principle
 * - Dependency Inversion Principle
 * =========================================================
 */
@Component
public class PoliticaAutenticacionResolver {

    private final Map<
            TipoAcceso,
            PoliticaAutenticacion
            > politicas;


    /**
     * Spring inyectará automáticamente todas las
     * implementaciones existentes de PoliticaAutenticacion.
     *
     * Actualmente:
     *
     * - PoliticaPaciente
     * - PoliticaInterna
     */
    public PoliticaAutenticacionResolver(
            List<PoliticaAutenticacion> politicasDisponibles
    ) {

        if (politicasDisponibles == null
                || politicasDisponibles.isEmpty()) {

            throw new IllegalStateException(
                    "No existen políticas de autenticación configuradas."
            );
        }


        this.politicas =
                new EnumMap<>(
                        TipoAcceso.class
                );


        for (PoliticaAutenticacion politica
                : politicasDisponibles) {

            if (politica == null) {
                continue;
            }


            TipoAcceso tipoAcceso =
                    politica.getTipoAcceso();


            if (tipoAcceso == null) {

                throw new IllegalStateException(
                        "Existe una política de autenticación "
                                + "sin TipoAcceso definido."
                );
            }


            PoliticaAutenticacion existente =
                    this.politicas.put(
                            tipoAcceso,
                            politica
                    );


            /*
             * Evitamos tener accidentalmente dos
             * estrategias registradas para el mismo
             * TipoAcceso.
             */
            if (existente != null) {

                throw new IllegalStateException(
                        "Existe más de una política "
                                + "configurada para el acceso "
                                + tipoAcceso
                                + "."
                );
            }
        }


        /*
         * Verificamos que todos los tipos actualmente
         * definidos tengan una estrategia.
         */
        for (TipoAcceso tipoAcceso
                : TipoAcceso.values()) {

            if (!this.politicas.containsKey(
                    tipoAcceso
            )) {

                throw new IllegalStateException(
                        "No existe una política de autenticación "
                                + "para el acceso "
                                + tipoAcceso
                                + "."
                );
            }
        }
    }


    // =====================================================
    // RESOLVER
    // =====================================================

    /**
     * Obtiene la estrategia correspondiente al tipo
     * de acceso solicitado.
     *
     * @param tipoAcceso portal desde el que se inicia sesión
     *
     * @return política correspondiente
     */
    public PoliticaAutenticacion resolver(
            TipoAcceso tipoAcceso
    ) {

        if (tipoAcceso == null) {

            throw new IllegalArgumentException(
                    "El tipo de acceso es obligatorio."
            );
        }


        PoliticaAutenticacion politica =
                politicas.get(
                        tipoAcceso
                );


        if (politica == null) {

            throw new IllegalStateException(
                    "No existe una política de autenticación "
                            + "para el acceso "
                            + tipoAcceso
                            + "."
            );
        }


        return politica;
    }
}