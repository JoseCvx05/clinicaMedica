package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.config.CacheConfig;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * =========================================================
 * SERVICIO: CATÁLOGO DE ESTADOS DE CITA
 * =========================================================
 *
 * Catálogo compartido por los diferentes casos de uso.
 *
 * CU-07 utiliza principalmente:
 *
 * - Paciente Presente
 * - Signos Vitales
 *
 * Los estados activos se cargan desde PostgreSQL una vez
 * y posteriormente se mantienen en Caffeine.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class CatalogoEstadoCitaService {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final EstadoCitaRepository
            estadoCitaRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CatalogoEstadoCitaService(
            EstadoCitaRepository estadoCitaRepository
    ) {

        this.estadoCitaRepository =
                estadoCitaRepository;
    }


    // =====================================================
    // CATÁLOGO DE ESTADOS ACTIVOS
    // =====================================================

    /**
     * La primera llamada consulta PostgreSQL.
     *
     * Las siguientes llamadas obtienen el resultado
     * directamente desde Caffeine.
     *
     * El Map utiliza:
     *
     * llave -> nombre normalizado
     * valor -> id del estado
     *
     * Ejemplo:
     *
     * "paciente presente" -> 5
     * "signos vitales"    -> 6
     */
    @Cacheable(
            value = CacheConfig.ESTADOS_CITA
    )
    public Map<String, Integer> listarEstadosActivos() {


        /*
         * Dejamos este mensaje para comprobar durante
         * las pruebas que PostgreSQL se consulta una
         * sola vez.
         */
        System.out.println(
                ">>> CARGANDO ESTADOS_CITA DESDE POSTGRESQL"
        );


        Map<String, Integer> estados =
                new LinkedHashMap<>();


        for (EstadoCita estado :
                estadoCitaRepository
                        .findByActivoTrueOrderByNombreAsc()) {


            if (estado.getId() == null
                    || estado.getNombre() == null
                    || estado.getNombre().isBlank()) {

                continue;
            }


            estados.put(

                    normalizar(
                            estado.getNombre()
                    ),

                    estado.getId()
            );
        }


        /*
         * La caché entrega una colección inmutable.
         *
         * Evitamos que otro servicio pueda modificar
         * accidentalmente el catálogo almacenado.
         */
        return Collections.unmodifiableMap(
                estados
        );
    }


    // =====================================================
    // OBTENER ID DE UN ESTADO
    // =====================================================

    public Integer obtenerIdEstadoActivo(
            String nombre,
            Map<String, Integer> catalogo
    ) {

        if (nombre == null
                || nombre.isBlank()
                || catalogo == null) {

            return null;
        }


        return catalogo.get(
                normalizar(
                        nombre
                )
        );
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor
                .trim()
                .toLowerCase();
    }
}