package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Responsabilidades:
 *
 * - Verificar si una cita ya posee consulta.
 * - Recuperar consulta por cita.
 * - Cargar la consulta únicamente para su Médico.
 * - Construir las secciones del panel médico.
 * - Bloquear la consulta durante operaciones críticas.
 *
 * =========================================================
 */
@Repository
public interface ConsultaMedicaRepository
        extends JpaRepository<ConsultaMedica, Integer> {


    // =====================================================
    // ¿YA EXISTE CONSULTA PARA LA CITA?
    // =====================================================

    boolean existsByCita_Id(
            Integer idCita
    );


    // =====================================================
    // CONSULTA POR CITA
    // =====================================================

    Optional<ConsultaMedica> findByCita_Id(
            Integer idCita
    );


    // =====================================================
    // CONSULTA COMPLETA PARA EL MÉDICO
    // =====================================================

    /**
     * Solamente devuelve la consulta cuando:
     *
     * - pertenece a la cita indicada;
     * - fue realizada por el Médico autenticado.
     *
     * Esto protege contra manipulación del ID en la URL.
     */
    @Query("""
            SELECT cm
            FROM ConsultaMedica cm

            JOIN FETCH cm.cita c

            JOIN FETCH c.paciente
            JOIN FETCH c.medico
            JOIN FETCH c.sucursal
            JOIN FETCH c.especialidad
            JOIN FETCH c.estadoCita

            JOIN FETCH cm.medico

            LEFT JOIN FETCH cm.cie10

            WHERE c.id = :idCita

              AND cm.medico.id = :idMedico
            """)
    Optional<ConsultaMedica> buscarPorCitaYMedico(

            @Param("idCita")
            Integer idCita,

            @Param("idMedico")
            Integer idMedico
    );


    // =====================================================
    // CU-08 - PANEL MÉDICO
    // EN CONSULTA / EVALUADOS
    // =====================================================

    /**
     * Devuelve las consultas correspondientes a una sección
     * del panel Médico.
     *
     * EN_CURSO:
     * sección "En Consulta Médica".
     *
     * FINALIZADA:
     * sección "Evaluados — Pendiente de cierre".
     *
     * La cita debe continuar en estado "Consulta Médica".
     */
    @Query("""
            SELECT cm
            FROM ConsultaMedica cm

            JOIN FETCH cm.cita c

            JOIN FETCH c.paciente
            JOIN FETCH c.medico
            JOIN FETCH c.sucursal
            JOIN FETCH c.especialidad
            JOIN FETCH c.estadoCita

            JOIN FETCH cm.medico

            LEFT JOIN FETCH cm.cie10

            WHERE cm.medico.id = :idMedico

              AND cm.estadoConsulta = :estadoConsulta

              AND LOWER(c.estadoCita.nombre)
                  = LOWER('Consulta Médica')

            ORDER BY

                CASE
                    WHEN LOWER(COALESCE(c.prioridad, ''))
                         = 'emergencia'
                    THEN 0
                    ELSE 1
                END ASC,

                c.fechaHoraCita ASC,

                c.id ASC
            """)
    List<ConsultaMedica> buscarParaPanelMedicoPorEstado(

            @Param("idMedico")
            Integer idMedico,

            @Param("estadoConsulta")
            EstadoConsultaMedica estadoConsulta
    );


    // =====================================================
    // BLOQUEO PARA MODIFICAR / FINALIZAR
    // =====================================================

    /**
     * Bloquea la consulta durante:
     *
     * - Guardar cambios.
     * - Finalizar consulta.
     * - Firmar.
     *
     * Además verifica que la consulta pertenezca
     * al Médico autenticado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cm
            FROM ConsultaMedica cm

            JOIN FETCH cm.cita c

            JOIN FETCH c.paciente
            JOIN FETCH c.medico
            JOIN FETCH c.sucursal
            JOIN FETCH c.especialidad
            JOIN FETCH c.estadoCita

            JOIN FETCH cm.medico

            LEFT JOIN FETCH cm.cie10

            WHERE c.id = :idCita

              AND cm.medico.id = :idMedico
            """)
    Optional<ConsultaMedica> buscarPorCitaYMedicoConBloqueo(

            @Param("idCita")
            Integer idCita,

            @Param("idMedico")
            Integer idMedico
    );
}