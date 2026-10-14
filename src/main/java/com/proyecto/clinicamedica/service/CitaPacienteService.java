
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.CitaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: CITAS DEL PACIENTE
 * =========================================================
 *
 * Consulta únicamente las citas pertenecientes al
 * paciente autenticado.
 *
 * =========================================================
 */
@Service
public class CitaPacienteService {


    private final UsuarioActualService usuarioActualService;

    private final CitaRepository citaRepository;

    // =====================================================
    // ZONA HORARIA DE LA CLÍNICA
    // =====================================================

    private final ZoneId zonaHoraria;


    public CitaPacienteService(
            UsuarioActualService usuarioActualService,
            CitaRepository citaRepository,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.usuarioActualService =
                usuarioActualService;

        this.citaRepository =
                citaRepository;

        this.zonaHoraria =
                ZoneId.of(zonaHoraria);
    }


    // =====================================================
    // LISTAR MIS CITAS
    // =====================================================

    @Transactional(readOnly = true)
    public List<CitaPacienteResumen> listarMisCitas() {

        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        return citaRepository
                .buscarCitasDelPaciente(
                        paciente.getId()
                )
                .stream()
                .map(this::convertirResumen)
                .toList();
    }


    // =====================================================
    // CONVERTIR
    // =====================================================

    private CitaPacienteResumen convertirResumen(
            Cita cita
    ) {

        return new CitaPacienteResumen(

                cita.getId(),

                cita.getMedico()
                        .getNombreCompleto(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getSucursal()
                        .getNombre(),

                // =========================================
                // FECHA Y HORA LOCAL DE GUATEMALA
                // =========================================
                // Mantiene el mismo instante almacenado,
                // pero presenta la hora en la zona
                // horaria configurada para la clínica.

                convertirAZonaHoraria(
                        cita.getFechaHoraCita()
                ),

                cita.getEstadoCita()
                        .getNombre()
        );
    }


    // =====================================================
    // CONVERTIR FECHA A ZONA HORARIA DE LA CLÍNICA
    // =====================================================

    private OffsetDateTime convertirAZonaHoraria(
            OffsetDateTime fechaHora
    ) {

        if (fechaHora == null) {
            return null;
        }

        return fechaHora
                .atZoneSameInstant(zonaHoraria)
                .toOffsetDateTime();
    }


    // =====================================================
    // DTO INTERNO
    // =====================================================

    public record CitaPacienteResumen(

            Integer id,

            String medico,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHora,

            String estado

    ) {
    }
}
