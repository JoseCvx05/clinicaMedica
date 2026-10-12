
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.AutorizacionMedicamentoControlado;
import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import com.proyecto.clinicamedica.repository.AutorizacionMedicamentoControladoRepository;
import com.proyecto.clinicamedica.repository.DetalleRecetaMedicaRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * =========================================================
 * SERVICE: CONSULTA DE AUTORIZACIONES MEDICAS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Prepara las recetas con medicamentos controlados
 * para mostrarlas al medico responsable.
 *
 * SEGURIDAD:
 *
 * - Solo permite usuarios con rol MEDICO.
 * - Valida usuario, rol y sucursal activos.
 * - Busca recetas del medico autenticado.
 * - No muestra recetas de otras sucursales.
 * - No permite registrar autorizaciones.
 * - No modifica el CU-08.
 *
 * IMPORTANTE:
 *
 * La autorizacion real se registra mediante
 * AutorizacionMedicamentoControladoService.
 *
 * =========================================================
 */
@Service
public class MedicoAutorizacionesConsultaService {

    private static final long DIAS_VIGENCIA = 7;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioActualService usuarioActualService;

    private final RecetaMedicaRepository recetaRepository;

    private final DetalleRecetaMedicaRepository
            detalleRecetaRepository;

    private final AutorizacionMedicamentoControladoRepository
            autorizacionRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MedicoAutorizacionesConsultaService(
            UsuarioActualService usuarioActualService,
            RecetaMedicaRepository recetaRepository,
            DetalleRecetaMedicaRepository detalleRecetaRepository,
            AutorizacionMedicamentoControladoRepository autorizacionRepository
    ) {

        this.usuarioActualService = usuarioActualService;
        this.recetaRepository = recetaRepository;
        this.detalleRecetaRepository = detalleRecetaRepository;
        this.autorizacionRepository = autorizacionRepository;
    }


    // =====================================================
    // DATOS PARA MOSTRAR CADA MEDICAMENTO
    // =====================================================

    public record MedicamentoAutorizacion(

            Integer idDetalleReceta,

            Integer idMedicamento,

            String nombre,

            Integer cantidadPrescrita,

            boolean autorizado,

            boolean puedeAutorizar,

            String estado

    ) {
    }


    // =====================================================
    // DATOS PARA MOSTRAR CADA RECETA
    // =====================================================

    public record RecetaAutorizacion(

            Integer idReceta,

            Integer idConsulta,

            OffsetDateTime fechaEmision,

            boolean vigente,

            long diasTranscurridos,

            String notas,

            int pendientes,

            int autorizados,

            List<MedicamentoAutorizacion> medicamentos

    ) {
    }


    // =====================================================
    // CONSULTAR RECETAS DEL MEDICO AUTENTICADO
    // =====================================================

    @Transactional(readOnly = true)
    public List<RecetaAutorizacion> consultarMisRecetas() {

        // -------------------------------------------------
        // VALIDAR MEDICO ACTUAL
        // -------------------------------------------------

        Usuario medico = obtenerMedicoAutorizado();


        // -------------------------------------------------
        // CONSULTAR SUS RECETAS
        // -------------------------------------------------

        List<RecetaMedica> recetas =
                recetaRepository
                        .findByMedico_IdOrderByFechaEmisionDesc(
                                medico.getId()
                        );

        List<RecetaAutorizacion> resultado =
                new ArrayList<>();

        Instant ahora = Instant.now();


        for (RecetaMedica receta : recetas) {

            if (receta == null
                    || receta.getId() == null
                    || !receta.estaActiva()) {

                continue;
            }


            // ---------------------------------------------
            // PROTEGER SUCURSAL
            // ---------------------------------------------

            Cita cita = receta.getCita();

            if (cita == null
                    || cita.getSucursal() == null
                    || cita.getSucursal().getId() == null
                    || !Objects.equals(
                    cita.getSucursal().getId(),
                    medico.getSucursal().getId()
            )) {

                continue;
            }


            // ---------------------------------------------
            // VALIDAR VIGENCIA
            // ---------------------------------------------

            boolean fechaValida = false;

            boolean vigente = false;

            long diasTranscurridos = 0;

            OffsetDateTime fechaEmision =
                    receta.getFechaEmision();

            if (fechaEmision != null) {

                Instant emision =
                        fechaEmision.toInstant();

                if (!emision.isAfter(ahora)) {

                    fechaValida = true;

                    diasTranscurridos =
                            ChronoUnit.DAYS.between(
                                    emision,
                                    ahora
                            );

                    vigente =
                            !ahora.isAfter(
                                    emision.plus(
                                            DIAS_VIGENCIA,
                                            ChronoUnit.DAYS
                                    )
                            );
                }
            }


            // ---------------------------------------------
            // CONSULTA FINALIZADA
            // ---------------------------------------------

            boolean consultaFinalizada =
                    receta.getConsultaMedica() != null
                            && receta.getConsultaMedica()
                            .getEstadoConsulta()
                            == EstadoConsultaMedica.FINALIZADA;


            // ---------------------------------------------
            // FIRMA REGISTRADA
            // ---------------------------------------------

            boolean firmaPresente =
                    receta.getFirmaDigitalHash() != null
                            && !receta.getFirmaDigitalHash()
                            .isBlank();


            // ---------------------------------------------
            // AUTORIZACIONES YA REGISTRADAS
            // ---------------------------------------------

            List<AutorizacionMedicamentoControlado>
                    autorizaciones =

                    autorizacionRepository
                            .findByDetalleRecetaMedica_RecetaMedica_IdOrderByIdAsc(
                                    receta.getId()
                            );

            Set<Integer> detallesAutorizados =
                    new HashSet<>();

            for (AutorizacionMedicamentoControlado autorizacion
                    : autorizaciones) {

                if (autorizacion.getDetalleRecetaMedica()
                        != null) {

                    detallesAutorizados.add(
                            autorizacion
                                    .getDetalleRecetaMedica()
                                    .getId()
                    );
                }
            }


            // ---------------------------------------------
            // MEDICAMENTOS DE LA RECETA
            // ---------------------------------------------

            List<DetalleRecetaMedica> detalles =
                    detalleRecetaRepository
                            .findByRecetaMedica_IdOrderByIdAsc(
                                    receta.getId()
                            );

            List<MedicamentoAutorizacion> medicamentos =
                    new ArrayList<>();

            int pendientes = 0;

            int autorizados = 0;


            for (DetalleRecetaMedica detalle : detalles) {

                if (detalle == null
                        || detalle.getId() == null) {

                    continue;
                }

                Medicamento medicamento =
                        detalle.getMedicamento();


                // -----------------------------------------
                // SOLO MEDICAMENTOS CONTROLADOS
                // -----------------------------------------

                if (medicamento == null
                        || !medicamento.esControlado()) {

                    continue;
                }


                boolean autorizado =
                        detallesAutorizados.contains(
                                detalle.getId()
                        );

                boolean medicamentoActivo =
                        medicamento.estaActivo();

                boolean cantidadValida =
                        detalle.getCantidad() != null
                                && detalle.getCantidad() > 0;

                boolean puedeAutorizar =
                        fechaValida
                                && vigente
                                && consultaFinalizada
                                && firmaPresente
                                && medicamentoActivo
                                && cantidadValida
                                && !autorizado;


                // -----------------------------------------
                // ESTADO PARA LA PANTALLA
                // -----------------------------------------

                String estado;

                if (autorizado) {

                    estado = "Autorizado";

                    autorizados++;

                } else if (!fechaValida) {

                    estado = "Fecha de emisión inválida";

                } else if (!vigente) {

                    estado = "Receta vencida";

                } else if (!consultaFinalizada) {

                    estado = "Consulta no finalizada";

                } else if (!firmaPresente) {

                    estado = "Sin firma digital";

                } else if (!medicamentoActivo) {

                    estado = "Medicamento inactivo";

                } else if (!cantidadValida) {

                    estado = "Cantidad inválida";

                } else {

                    estado = "Pendiente de autorización";

                    pendientes++;
                }


                // -----------------------------------------
                // AGREGAR MEDICAMENTO
                // -----------------------------------------

                medicamentos.add(
                        new MedicamentoAutorizacion(

                                detalle.getId(),

                                medicamento.getId(),

                                detalle.getNombreMedicamento(),

                                detalle.getCantidad(),

                                autorizado,

                                puedeAutorizar,

                                estado
                        )
                );
            }


            // ---------------------------------------------
            // OMITIR RECETAS SIN CONTROLADOS
            // ---------------------------------------------

            if (medicamentos.isEmpty()) {

                continue;
            }


            // ---------------------------------------------
            // CONSULTA ASOCIADA
            // ---------------------------------------------

            Integer idConsulta =
                    receta.getConsultaMedica() == null
                            ? null
                            : receta.getConsultaMedica()
                            .getId();


            // ---------------------------------------------
            // RESULTADO DE LA RECETA
            // ---------------------------------------------

            resultado.add(
                    new RecetaAutorizacion(

                            receta.getId(),

                            idConsulta,

                            fechaEmision,

                            vigente,

                            diasTranscurridos,

                            receta.getNotas(),

                            pendientes,

                            autorizados,

                            List.copyOf(medicamentos)
                    )
            );
        }

        return List.copyOf(resultado);
    }


    // =====================================================
    // VALIDAR MEDICO AUTENTICADO
    // =====================================================

    private Usuario obtenerMedicoAutorizado() {

        Usuario medico =
                usuarioActualService
                        .obtenerUsuarioActual();

        if (medico == null
                || medico.getId() == null
                || !Boolean.TRUE.equals(
                medico.getActivo()
        )
                || medico.getRol() == null
                || !Boolean.TRUE.equals(
                medico.getRol().getActivo()
        )
                || medico.getSucursal() == null
                || medico.getSucursal().getId() == null
                || !Boolean.TRUE.equals(
                medico.getSucursal().getActivo()
        )) {

            throw new AccessDeniedException(
                    "No tiene permisos para consultar las autorizaciones médicas."
            );
        }


        String nombreRol =
                medico.getRol().getNombre();

        if (nombreRol == null) {

            throw new AccessDeniedException(
                    "El médico no tiene un rol válido."
            );
        }


        String rolNormalizado =
                Normalizer.normalize(
                                nombreRol.trim(),
                                Normalizer.Form.NFD
                        )
                        .replaceAll("\\p{M}+", "")
                        .toUpperCase(Locale.ROOT);


        if (!"MEDICO".equals(rolNormalizado)) {

            throw new AccessDeniedException(
                    "Solo un médico autorizado puede consultar estas recetas."
            );
        }

        return medico;
    }
}
