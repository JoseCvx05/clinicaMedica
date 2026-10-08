package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.ConsultaMedicaFormulario;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Cie10;
import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.EstadoCita;
import com.proyecto.clinicamedica.entity.SignosVitales;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.ResultadoLaboratorio;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import com.proyecto.clinicamedica.repository.Cie10Repository;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.ConsultaMedicaRepository;
import com.proyecto.clinicamedica.repository.EstadoCitaRepository;
import com.proyecto.clinicamedica.repository.SignosVitalesRepository;
import com.proyecto.clinicamedica.repository.ResultadoLaboratorioRepository;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: CONSULTA MÉDICA
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Responsabilidades:
 *
 * - Validar Médico autenticado.
 * - Validar cita asignada al Médico.
 * - Validar signos vitales.
 * - Iniciar consulta.
 * - Cargar expediente.
 * - Guardar consulta En curso.
 * - Finalizar consulta.
 * - Aplicar RN-CU08-01.
 * - Aplicar RN-CU08-02.
 * - Aplicar FA05.
 * - Aplicar FA06.
 * - Versionar historial RNF-026.
 * - Registrar auditoría.
 * - Generar mensaje TTS.
 *
 * =========================================================
 */
@Service
public class ConsultaMedicaService {


    // =====================================================
    // ESTADOS DE CITA
    // =====================================================

    private static final String ESTADO_EN_ESPERA =
            "En Espera";

    private static final String ESTADO_CONSULTA_MEDICA =
            "Consulta Médica";

    private static final String ESTADO_NO_ASISTIO =
            "No Asistió";

    private static final String ESTADO_ATENCION_FINALIZADA =
            "Atención Finalizada";

    // =====================================================
    // ROL
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";


    // =====================================================
    // DIAGNÓSTICO
    // =====================================================

    private static final int DIAGNOSTICO_MINIMO =
            10;

    private static final int DIAGNOSTICO_MAXIMO =
            5000;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository
            citaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final SignosVitalesRepository
            signosVitalesRepository;

    private final Cie10Repository
            cie10Repository;

    private final EstadoCitaRepository
            estadoCitaRepository;

    private final CatalogoEstadoCitaService
            catalogoEstadoCitaService;

    private final UsuarioActualService
            usuarioActualService;

    private final ConsultaMedicaVersionService
            consultaMedicaVersionService;

    private final AuditoriaService
            auditoriaService;

    private final ZoneId
            zonaHoraria;


    private final FirmaDigitalMedicaService
            firmaDigitalMedicaService;

    private final ResultadoLaboratorioRepository
            resultadoLaboratorioRepository;
    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ConsultaMedicaService(

            CitaRepository citaRepository,

            ConsultaMedicaRepository consultaMedicaRepository,

            SignosVitalesRepository signosVitalesRepository,

            Cie10Repository cie10Repository,

            EstadoCitaRepository estadoCitaRepository,

            CatalogoEstadoCitaService catalogoEstadoCitaService,

            UsuarioActualService usuarioActualService,

            ConsultaMedicaVersionService consultaMedicaVersionService,

            AuditoriaService auditoriaService,

            FirmaDigitalMedicaService firmaDigitalMedicaService,

            ResultadoLaboratorioRepository
                    resultadoLaboratorioRepository,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.signosVitalesRepository =
                signosVitalesRepository;

        this.cie10Repository =
                cie10Repository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.catalogoEstadoCitaService =
                catalogoEstadoCitaService;

        this.usuarioActualService =
                usuarioActualService;

        this.consultaMedicaVersionService =
                consultaMedicaVersionService;

        this.auditoriaService =
                auditoriaService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );

        this.firmaDigitalMedicaService =
                firmaDigitalMedicaService;

        this.resultadoLaboratorioRepository =
                resultadoLaboratorioRepository;
    }


    // =====================================================
    // INICIAR CONSULTA
    // =====================================================

    @Transactional
    public InicioConsulta iniciarConsulta(
            Integer idCita
    ) {

        validarIdCita(
                idCita
        );


        Usuario medico =
                validarMedicoActual();


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


        validarEstadoCita(
                cita,
                ESTADO_EN_ESPERA,
                "La cita ya no se encuentra en estado En Espera."
        );


        // =================================================
        // PRECONDICIÓN CU-08
        // =================================================

        if (!signosVitalesRepository
                .existsByCita_Id(
                        idCita
                )) {

            throw new IllegalStateException(
                    "No es posible iniciar la consulta médica "
                            + "porque el paciente no posee signos vitales registrados."
            );
        }


        // =================================================
        // UNA CONSULTA POR CITA
        // =================================================

        if (consultaMedicaRepository
                .existsByCita_Id(
                        idCita
                )) {

            throw new IllegalStateException(
                    "La cita ya posee una consulta médica registrada."
            );
        }


        // =================================================
        // MOTIVO INICIAL
        // =================================================

        String motivoInicial =
                limpiar(
                        cita.getMotivoConsulta()
                );


        if (motivoInicial == null) {

            throw new IllegalStateException(
                    "La cita no posee un motivo de consulta válido."
            );
        }


        EstadoCita estadoConsultaMedica =
                obtenerEstadoActivo(
                        ESTADO_CONSULTA_MEDICA
                );


        OffsetDateTime ahora =
                ahora();


        // =================================================
        // CREAR CONSULTA
        // =================================================

        ConsultaMedica consulta =
                new ConsultaMedica();


        consulta.setCita(
                cita
        );


        consulta.setMedico(
                medico
        );


        consulta.setMotivoVisita(
                motivoInicial
        );


        consulta.setEstadoConsulta(
                EstadoConsultaMedica.EN_CURSO
        );


        consulta.setVersion(
                1
        );


        consulta.setFechaHoraInicio(
                ahora
        );


        consulta.setCreadoPor(
                medico
        );


        ConsultaMedica guardada =
                consultaMedicaRepository
                        .saveAndFlush(
                                consulta
                        );

        // =================================================
        // CITA → CONSULTA MÉDICA
        // =================================================

        String estadoAnterior =
                cita.getEstadoCita()
                        .getNombre();


        cita.setEstadoCita(
                estadoConsultaMedica
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                medico
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        // =================================================
        // AUDITORÍA CONSULTA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(

                        "consulta_medica",

                        String.valueOf(
                                guardada.getId()
                        ),

                        AccionAuditoria.CREAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        null,

                        crearSnapshotControl(
                                guardada
                        ),

                        null
                )
        );


        // =================================================
        // AUDITORÍA CITA
        // =================================================

        Map<String, Object> anteriorCita =
                new LinkedHashMap<>();

        anteriorCita.put(
                "estado",
                estadoAnterior
        );


        Map<String, Object> nuevaCita =
                new LinkedHashMap<>();

        nuevaCita.put(
                "estado",
                ESTADO_CONSULTA_MEDICA
        );


        auditoriaService.registrar(
                new RegistroAuditoria(

                        "cita",

                        String.valueOf(
                                cita.getId()
                        ),

                        AccionAuditoria.ACTUALIZAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        anteriorCita,

                        nuevaCita,

                        null
                )
        );


        // =================================================
        // TTS
        // =================================================

        String nombrePaciente =
                cita.getPaciente()
                        .getNombreCompleto();


        String mensajeTts =
                "Turno número "
                        + cita.getId()
                        + ". Paciente "
                        + nombrePaciente
                        + ", favor pasar a consulta médica.";


        return new InicioConsulta(

                cita.getId(),

                guardada.getId(),

                nombrePaciente,

                mensajeTts
        );
    }


    // =====================================================
    // OBTENER EXPEDIENTE
    // =====================================================

    /**
     * Recupera los datos necesarios para:
     *
     * Ver / Completar Consulta.
     */
    @Transactional(readOnly = true)
    public ExpedienteConsulta obtenerExpediente(
            Integer idCita
    ) {

        validarIdCita(
                idCita
        );


        Usuario medico =
                validarMedicoActual();


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


        validarEstadoCita(
                cita,
                ESTADO_CONSULTA_MEDICA,
                "La cita no se encuentra actualmente "
                        + "en Consulta Médica."
        );


        SignosVitales signos =
                signosVitalesRepository
                        .findByCita_Id(
                                idCita
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No se encontraron signos vitales "
                                                        + "registrados para la cita."
                                        )
                        );


        Cie10 cie10 =
                consulta.getCie10();

        // =================================================
// CU-09
// RESULTADOS DE LABORATORIO PUBLICADOS
// =================================================
//
// El Repository limita:
//
// - La cita consultada.
// - El Médico autenticado.
// - Solo resultados publicados.
//
// Por lo tanto:
//
// Guardado sin publicar -> NO visible.
// Validado sin publicar -> NO visible.
// Publicado -> SÍ visible.
//
// =================================================

        List<ResultadoLaboratorioExpediente>
                resultadosLaboratorio =

                resultadoLaboratorioRepository
                        .listarPublicadosParaMedicoPorCita(

                                idCita,

                                medico.getId()
                        )
                        .stream()
                        .map(
                                this::convertirResultadoLaboratorio
                        )
                        .toList();


        return new ExpedienteConsulta(

                cita.getId(),

                consulta.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                medico.getNombreCompleto(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getSucursal()
                        .getNombre(),

                cita.getFechaHoraCita(),

                cita.getPrioridad(),

                cita.getMotivoConsulta(),

                consulta.getMotivoVisita(),

                consulta.getHallazgosClinicos(),

                cie10 != null
                        ? cie10.getId()
                        : null,

                cie10 != null
                        ? cie10.getCodigo()
                        : null,

                cie10 != null
                        ? cie10.getDescripcion()
                        : null,

                consulta.getDiagnostico(),

                consulta.getPlanTratamiento(),

                consulta.getNotasAdicionales(),

                consulta.getEstadoConsulta(),

                consulta.getVersion(),

                consulta.getFechaHoraInicio(),

                consulta.getFechaHoraFin(),

                convertirSignos(
                        signos
                ),

                resultadosLaboratorio
        );
    }


    // =====================================================
    // GUARDAR EN CURSO
    // =====================================================

    /**
     * Guarda un avance sin finalizar la consulta.
     *
     * Hallazgos, diagnóstico y tratamiento todavía pueden
     * estar incompletos.
     */
    @Transactional
    public ResultadoGuardado guardarEnCurso(
            ConsultaMedicaFormulario formulario
    ) {

        validarFormularioBasico(
                formulario
        );


        Usuario medico =
                validarMedicoActual();


        ConsultaMedica consulta =
                buscarConsultaParaModificar(
                        formulario.getIdCita(),
                        medico
                );


        validarConsultaEditable(
                consulta
        );


        // =================================================
        // MOTIVO SIEMPRE OBLIGATORIO
        // =================================================

        String motivo =
                limpiar(
                        formulario.getMotivoVisita()
                );


        if (motivo == null) {

            throw new IllegalArgumentException(
                    "El Motivo de Visita es obligatorio."
            );
        }


        validarLongitudMotivo(
                motivo
        );


        String diagnostico =
                limpiar(
                        formulario.getDiagnostico()
                );


        /*
         * Si ya comenzó a escribir diagnóstico, debe cumplir
         * la regla 10-5000. Puede seguir siendo null mientras
         * está En curso.
         */
        validarDiagnosticoSiExiste(
                diagnostico
        );


        Cie10 cie10 =
                resolverCie10(
                        formulario.getIdCie10()
                );


        OffsetDateTime ahora =
                ahora();


        Map<String, Object> valoresAnteriores =
                crearSnapshotControl(
                        consulta
                );


        // =================================================
        // RNF-026
        // GUARDAR VERSIÓN ANTERIOR
        // =================================================

        consultaMedicaVersionService
                .versionarAntesDeModificar(

                        consulta,

                        medico,

                        ahora
                );


        // =================================================
        // APLICAR CAMBIOS
        // =================================================

        consulta.setMotivoVisita(
                motivo
        );


        consulta.setHallazgosClinicos(
                limpiar(
                        formulario.getHallazgosClinicos()
                )
        );


        consulta.setCie10(
                cie10
        );


        consulta.setDiagnostico(
                diagnostico
        );


        consulta.setPlanTratamiento(
                limpiar(
                        formulario.getPlanTratamiento()
                )
        );


        consulta.setNotasAdicionales(
                limpiar(
                        formulario.getNotasAdicionales()
                )
        );


        consulta.setEstadoConsulta(
                EstadoConsultaMedica.EN_CURSO
        );


        consulta.setModificadoPor(
                medico
        );


        consulta.setFechaModificacion(
                ahora
        );


        ConsultaMedica guardada =
                consultaMedicaRepository
                        .saveAndFlush(
                                consulta
                        );


        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(

                        "consulta_medica",

                        String.valueOf(
                                guardada.getId()
                        ),

                        AccionAuditoria.ACTUALIZAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        valoresAnteriores,

                        crearSnapshotControl(
                                guardada
                        ),

                        null
                )
        );


        return new ResultadoGuardado(

                guardada.getId(),

                guardada.getVersion(),

                guardada.getEstadoConsulta(),

                "Consulta médica guardada correctamente."
        );
    }


    // =====================================================
    // FINALIZAR CONSULTA
    // =====================================================

    /**
     * CU-08 flujo normal + RN-CU08-01 + RN-CU08-02 + FA05.
     */
    @Transactional
    public ResultadoGuardado finalizarConsulta(
            ConsultaMedicaFormulario formulario
    ) {

        validarFormularioBasico(
                formulario
        );


        Usuario medico =
                validarMedicoActual();


        ConsultaMedica consulta =
                buscarConsultaParaModificar(
                        formulario.getIdCita(),
                        medico
                );


        validarConsultaEditable(
                consulta
        );


        // =================================================
        // NORMALIZAR
        // =================================================

        String motivo =
                limpiar(
                        formulario.getMotivoVisita()
                );


        String hallazgos =
                limpiar(
                        formulario.getHallazgosClinicos()
                );


        String diagnostico =
                limpiar(
                        formulario.getDiagnostico()
                );


        String plan =
                limpiar(
                        formulario.getPlanTratamiento()
                );


        // =================================================
        // FA05
        // DIAGNÓSTICO OBLIGATORIO
        // =================================================

        if (diagnostico == null) {

            throw new IllegalStateException(
                    "No es posible finalizar la consulta "
                            + "sin registrar un diagnóstico. "
                            + "El campo Diagnóstico es obligatorio."
            );
        }


        // =================================================
        // RN-CU08-01
        // DIAGNÓSTICO 10 - 5000
        // =================================================

        validarDiagnosticoSiExiste(
                diagnostico
        );


        // =================================================
        // RN-CU08-02
        // CAMPOS OBLIGATORIOS
        // =================================================

        if (motivo == null
                || hallazgos == null
                || plan == null) {

            throw new IllegalArgumentException(
                    "Debe completar todos los campos obligatorios "
                            + "para cerrar la consulta."
            );
        }


        // =================================================
        // LONGITUD DEL MOTIVO
        // =================================================

        validarLongitudMotivo(
                motivo
        );


        // =================================================
        // CIE-10
        // OPCIONAL
        // =================================================

        Cie10 cie10 =
                resolverCie10(
                        formulario.getIdCie10()
                );


        // =================================================
        // FECHA / HORA
        // =================================================

        OffsetDateTime ahora =
                ahora();


        // =================================================
        // SNAPSHOT ANTES DE MODIFICAR
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshotControl(
                        consulta
                );


        // =================================================
        // RNF-026
        // VERSIONAMIENTO
        // =================================================

        consultaMedicaVersionService
                .versionarAntesDeModificar(

                        consulta,

                        medico,

                        ahora
                );


        // =================================================
        // APLICAR DATOS DEFINITIVOS
        // =================================================

        consulta.setMotivoVisita(
                motivo
        );


        consulta.setHallazgosClinicos(
                hallazgos
        );


        consulta.setCie10(
                cie10
        );


        consulta.setDiagnostico(
                diagnostico
        );


        consulta.setPlanTratamiento(
                plan
        );


        consulta.setNotasAdicionales(
                limpiar(
                        formulario.getNotasAdicionales()
                )
        );


        // =================================================
        // FINALIZAR CONSULTA
        // =================================================

        consulta.setEstadoConsulta(
                EstadoConsultaMedica.FINALIZADA
        );


        consulta.setFechaHoraFin(
                ahora
        );


        consulta.setFechaModificacion(
                ahora
        );


        consulta.setModificadoPor(
                medico
        );


        // =================================================
        // GUARDAR
        // =================================================

        ConsultaMedica guardada =
                consultaMedicaRepository
                        .saveAndFlush(
                                consulta
                        );

        // =================================================
// RNF-031
// CONSTRUIR CONTENIDO CANÓNICO FINAL
// =================================================

        String contenidoCanonico =
                construirContenidoCanonicoConsulta(
                        guardada
                );


// =================================================
// RNF-031
// FIRMA DIGITAL DEL MÉDICO
// =================================================

        FirmaDocumentoClinico firma =
                firmaDigitalMedicaService
                        .firmarDocumento(

                                medico,

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .CONSULTA_MEDICA,

                                guardada.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


// =================================================
// ASOCIAR HASH A LA CONSULTA
// =================================================

        guardada.setFirmaDigitalHash(
                firma.getHashContenido()
        );


// =================================================
// PERSISTIR HASH
// =================================================

        guardada =
                consultaMedicaRepository
                        .saveAndFlush(
                                guardada
                        );


// =================================================
// DEFENSA FINAL
// =================================================

        if (guardada.getFirmaDigitalHash() == null

                || !firma.getHashContenido()
                .equalsIgnoreCase(
                        guardada.getFirmaDigitalHash()
                )) {

            throw new IllegalStateException(
                    "No fue posible asociar correctamente "
                            + "la firma digital con la consulta médica."
            );
        }


        // =================================================
        // IMPORTANTE:
        //
        // La Cita continúa en:
        //
        // "Consulta Médica"
        //
        // mientras ConsultaMedica queda:
        //
        // "Finalizada"
        //
        // Esto permite que aparezca en:
        //
        // Evaluados — Pendiente de cierre.
        // =================================================


        // =================================================
        // AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(

                        "consulta_medica",

                        String.valueOf(
                                guardada.getId()
                        ),

                        AccionAuditoria.ACTUALIZAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        valoresAnteriores,

                        crearSnapshotControl(
                                guardada
                        ),

                        null
                )
        );


        // =================================================
        // RESULTADO
        // =================================================

        return new ResultadoGuardado(

                guardada.getId(),

                guardada.getVersion(),

                guardada.getEstadoConsulta(),

                "La consulta ha sido finalizada exitosamente. "
                        + "El paciente puede proceder a las "
                        + "siguientes indicaciones médicas."
        );
    }

    // =====================================================
// CU-08
// FINALIZAR ATENCIÓN
// =====================================================
//
// ConsultaMedica:
// FINALIZADA
//
// Cita:
// Consulta Médica
//        ↓
// Atención Finalizada
// =====================================================

    @Transactional
    public ResultadoAtencionFinalizada finalizarAtencion(
            Integer idCita
    ) {

        validarIdCita(
                idCita
        );


        // =================================================
        // MÉDICO AUTENTICADO
        // =================================================

        Usuario medico =
                validarMedicoActual();


        // =================================================
        // CONSULTA + BLOQUEO
        //
        // Este repository ya limita:
        //
        // - cita solicitada;
        // - médico autenticado.
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


        Cita cita =
                consulta.getCita();


        // =================================================
        // ESTADO ACTUAL DE LA CITA
        // =================================================

        validarEstadoCita(
                cita,
                ESTADO_CONSULTA_MEDICA,
                "La cita no se encuentra pendiente "
                        + "de finalizar atención."
        );


        // =================================================
        // LA CONSULTA DEBE ESTAR FINALIZADA
        // =================================================

        if (consulta.getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "No es posible finalizar la atención "
                            + "porque la consulta médica aún no ha sido finalizada."
            );
        }


        // =================================================
        // RNF-031
        // DEBE EXISTIR FIRMA DIGITAL
        // =================================================

        if (consulta.getFirmaDigitalHash() == null
                || consulta.getFirmaDigitalHash().isBlank()) {

            throw new IllegalStateException(
                    "No es posible finalizar la atención "
                            + "porque la consulta médica no posee firma digital."
            );
        }


        // =================================================
        // RNF-031
        // VERIFICAR CRIPTOGRÁFICAMENTE ANTES DEL CIERRE
        // =================================================

        String contenidoCanonico =
                construirContenidoCanonicoConsulta(
                        consulta
                );


        boolean firmaValida =
                firmaDigitalMedicaService
                        .verificarDocumento(

                                FirmaDocumentoClinico
                                        .TipoDocumentoClinico
                                        .CONSULTA_MEDICA,

                                consulta.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


        if (!firmaValida) {

            throw new IllegalStateException(
                    "No es posible finalizar la atención "
                            + "porque la firma digital de la consulta "
                            + "no es válida."
            );
        }


        // =================================================
        // ESTADO DESTINO
        // =================================================

        EstadoCita estadoFinalizada =
                obtenerEstadoActivo(
                        ESTADO_ATENCION_FINALIZADA
                );


        String estadoAnterior =
                cita.getEstadoCita()
                        .getNombre();


        OffsetDateTime ahora =
                ahora();


        // =================================================
        // CAMBIAR ESTADO
        // =================================================

        cita.setEstadoCita(
                estadoFinalizada
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                medico
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        // =================================================
        // AUDITORÍA
        // =================================================

        Map<String, Object> anterior =
                new LinkedHashMap<>();

        anterior.put(
                "estado",
                estadoAnterior
        );


        Map<String, Object> nuevo =
                new LinkedHashMap<>();

        nuevo.put(
                "estado",
                ESTADO_ATENCION_FINALIZADA
        );

        nuevo.put(
                "consultaFinalizada",
                true
        );

        nuevo.put(
                "firmaDigitalValidada",
                true
        );


        auditoriaService.registrar(
                new RegistroAuditoria(

                        "cita",

                        String.valueOf(
                                cita.getId()
                        ),

                        AccionAuditoria.ACTUALIZAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        anterior,

                        nuevo,

                        null
                )
        );


        // =================================================
        // RESULTADO CU-08
        // =================================================

        return new ResultadoAtencionFinalizada(

                cita.getId(),

                "Atención finalizada para cita #"
                        + cita.getId()
                        + "."
        );
    }

    // =====================================================
    // FA06 - NO ASISTIÓ
    // =====================================================

    @Transactional
    public ResultadoNoAsistio marcarNoAsistio(
            Integer idCita
    ) {

        validarIdCita(
                idCita
        );


        Usuario medico =
                validarMedicoActual();


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


        validarEstadoCita(
                cita,
                ESTADO_EN_ESPERA,
                "Solo es posible marcar No Asistió "
                        + "cuando la cita se encuentra En Espera."
        );


        if (consultaMedicaRepository
                .existsByCita_Id(
                        idCita
                )) {

            throw new IllegalStateException(
                    "No es posible marcar No Asistió "
                            + "porque la consulta médica ya fue iniciada."
            );
        }


        EstadoCita estadoNoAsistio =
                obtenerEstadoActivo(
                        ESTADO_NO_ASISTIO
                );


        String estadoAnterior =
                cita.getEstadoCita()
                        .getNombre();


        OffsetDateTime ahora =
                ahora();


        cita.setEstadoCita(
                estadoNoAsistio
        );


        cita.setFechaModificacion(
                ahora
        );


        cita.setModificadoPor(
                medico
        );


        citaRepository
                .saveAndFlush(
                        cita
                );


        Map<String, Object> anterior =
                new LinkedHashMap<>();

        anterior.put(
                "estado",
                estadoAnterior
        );


        Map<String, Object> nuevo =
                new LinkedHashMap<>();

        nuevo.put(
                "estado",
                ESTADO_NO_ASISTIO
        );


        auditoriaService.registrar(
                new RegistroAuditoria(

                        "cita",

                        String.valueOf(
                                cita.getId()
                        ),

                        AccionAuditoria.ACTUALIZAR,

                        medico.getId(),

                        medico.getNombreUsuario(),

                        anterior,

                        nuevo,

                        null
                )
        );


        String mensaje =
                "Cita #"
                        + cita.getId()
                        + " marcada como No Asistió.";


        return new ResultadoNoAsistio(

                cita.getId(),

                mensaje
        );
    }


    // =====================================================
    // BUSCAR CONSULTA PARA MODIFICAR
    // =====================================================

    private ConsultaMedica buscarConsultaParaModificar(

            Integer idCita,

            Usuario medico
    ) {

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


        validarEstadoCita(
                consulta.getCita(),
                ESTADO_CONSULTA_MEDICA,
                "La cita no se encuentra actualmente "
                        + "en Consulta Médica."
        );


        return consulta;
    }


    // =====================================================
    // CONSULTA EDITABLE
    // =====================================================

    private void validarConsultaEditable(
            ConsultaMedica consulta
    ) {

        if (consulta.getEstadoConsulta()
                != EstadoConsultaMedica.EN_CURSO) {

            throw new IllegalStateException(
                    "La consulta médica ya fue finalizada "
                            + "y no puede modificarse desde este formulario."
            );
        }
    }


    // =====================================================
    // CIE-10 OPCIONAL
    // =====================================================

    private Cie10 resolverCie10(
            Integer idCie10
    ) {

        if (idCie10 == null) {

            return null;
        }


        if (idCie10 <= 0) {

            throw new IllegalArgumentException(
                    "El código CIE-10 seleccionado no es válido."
            );
        }


        Cie10 cie10 =
                cie10Repository
                        .findById(
                                idCie10
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El código CIE-10 seleccionado no existe."
                                        )
                        );


        if (!Boolean.TRUE.equals(
                cie10.getActivo()
        )) {

            throw new IllegalStateException(
                    "El código CIE-10 seleccionado "
                            + "se encuentra inactivo."
            );
        }


        return cie10;
    }


    // =====================================================
    // VALIDAR FORMULARIO
    // =====================================================

    private void validarFormularioBasico(
            ConsultaMedicaFormulario formulario
    ) {

        if (formulario == null) {

            throw new IllegalArgumentException(
                    "Los datos de la consulta médica son obligatorios."
            );
        }


        validarIdCita(
                formulario.getIdCita()
        );
    }


    // =====================================================
    // VALIDAR DIAGNÓSTICO
    // =====================================================

    private void validarDiagnosticoSiExiste(
            String diagnostico
    ) {

        if (diagnostico == null) {

            return;
        }


        int longitud =
                diagnostico.length();


        if (longitud < DIAGNOSTICO_MINIMO
                || longitud > DIAGNOSTICO_MAXIMO) {

            throw new IllegalArgumentException(
                    "El diagnóstico es obligatorio. "
                            + "Debe contener entre 10 y 5000 caracteres."
            );
        }
    }


    // =====================================================
    // MOTIVO
    // =====================================================

    private void validarLongitudMotivo(
            String motivo
    ) {

        if (motivo != null
                && motivo.length() > 2000) {

            throw new IllegalArgumentException(
                    "El Motivo de Visita no puede exceder "
                            + "los 2000 caracteres."
            );
        }
    }


    // =====================================================
    // ESTADO ACTIVO
    // =====================================================

    private EstadoCita obtenerEstadoActivo(
            String nombre
    ) {

        Map<String, Integer> catalogo =
                catalogoEstadoCitaService
                        .listarEstadosActivos();


        Integer idEstado =
                catalogoEstadoCitaService
                        .obtenerIdEstadoActivo(

                                nombre,

                                catalogo
                        );


        if (idEstado == null) {

            throw new IllegalStateException(
                    "No existe el estado de cita '"
                            + nombre
                            + "'."
            );
        }


        return estadoCitaRepository
                .getReferenceById(
                        idEstado
                );
    }


    // =====================================================
    // VALIDAR ESTADO
    // =====================================================

    private void validarEstadoCita(

            Cita cita,

            String estadoEsperado,

            String mensajeError
    ) {

        if (cita == null

                || cita.getEstadoCita() == null

                || cita.getEstadoCita()
                .getNombre() == null

                || !estadoEsperado
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    mensajeError
            );
        }
    }


    // =====================================================
    // ID CITA
    // =====================================================

    private void validarIdCita(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            throw new IllegalArgumentException(
                    "La cita seleccionada no es válida."
            );
        }
    }


    // =====================================================
    // VALIDAR MÉDICO
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

                || usuario.getRol()
                .getNombre() == null

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
    // SNAPSHOT DE CONTROL PARA AUDITORÍA
    // =====================================================

    /**
     * La información clínica completa queda preservada
     * en consulta_medica_historial.
     *
     * La bitácora general registra principalmente
     * metadatos de control para evitar duplicar
     * innecesariamente información clínica.
     */
    private Map<String, Object> crearSnapshotControl(
            ConsultaMedica consulta
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();


        snapshot.put(
                "version",
                consulta.getVersion()
        );


        snapshot.put(
                "estadoConsulta",

                consulta.getEstadoConsulta() != null
                        ? consulta
                        .getEstadoConsulta()
                        .getValorBaseDatos()
                        : null
        );


        snapshot.put(
                "idCie10",

                consulta.getCie10() != null
                        ? consulta.getCie10()
                        .getId()
                        : null
        );


        snapshot.put(
                "fechaHoraFin",

                consulta.getFechaHoraFin() != null
                        ? consulta.getFechaHoraFin()
                        .toString()
                        : null
        );

        snapshot.put(
                "firmaDigitalHash",
                consulta.getFirmaDigitalHash()
        );


        return snapshot;
    }


    // =====================================================
// CU-09
// CONVERTIR RESULTADO PUBLICADO PARA EXPEDIENTE
// =====================================================

    private ResultadoLaboratorioExpediente
    convertirResultadoLaboratorio(
            ResultadoLaboratorio resultado
    ) {

        if (resultado == null) {

            throw new IllegalStateException(
                    "Se encontró un resultado de laboratorio inválido."
            );
        }


        DetalleOrdenLaboratorio detalle =
                resultado
                        .getDetalleOrdenLaboratorio();


        if (detalle == null) {

            throw new IllegalStateException(
                    "El resultado de laboratorio no posee un examen asociado."
            );
        }


        OrdenLaboratorio orden =
                detalle
                        .getOrdenLaboratorio();


        if (orden == null
                || orden.getId() == null) {

            throw new IllegalStateException(
                    "El resultado de laboratorio no posee una orden válida."
            );
        }


        return new ResultadoLaboratorioExpediente(

                resultado.getId(),

                orden.getId(),

                orden.getNumeroOrden(),

                detalle.getNombreExamen(),

                resultado.getValorResultado(),

                resultado.getUnidadResultado(),

                detalle.getRangoReferencia(),

                resultado.estaFueraDeRango(),

                resultado.getNotasResultado(),

                resultado.getFechaResultado(),

                resultado.getFechaHoraPublicacion()
        );
    }

    // =====================================================
    // SIGNOS PARA EXPEDIENTE
    // =====================================================

    private SignosVitalesExpediente convertirSignos(
            SignosVitales signos
    ) {

        return new SignosVitalesExpediente(

                signos.getPresionSistolica(),

                signos.getPresionDiastolica(),

                signos.getTemperatura(),

                signos.getPeso(),

                signos.getTalla(),

                signos.getFrecuenciaCardiaca(),

                Boolean.TRUE.equals(
                        signos.getEsEmergencia()
                ),

                Boolean.TRUE.equals(
                        signos.getAlertaPresion()
                ),

                Boolean.TRUE.equals(
                        signos.getAlertaTemperatura()
                ),

                Boolean.TRUE.equals(
                        signos.getAlertaFrecuenciaCardiaca()
                ),

                signos.getFechaHoraRegistro()
        );
    }


    // =====================================================
    // AHORA
    // =====================================================

    private OffsetDateTime ahora() {

        return OffsetDateTime.now(
                zonaHoraria
        );
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isEmpty()
                ? null
                : limpio;
    }


    // =====================================================
    // RESULTADO INICIO
    // =====================================================

    public record InicioConsulta(

            Integer numeroCita,

            Integer idConsulta,

            String nombrePaciente,

            String mensajeTts

    ) {
    }
    // =====================================================
// RNF-031
// CONTENIDO CANÓNICO DE LA CONSULTA
// =====================================================

    /**
     * Construye una representación estable del documento
     * clínico que será firmada criptográficamente.
     *
     * IMPORTANTE:
     *
     * Debe producir exactamente el mismo texto cuando
     * posteriormente queramos verificar la firma.
     */
    private String construirContenidoCanonicoConsulta(
            ConsultaMedica consulta
    ) {

        if (consulta == null
                || consulta.getId() == null) {

            throw new IllegalArgumentException(
                    "La consulta médica debe estar guardada "
                            + "antes de generar su firma digital."
            );
        }


        if (consulta.getCita() == null
                || consulta.getCita().getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee una cita válida."
            );
        }


        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null) {

            throw new IllegalStateException(
                    "La consulta médica no posee un Médico válido."
            );
        }


        String idCie10 =
                consulta.getCie10() != null
                        && consulta.getCie10().getId() != null

                        ? String.valueOf(
                        consulta.getCie10()
                                .getId()
                )

                        : "";


        String codigoCie10 =
                consulta.getCie10() != null

                        ? valorCanonico(
                        consulta.getCie10()
                                .getCodigo()
                )

                        : "";


        Integer idPaciente =
                consulta.getCita()
                        .getPaciente() != null

                        ? consulta.getCita()
                        .getPaciente()
                        .getId()

                        : null;


        Integer idSucursal =
                consulta.getCita()
                        .getSucursal() != null

                        ? consulta.getCita()
                        .getSucursal()
                        .getId()

                        : null;


        Integer idEspecialidad =
                consulta.getCita()
                        .getEspecialidad() != null

                        ? consulta.getCita()
                        .getEspecialidad()
                        .getId()

                        : null;


        return String.join(
                "\n",

                "TIPO=CONSULTA_MEDICA",

                "ID_CONSULTA="
                        + consulta.getId(),

                "ID_CITA="
                        + consulta.getCita()
                        .getId(),

                "ID_PACIENTE="
                        + valorCanonico(
                        idPaciente
                ),

                "ID_MEDICO="
                        + consulta.getMedico()
                        .getId(),

                "ID_SUCURSAL="
                        + valorCanonico(
                        idSucursal
                ),

                "ID_ESPECIALIDAD="
                        + valorCanonico(
                        idEspecialidad
                ),

                "VERSION="
                        + consulta.getVersion(),

                "MOTIVO_VISITA="
                        + valorCanonico(
                        consulta.getMotivoVisita()
                ),

                "HALLAZGOS_CLINICOS="
                        + valorCanonico(
                        consulta.getHallazgosClinicos()
                ),

                "ID_CIE10="
                        + idCie10,

                "CODIGO_CIE10="
                        + codigoCie10,

                "DIAGNOSTICO="
                        + valorCanonico(
                        consulta.getDiagnostico()
                ),

                "PLAN_TRATAMIENTO="
                        + valorCanonico(
                        consulta.getPlanTratamiento()
                ),

                "NOTAS_ADICIONALES="
                        + valorCanonico(
                        consulta.getNotasAdicionales()
                ),

                "ESTADO="
                        + (
                        consulta.getEstadoConsulta() != null

                                ? consulta
                                .getEstadoConsulta()
                                .getValorBaseDatos()

                                : ""
                )
        );
    }

    // =====================================================
// NORMALIZACIÓN CANÓNICA
// =====================================================

    private String valorCanonico(
            String valor
    ) {

        if (valor == null) {

            return "";
        }


        /*
         * Normalizamos saltos de línea para que Windows,
         * Linux o el navegador no produzcan documentos
         * criptográficamente distintos solo por \r\n / \n.
         */
        return valor
                .trim()
                .replace(
                        "\r\n",
                        "\n"
                )
                .replace(
                        "\r",
                        "\n"
                );
    }


// =====================================================
// VALOR CANÓNICO GENÉRICO
// =====================================================

    private String valorCanonico(
            Object valor
    ) {

        return valor == null
                ? ""
                : String.valueOf(
                valor
        );
    }


    // =====================================================
    // RESULTADO GUARDADO
    // =====================================================

    public record ResultadoGuardado(

            Integer idConsulta,

            Integer version,

            EstadoConsultaMedica estado,

            String mensaje

    ) {
    }


    // =====================================================
    // RESULTADO NO ASISTIÓ
    // =====================================================

    public record ResultadoNoAsistio(

            Integer numeroCita,

            String mensaje

    ) {
    }


    // =====================================================
// RESULTADO FINALIZAR ATENCIÓN
// =====================================================

    public record ResultadoAtencionFinalizada(

            Integer numeroCita,

            String mensaje

    ) {
    }

    // =====================================================
// EXPEDIENTE
// =====================================================

    public record ExpedienteConsulta(

            Integer numeroCita,

            Integer idConsulta,

            String nombrePaciente,

            String nombreMedico,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            String prioridad,

            String motivoOriginalCita,

            String motivoVisita,

            String hallazgosClinicos,

            Integer idCie10,

            String codigoCie10,

            String descripcionCie10,

            String diagnostico,

            String planTratamiento,

            String notasAdicionales,

            EstadoConsultaMedica estadoConsulta,

            Integer version,

            OffsetDateTime fechaHoraInicio,

            OffsetDateTime fechaHoraFin,

            SignosVitalesExpediente signosVitales,

            List<ResultadoLaboratorioExpediente>
            resultadosLaboratorio

    ) {
    }


// =====================================================
// CU-09
// RESULTADO DE LABORATORIO PARA EXPEDIENTE MÉDICO
// =====================================================

    public record ResultadoLaboratorioExpediente(

            Integer idResultadoLaboratorio,

            Integer idOrdenLaboratorio,

            String numeroOrden,

            String nombreExamen,

            String valorResultado,

            String unidadResultado,

            String rangoReferencia,

            boolean fueraRango,

            String notasResultado,

            OffsetDateTime fechaResultado,

            OffsetDateTime fechaHoraPublicacion

    ) {
    }


// =====================================================
// SIGNOS DEL EXPEDIENTE
// =====================================================

    public record SignosVitalesExpediente(

            Short presionSistolica,

            Short presionDiastolica,

            BigDecimal temperatura,

            BigDecimal peso,

            BigDecimal talla,

            Short frecuenciaCardiaca,

            boolean emergencia,

            boolean alertaPresion,

            boolean alertaTemperatura,

            boolean alertaFrecuenciaCardiaca,

            OffsetDateTime fechaHoraRegistro

    ) {
    }
}
