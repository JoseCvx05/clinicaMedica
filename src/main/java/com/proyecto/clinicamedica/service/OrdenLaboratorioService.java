package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.entity.ExamenLaboratorio;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.ConsultaMedicaRepository;
import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.ExamenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.OrdenLaboratorioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;


/**
 * =========================================================
 * SERVICIO: ORDEN DE LABORATORIO
 * =========================================================
 *
 * CU-08 - FA01.
 *
 * Responsabilidades:
 *
 * - Validar Médico autenticado.
 * - Validar cita perteneciente al Médico.
 * - Validar cita pendiente de cierre.
 * - Validar consulta FINALIZADA.
 * - Validar firma de la consulta.
 * - Listar exámenes activos.
 * - Validar exámenes enviados desde HTML.
 * - Evitar exámenes duplicados.
 * - Guardar cabecera de orden.
 * - Guardar detalles con snapshot histórico.
 * - Firmar digitalmente la orden.
 * - Registrar auditoría.
 *
 * =========================================================
 */
@Service
public class OrdenLaboratorioService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";


    /**
     * IMPORTANTE:
     *
     * En el modelo actual:
     *
     * Cita = Consulta Médica
     * ConsultaMedica = Finalizada
     *
     * representa:
     *
     * Evaluados — Pendiente de cierre.
     */
    private static final String ESTADO_CITA_EVALUADA =
            "Consulta Médica";


    private static final int MAX_OBSERVACIONES =
            5000;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository
            citaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final ExamenLaboratorioRepository
            examenLaboratorioRepository;

    private final OrdenLaboratorioRepository
            ordenLaboratorioRepository;

    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final FirmaDigitalMedicaService
            firmaDigitalMedicaService;

    private final AuditoriaService
            auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public OrdenLaboratorioService(

            CitaRepository citaRepository,

            ConsultaMedicaRepository consultaMedicaRepository,

            ExamenLaboratorioRepository examenLaboratorioRepository,

            OrdenLaboratorioRepository ordenLaboratorioRepository,

            DetalleOrdenLaboratorioRepository
                    detalleOrdenLaboratorioRepository,

            UsuarioActualService usuarioActualService,

            FirmaDigitalMedicaService
                    firmaDigitalMedicaService,

            AuditoriaService auditoriaService
    ) {

        this.citaRepository =
                citaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.examenLaboratorioRepository =
                examenLaboratorioRepository;

        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.firmaDigitalMedicaService =
                firmaDigitalMedicaService;

        this.auditoriaService =
                auditoriaService;
    }


    // =====================================================
    // PREPARAR FORMULARIO
    // =====================================================

    /**
     * Se ejecutará antes de mostrar la pantalla
     * "Generar orden de laboratorio".
     *
     * No modifica ninguna tabla.
     */
    @Transactional(
            readOnly = true
    )
    public PreparacionOrden prepararOrden(
            Integer idCita
    ) {

        validarId(
                idCita,
                "cita"
        );


        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                validarMedicoActual();


        // =================================================
        // CONSULTA DEL MÉDICO
        // =================================================

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .buscarPorCitaYMedico(

                                idCita,

                                medico.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La consulta médica no existe "
                                                        + "o no pertenece al Médico autenticado."
                                        )
                        );


        Cita cita =
                consulta.getCita();


        // =================================================
        // VALIDAR CONTEXTO
        // =================================================

        validarContextoOrden(
                cita,
                consulta,
                medico
        );


        // =================================================
        // EXÁMENES DISPONIBLES
        // =================================================

        List<ExamenLaboratorio> examenes =
                examenLaboratorioRepository
                        .findByActivoTrueOrderByNombreAsc();


        List<ExamenDisponible> disponibles =
                new ArrayList<>();


        for (ExamenLaboratorio examen : examenes) {

            if (examen == null) {

                continue;
            }


            /*
             * No confiamos solamente en activo=true.
             *
             * También verificamos que el laboratorio
             * asociado continúe activo.
             */
            if (!examen.estaActivo()) {

                continue;
            }


            if (examen.getLaboratorio() == null
                    || !examen
                    .getLaboratorio()
                    .estaActivo()) {

                continue;
            }


            disponibles.add(
                    new ExamenDisponible(

                            examen.getId(),

                            examen
                                    .getLaboratorio()
                                    .getId(),

                            examen
                                    .getLaboratorio()
                                    .getNombre(),

                            examen.getNombre(),

                            examen.getDescripcion(),

                            examen.getPrecio(),

                            examen.getRangoReferencia(),

                            examen.getUnidad()
                    )
            );
        }


        return new PreparacionOrden(

                cita.getId(),

                consulta.getId(),

                List.copyOf(
                        disponibles
                )
        );
    }


    // =====================================================
// CREAR ORDEN
// COMPATIBILIDAD CU-08
// =====================================================
//
// El flujo anterior continúa funcionando.
// Si no recibe ordenExterna, siempre será interna.
//
// =====================================================

    @Transactional
    public ResultadoOrden crearOrden(

            Integer idCita,

            List<Integer> idsExamenes,

            String observaciones
    ) {

        return crearOrden(

                idCita,

                idsExamenes,

                observaciones,

                false
        );
    }


// =====================================================
// CREAR ORDEN
// CU-08 + CU-09 FA01
// =====================================================

    @Transactional
    public ResultadoOrden crearOrden(

            Integer idCita,

            List<Integer> idsExamenes,

            String observaciones,

            boolean ordenExterna
    ) {

        validarId(
                idCita,
                "cita"
        );


        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                validarMedicoActual();


        // =================================================
        // BLOQUEAR CITA
        // =================================================
        //
        // Evita que simultáneamente:
        //
        // - se finalice la atención;
        // - se genere una orden;
        // - cambie el estado clínico.
        //
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaOperacionMedicaConBloqueo(

                                idCita,

                                medico.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La cita seleccionada no existe "
                                                        + "o no está asignada al Médico autenticado."
                                        )
                        );


        // =================================================
        // CONSULTA CON BLOQUEO
        // =================================================

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .buscarPorCitaYMedicoConBloqueo(

                                idCita,

                                medico.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La consulta médica no existe "
                                                        + "o no pertenece al Médico autenticado."
                                        )
                        );


        // =================================================
        // VALIDAR CONTEXTO CLÍNICO
        // =================================================

        validarContextoOrden(
                cita,
                consulta,
                medico
        );


        // =================================================
        // OBSERVACIONES
        // =================================================

        String observacionesLimpias =
                limpiarObservaciones(
                        observaciones
                );


        // =================================================
        // EXÁMENES
        // =================================================

        List<ExamenLaboratorio> examenes =
                obtenerExamenesValidos(
                        idsExamenes
                );


        // =================================================
        // CREAR CABECERA
        // =================================================

        OrdenLaboratorio orden =
                new OrdenLaboratorio();


        orden.setCita(
                cita
        );


        orden.setConsultaMedica(
                consulta
        );


        orden.setMedico(
                medico
        );


        orden.setObservaciones(
                observacionesLimpias
        );


// =================================================
// CU-09 - FA01
// TIPO DE ORDEN
// =================================================

        orden.setOrdenExterna(
                ordenExterna
        );


        orden.setEstado(
                OrdenLaboratorio
                        .EstadoOrdenLaboratorio
                        .PENDIENTE
        );


        /*
         * No llamamos orden.validar() todavía porque
         * numeroOrden se genera automáticamente en
         * @PrePersist.
         */
        orden =
                ordenLaboratorioRepository
                        .saveAndFlush(
                                orden
                        );


        if (orden.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible registrar la orden de laboratorio."
            );
        }


        if (orden.getNumeroOrden() == null
                || orden.getNumeroOrden().isBlank()) {

            throw new IllegalStateException(
                    "No fue posible generar el número de la orden de laboratorio."
            );
        }


        // =================================================
        // CREAR DETALLES
        // =================================================

        List<DetalleOrdenLaboratorio> detalles =
                new ArrayList<>();


        for (ExamenLaboratorio examen : examenes) {

            DetalleOrdenLaboratorio detalle =
                    new DetalleOrdenLaboratorio(

                            orden,

                            examen
                    );


            /*
             * Defensa de dominio antes de persistir.
             */
            detalle.validar();


            detalles.add(
                    detalle
            );
        }


        // =================================================
        // GUARDAR DETALLES
        // =================================================

        detalleOrdenLaboratorioRepository
                .saveAll(
                        detalles
                );


        detalleOrdenLaboratorioRepository
                .flush();


        // =================================================
        // VERIFICACIÓN DE PERSISTENCIA
        // =================================================

        long cantidadGuardada =
                detalleOrdenLaboratorioRepository
                        .countByOrdenLaboratorio_Id(
                                orden.getId()
                        );


        if (cantidadGuardada
                != detalles.size()) {

            throw new IllegalStateException(
                    "No fue posible guardar correctamente "
                            + "todos los exámenes de la orden."
            );
        }


        if (cantidadGuardada <= 0) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe contener "
                            + "al menos un examen."
            );
        }


        // =================================================
        // RNF-031
        // CONTENIDO CANÓNICO
        // =================================================

        String contenidoCanonico =
                construirContenidoCanonicoV2(

                        orden,

                        detalles
                );


        // =================================================
        // RNF-031
        // FIRMA DIGITAL
        // =================================================

        FirmaDocumentoClinico firma =
                firmaDigitalMedicaService
                        .firmarDocumento(

                                medico,

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .ORDEN_LABORATORIO,

                                orden.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


        if (firma == null
                || firma.getHashContenido() == null
                || firma.getHashContenido().isBlank()) {

            throw new IllegalStateException(
                    "No fue posible generar la firma digital "
                            + "de la orden de laboratorio."
            );
        }


        // =================================================
        // ASOCIAR HASH
        // =================================================

        orden.setFirmaDigitalHash(
                firma.getHashContenido()
        );


        orden =
                ordenLaboratorioRepository
                        .saveAndFlush(
                                orden
                        );


        // =================================================
        // COMPROBAR HASH
        // =================================================

        if (orden.getFirmaDigitalHash() == null
                || !firma
                .getHashContenido()
                .equalsIgnoreCase(
                        orden.getFirmaDigitalHash()
                )) {

            throw new IllegalStateException(
                    "No fue posible asociar correctamente "
                            + "la firma digital con la orden de laboratorio."
            );
        }


        // =================================================
        // VERIFICAR FIRMA
        // =================================================

        boolean firmaValida =
                firmaDigitalMedicaService
                        .verificarDocumento(

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .ORDEN_LABORATORIO,

                                orden.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


        if (!firmaValida) {

            throw new IllegalStateException(
                    "La firma digital de la orden de laboratorio "
                            + "no pudo ser verificada."
            );
        }


        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoriaCreacion(

                orden,

                detalles,

                medico
        );


        // =================================================
        // MENSAJE CU-08 FA01
        // =================================================

        List<String> nombresExamenes =
                detalles
                        .stream()

                        .map(
                                DetalleOrdenLaboratorio
                                        ::getNombreExamen
                        )

                        .sorted(
                                String.CASE_INSENSITIVE_ORDER
                        )

                        .toList();


        String listaExamenes =
                String.join(
                        ", ",
                        nombresExamenes
                );


        String mensaje;


        if (orden.isOrdenExterna()) {

            mensaje =
                    "Orden de laboratorio externa generada exitosamente. "
                            + "Número de orden: "
                            + orden.getNumeroOrden()
                            + ". Exámenes: "
                            + listaExamenes
                            + ". El paciente realizará los exámenes "
                            + "en un laboratorio externo.";

        } else {

            /*
             * Mensaje original CU-08.
             * No lo modificamos.
             */
            mensaje =
                    "Orden de laboratorio generada exitosamente. "
                            + "Número de orden: "
                            + orden.getNumeroOrden()
                            + ". Exámenes: "
                            + listaExamenes
                            + ". El paciente debe dirigirse "
                            + "al área de laboratorio.";
        }


        return new ResultadoOrden(

                orden.getId(),

                orden.getNumeroOrden(),

                List.copyOf(
                        nombresExamenes
                ),

                mensaje
        );
    }


    // =====================================================
    // OBTENER EXÁMENES VÁLIDOS
    // =====================================================

    private List<ExamenLaboratorio>
    obtenerExamenesValidos(
            List<Integer> idsExamenes
    ) {

        if (idsExamenes == null
                || idsExamenes.isEmpty()) {

            throw new IllegalStateException(
                    "Debe seleccionar al menos un examen de laboratorio."
            );
        }


        // =================================================
        // EVITAR DUPLICADOS
        // =================================================

        Set<Integer> idsUnicos =
                new HashSet<>();


        List<ExamenLaboratorio> examenes =
                new ArrayList<>();


        for (Integer idExamen : idsExamenes) {

            validarId(
                    idExamen,
                    "examen de laboratorio"
            );


            if (!idsUnicos.add(
                    idExamen
            )) {

                throw new IllegalStateException(
                        "No se puede seleccionar el mismo "
                                + "examen de laboratorio más de una vez."
                );
            }


            // =============================================
            // RECARGAR DESDE POSTGRESQL
            // =============================================
            //
            // Nunca confiamos en nombre/precio/rango
            // enviados desde HTML.
            //
            // =============================================

            ExamenLaboratorio examen =
                    examenLaboratorioRepository
                            .findByIdAndActivoTrue(
                                    idExamen
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Uno de los exámenes seleccionados "
                                                            + "no existe o se encuentra inactivo."
                                            )
                            );


            examen.validar();


            if (!examen.estaActivo()) {

                throw new IllegalStateException(
                        "No se pueden solicitar exámenes inactivos."
                );
            }


            if (examen.getLaboratorio() == null
                    || !examen
                    .getLaboratorio()
                    .estaActivo()) {

                throw new IllegalStateException(
                        "El laboratorio asociado a uno de los "
                                + "exámenes se encuentra inactivo."
                );
            }


            examenes.add(
                    examen
            );
        }


        // =================================================
        // ORDEN DETERMINISTA
        // =================================================
        //
        // También ayuda a generar siempre el mismo
        // contenido para la firma digital.
        //
        // =================================================

        examenes.sort(

                Comparator
                        .comparing(
                                ExamenLaboratorio::getNombre,
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                ExamenLaboratorio::getId
                        )
        );


        return examenes;
    }


    // =====================================================
    // VALIDAR CONTEXTO DE LA ORDEN
    // =====================================================

    private void validarContextoOrden(

            Cita cita,

            ConsultaMedica consulta,

            Usuario medico
    ) {

        if (cita == null
                || cita.getId() == null) {

            throw new IllegalStateException(
                    "La cita seleccionada no es válida."
            );
        }


        if (consulta == null
                || consulta.getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no es válida."
            );
        }


        // =================================================
        // MISMA CITA
        // =================================================

        if (consulta.getCita() == null
                || consulta.getCita().getId() == null
                || !Objects.equals(

                consulta.getCita().getId(),

                cita.getId()
        )) {

            throw new IllegalStateException(
                    "La consulta médica no corresponde "
                            + "a la cita seleccionada."
            );
        }


        // =================================================
        // CITA EN EVALUADOS
        // =================================================

        if (cita.getEstadoCita() == null
                || cita.getEstadoCita().getNombre() == null
                || !ESTADO_CITA_EVALUADA
                .equalsIgnoreCase(

                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "Solo una cita evaluada y pendiente de cierre "
                            + "puede generar órdenes de laboratorio."
            );
        }


        // =================================================
        // CONSULTA FINALIZADA
        // =================================================

        if (consulta.getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada "
                            + "antes de generar una orden de laboratorio."
            );
        }


        // =================================================
        // MISMO MÉDICO
        // =================================================

        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null
                || !Objects.equals(

                consulta.getMedico().getId(),

                medico.getId()
        )) {

            throw new IllegalStateException(
                    "La consulta médica no pertenece "
                            + "al Médico autenticado."
            );
        }


        // =================================================
        // MÉDICO ASIGNADO A LA CITA
        // =================================================

        if (cita.getMedico() == null
                || cita.getMedico().getId() == null
                || !Objects.equals(

                cita.getMedico().getId(),

                medico.getId()
        )) {

            throw new IllegalStateException(
                    "La cita no está asignada "
                            + "al Médico autenticado."
            );
        }


        // =================================================
        // MISMA SUCURSAL
        // =================================================

        if (medico.getSucursal() == null
                || medico.getSucursal().getId() == null
                || cita.getSucursal() == null
                || cita.getSucursal().getId() == null
                || !Objects.equals(

                medico.getSucursal().getId(),

                cita.getSucursal().getId()
        )) {

            throw new IllegalStateException(
                    "La cita no pertenece a la sucursal "
                            + "del Médico autenticado."
            );
        }


        // =================================================
        // RNF-031
        // CONSULTA DEBE ESTAR FIRMADA
        // =================================================

        if (consulta.getFirmaDigitalHash() == null
                || consulta.getFirmaDigitalHash().isBlank()) {

            throw new IllegalStateException(
                    "La consulta médica debe poseer una firma "
                            + "digital válida antes de generar "
                            + "una orden de laboratorio."
            );
        }


        FirmaDocumentoClinico firmaConsulta =
                firmaDigitalMedicaService
                        .buscarFirma(

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .CONSULTA_MEDICA,

                                consulta.getId()
                                        .longValue()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No existe el registro de firma "
                                                        + "digital de la consulta médica."
                                        )
                        );


        if (firmaConsulta.getHashContenido() == null
                || !firmaConsulta
                .getHashContenido()
                .equalsIgnoreCase(
                        consulta.getFirmaDigitalHash()
                )) {

            throw new IllegalStateException(
                    "La firma digital registrada para la consulta "
                            + "no coincide con el documento clínico."
            );
        }
    }


    // =====================================================
    // VALIDAR MÉDICO ACTUAL
    // =====================================================

    private Usuario validarMedicoActual() {

        Usuario usuario =
                usuarioActualService
                        .obtenerUsuarioActual();


        if (usuario == null
                || !Boolean.TRUE.equals(
                usuario.getActivo()
        )
                || usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || !ROL_MEDICO
                .equalsIgnoreCase(

                        usuario.getRol()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para realizar operaciones médicas."
            );
        }


        return usuario;
    }


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    private String limpiarObservaciones(
            String observaciones
    ) {

        if (observaciones == null
                || observaciones.isBlank()) {

            return null;
        }


        String limpio =
                observaciones.trim();


        if (limpio.length()
                > MAX_OBSERVACIONES) {

            throw new IllegalStateException(
                    "Las observaciones no pueden superar "
                            + "los 5000 caracteres."
            );
        }


        return limpio;
    }


    // =====================================================
    // CONTENIDO CANÓNICO RNF-031
    // =====================================================

    private String construirContenidoCanonico(

            OrdenLaboratorio orden,

            List<DetalleOrdenLaboratorio> detalles
    ) {

        if (orden == null
                || orden.getId() == null) {

            throw new IllegalArgumentException(
                    "La orden de laboratorio debe estar guardada "
                            + "antes de generar su firma digital."
            );
        }


        if (detalles == null
                || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La orden de laboratorio debe contener "
                            + "al menos un examen."
            );
        }


        List<DetalleOrdenLaboratorio> ordenados =
                detalles
                        .stream()

                        .sorted(
                                Comparator.comparing(
                                        detalle ->
                                                detalle
                                                        .getExamenLaboratorio()
                                                        .getId()
                                )
                        )

                        .toList();


        StringBuilder contenido =
                new StringBuilder();


        contenido
                .append(
                        "TIPO=ORDEN_LABORATORIO\n"
                )

                .append(
                        "ID_ORDEN="
                )
                .append(
                        orden.getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "NUMERO_ORDEN="
                )
                .append(
                        valorCanonico(
                                orden.getNumeroOrden()
                        )
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_CITA="
                )
                .append(
                        orden.getCita().getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_CONSULTA="
                )
                .append(
                        orden.getConsultaMedica()
                                .getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_MEDICO="
                )
                .append(
                        orden.getMedico()
                                .getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ESTADO="
                )
                .append(
                        orden.getEstado()
                                .name()
                )
                .append(
                        '\n'
                )

                .append(
                        "OBSERVACIONES="
                )
                .append(
                        valorCanonico(
                                orden.getObservaciones()
                        )
                )
                .append(
                        '\n'
                )

                .append(
                        "CANTIDAD_EXAMENES="
                )
                .append(
                        ordenados.size()
                );


        int numero =
                1;


        for (DetalleOrdenLaboratorio detalle : ordenados) {

            contenido
                    .append(
                            '\n'
                    )

                    .append(
                            "EXAMEN_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_ID="
                    )
                    .append(
                            detalle
                                    .getExamenLaboratorio()
                                    .getId()
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "EXAMEN_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_NOMBRE="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getNombreExamen()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "EXAMEN_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_PRECIO="
                    )
                    .append(
                            valorPrecio(
                                    detalle.getPrecioExamen()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "EXAMEN_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_RANGO="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getRangoReferencia()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "EXAMEN_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_UNIDAD="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getUnidad()
                            )
                    );


            numero++;
        }


        /*
         * NO incluimos:
         *
         * - firmaDigitalHash
         * - @Version
         *
         * porque ambos cambian después de firmar y harían
         * imposible verificar posteriormente el mismo
         * contenido.
         */
        return contenido.toString();
    }


    // =====================================================
    // NORMALIZACIÓN CANÓNICA
    // =====================================================
    // =====================================================
// CONTENIDO CANÓNICO RNF-031 - V2
// CU-09
// =====================================================
//
// Mantiene todo el contenido original de V1
// y protege adicionalmente si la orden es externa.
//
// =====================================================

    private String construirContenidoCanonicoV2(

            OrdenLaboratorio orden,

            List<DetalleOrdenLaboratorio> detalles
    ) {

        String contenidoV1 =
                construirContenidoCanonico(

                        orden,

                        detalles
                );


        return contenidoV1
                + "\nORDEN_EXTERNA="
                + orden.isOrdenExterna();
    }

    private String valorCanonico(
            String valor
    ) {

        if (valor == null) {

            return "";
        }


        return valor
                .trim()

                .replace(
                        "\\",
                        "\\\\"
                )

                .replace(
                        "\r\n",
                        "\n"
                )

                .replace(
                        "\r",
                        "\n"
                )

                .replace(
                        "\n",
                        "\\n"
                )

                .replace(
                        "=",
                        "\\="
                );
    }


    private String valorPrecio(
            BigDecimal valor
    ) {

        if (valor == null) {

            return "";
        }


        return valor
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }


    // =====================================================
    // AUDITORÍA
    // =====================================================

    private void registrarAuditoriaCreacion(

            OrdenLaboratorio orden,

            List<DetalleOrdenLaboratorio> detalles,

            Usuario medico
    ) {

        Map<String, Object> nuevo =
                new LinkedHashMap<>();


        nuevo.put(
                "numeroOrden",
                orden.getNumeroOrden()
        );


        nuevo.put(
                "idCita",
                orden.getCita().getId()
        );


        nuevo.put(
                "idConsulta",
                orden.getConsultaMedica()
                        .getId()
        );


        nuevo.put(
                "idMedico",
                medico.getId()
        );


        nuevo.put(
                "estado",
                orden.getEstado().name()
        );


        nuevo.put(
                "ordenExterna",
                orden.isOrdenExterna()
        );


        nuevo.put(
                "cantidadExamenes",
                detalles.size()
        );


        nuevo.put(
                "firmaDigitalHash",
                orden.getFirmaDigitalHash()
        );


        nuevo.put(
                "idsExamenes",
                detalles
                        .stream()

                        .map(
                                detalle ->
                                        detalle
                                                .getExamenLaboratorio()
                                                .getId()
                        )

                        .sorted()

                        .collect(
                                Collectors.toList()
                        )
        );


        auditoriaService.registrar(
                new RegistroAuditoria(

                        "orden_laboratorio",

                        String.valueOf(
                                orden.getId()
                        ),

                        AccionAuditoria.CREAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        null,

                        nuevo,

                        null
                )
        );
    }


    // =====================================================
    // VALIDAR ID
    // =====================================================

    private void validarId(

            Integer id,

            String nombre
    ) {

        if (id == null
                || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador de "
                            + nombre
                            + " no es válido."
            );
        }
    }


    // =====================================================
    // DTO INTERNO PARA EL FORMULARIO
    // =====================================================

    public record ExamenDisponible(

            Integer idExamen,

            Integer idLaboratorio,

            String laboratorio,

            String nombre,

            String descripcion,

            BigDecimal precio,

            String rangoReferencia,

            String unidad

    ) {
    }


    // =====================================================
    // RESULTADO DE PREPARACIÓN
    // =====================================================

    public record PreparacionOrden(

            Integer idCita,

            Integer idConsulta,

            List<ExamenDisponible> examenes

    ) {
    }


    // =====================================================
    // RESULTADO DE CREACIÓN
    // =====================================================

    public record ResultadoOrden(

            Integer idOrden,

            String numeroOrden,

            List<String> examenes,

            String mensaje

    ) {
    }
}