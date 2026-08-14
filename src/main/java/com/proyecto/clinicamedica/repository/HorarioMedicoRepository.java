package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.HorarioMedico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * =========================================================
 * REPOSITORY: HORARIO MÉDICO
 * =========================================================
 *
 * CU-03 Agendar Citas.
 *
 * Responsabilidad:
 *
 * - Consultar horarios habituales activos del médico.
 *
 * No determina disponibilidad final.
 * Esa lógica pertenecerá a DisponibilidadCitaService.
 * =========================================================
 */
@Repository
public interface HorarioMedicoRepository
        extends JpaRepository<HorarioMedico, Integer> {


    /**
     * Obtiene los bloques habituales de atención
     * de un médico para un día específico.
     *
     * diaSemana:
     *
     * 1 = lunes
     * 2 = martes
     * 3 = miércoles
     * 4 = jueves
     * 5 = viernes
     * 6 = sábado
     * 7 = domingo
     */
    List<HorarioMedico>
    findByMedico_IdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
            Integer idMedico,
            Short diaSemana
    );
}