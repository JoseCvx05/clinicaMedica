package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.ConsultaMedica;
import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;
import com.proyecto.clinicamedica.entity.FirmaDocumentoClinico;
import com.proyecto.clinicamedica.entity.Medicamento;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.ConsultaMedicaRepository;
import com.proyecto.clinicamedica.repository.DetalleRecetaMedicaRepository;
import com.proyecto.clinicamedica.repository.MedicamentoRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * =========================================================
 * SERVICIO: RECETA MÉDICA
 * =========================================================
 *
 * CU-08 - FA04.
 *
 * Responsabilidades:
 *
 * - Validar Médico autenticado.
 * - Validar cita asignada al Médico.
 * - Validar consulta FINALIZADA.
 * - Validar que la atención siga pendiente de cierre.
 * - Validar firma de la consulta.
 * - Mostrar catálogo real de medicamentos.
 * - Recargar medicamentos desde PostgreSQL.
 * - No confiar en nombres/precios enviados desde HTML.
 * - Validar dosis, frecuencia y duración.
 * - Validar cantidad prescrita.
 * - Guardar receta.
 * - Guardar detalle con snapshot histórico.
 * - Firmar digitalmente la receta.
 * - Registrar auditoría.
 *
 * =========================================================
 */
@Service
public class RecetaMedicaService {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ROL_MEDICO =
            "Médico";


    /**
     * En el modelo actual:
     *
     * Cita = "Consulta Médica"
     * ConsultaMedica = FINALIZADA
     *
     * significa:
     *
     * Evaluados — Pendiente de cierre.
     */
    private static final String ESTADO_CITA_EVALUADA =
            "Consulta Médica";


    private static final int MAX_DOSIS =
            150;

    private static final int MAX_FRECUENCIA =
            150;

    private static final int MAX_DURACION =
            150;

    private static final int MAX_INDICACIONES =
            1000;


    private static final String MENSAJE_CAMPOS_OBLIGATORIOS =
            "Todos los campos de la receta son obligatorios "
                    + "excepto indicaciones especiales.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository
            citaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final RecetaMedicaRepository
            recetaMedicaRepository;

    private final DetalleRecetaMedicaRepository
            detalleRecetaMedicaRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final FirmaDigitalMedicaService
            firmaDigitalMedicaService;

    private final AuditoriaService
            auditoriaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RecetaMedicaService(

            CitaRepository citaRepository,

            ConsultaMedicaRepository consultaMedicaRepository,

            MedicamentoRepository medicamentoRepository,

            RecetaMedicaRepository recetaMedicaRepository,

            DetalleRecetaMedicaRepository
                    detalleRecetaMedicaRepository,

            UsuarioActualService usuarioActualService,

            FirmaDigitalMedicaService
                    firmaDigitalMedicaService,

            AuditoriaService auditoriaService
    ) {

        this.citaRepository =
                citaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.medicamentoRepository =
                medicamentoRepository;

        this.recetaMedicaRepository =
                recetaMedicaRepository;

        this.detalleRecetaMedicaRepository =
                detalleRecetaMedicaRepository;

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
     * Valida que la receta pueda generarse y devuelve
     * el catálogo de medicamentos activos.
     *
     * No modifica ninguna tabla.
     */
    @Transactional(
            readOnly = true
    )
    public PreparacionReceta prepararReceta(
            Integer idCita
    ) {

        validarId(
                idCita,
                "cita"
        );


        // =================================================
        // MÉDICO
        // =================================================

        Usuario medico =
                validarMedicoActual();


        // =================================================
        // CONSULTA
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

        validarContextoReceta(
                cita,
                consulta,
                medico
        );


        // =================================================
        // CATÁLOGO
        // =================================================

        List<Medicamento> medicamentos =
                medicamentoRepository
                        .findByActivoTrueOrderByNombreAsc();


        List<MedicamentoDisponible> disponibles =
                new ArrayList<>();


        for (Medicamento medicamento : medicamentos) {

            if (medicamento == null) {

                continue;
            }


            if (!medicamento.estaActivo()) {

                continue;
            }


            disponibles.add(
                    new MedicamentoDisponible(

                            medicamento.getId(),

                            medicamento.getNombre(),

                            medicamento.getDescripcion(),

                            medicamento.getPrecio(),

                            medicamento.getUnidad(),

                            medicamento.esControlado()
                    )
            );
        }


        return new PreparacionReceta(

                cita.getId(),

                consulta.getId(),

                List.copyOf(
                        disponibles
                )
        );
    }


    // =====================================================
    // CREAR RECETA
    // =====================================================

    @Transactional
    public ResultadoReceta crearReceta(

            Integer idCita,

            List<Integer> idsMedicamentos,

            List<Integer> cantidades,

            List<String> dosis,

            List<String> frecuencias,

            List<String> duraciones,

            List<String> indicaciones
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
        // Mantiene el mismo orden de bloqueo utilizado
        // por las demás operaciones médicas.
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
        // BLOQUEAR CONSULTA
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
        // VALIDAR CONTEXTO
        // =================================================

        validarContextoReceta(
                cita,
                consulta,
                medico
        );


        // =================================================
        // VALIDAR ESTRUCTURA RECIBIDA
        // =================================================

        validarListas(

                idsMedicamentos,

                cantidades,

                dosis,

                frecuencias,

                duraciones,

                indicaciones
        );


        // =================================================
        // MEDICAMENTOS REALES DESDE POSTGRESQL
        // =================================================

        List<Medicamento> medicamentos =
                obtenerMedicamentosValidos(
                        idsMedicamentos
                );


        // =================================================
        // CREAR CABECERA
        // =================================================

        RecetaMedica receta =
                new RecetaMedica();


        receta.setCita(
                cita
        );


        receta.setConsultaMedica(
                consulta
        );


        receta.setMedico(
                medico
        );


        receta.setActivo(
                true
        );


        receta =
                recetaMedicaRepository
                        .saveAndFlush(
                                receta
                        );


        if (receta.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible registrar la receta médica."
            );
        }


        // =================================================
        // DETALLES
        // =================================================

        List<DetalleRecetaMedica> detalles =
                new ArrayList<>();


        for (
                int i = 0;
                i < medicamentos.size();
                i++
        ) {

            Medicamento medicamento =
                    medicamentos.get(i);


            String dosisLimpia =
                    limpiarObligatorio(
                            dosis.get(i)
                    );


            String frecuenciaLimpia =
                    limpiarObligatorio(
                            frecuencias.get(i)
                    );


            String duracionLimpia =
                    limpiarObligatorio(
                            duraciones.get(i)
                    );


            String indicacionLimpia =
                    obtenerIndicacion(
                            indicaciones,
                            i
                    );


            DetalleRecetaMedica detalle =
                    new DetalleRecetaMedica(

                            receta,

                            medicamento,

                            cantidades.get(i),

                            dosisLimpia,

                            frecuenciaLimpia,

                            duracionLimpia,

                            indicacionLimpia
                    );


            detalle.validar();


            detalles.add(
                    detalle
            );
        }


        // =================================================
        // GUARDAR DETALLES
        // =================================================

        detalleRecetaMedicaRepository
                .saveAll(
                        detalles
                );


        detalleRecetaMedicaRepository
                .flush();


        // =================================================
        // COMPROBAR PERSISTENCIA
        // =================================================

        long cantidadGuardada =
                detalleRecetaMedicaRepository
                        .countByRecetaMedica_Id(
                                receta.getId()
                        );


        if (cantidadGuardada
                != detalles.size()) {

            throw new IllegalStateException(
                    "No fue posible guardar correctamente "
                            + "todos los medicamentos de la receta."
            );
        }


        if (cantidadGuardada <= 0) {

            throw new IllegalStateException(
                    "La receta médica debe contener "
                            + "al menos un medicamento."
            );
        }


        // =================================================
        // RNF-031
        // CONTENIDO CANÓNICO
        // =================================================

        String contenidoCanonico =
                construirContenidoCanonico(

                        receta,

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
                                        .RECETA_MEDICA,

                                receta.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


        if (firma == null
                || firma.getHashContenido() == null
                || firma.getHashContenido().isBlank()) {

            throw new IllegalStateException(
                    "No fue posible generar la firma digital "
                            + "de la receta médica."
            );
        }


        // =================================================
        // GUARDAR HASH EN RECETA
        // =================================================

        receta.setFirmaDigitalHash(
                firma.getHashContenido()
        );


        receta =
                recetaMedicaRepository
                        .saveAndFlush(
                                receta
                        );


        // =================================================
        // VALIDAR HASH
        // =================================================

        if (receta.getFirmaDigitalHash() == null
                || !firma
                .getHashContenido()
                .equalsIgnoreCase(
                        receta.getFirmaDigitalHash()
                )) {

            throw new IllegalStateException(
                    "No fue posible asociar correctamente "
                            + "la firma digital con la receta médica."
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
                                        .RECETA_MEDICA,

                                receta.getId()
                                        .longValue(),

                                contenidoCanonico
                        );


        if (!firmaValida) {

            throw new IllegalStateException(
                    "La firma digital de la receta médica "
                            + "no pudo ser verificada."
            );
        }


        // =================================================
        // AUDITORÍA
        // =================================================

        registrarAuditoriaCreacion(

                receta,

                detalles,

                medico
        );


        // =================================================
        // MENSAJE FA04
        // =================================================

        List<String> nombresMedicamentos =
                detalles
                        .stream()

                        .map(
                                DetalleRecetaMedica
                                        ::getNombreMedicamento
                        )

                        .toList();


        String lista =
                String.join(
                        ", ",
                        nombresMedicamentos
                );


        String mensaje =
                "Receta médica generada exitosamente. "
                        + "Medicamentos: "
                        + lista
                        + ". El paciente puede adquirirlos "
                        + "en la farmacia de la clínica.";


        return new ResultadoReceta(

                receta.getId(),

                List.copyOf(
                        nombresMedicamentos
                ),

                mensaje
        );
    }


    // =====================================================
    // VALIDAR ESTRUCTURA DEL FORMULARIO
    // =====================================================

    private void validarListas(

            List<Integer> idsMedicamentos,

            List<Integer> cantidades,

            List<String> dosis,

            List<String> frecuencias,

            List<String> duraciones,

            List<String> indicaciones
    ) {

        if (idsMedicamentos == null
                || idsMedicamentos.isEmpty()) {

            throw new IllegalStateException(
                    "Debe registrar al menos un medicamento."
            );
        }


        int cantidadItems =
                idsMedicamentos.size();


        /*
         * Las cuatro listas obligatorias deben tener
         * exactamente el mismo tamaño.
         *
         * Esto también detecta manipulación del POST.
         */
        if (cantidades == null
                || dosis == null
                || frecuencias == null
                || duraciones == null
                || cantidades.size() != cantidadItems
                || dosis.size() != cantidadItems
                || frecuencias.size() != cantidadItems
                || duraciones.size() != cantidadItems) {

            throw new IllegalStateException(
                    "La información de la receta fue "
                            + "manipulada o está incompleta."
            );
        }


        for (
                int i = 0;
                i < cantidadItems;
                i++
        ) {

            validarId(
                    idsMedicamentos.get(i),
                    "medicamento"
            );


            Integer cantidad =
                    cantidades.get(i);


            if (cantidad == null
                    || cantidad <= 0) {

                throw new IllegalStateException(
                        "La cantidad prescrita debe ser mayor a cero."
                );
            }


            String dosisActual =
                    limpiarObligatorio(
                            dosis.get(i)
                    );


            String frecuenciaActual =
                    limpiarObligatorio(
                            frecuencias.get(i)
                    );


            String duracionActual =
                    limpiarObligatorio(
                            duraciones.get(i)
                    );


            if (dosisActual == null
                    || frecuenciaActual == null
                    || duracionActual == null) {

                throw new IllegalStateException(
                        MENSAJE_CAMPOS_OBLIGATORIOS
                );
            }


            if (dosisActual.length()
                    > MAX_DOSIS) {

                throw new IllegalStateException(
                        "La dosis no puede superar "
                                + MAX_DOSIS
                                + " caracteres."
                );
            }


            if (frecuenciaActual.length()
                    > MAX_FRECUENCIA) {

                throw new IllegalStateException(
                        "La frecuencia no puede superar "
                                + MAX_FRECUENCIA
                                + " caracteres."
                );
            }


            if (duracionActual.length()
                    > MAX_DURACION) {

                throw new IllegalStateException(
                        "La duración no puede superar "
                                + MAX_DURACION
                                + " caracteres."
                );
            }


            String indicacion =
                    obtenerIndicacion(
                            indicaciones,
                            i
                    );


            if (indicacion != null
                    && indicacion.length()
                    > MAX_INDICACIONES) {

                throw new IllegalStateException(
                        "Las indicaciones especiales no pueden superar "
                                + MAX_INDICACIONES
                                + " caracteres."
                );
            }
        }
    }


    // =====================================================
    // RECUPERAR MEDICAMENTOS REALES
    // =====================================================

    private List<Medicamento>
    obtenerMedicamentosValidos(
            List<Integer> idsMedicamentos
    ) {

        List<Medicamento> medicamentos =
                new ArrayList<>();


        for (Integer idMedicamento : idsMedicamentos) {

            validarId(
                    idMedicamento,
                    "medicamento"
            );


            /*
             * Nunca confiamos en:
             *
             * - nombre
             * - precio
             * - unidad
             *
             * enviados desde HTML.
             */
            Medicamento medicamento =
                    medicamentoRepository
                            .findByIdAndActivoTrue(
                                    idMedicamento
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Uno de los medicamentos "
                                                            + "seleccionados no existe "
                                                            + "o se encuentra inactivo."
                                            )
                            );


            medicamento.validar();


            if (!medicamento.estaActivo()) {

                throw new IllegalStateException(
                        "No se pueden recetar medicamentos inactivos."
                );
            }


            medicamentos.add(
                    medicamento
            );
        }


        /*
         * NO eliminamos duplicados.
         *
         * El mismo medicamento podría aparecer en más
         * de una línea si el médico necesita registrar
         * indicaciones diferentes.
         */
        return medicamentos;
    }


    // =====================================================
    // VALIDAR CONTEXTO CLÍNICO
    // =====================================================

    private void validarContextoReceta(

            Cita cita,

            ConsultaMedica consulta,

            Usuario medico
    ) {

        // =================================================
        // CITA
        // =================================================

        if (cita == null
                || cita.getId() == null) {

            throw new IllegalStateException(
                    "La cita seleccionada no es válida."
            );
        }


        // =================================================
        // CONSULTA
        // =================================================

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
        // EVALUADOS — PENDIENTE DE CIERRE
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
                    "Solo una cita evaluada y pendiente "
                            + "de cierre puede generar recetas médicas."
            );
        }


        // =================================================
        // CONSULTA FINALIZADA
        // =================================================

        if (consulta.getEstadoConsulta()
                != EstadoConsultaMedica.FINALIZADA) {

            throw new IllegalStateException(
                    "La consulta médica debe estar finalizada "
                            + "antes de generar una receta."
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
        // LA CONSULTA DEBE ESTAR FIRMADA
        // =================================================

        if (consulta.getFirmaDigitalHash() == null
                || consulta.getFirmaDigitalHash().isBlank()) {

            throw new IllegalStateException(
                    "La consulta médica debe poseer una firma "
                            + "digital válida antes de generar una receta."
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
    // VALIDAR MÉDICO AUTENTICADO
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
    // CONTENIDO CANÓNICO RNF-031
    // =====================================================

    private String construirContenidoCanonico(

            RecetaMedica receta,

            List<DetalleRecetaMedica> detalles
    ) {

        if (receta == null
                || receta.getId() == null) {

            throw new IllegalArgumentException(
                    "La receta debe estar guardada antes "
                            + "de generar su firma digital."
            );
        }


        if (detalles == null
                || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La receta médica debe contener "
                            + "al menos un medicamento."
            );
        }


        /*
         * Usamos ID del detalle como orden estable.
         *
         * De esa forma el contenido podrá reconstruirse
         * posteriormente sin depender del orden enviado
         * por el navegador.
         */
        List<DetalleRecetaMedica> ordenados =
                detalles
                        .stream()

                        .sorted(
                                Comparator.comparing(
                                        DetalleRecetaMedica::getId
                                )
                        )

                        .toList();


        StringBuilder contenido =
                new StringBuilder();


        contenido
                .append(
                        "TIPO=RECETA_MEDICA\n"
                )

                .append(
                        "ID_RECETA="
                )
                .append(
                        receta.getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_CITA="
                )
                .append(
                        receta.getCita().getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_CONSULTA="
                )
                .append(
                        receta.getConsultaMedica().getId()
                )
                .append(
                        '\n'
                )

                .append(
                        "ID_MEDICO="
                )
                .append(
                        receta.getMedico().getId()
                )
                .append(
                        '\n'
                );


        /*
         * Se firma con precisión de milisegundos.
         *
         * Esto evita diferencias posteriores entre la
         * precisión de OffsetDateTime en Java y TIMESTAMPTZ
         * de PostgreSQL.
         */
        if (receta.getFechaEmision() != null) {

            contenido
                    .append(
                            "FECHA_EMISION_EPOCH_MS="
                    )
                    .append(
                            receta
                                    .getFechaEmision()
                                    .toInstant()
                                    .toEpochMilli()
                    )
                    .append(
                            '\n'
                    );
        }


        contenido
                .append(
                        "CANTIDAD_MEDICAMENTOS="
                )
                .append(
                        ordenados.size()
                );


        int numero =
                1;


        for (DetalleRecetaMedica detalle : ordenados) {

            contenido
                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_ID_DETALLE="
                    )
                    .append(
                            detalle.getId()
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_ID_MEDICAMENTO="
                    )
                    .append(
                            detalle
                                    .getMedicamento()
                                    .getId()
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_NOMBRE="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getNombreMedicamento()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_CANTIDAD="
                    )
                    .append(
                            detalle.getCantidad()
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_DOSIS="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getDosis()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_FRECUENCIA="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getFrecuencia()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_DURACION="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getDuracion()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_INDICACIONES="
                    )
                    .append(
                            valorCanonico(
                                    detalle.getIndicacionesEspeciales()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
                    )
                    .append(
                            numero
                    )
                    .append(
                            "_PRECIO_UNITARIO="
                    )
                    .append(
                            valorPrecio(
                                    detalle.getPrecioUnitario()
                            )
                    )

                    .append(
                            '\n'
                    )

                    .append(
                            "ITEM_"
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
         * - activo
         * - @Version
         *
         * porque activo podría cambiar posteriormente
         * cuando Farmacia procese la receta, y eso no
         * debe invalidar la firma del documento clínico
         * originalmente emitido.
         */
        return contenido.toString();
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

            RecetaMedica receta,

            List<DetalleRecetaMedica> detalles,

            Usuario medico
    ) {

        Map<String, Object> nuevo =
                new LinkedHashMap<>();


        nuevo.put(
                "idCita",
                receta.getCita().getId()
        );


        nuevo.put(
                "idConsulta",
                receta
                        .getConsultaMedica()
                        .getId()
        );


        nuevo.put(
                "idMedico",
                medico.getId()
        );


        nuevo.put(
                "activo",
                receta.getActivo()
        );


        nuevo.put(
                "cantidadMedicamentos",
                detalles.size()
        );


        nuevo.put(
                "firmaDigitalHash",
                receta.getFirmaDigitalHash()
        );


        nuevo.put(
                "idsMedicamentos",
                detalles
                        .stream()

                        .map(
                                detalle ->
                                        detalle
                                                .getMedicamento()
                                                .getId()
                        )

                        .toList()
        );


        nuevo.put(
                "cantidades",
                detalles
                        .stream()

                        .map(
                                DetalleRecetaMedica
                                        ::getCantidad
                        )

                        .toList()
        );


        auditoriaService.registrar(
                new RegistroAuditoria(

                        "receta_medica",

                        String.valueOf(
                                receta.getId()
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
    // TEXTOS
    // =====================================================

    private String limpiarObligatorio(
            String valor
    ) {

        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isBlank()
                ? null
                : limpio;
    }


    private String obtenerIndicacion(

            List<String> indicaciones,

            int indice
    ) {

        if (indicaciones == null
                || indice < 0
                || indice >= indicaciones.size()) {

            return null;
        }


        String valor =
                indicaciones.get(indice);


        if (valor == null) {

            return null;
        }


        String limpio =
                valor.trim();


        return limpio.isBlank()
                ? null
                : limpio;
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
    // DTO DEL CATÁLOGO
    // =====================================================

    public record MedicamentoDisponible(

            Integer idMedicamento,

            String nombre,

            String descripcion,

            BigDecimal precio,

            String unidad,

            boolean controlado

    ) {
    }


    // =====================================================
    // RESULTADO PREPARACIÓN
    // =====================================================

    public record PreparacionReceta(

            Integer idCita,

            Integer idConsulta,

            List<MedicamentoDisponible> medicamentos

    ) {
    }


    // =====================================================
    // RESULTADO CREACIÓN
    // =====================================================

    public record ResultadoReceta(

            Integer idReceta,

            List<String> medicamentos,

            String mensaje

    ) {
    }
}