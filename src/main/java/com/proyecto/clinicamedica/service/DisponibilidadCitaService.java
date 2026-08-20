package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.HorarioDisponibleDTO;

import com.proyecto.clinicamedica.entity.HorarioMedico;

import com.proyecto.clinicamedica.repository.HorarioMedicoRepository;

import com.proyecto.clinicamedica.service.disponibilidad.FuenteOcupacionHorario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: DISPONIBILIDAD DE CITAS
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Calcula los horarios disponibles de un médico.
 *
 * La disponibilidad se obtiene mediante:
 *
 * Horario habitual
 *      MENOS
 * citas existentes
 *      MENOS
 * eventos de agenda
 *      MENOS
 * reservas temporales vigentes
 *
 * Las distintas fuentes de ocupación utilizan
 * polimorfismo mediante:
 *
 * List<FuenteOcupacionHorario>
 *
 * La disponibilidad NO utiliza caché porque debe
 * mantenerse actualizada en tiempo real.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadCitaService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final HorarioMedicoRepository
            horarioMedicoRepository;

    private final List<FuenteOcupacionHorario>
            fuentesOcupacion;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DisponibilidadCitaService(
            HorarioMedicoRepository horarioMedicoRepository,
            List<FuenteOcupacionHorario> fuentesOcupacion,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.horarioMedicoRepository =
                horarioMedicoRepository;

        this.fuentesOcupacion =
                fuentesOcupacion;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // OBTENER HORARIOS DEL DÍA
    // =====================================================

    public List<HorarioDisponibleDTO> obtenerHorarios(
            Integer idMedico,
            LocalDate fecha
    ) {

        if (idMedico == null
                || fecha == null) {

            return List.of();
        }


        // =================================================
        // NO GENERAR DÍAS PASADOS
        // =================================================

        LocalDate hoy =
                LocalDate.now(
                        zonaHoraria
                );


        if (fecha.isBefore(
                hoy
        )) {

            return List.of();
        }


        // =================================================
        // DÍA ISO
        //
        // 1 = Lunes
        // ...
        // 7 = Domingo
        // =================================================

        short diaSemana =
                (short) fecha
                        .getDayOfWeek()
                        .getValue();


        List<HorarioMedico> bloques =
                horarioMedicoRepository
                        .findByMedico_IdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                idMedico,
                                diaSemana
                        );


        if (bloques.isEmpty()) {

            return List.of();
        }


        List<HorarioDisponibleDTO> resultado =
                new ArrayList<>();


        // =================================================
        // GENERAR INTERVALOS
        // =================================================

        for (HorarioMedico bloque :
                bloques) {

            generarHorariosDelBloque(
                    idMedico,
                    fecha,
                    bloque,
                    resultado
            );
        }


        return resultado
                .stream()
                .sorted(
                        Comparator.comparing(
                                HorarioDisponibleDTO::inicio
                        )
                )
                .toList();
    }


    // =====================================================
    // VALIDAR HORARIO ESPECÍFICO
    // =====================================================

    public boolean estaDisponible(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        if (idMedico == null
                || inicio == null
                || fin == null) {

            return false;
        }


        if (!fin.isAfter(
                inicio
        )) {

            return false;
        }


        // =================================================
        // DEBE SER FUTURO
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        if (!inicio.isAfter(
                ahora
        )) {

            return false;
        }


        /*
         * Normalizamos a la zona horaria del hospital
         * antes de comprobar el horario habitual.
         */
        OffsetDateTime inicioLocal =
                inicio
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toOffsetDateTime();


        OffsetDateTime finLocal =
                fin
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toOffsetDateTime();


        // =================================================
        // DEBE PERTENECER AL HORARIO REAL DEL MÉDICO
        // =================================================

        if (!perteneceAHorarioMedico(
                idMedico,
                inicioLocal,
                finLocal
        )) {

            return false;
        }


        // =================================================
        // NO DEBE ESTAR OCUPADO
        // =================================================

        return !estaOcupado(
                idMedico,
                inicioLocal,
                finLocal
        );
    }
    // =====================================================
// VALIDAR DISPONIBILIDAD PARA REASIGNACIÓN
// =====================================================
//
// FA07 CU-05.
//
// A diferencia del agendamiento normal, una cita puede
// estar ya en estado "Paciente Presente", por lo que no
// exigimos que la hora de inicio sea posterior a "ahora".
//
// Sí mantenemos:
// - horario habitual del médico,
// - citas existentes,
// - eventos,
// - reservas temporales.
//
// =====================================================

    public boolean estaDisponibleParaReasignacion(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        if (idMedico == null
                || inicio == null
                || fin == null) {

            return false;
        }


        if (!fin.isAfter(
                inicio
        )) {

            return false;
        }


        OffsetDateTime inicioLocal =
                inicio
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toOffsetDateTime();


        OffsetDateTime finLocal =
                fin
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toOffsetDateTime();


        // =================================================
        // HORARIO REAL DEL MÉDICO
        // =================================================

        if (!perteneceAHorarioMedico(
                idMedico,
                inicioLocal,
                finLocal
        )) {

            return false;
        }


        // =================================================
        // FUENTES DE OCUPACIÓN
        // =================================================

        return !estaOcupado(
                idMedico,
                inicioLocal,
                finLocal
        );
    }


    // =====================================================
    // GENERAR HORARIOS DE UN BLOQUE
    // =====================================================

    private void generarHorariosDelBloque(
            Integer idMedico,
            LocalDate fecha,
            HorarioMedico bloque,
            List<HorarioDisponibleDTO> resultado
    ) {

        if (bloque == null
                || bloque.getHoraInicio() == null
                || bloque.getHoraFin() == null
                || bloque.getDuracionCitaMinutos() == null) {

            return;
        }


        int duracionMinutos =
                bloque.getDuracionCitaMinutos();


        if (duracionMinutos <= 0) {

            return;
        }


        OffsetDateTime inicioBloque =
                convertir(
                        fecha,
                        bloque.getHoraInicio()
                );


        OffsetDateTime finBloque =
                convertir(
                        fecha,
                        bloque.getHoraFin()
                );


        OffsetDateTime cursor =
                inicioBloque;


        while (!cursor
                .plusMinutes(
                        duracionMinutos
                )
                .isAfter(
                        finBloque
                )) {


            OffsetDateTime inicio =
                    cursor;


            OffsetDateTime fin =
                    cursor.plusMinutes(
                            duracionMinutos
                    );


            // =============================================
            // SOLO HORARIOS FUTUROS
            // =============================================

            if (inicio.isAfter(
                    OffsetDateTime.now(
                            zonaHoraria
                    )
            )) {


                // =========================================
                // CONSULTAR TODAS LAS FUENTES
                // =========================================

                if (!estaOcupado(
                        idMedico,
                        inicio,
                        fin
                )) {

                    resultado.add(
                            new HorarioDisponibleDTO(
                                    inicio,
                                    fin
                            )
                    );
                }
            }


            cursor =
                    cursor.plusMinutes(
                            duracionMinutos
                    );
        }
    }


    // =====================================================
    // POLIMORFISMO DE OCUPACIÓN
    // =====================================================

    private boolean estaOcupado(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        return fuentesOcupacion
                .stream()
                .anyMatch(
                        fuente ->
                                fuente.estaOcupado(
                                        idMedico,
                                        inicio,
                                        fin
                                )
                );
    }


    // =====================================================
    // VALIDAR CONTRA HORARIO HABITUAL
    // =====================================================

    private boolean perteneceAHorarioMedico(
            Integer idMedico,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {

        LocalDate fecha =
                inicio
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toLocalDate();


        // =================================================
        // UNA CITA NO PUEDE CRUZAR DE DÍA
        // =================================================

        LocalDate fechaFin =
                fin
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toLocalDate();


        if (!fecha.equals(
                fechaFin
        )) {

            return false;
        }


        short diaSemana =
                (short) fecha
                        .getDayOfWeek()
                        .getValue();


        List<HorarioMedico> bloques =
                horarioMedicoRepository
                        .findByMedico_IdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                idMedico,
                                diaSemana
                        );


        LocalTime horaInicio =
                inicio
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toLocalTime();


        LocalTime horaFin =
                fin
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toLocalTime();


        for (HorarioMedico bloque :
                bloques) {

            if (esIntervaloValidoDelBloque(
                    bloque,
                    horaInicio,
                    horaFin
            )) {

                return true;
            }
        }


        return false;
    }


    // =====================================================
    // VALIDAR INTERVALO DEL BLOQUE
    // =====================================================

    private boolean esIntervaloValidoDelBloque(
            HorarioMedico bloque,
            LocalTime inicio,
            LocalTime fin
    ) {

        if (bloque.getHoraInicio() == null
                || bloque.getHoraFin() == null
                || bloque.getDuracionCitaMinutos() == null) {

            return false;
        }


        // =================================================
        // DENTRO DE LOS LÍMITES
        // =================================================

        if (inicio.isBefore(
                bloque.getHoraInicio()
        )) {

            return false;
        }


        if (fin.isAfter(
                bloque.getHoraFin()
        )) {

            return false;
        }


        // =================================================
        // DURACIÓN EXACTA
        // =================================================

        long duracion =
                Duration.between(
                                inicio,
                                fin
                        )
                        .toMinutes();


        if (duracion
                != bloque.getDuracionCitaMinutos()) {

            return false;
        }


        // =================================================
        // ALINEACIÓN DEL SLOT
        // =================================================
        //
        // Bloque:
        // 08:00 - ...
        // duración = 30
        //
        // Válidos:
        // 08:00
        // 08:30
        // 09:00
        //
        // Inválido:
        // 08:17
        // =================================================

        long minutosDesdeInicio =
                Duration.between(
                                bloque.getHoraInicio(),
                                inicio
                        )
                        .toMinutes();


        return minutosDesdeInicio
                % bloque.getDuracionCitaMinutos()
                == 0;
    }


    // =====================================================
    // LOCAL DATE/TIME -> OFFSET DATE TIME
    // =====================================================

    private OffsetDateTime convertir(
            LocalDate fecha,
            LocalTime hora
    ) {

        LocalDateTime fechaHora =
                LocalDateTime.of(
                        fecha,
                        hora
                );


        return fechaHora
                .atZone(
                        zonaHoraria
                )
                .toOffsetDateTime();
    }
}