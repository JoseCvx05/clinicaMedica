
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Despacho;
import com.proyecto.clinicamedica.entity.DetalleDespacho;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * =========================================================
 * CU-11 - SERVICE: MEDICAMENTOS DEL PACIENTE
 * =========================================================
 *
 * Permite consultar el historial de medicamentos
 * entregados al paciente mediante Farmacia.
 *
 * SEGURIDAD:
 *
 * - Identifica al paciente autenticado.
 * - No recibe identificadores desde el navegador.
 * - Consulta únicamente despachos CONFIRMADOS.
 * - No permite consultar datos de otro paciente.
 *
 * Este servicio es exclusivamente de lectura.
 *
 * No modifica recetas, despachos ni inventario.
 *
 * =========================================================
 */
@Service
public class PacienteMedicamentosService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioActualService
            usuarioActualService;

    private final DetalleDespachoRepository
            detalleDespachoRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PacienteMedicamentosService(
            UsuarioActualService usuarioActualService,
            DetalleDespachoRepository detalleDespachoRepository
    ) {

        this.usuarioActualService =
                usuarioActualService;

        this.detalleDespachoRepository =
                detalleDespachoRepository;
    }


    // =====================================================
    // LISTAR MIS MEDICAMENTOS
    // =====================================================

    @Transactional(readOnly = true)
    public List<MedicamentoPacienteResumen>
    listarMisMedicamentos() {


        // -------------------------------------------------
        // 1. IDENTIFICAR PACIENTE AUTENTICADO
        // -------------------------------------------------

        Usuario paciente =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (paciente == null
                || paciente.getId() == null) {

            throw new IllegalStateException(
                    "No se pudo identificar al paciente autenticado."
            );
        }


        // -------------------------------------------------
        // 2. CONSULTAR MEDICAMENTOS ENTREGADOS
        // -------------------------------------------------
        //
        // El ID proviene del usuario autenticado.
        //
        // El repositorio filtra por paciente
        // y por despachos CONFIRMADOS.
        //
        // -------------------------------------------------

        List<DetalleDespacho> detalles =
                detalleDespachoRepository
                        .buscarHistorialMedicamentosPaciente(

                                paciente.getId(),

                                Despacho.EstadoDespacho.CONFIRMADO

                        );


        // -------------------------------------------------
        // 3. CONVERTIR A DTO
        // -------------------------------------------------
        //
        // Solo se devuelve la información necesaria
        // para mostrar el historial.
        //
        // -------------------------------------------------

        return detalles
                .stream()
                .map(this::convertirResumen)
                .toList();
    }


    // =====================================================
    // CONVERTIR DETALLE A RESUMEN
    // =====================================================

    private MedicamentoPacienteResumen convertirResumen(
            DetalleDespacho detalle
    ) {

        Despacho despacho =
                detalle.getDespacho();


        return new MedicamentoPacienteResumen(

                despacho.getId(),

                despacho.getRecetaMedica().getId(),

                despacho.getFechaHoraRegistro(),

                detalle.getNombreMedicamentoOriginal(),

                detalle.getNombreMedicamentoEntregado(),

                detalle.getCantidadEntregada(),

                detalle.getPrecioUnitario(),

                detalle.getSubtotal(),

                detalle.getRazonSustitucion()

        );
    }


    // =====================================================
    // DTO: HISTORIAL DEL PACIENTE
    // =====================================================
    //
    // Guarda únicamente los datos necesarios
    // para la vista de Thymeleaf.
    //
    // =====================================================

    public record MedicamentoPacienteResumen(

            Integer idDespacho,

            Integer idReceta,

            OffsetDateTime fechaDespacho,

            String medicamentoOriginal,

            String medicamentoEntregado,

            Integer cantidadEntregada,

            BigDecimal precioUnitario,

            BigDecimal subtotal,

            String razonSustitucion

    ) {
    }

}
