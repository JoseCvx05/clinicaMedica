
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.AutorizacionMedicamentoControlado;
import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * =========================================================
 * SERVICE: AUTORIZACION DE MEDICAMENTOS CONTROLADOS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 * RNF-017 - Autorizacion adicional y auditoria.
 *
 * Solo el medico tratante puede autorizar
 * medicamentos controlados de sus propias recetas.
 *
 * VALIDACIONES BACKEND:
 *
 * - Medico autenticado y activo.
 * - Rol MEDICO activo.
 * - Sucursal activa y correspondiente.
 * - Receta activa y vigente.
 * - Consulta finalizada.
 * - Firma digital registrada.
 * - Medicamento controlado y activo.
 * - Detalle perteneciente a la receta.
 * - Cantidad autorizada desde PostgreSQL.
 * - Prevencion de autorizaciones duplicadas.
 * - Auditoria en la misma transaccion.
 *
 * IMPORTANTE:
 *
 * No modifica la receta ni la consulta.
 * No descuenta inventario.
 * No registra pagos.
 *
 * =========================================================
 */
@Service
public class AutorizacionMedicamentoControladoService {

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

    private final FirmaDigitalMedicaService
            firmaDigitalMedicaService;

    private final AuditoriaService auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AutorizacionMedicamentoControladoService(
            UsuarioActualService usuarioActualService,
            RecetaMedicaRepository recetaRepository,
            DetalleRecetaMedicaRepository detalleRecetaRepository,
            AutorizacionMedicamentoControladoRepository autorizacionRepository,
            FirmaDigitalMedicaService firmaDigitalMedicaService,
            AuditoriaService auditoriaService
    ) {

        this.usuarioActualService = usuarioActualService;
        this.recetaRepository = recetaRepository;
        this.detalleRecetaRepository = detalleRecetaRepository;
        this.autorizacionRepository = autorizacionRepository;
        this.firmaDigitalMedicaService = firmaDigitalMedicaService;
        this.auditoriaService = auditoriaService;
    }


    // =====================================================
    // RESULTADO
    // =====================================================

    public record ResultadoAutorizacion(
            Integer idAutorizacion,
            Integer idReceta,
            Integer idDetalleReceta,
            Integer idMedicamento,
            Integer cantidadAutorizada,
            OffsetDateTime fechaHoraAutorizacion
    ) {
    }


    // =====================================================
    // AUTORIZAR MEDICAMENTO CONTROLADO
    // =====================================================

    @Transactional
    public ResultadoAutorizacion autorizar(
            Integer idReceta,
            Integer idDetalleReceta,
            String observaciones
    ) {

        // -------------------------------------------------
        // VALIDAR IDENTIFICADORES
        // -------------------------------------------------

        validarId(idReceta, "receta");
        validarId(idDetalleReceta, "detalle de receta");


        // -------------------------------------------------
        // MEDICO AUTENTICADO
        // -------------------------------------------------

        Usuario medico =
                obtenerMedicoAutorizado();


        // -------------------------------------------------
        // BLOQUEAR RECETA
        // -------------------------------------------------
        //
        // Serializa autorizaciones de una misma receta.
        //
        // -------------------------------------------------

        RecetaMedica receta =
                recetaRepository
                        .findWithLockById(idReceta)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "No se encontró la receta médica."
                                )
                        );


        // -------------------------------------------------
        // VALIDAR PROPIEDAD Y SUCURSAL
        // -------------------------------------------------

        validarPropiedadReceta(receta, medico);


        // -------------------------------------------------
        // VALIDAR RECETA
        // -------------------------------------------------

        validarRecetaVigente(receta);


        // -------------------------------------------------
        // VERIFICAR FIRMA REGISTRADA
        // -------------------------------------------------

        validarFirmaRegistrada(receta);


        // -------------------------------------------------
        // OBTENER DETALLE DESDE POSTGRESQL
        // -------------------------------------------------

        DetalleRecetaMedica detalle =
                detalleRecetaRepository
                        .findById(idDetalleReceta)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "No se encontró el medicamento recetado."
                                )
                        );


        // -------------------------------------------------
        // MISMA RECETA
        // -------------------------------------------------

        if (detalle.getRecetaMedica() == null
                || !Objects.equals(
                detalle.getRecetaMedica().getId(),
                receta.getId()
        )) {

            throw new AccessDeniedException(
                    "El medicamento no pertenece a la receta seleccionada."
            );
        }


        // -------------------------------------------------
        // VALIDAR MEDICAMENTO CONTROLADO
        // -------------------------------------------------

        Medicamento medicamento =
                detalle.getMedicamento();

        if (medicamento == null
                || medicamento.getId() == null
                || !medicamento.estaActivo()) {

            throw new IllegalStateException(
                    "El medicamento no existe o está inactivo."
            );
        }

        if (!medicamento.esControlado()) {

            throw new IllegalStateException(
                    "Este medicamento no requiere autorización especial."
            );
        }

        medicamento.validar();


        // -------------------------------------------------
        // CANTIDAD PRESCRITA
        // -------------------------------------------------

        if (detalle.getCantidad() == null
                || detalle.getCantidad() <= 0) {

            throw new IllegalStateException(
                    "La cantidad prescrita no es válida."
            );
        }


        // -------------------------------------------------
        // NO PERMITIR AUTORIZACIONES DUPLICADAS
        // -------------------------------------------------

        if (autorizacionRepository
                .existsByDetalleRecetaMedica_Id(
                        detalle.getId()
                )) {

            throw new IllegalStateException(
                    "Este medicamento ya tiene una autorización médica registrada."
            );
        }


        // -------------------------------------------------
        // NORMALIZAR OBSERVACIONES
        // -------------------------------------------------

        String notas =
                observaciones == null
                        ? null
                        : observaciones.trim();

        if (notas != null && notas.isBlank()) {
            notas = null;
        }

        if (notas != null && notas.length() > 1000) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // CREAR AUTORIZACION
        // -------------------------------------------------
        //
        // El servidor obtiene la cantidad prescrita.
        // No recibe una cantidad autorizada desde HTML.
        //
        // -------------------------------------------------

        AutorizacionMedicamentoControlado autorizacion =
                new AutorizacionMedicamentoControlado(
                        detalle,
                        medico,
                        notas
                );


        // -------------------------------------------------
        // GUARDAR EN POSTGRESQL
        // -------------------------------------------------

        autorizacion =
                autorizacionRepository.saveAndFlush(
                        autorizacion
                );

        if (autorizacion.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible registrar la autorización."
            );
        }


        // -------------------------------------------------
        // AUDITORIA OBLIGATORIA
        // -------------------------------------------------
        //
        // Se ejecuta en la misma transaccion.
        // Si falla, la autorizacion tambien se revierte.
        //
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(

                        "autorizacion_medicamento_controlado",

                        String.valueOf(
                                autorizacion.getId()
                        ),

                        AccionAuditoria.CREAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        null,

                        Map.of(
                                "idReceta",
                                receta.getId(),

                                "idDetalleReceta",
                                detalle.getId(),

                                "idMedicamento",
                                medicamento.getId(),

                                "idMedicoAutorizador",
                                medico.getId(),

                                "idSucursal",
                                medico.getSucursal().getId(),

                                "cantidadAutorizada",
                                autorizacion.getCantidadAutorizada()
                        ),

                        null
                )
        );


        // -------------------------------------------------
        // RESULTADO
        // -------------------------------------------------

        return new ResultadoAutorizacion(

                autorizacion.getId(),

                receta.getId(),

                detalle.getId(),

                medicamento.getId(),

                autorizacion.getCantidadAutorizada(),

                autorizacion.getFechaHoraAutorizacion()
        );
    }


    // =====================================================
    // VERIFICAR MEDICO AUTENTICADO
    // =====================================================

    private Usuario obtenerMedicoAutorizado() {

        Usuario medico =
                usuarioActualService.obtenerUsuarioActual();

        if (medico == null
                || medico.getId() == null
                || !Boolean.TRUE.equals(medico.getActivo())
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
                    "El usuario no está autorizado para aprobar medicamentos."
            );
        }

        String nombreRol =
                medico.getRol().getNombre();

        if (nombreRol == null) {

            throw new AccessDeniedException(
                    "El usuario no tiene un rol válido."
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
                    "Solo el médico tratante puede autorizar medicamentos controlados."
            );
        }

        return medico;
    }


    // =====================================================
    // VERIFICAR RECETA DEL MEDICO
    // =====================================================

    private void validarPropiedadReceta(
            RecetaMedica receta,
            Usuario medico
    ) {

        if (receta.getMedico() == null
                || !Objects.equals(
                receta.getMedico().getId(),
                medico.getId()
        )) {

            throw new AccessDeniedException(
                    "La receta no pertenece al médico autenticado."
            );
        }

        Cita cita = receta.getCita();

        if (cita == null
                || cita.getSucursal() == null
                || !Objects.equals(
                cita.getSucursal().getId(),
                medico.getSucursal().getId()
        )
                || cita.getMedico() == null
                || !Objects.equals(
                cita.getMedico().getId(),
                medico.getId()
        )) {

            throw new AccessDeniedException(
                    "La receta no corresponde al médico y sucursal autorizados."
            );
        }

        ConsultaMedica consulta =
                receta.getConsultaMedica();

        if (consulta == null
                || consulta.getCita() == null
                || !Objects.equals(
                consulta.getCita().getId(),
                cita.getId()
        )
                || consulta.getMedico() == null
                || !Objects.equals(
                consulta.getMedico().getId(),
                medico.getId()
        )) {

            throw new AccessDeniedException(
                    "La consulta médica no corresponde a la receta autorizada."
            );
        }
    }


    // =====================================================
    // VALIDAR VIGENCIA Y ESTADO
    // =====================================================

    private void validarRecetaVigente(
            RecetaMedica receta
    ) {

        if (!receta.estaActiva()) {

            throw new IllegalStateException(
                    "La receta médica no se encuentra activa."
            );
        }

        if (receta.getConsultaMedica().getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada."
            );
        }

        if (receta.getFechaEmision() == null) {

            throw new IllegalStateException(
                    "La receta no tiene fecha de emisión válida."
            );
        }

        Instant emision =
                receta.getFechaEmision().toInstant();

        Instant ahora =
                Instant.now();

        if (emision.isAfter(ahora)) {

            throw new IllegalStateException(
                    "La fecha de emisión no puede ser futura."
            );
        }

        if (ahora.isAfter(
                emision.plus(
                        DIAS_VIGENCIA,
                        ChronoUnit.DAYS
                )
        )) {

            throw new IllegalStateException(
                    "No se puede autorizar un medicamento de una receta vencida."
            );
        }
    }


    // =====================================================
    // VERIFICAR FIRMA DIGITAL REGISTRADA
    // =====================================================

    private void validarFirmaRegistrada(
            RecetaMedica receta
    ) {

        if (receta.getFirmaDigitalHash() == null
                || receta.getFirmaDigitalHash().isBlank()) {

            throw new IllegalStateException(
                    "La receta debe tener una firma digital registrada."
            );
        }

        FirmaDocumentoClinico firma =
                firmaDigitalMedicaService
                        .buscarFirma(

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .RECETA_MEDICA,

                                receta.getId().longValue()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe la firma digital de la receta."
                                )
                        );

        if (firma.getHashContenido() == null
                || !firma.getHashContenido()
                .equalsIgnoreCase(
                        receta.getFirmaDigitalHash()
                )) {

            throw new IllegalStateException(
                    "La firma digital registrada no coincide con la receta."
            );
        }
    }


    // =====================================================
    // VALIDAR IDENTIFICADORES
    // =====================================================

    private void validarId(
            Integer id,
            String nombre
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador de " + nombre + " no es válido."
            );
        }
    }
}
