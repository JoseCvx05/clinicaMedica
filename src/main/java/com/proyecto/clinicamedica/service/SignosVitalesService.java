package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.SignosVitales;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.SignosVitalesRepository;

import com.proyecto.clinicamedica.service.signos.ReglaAlertaFrecuenciaCardiaca;
import com.proyecto.clinicamedica.service.signos.ReglaAlertaPresion;
import com.proyecto.clinicamedica.service.signos.ReglaAlertaTemperatura;
import com.proyecto.clinicamedica.service.signos.ReglaAlertaVital;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;
import com.proyecto.clinicamedica.service.signos.DatosSignosVitalesEntrada;

/**
 * =========================================================
 * SERVICIO: SIGNOS VITALES
 * =========================================================
 *
 * CU-07 - Toma de Signos Vitales.
 *
 * Responsabilidades:
 *
 * - Validar al Enfermero autenticado.
 * - Validar que la cita esté en "Signos Vitales".
 * - Evitar registros duplicados.
 * - Validar RN-CU07-01 a RN-CU07-05.
 * - Ejecutar reglas clínicas polimórficas RN-CU07-06.
 * - Registrar emergencia.
 * - Persistir signos vitales y alertas.
 * - Dejar la información disponible para el expediente.
 *
 * Las alertas clínicas NO bloquean el registro.
 *
 * =========================================================
 */
@Service
public class SignosVitalesService {


    // =====================================================
    // ESTADO / ROL
    // =====================================================

    private static final String ESTADO_SIGNOS_VITALES =
            "Signos Vitales";


    private static final String ROL_ENFERMERO =
            "Enfermero";


    // =====================================================
    // MENSAJES RN-CU07
    // =====================================================

    private static final String ERROR_PRESION =
            "La presión arterial debe ingresarse en formato "
                    + "sistólica/diastólica (ej: 120/80) "
                    + "dentro de rangos válidos.";


    private static final String ERROR_TEMPERATURA =
            "La temperatura debe estar entre 34.0 y 42.0°C "
                    + "con un decimal.";


    private static final String ERROR_PESO =
            "El peso debe estar entre 0.5 y 300 kg "
                    + "con dos decimales.";


    private static final String ERROR_TALLA =
            "La talla debe estar entre 30 y 250 cm "
                    + "con dos decimales.";


    private static final String ERROR_FRECUENCIA =
            "La frecuencia cardíaca debe estar entre 30 y 220 "
                    + "latidos por minuto.";


    // =====================================================
    // RANGOS DE CAPTURA
    // =====================================================

    private static final int SISTOLICA_MINIMA = 60;
    private static final int SISTOLICA_MAXIMA = 250;

    private static final int DIASTOLICA_MINIMA = 40;
    private static final int DIASTOLICA_MAXIMA = 150;


    private static final BigDecimal TEMPERATURA_MINIMA =
            new BigDecimal("34.0");

    private static final BigDecimal TEMPERATURA_MAXIMA =
            new BigDecimal("42.0");


    private static final BigDecimal PESO_MINIMO =
            new BigDecimal("0.50");

    private static final BigDecimal PESO_MAXIMO =
            new BigDecimal("300.00");


    private static final BigDecimal TALLA_MINIMA =
            new BigDecimal("30.00");

    private static final BigDecimal TALLA_MAXIMA =
            new BigDecimal("250.00");


    private static final int FRECUENCIA_MINIMA = 30;
    private static final int FRECUENCIA_MAXIMA = 220;


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final SignosVitalesRepository
            signosVitalesRepository;

    private final UsuarioActualService
            usuarioActualService;

    private final List<ReglaAlertaVital>
            reglasAlertas;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SignosVitalesService(

            CitaRepository citaRepository,

            SignosVitalesRepository signosVitalesRepository,

            UsuarioActualService usuarioActualService,

            List<ReglaAlertaVital> reglasAlertas,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.signosVitalesRepository =
                signosVitalesRepository;

        this.usuarioActualService =
                usuarioActualService;

        this.reglasAlertas =
                reglasAlertas;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // CONTEXTO DEL FORMULARIO
    // =====================================================

    /**
     * Recupera únicamente el contexto necesario para
     * abrir el formulario.
     *
     * El ID real del Enfermero se obtiene siempre
     * desde la sesión autenticada.
     */
    @Transactional(readOnly = true)
    public ContextoRegistroSignos obtenerContextoRegistro(
            Integer idCita
    ) {

        if (idCita == null
                || idCita <= 0) {

            throw new IllegalArgumentException(
                    "La cita seleccionada no es válida."
            );
        }


        Usuario enfermero =
                validarEnfermeroActual();


        Cita cita =
                citaRepository
                        .findById(
                                idCita
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La cita seleccionada no existe."
                                        )
                        );


        validarCitaParaRegistro(
                cita
        );


        if (signosVitalesRepository
                .existsByCita_Id(
                        idCita
                )) {

            throw new IllegalStateException(
                    "La cita ya posee un registro de signos vitales."
            );
        }


        boolean emergenciaPrevia =
                esEmergenciaPrevia(
                        cita
                );


        return new ContextoRegistroSignos(

                cita.getId(),

                cita.getPaciente()
                        .getNombreCompleto(),

                enfermero.getId(),

                emergenciaPrevia
        );
    }


    // =====================================================
    // REGISTRAR SIGNOS VITALES
    // =====================================================

    @Transactional
    public ResultadoRegistroSignos registrar(
            DatosSignosVitalesEntrada formulario
    ) {

        // =================================================
        // 1. VALIDACIÓN GENERAL
        // =================================================

        if (formulario == null
                || formulario.getIdCita() == null
                || formulario.getIdCita() <= 0) {

            return ResultadoRegistroSignos.error(
                    "La cita seleccionada no es válida."
            );
        }


        // =================================================
        // 2. ENFERMERO AUTENTICADO
        // =================================================

        Usuario enfermero =
                validarEnfermeroActual();


        // =================================================
        // 3. BLOQUEAR CITA
        // =================================================

        Cita cita =
                citaRepository
                        .buscarParaRegistrarSignosVitalesConBloqueo(
                                formulario.getIdCita()
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoRegistroSignos.error(
                    "La cita seleccionada no existe."
            );
        }


        // =================================================
        // 4. REVALIDAR ESTADO
        // =================================================

        try {

            validarCitaParaRegistro(
                    cita
            );

        } catch (IllegalStateException ex) {

            return ResultadoRegistroSignos.error(
                    ex.getMessage()
            );
        }


        // =================================================
        // 5. EVITAR DUPLICADOS
        // =================================================

        if (signosVitalesRepository
                .existsByCita_Id(
                        cita.getId()
                )) {

            return ResultadoRegistroSignos.error(
                    "La cita ya posee un registro de signos vitales."
            );
        }


        // =================================================
        // 6. FA02 - VALIDAR RANGOS DE CAPTURA
        // =================================================

        String errorValidacion =
                validarDatosCaptura(
                        formulario
                );


        if (errorValidacion != null) {

            return ResultadoRegistroSignos.error(
                    errorValidacion
            );
        }


        // =================================================
        // 7. FA03 - EVALUAR ALERTAS
        // =================================================

        EvaluacionAlertas alertas =
                evaluarAlertas(
                        formulario
                );


        // =================================================
        // 8. FA01 - EMERGENCIA
        // =================================================
        //
        // Si Recepción ya indicó prioridad de emergencia,
        // no permitimos perder esa condición aunque el
        // checkbox venga desmarcado.
        // =================================================

        boolean esEmergencia =
                Boolean.TRUE.equals(
                        formulario.getEsEmergencia()
                )
                        || esEmergenciaPrevia(
                        cita
                );


        // =================================================
        // 9. FECHA/HORA
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        // =================================================
        // 10. CONSTRUIR ENTIDAD
        // =================================================

        SignosVitales signos =
                new SignosVitales();


        signos.setCita(
                cita
        );


        signos.setEnfermero(
                enfermero
        );


        signos.setPresionSistolica(
                formulario.getPresionSistolica()
        );


        signos.setPresionDiastolica(
                formulario.getPresionDiastolica()
        );


        signos.setTemperatura(
                formulario.getTemperatura()
        );


        signos.setPeso(
                formulario.getPeso()
        );


        signos.setTalla(
                formulario.getTalla()
        );


        signos.setFrecuenciaCardiaca(
                formulario.getFrecuenciaCardiaca()
        );


        signos.setEsEmergencia(
                esEmergencia
        );


        signos.setAlertaPresion(
                alertas.alertaPresion()
        );


        signos.setAlertaTemperatura(
                alertas.alertaTemperatura()
        );


        signos.setAlertaFrecuenciaCardiaca(
                alertas.alertaFrecuenciaCardiaca()
        );


        signos.setFechaHoraRegistro(
                ahora
        );


        signos.setCreadoPor(
                enfermero
        );


        // =================================================
        // 11. GUARDAR
        // =================================================
        //
        // Al completarse esta transacción, el registro
        // queda inmediatamente disponible para el
        // expediente del paciente.
        // =================================================

        SignosVitales guardados =
                signosVitalesRepository
                        .saveAndFlush(
                                signos
                        );


        // =================================================
        // 12. MENSAJE FINAL
        // =================================================

        String nombrePaciente =
                cita.getPaciente()
                        .getNombreCompleto();


        String mensaje;


        if (esEmergencia) {

            mensaje =
                    "Signos vitales de emergencia registrados para paciente "
                            + nombrePaciente
                            + ". El paciente debe pasar directamente "
                            + "a consulta médica.";

        } else {

            mensaje =
                    "Signos vitales del paciente "
                            + nombrePaciente
                            + " registrados correctamente. "
                            + "El paciente puede regresar "
                            + "a la sala de espera.";
        }


        return ResultadoRegistroSignos.exito(

                mensaje,

                guardados.getId(),

                esEmergencia,

                alertas.mensajes()
        );
    }


    // =====================================================
    // VALIDAR DATOS DE CAPTURA
    // =====================================================

    /**
     * FA02.
     *
     * Devuelve el mensaje exacto de la primera regla
     * de captura que falle.
     *
     * Si todo es válido devuelve null.
     */
    private String validarDatosCaptura(
            DatosSignosVitalesEntrada datos
    ) {

        // =================================================
        // RN-CU07-01 - PRESIÓN
        // =================================================

        if (datos.getPresionSistolica() == null
                || datos.getPresionDiastolica() == null

                || datos.getPresionSistolica()
                < SISTOLICA_MINIMA

                || datos.getPresionSistolica()
                > SISTOLICA_MAXIMA

                || datos.getPresionDiastolica()
                < DIASTOLICA_MINIMA

                || datos.getPresionDiastolica()
                > DIASTOLICA_MAXIMA) {

            return ERROR_PRESION;
        }


        // =================================================
        // RN-CU07-02 - TEMPERATURA
        // =================================================

        if (datos.getTemperatura() == null

                || datos.getTemperatura()
                .compareTo(
                        TEMPERATURA_MINIMA
                ) < 0

                || datos.getTemperatura()
                .compareTo(
                        TEMPERATURA_MAXIMA
                ) > 0

                || !tieneDecimalesExactos(
                datos.getTemperatura(),
                1
        )) {

            return ERROR_TEMPERATURA;
        }


        // =================================================
        // RN-CU07-03 - PESO
        // =================================================

        if (datos.getPeso() == null

                || datos.getPeso()
                .compareTo(
                        PESO_MINIMO
                ) < 0

                || datos.getPeso()
                .compareTo(
                        PESO_MAXIMO
                ) > 0

                || !tieneDecimalesExactos(
                datos.getPeso(),
                2
        )) {

            return ERROR_PESO;
        }


        // =================================================
        // RN-CU07-04 - TALLA
        // =================================================

        if (datos.getTalla() == null

                || datos.getTalla()
                .compareTo(
                        TALLA_MINIMA
                ) < 0

                || datos.getTalla()
                .compareTo(
                        TALLA_MAXIMA
                ) > 0

                || !tieneDecimalesExactos(
                datos.getTalla(),
                2
        )) {

            return ERROR_TALLA;
        }


        // =================================================
        // RN-CU07-05 - FRECUENCIA CARDÍACA
        // =================================================

        if (datos.getFrecuenciaCardiaca() == null

                || datos.getFrecuenciaCardiaca()
                < FRECUENCIA_MINIMA

                || datos.getFrecuenciaCardiaca()
                > FRECUENCIA_MAXIMA) {

            return ERROR_FRECUENCIA;
        }


        return null;
    }


    // =====================================================
    // DECIMALES EXACTOS
    // =====================================================

    /**
     * La regla consolidada exige:
     *
     * Temperatura -> 1 decimal.
     * Peso        -> 2 decimales.
     * Talla       -> 2 decimales.
     *
     * Ejemplos:
     *
     * 36.0  -> válido para temperatura.
     * 36    -> inválido.
     *
     * 70.50 -> válido para peso.
     * 70.5  -> inválido.
     */
    private boolean tieneDecimalesExactos(

            BigDecimal valor,

            int decimales
    ) {

        return valor != null
                && valor.scale() == decimales;
    }


    // =====================================================
    // EVALUAR ALERTAS POLIMÓRFICAS
    // =====================================================

    private EvaluacionAlertas evaluarAlertas(
            DatosSignosVitalesEntrada datos
    ) {

        boolean alertaPresion =
                false;

        boolean alertaTemperatura =
                false;

        boolean alertaFrecuencia =
                false;


        List<String> mensajes =
                new ArrayList<>();


        for (ReglaAlertaVital regla :
                reglasAlertas) {


            ReglaAlertaVital.EvaluacionAlertaVital evaluacion =
                    regla.evaluar(
                            datos
                    );


            if (evaluacion == null
                    || !evaluacion.alerta()) {

                continue;
            }


            if (evaluacion.mensaje() != null
                    && !evaluacion.mensaje()
                    .isBlank()) {

                mensajes.add(
                        evaluacion.mensaje()
                );
            }


            // =============================================
            // MAPEO DE ALERTA AL REGISTRO
            // =============================================

            switch (evaluacion.codigo()) {

                case ReglaAlertaPresion.CODIGO ->
                        alertaPresion = true;


                case ReglaAlertaTemperatura.CODIGO ->
                        alertaTemperatura = true;


                case ReglaAlertaFrecuenciaCardiaca.CODIGO ->
                        alertaFrecuencia = true;


                default -> {

                    /*
                     * Una futura regla clínica puede existir
                     * sin romper el flujo.
                     *
                     * Para persistir un nuevo indicador
                     * específico también requeriría su
                     * correspondiente cambio de esquema.
                     */
                }
            }
        }


        return new EvaluacionAlertas(

                alertaPresion,

                alertaTemperatura,

                alertaFrecuencia,

                List.copyOf(
                        mensajes
                )
        );
    }


    // =====================================================
    // VALIDAR CITA
    // =====================================================

    private void validarCitaParaRegistro(
            Cita cita
    ) {

        if (cita == null
                || cita.getEstadoCita() == null
                || cita.getEstadoCita()
                .getNombre() == null

                || !ESTADO_SIGNOS_VITALES
                .equalsIgnoreCase(
                        cita.getEstadoCita()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "La cita no se encuentra en proceso "
                            + "de toma de signos vitales."
            );
        }
    }


    // =====================================================
    // EMERGENCIA PREVIA
    // =====================================================

    private boolean esEmergenciaPrevia(
            Cita cita
    ) {

        return cita != null
                && cita.getPrioridad() != null
                && "Emergencia"
                .equalsIgnoreCase(
                        cita.getPrioridad()
                                .trim()
                );
    }


    // =====================================================
    // VALIDAR ENFERMERO
    // =====================================================

    private Usuario validarEnfermeroActual() {

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

                || !ROL_ENFERMERO
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para registrar signos vitales."
            );
        }


        return usuario;
    }


    // =====================================================
    // CONTEXTO DE FORMULARIO
    // =====================================================

    public record ContextoRegistroSignos(

            Integer numeroCita,

            String nombrePaciente,

            Integer idEnfermero,

            boolean emergenciaPrevia

    ) {
    }


    // =====================================================
    // ALERTAS EVALUADAS
    // =====================================================

    private record EvaluacionAlertas(

            boolean alertaPresion,

            boolean alertaTemperatura,

            boolean alertaFrecuenciaCardiaca,

            List<String> mensajes

    ) {
    }


    // =====================================================
    // RESULTADO DEL REGISTRO
    // =====================================================

    public record ResultadoRegistroSignos(

            boolean exitoso,

            String mensaje,

            Integer idSignosVitales,

            boolean emergencia,

            List<String> alertas

    ) {


        public static ResultadoRegistroSignos exito(

                String mensaje,

                Integer idSignosVitales,

                boolean emergencia,

                List<String> alertas
        ) {

            return new ResultadoRegistroSignos(

                    true,

                    mensaje,

                    idSignosVitales,

                    emergencia,

                    alertas == null
                            ? List.of()
                            : List.copyOf(
                            alertas
                    )
            );
        }


        public static ResultadoRegistroSignos error(
                String mensaje
        ) {

            return new ResultadoRegistroSignos(

                    false,

                    mensaje,

                    null,

                    false,

                    List.of()
            );
        }
    }
}