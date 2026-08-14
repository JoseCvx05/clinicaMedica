package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.CitaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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


    public CitaPacienteService(
            UsuarioActualService usuarioActualService,
            CitaRepository citaRepository
    ) {

        this.usuarioActualService =
                usuarioActualService;

        this.citaRepository =
                citaRepository;
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

                cita.getFechaHoraCita(),

                cita.getEstadoCita()
                        .getNombre()
        );
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