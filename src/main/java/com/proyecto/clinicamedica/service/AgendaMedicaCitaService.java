
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;

import com.proyecto.clinicamedica.repository.CitaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Locale;

/**
 * =========================================================
 * CU-16 - SERVICE: CITAS DEL CALENDARIO MÉDICO
 * =========================================================
 *
 * RESPONSABILIDADES:
 *
 * - Identificar al Médico autenticado.
 * - Consultar sus citas desde PostgreSQL.
 * - Respetar los estados permitidos por CU-16.
 * - Consultar citas dentro del período solicitado.
 * - Preparar información para el calendario.
 * - Asignar colores según el estado de las citas.
 *
 * SEGURIDAD:
 *
 * - Acceso exclusivo para el rol Médico.
 * - No recibe el ID del Médico desde el navegador.
 * - Consulta únicamente citas del propietario.
 *
 * COMPATIBILIDAD:
 *
 * - Solo realiza consultas.
 * - No modifica las citas.
 * - No cambia estados.
 * - No modifica disponibilidad del CU-03.
 * - No modifica el CU-08.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class AgendaMedicaCitaService {

    // =====================================================
    // ZONA HORARIA
    // =====================================================

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final AgendaMedicaSeguridadService seguridadService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AgendaMedicaCitaService(

            CitaRepository citaRepository,

            AgendaMedicaSeguridadService seguridadService

    ) {

        this.citaRepository = citaRepository;

        this.seguridadService = seguridadService;
    }

    // =====================================================
    // DTO DE RESPUESTA
    // =====================================================
    //
    // No devolvemos entidades JPA directamente.
    //
    // fechaFin puede ser null si la cita no tiene
    // una hora de finalización registrada.
    //
    // No inventamos duraciones médicas.
    //
    // =====================================================

    public record CitaCalendario(

            Integer id,

            String tipo,

            String titulo,

            String descripcion,

            String nombrePaciente,

            String estado,

            OffsetDateTime fechaInicio,

            OffsetDateTime fechaFin,

            String color

    ) {
    }

    // =====================================================
    // CU-16 - CONSULTAR CITAS PARA CALENDARIO
    // =====================================================
    //
    // Recibe:
    //
    // fechaDesde = primer día solicitado.
    // fechaHasta = último día solicitado (inclusivo).
    //
    // Consulta únicamente citas del Médico actual.
    //
    // =====================================================

    public List<CitaCalendario> listarCitas(

            LocalDate fechaDesde,

            LocalDate fechaHasta

    ) {

        // =============================================
        // 1. VALIDAR MÉDICO AUTENTICADO
        // =============================================

        Integer idMedico =
                seguridadService.obtenerIdMedicoActual();

        // =============================================
        // 2. VALIDAR FECHAS
        // =============================================

        if (fechaDesde == null
                || fechaHasta == null) {

            throw new IllegalArgumentException(
                    "Debe indicar las fechas del calendario."
            );
        }

        if (fechaHasta.isBefore(fechaDesde)) {

            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior "
                            + "a la fecha inicial."
            );
        }

        // =============================================
        // 3. CONTROLAR EL PERÍODO CONSULTADO
        // =============================================
        //
        // Permitimos hasta 63 días consecutivos.
        //
        // Esto permite consultar el calendario mensual,
        // incluyendo días de meses adyacentes.
        //
        // =============================================

        long diferenciaDias =
                ChronoUnit.DAYS.between(
                        fechaDesde,
                        fechaHasta
                );

        if (diferenciaDias > 62) {

            throw new IllegalArgumentException(
                    "El período solicitado no puede "
                            + "superar 63 días."
            );
        }

        // =============================================
        // 4. CONVERTIR FECHAS A GUATEMALA
        // =============================================

        OffsetDateTime desde =
                fechaDesde
                        .atStartOfDay(ZONA_GUATEMALA)
                        .toOffsetDateTime();

        // =============================================
        // FIN EXCLUSIVO
        // =============================================
        //
        // Si el usuario consulta el 21 de octubre,
        // incluimos todo ese día hasta antes de
        // las 00:00 del 22 de octubre.
        //
        // =============================================

        OffsetDateTime hasta =
                fechaHasta
                        .plusDays(1)
                        .atStartOfDay(ZONA_GUATEMALA)
                        .toOffsetDateTime();

        // =============================================
        // 5. CONSULTAR POSTGRESQL
        // =============================================
        //
        // El Repository:
        //
        // - Filtra por el Médico autenticado.
        // - Excluye Pendiente.
        // - Excluye Pendiente de Pago.
        // - Excluye No Asistió.
        // - Excluye Cancelada.
        // - Busca citas dentro del período.
        //
        // =============================================

        List<Cita> citas =
                citaRepository
                        .buscarCitasParaAgendaMedica(
                                idMedico,
                                desde,
                                hasta
                        );

        // =============================================
        // 6. CONVERTIR A RESPUESTA SEGURA
        // =============================================

        return citas.stream()
                .map(this::convertirCita)
                .toList();
    }

    // =====================================================
    // CONVERTIR CITA A ELEMENTO DEL CALENDARIO
    // =====================================================

    private CitaCalendario convertirCita(
            Cita cita
    ) {

        // =============================================
        // NOMBRE DEL PACIENTE
        // =============================================

        String nombrePaciente =
                cita.getPaciente()
                        .getNombreCompleto();

        // =============================================
        // ESTADO ACTUAL
        // =============================================

        String estado =
                cita.getEstadoCita()
                        .getNombre();

        // =============================================
        // TÍTULO DEL CALENDARIO
        // =============================================

        String titulo =
                "Consulta - " + nombrePaciente;

        // =============================================
        // DESCRIPCIÓN DE LA CITA
        // =============================================

        String descripcion =
                "Cita médica del paciente "
                        + nombrePaciente
                        + ". Estado: "
                        + estado
                        + ".";

        // =============================================
        // DEVOLVER REGISTRO
        // =============================================

        return new CitaCalendario(

                cita.getId(),

                "CITA",

                titulo,

                descripcion,

                nombrePaciente,

                estado,

                cita.getFechaHoraCita(),

                cita.getFechaHoraFin(),

                obtenerColorEstado(estado)

        );
    }

    // =====================================================
    // ASIGNAR COLOR SEGÚN ESTADO DE LA CITA
    // =====================================================
    //
    // Los eventos personales del CU-16 tendrán
    // siempre el color violeta #8B5CF6.
    //
    // Las citas utilizan colores diferentes
    // según su estado operativo.
    //
    // =====================================================

    private String obtenerColorEstado(
            String estado
    ) {

        if (estado == null) {

            return "#64748B";
        }

        String normalizado =
                Normalizer.normalize(
                        estado.trim()
                                .toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                ).replaceAll("\\p{M}", "");

        return switch (normalizado) {

            case "pagada",
                 "confirmada" -> "#2563EB";

            case "paciente presente" -> "#16A34A";

            case "signos vitales" -> "#D97706";

            case "en espera",
                 "en espera de consulta" -> "#EA580C";

            case "consulta medica",
                 "en consulta medica" -> "#0891B2";

            case "atencion finalizada" -> "#15803D";

            default -> "#64748B";
        };
    }
}
