package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.laboratorio.ResultadoLaboratorioDTO;

import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;
import com.proyecto.clinicamedica.repository.OrdenLaboratorioRepository;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Pago;
import com.proyecto.clinicamedica.entity.ResultadoLaboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;
import com.proyecto.clinicamedica.repository.PagoRepository;
import com.proyecto.clinicamedica.repository.ResultadoLaboratorioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;


/**
 * =========================================================
 * SERVICIO: RESULTADOS DE LABORATORIO
 * =========================================================
 *
 * CU-09 Gestión de Laboratorio.
 *
 * Responsabilidades:
 *
 * - Preparar captura de resultado.
 * - Validar orden y examen.
 * - Exigir pago aprobado.
 * - Exigir estado EN_PROCESO.
 * - Evitar resultados en órdenes externas.
 * - Evitar modificar resultados publicados.
 * - Guardar o actualizar el resultado.
 * - Manejar manualmente Fuera de Rango.
 * - Validar resultados por Supervisor de Laboratorio.
 * - Impedir cambios después de validación.
 * - Impedir auto-validación del resultado.
 *
 * RN-CU09-02:
 *
 * - Todo resultado debe ser validado por supervisor
 *   antes de su publicación.
 *
 * =========================================================
 */
@Service
public class ResultadoLaboratorioService {


    // =====================================================
    // MENSAJES CU-09
    // =====================================================

    public static final String MENSAJE_GUARDADO =
            "Resultado guardado exitosamente.";

    public static final String MENSAJE_FUERA_RANGO =
            "Los resultados están fuera del rango de referencia normal. "
                    + "Requiere revisión.";

    public static final String MENSAJE_VALIDADO =
            "Resultado validado correctamente por Supervisor de Laboratorio.";


    public static final String MENSAJE_PUBLICADO =
            "Resultado publicado exitosamente.";

    public static final String MENSAJE_CORRECCION_AUTORIZADA =
            "Corrección autorizada correctamente. "
                    + "El resultado debe corregirse, validarse "
                    + "nuevamente y volver a publicarse.";
    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ResultadoLaboratorioRepository
            resultadoLaboratorioRepository;

    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;

    private final PagoRepository
            pagoRepository;

    private final LaboratorioService
            laboratorioService;

    private final ZoneId
            zonaHoraria;

    private final OrdenLaboratorioRepository
            ordenLaboratorioRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ResultadoLaboratorioService(

            ResultadoLaboratorioRepository
                    resultadoLaboratorioRepository,

            DetalleOrdenLaboratorioRepository
                    detalleOrdenLaboratorioRepository,

            OrdenLaboratorioRepository
                    ordenLaboratorioRepository,

            PagoRepository pagoRepository,

            LaboratorioService laboratorioService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.resultadoLaboratorioRepository =
                resultadoLaboratorioRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.pagoRepository =
                pagoRepository;

        this.laboratorioService =
                laboratorioService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // PREPARAR FORMULARIO
    // =====================================================

    @Transactional(readOnly = true)
    public ContextoResultadoLaboratorio prepararResultado(

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio
    ) {

        validarId(
                idOrdenLaboratorio,
                "orden de laboratorio"
        );

        validarId(
                idDetalleOrdenLaboratorio,
                "examen"
        );


        // =============================================
        // ORDEN SEGURA
        // =============================================
        //
        // buscarOrdenSegura mantiene las validaciones
        // generales del módulo:
        //
        // - Usuario autenticado.
        // - Laboratorista.
        // - Sucursal correspondiente.
        // - Orden válida.
        //
        // =============================================

        OrdenLaboratorio ordenSegura =
                laboratorioService
                        .buscarOrdenSegura(
                                idOrdenLaboratorio
                        );


        if (ordenSegura.getEstado() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee un estado válido."
            );
        }


        // =============================================
        // USUARIO ACTUAL
        // =============================================

        Usuario laboratorista =
                laboratorioService
                        .obtenerLaboratoristaActual();


        if (laboratorista == null
                || laboratorista.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Laboratorista autenticado."
            );
        }


        // =============================================
        // ESTADO EN_PROCESO
        // =============================================
        //
        // Flujo normal del CU-09.
        //
        // Conservamos exactamente la validación existente.
        //
        // =============================================

        if (ordenSegura.getEstado()
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .EN_PROCESO) {

            laboratorioService
                    .validarOrdenParaResultados(
                            ordenSegura
                    );
        }


        // =============================================
        // RNF-024
        // ORDEN COMPLETADA
        // =============================================
        //
        // Una orden COMPLETADA solamente puede abrirse
        // desde este formulario si el usuario actual es
        // Supervisor de Laboratorio.
        //
        // IMPORTANTE:
        //
        // Esto NO habilita edición.
        //
        // El resultado sigue publicado y, por lo tanto,
        // continúa bloqueado hasta ejecutar expresamente
        // "Autorizar corrección".
        //
        // =============================================

        else if (ordenSegura.getEstado()
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .COMPLETADA) {

            if (!Boolean.TRUE.equals(
                    laboratorista.getSupervisorLaboratorio()
            )) {

                throw new IllegalStateException(
                        "Solamente un Supervisor de Laboratorio "
                                + "puede consultar un resultado publicado "
                                + "para autorizar su corrección."
                );
            }
        }


        // =============================================
        // OTROS ESTADOS
        // =============================================

        else {

            throw new IllegalStateException(
                    "El estado actual de la orden no permite "
                            + "consultar resultados de laboratorio."
            );
        }


        // =============================================
        // PAGO APROBADO
        // =============================================

        validarPagoAprobado(
                idOrdenLaboratorio
        );


        // =============================================
        // DETALLE DE ESA MISMA ORDEN
        // =============================================

        DetalleOrdenLaboratorio detalle =
                buscarDetalleSeguro(
                        idOrdenLaboratorio,
                        idDetalleOrdenLaboratorio
                );


        OrdenLaboratorio orden =
                detalle.getOrdenLaboratorio();


        // =============================================
        // RESULTADO EXISTENTE
        // =============================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findByDetalleOrdenLaboratorio_Id(
                                idDetalleOrdenLaboratorio
                        )
                        .orElse(
                                null
                        );


        // =============================================
        // RNF-024
        // DEFENSA PARA ORDEN COMPLETADA
        // =============================================
        //
        // Si la orden está COMPLETADA:
        //
        // - debe existir resultado;
        // - debe continuar publicado.
        //
        // No permitimos usar esta excepción para crear
        // resultados nuevos ni para saltarse el flujo.
        //
        // =============================================

        if (ordenSegura.getEstado()
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .COMPLETADA) {

            if (resultado == null) {

                throw new IllegalStateException(
                        "La orden está completada, pero el examen "
                                + "no posee un resultado registrado."
                );
            }


            if (!resultado.estaPublicado()) {

                throw new IllegalStateException(
                        "Solamente puede abrirse un resultado publicado "
                                + "desde una orden completada."
                );
            }
        }


        // =============================================
        // VALORES PARA FORMULARIO
        // =============================================

        String valorResultado =
                resultado != null
                        ? resultado.getValorResultado()
                        : null;


        String unidadResultado =
                resultado != null
                        ? resultado.getUnidadResultado()
                        : detalle.getUnidad();


        LocalDateTime fechaResultado =
                resultado != null
                        && resultado.getFechaResultado() != null

                        ? resultado
                        .getFechaResultado()
                        .atZoneSameInstant(
                                zonaHoraria
                        )
                        .toLocalDateTime()

                        : LocalDateTime.now(
                        zonaHoraria
                );


        boolean fueraRango =
                resultado != null
                        && resultado.estaFueraDeRango();


        String notasResultado =
                resultado != null
                        ? resultado.getNotasResultado()
                        : null;


        boolean publicado =
                resultado != null
                        && resultado.estaPublicado();


        boolean validado =
                resultado != null
                        && resultado.estaValidado();


        Integer idResultado =
                resultado != null
                        ? resultado.getId()
                        : null;


        return new ContextoResultadoLaboratorio(

                idOrdenLaboratorio,

                idDetalleOrdenLaboratorio,

                idResultado,

                orden.getNumeroOrden(),

                detalle.getNombreExamen(),

                detalle.getUnidad(),

                detalle.getRangoReferencia(),

                valorResultado,

                unidadResultado,

                fechaResultado,

                fueraRango,

                notasResultado,

                validado,

                publicado
        );
    }


    // =====================================================
    // GUARDAR RESULTADO
    // =====================================================

    @Transactional
    public ResultadoGuardado guardarResultado(
            ResultadoLaboratorioDTO formulario
    ) {

        if (formulario == null) {

            throw new IllegalArgumentException(
                    "Los datos del resultado son obligatorios."
            );
        }


        validarId(
                formulario.getIdOrdenLaboratorio(),
                "orden de laboratorio"
        );

        validarId(
                formulario.getIdDetalleOrdenLaboratorio(),
                "examen"
        );


        // =============================================
        // ORDEN VÁLIDA PARA RESULTADOS
        // =============================================

        OrdenLaboratorio ordenSegura =
                laboratorioService
                        .buscarOrdenSegura(
                                formulario.getIdOrdenLaboratorio()
                        );

        laboratorioService
                .validarOrdenParaResultados(
                        ordenSegura
                );


        // =============================================
        // PAGO PREVIO
        // =============================================

        validarPagoAprobado(
                formulario.getIdOrdenLaboratorio()
        );


        // =============================================
        // LABORATORISTA AUTENTICADO
        // =============================================

        Usuario laboratorista =
                laboratorioService
                        .obtenerLaboratoristaActual();


        if (laboratorista == null
                || laboratorista.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Laboratorista autenticado."
            );
        }


        // =============================================
        // EXAMEN SEGURO
        // =============================================

        DetalleOrdenLaboratorio detalle =
                buscarDetalleSeguro(

                        formulario.getIdOrdenLaboratorio(),

                        formulario
                                .getIdDetalleOrdenLaboratorio()
                );


        // =============================================
        // LIMPIAR DATOS
        // =============================================

        String valorResultado =
                limpiarObligatorio(
                        formulario.getValorResultado(),
                        "El valor del resultado es obligatorio."
                );


        String unidadResultado =
                limpiarObligatorio(
                        formulario.getUnidadResultado(),
                        "La unidad del resultado es obligatoria."
                );


        String notasResultado =
                limpiarOpcional(
                        formulario.getNotasResultado()
                );


        // =============================================
        // LONGITUDES
        // =============================================

        if (valorResultado.length() > 255) {

            throw new IllegalArgumentException(
                    "El valor del resultado no puede superar "
                            + "los 255 caracteres."
            );
        }


        if (unidadResultado.length() > 100) {

            throw new IllegalArgumentException(
                    "La unidad del resultado no puede superar "
                            + "los 100 caracteres."
            );
        }


        if (notasResultado != null
                && notasResultado.length() > 5000) {

            throw new IllegalArgumentException(
                    "Las notas del resultado no pueden superar "
                            + "los 5000 caracteres."
            );
        }


        // =============================================
        // FECHA RESULTADO
        // =============================================

        if (formulario.getFechaResultado() == null) {

            throw new IllegalArgumentException(
                    "La fecha del resultado es obligatoria."
            );
        }


        OffsetDateTime fechaResultado =
                formulario
                        .getFechaResultado()
                        .atZone(
                                zonaHoraria
                        )
                        .toOffsetDateTime();


        // =============================================
        // BUSCAR SI YA EXISTE
        // =============================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findByDetalleOrdenLaboratorio_Id(
                                detalle.getId()
                        )
                        .orElse(
                                null
                        );


        // =============================================
        // NUEVO
        // =============================================

        if (resultado == null) {

            resultado =
                    new ResultadoLaboratorio();


            resultado.setDetalleOrdenLaboratorio(
                    detalle
            );


            resultado.setUsuarioRegistro(
                    laboratorista
            );
        }


        // =============================================
        // RESULTADO PUBLICADO = INMUTABLE
        // =============================================

        if (resultado.estaPublicado()) {

            throw new IllegalStateException(
                    "El resultado ya fue publicado y no puede "
                            + "ser modificado sin autorización."
            );
        }


        // =============================================
        // RESULTADO VALIDADO
        //
        // Seguridad:
        //
        // No se permite cambiar el contenido después
        // de que el supervisor lo revisó.
        //
        // Evita:
        //
        // 1. Supervisor valida valor A.
        // 2. Laboratorista cambia a valor B.
        // 3. Se publica B usando la validación de A.
        // =============================================

        if (resultado.estaValidado()) {

            throw new IllegalStateException(
                    "El resultado ya fue validado por un supervisor "
                            + "y no puede modificarse antes de su publicación."
            );
        }


        // =============================================
        // ACTUALIZAR DATOS
        // =============================================

        resultado.setValorResultado(
                valorResultado
        );


        resultado.setUnidadResultado(
                unidadResultado
        );


        resultado.setFechaResultado(
                fechaResultado
        );


        resultado.setFueraRango(
                formulario.isFueraRango()
        );


        resultado.setNotasResultado(
                notasResultado
        );


        // =============================================
        // GUARDAR
        // =============================================

        ResultadoLaboratorio guardado =
                resultadoLaboratorioRepository
                        .saveAndFlush(
                                resultado
                        );


        return new ResultadoGuardado(

                guardado.getId(),

                formulario.getIdOrdenLaboratorio(),

                detalle.getId(),

                detalle.getNombreExamen(),

                formulario.isFueraRango(),

                MENSAJE_GUARDADO,

                formulario.isFueraRango()
                        ? MENSAJE_FUERA_RANGO
                        : null
        );
    }


    // =====================================================
    // VALIDAR RESULTADO POR SUPERVISOR
    // =====================================================
    //
    // RN-CU09-02:
    //
    // Ningún resultado puede publicarse sin revisión
    // previa de un Supervisor de Laboratorio.
    //
    // Seguridad:
    //
    // - Usuario autenticado.
    // - Debe ser Laboratorista.
    // - supervisorLaboratorio = true.
    // - Orden interna.
    // - Orden EN_PROCESO.
    // - Pago aprobado.
    // - Resultado no publicado.
    // - Resultado no validado previamente.
    // - No se permite auto-validación.
    //
    // =====================================================

    @Transactional
    public ResultadoValidado validarResultadoSupervisor(
            Integer idResultadoLaboratorio
    ) {

        validarId(
                idResultadoLaboratorio,
                "resultado de laboratorio"
        );


        // =============================================
        // SUPERVISOR AUTENTICADO
        //
        // obtenerLaboratoristaActual() ya exige que
        // el usuario autenticado pertenezca al rol
        // Laboratorista.
        // =============================================

        Usuario supervisor =
                laboratorioService
                        .obtenerLaboratoristaActual();


        if (supervisor == null
                || supervisor.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Supervisor "
                            + "de Laboratorio autenticado."
            );
        }


        // =============================================
        // BANDERA DE SUPERVISOR
        // =============================================

        if (!Boolean.TRUE.equals(
                supervisor.getSupervisorLaboratorio()
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "de Supervisor de Laboratorio."
            );
        }


        // =============================================
        // BUSCAR RESULTADO
        // =============================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findById(
                                idResultadoLaboratorio
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El resultado de laboratorio "
                                                        + "no existe."
                                        )
                        );


        // =============================================
        // VALIDAR RELACIÓN
        // RESULTADO -> DETALLE -> ORDEN
        // =============================================

        if (resultado.getDetalleOrdenLaboratorio() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a un examen válido."
            );
        }


        DetalleOrdenLaboratorio detalle =
                resultado
                        .getDetalleOrdenLaboratorio();


        if (detalle.getOrdenLaboratorio() == null
                || detalle.getOrdenLaboratorio().getId() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a una orden válida."
            );
        }


        OrdenLaboratorio orden =
                detalle.getOrdenLaboratorio();


        // =============================================
        // SEGURIDAD DE LA ORDEN
        //
        // Reutilizamos la validación central para no
        // duplicar reglas.
        //
        // Aquí debe quedar protegida:
        //
        // - Sucursal.
        // - Orden externa.
        // - Estado EN_PROCESO.
        // - Laboratorista autenticado.
        // =============================================

        laboratorioService
                .validarOrdenParaResultados(
                        orden
                );


        // =============================================
        // PAGO DEBE SEGUIR APROBADO
        // =============================================

        validarPagoAprobado(
                orden.getId()
        );


        // =============================================
        // RESULTADO PUBLICADO
        // =============================================

        if (resultado.estaPublicado()) {

            throw new IllegalStateException(
                    "El resultado ya fue publicado "
                            + "y no puede volver a validarse."
            );
        }


        // =============================================
        // RESULTADO YA VALIDADO
        // =============================================

        if (resultado.estaValidado()) {

            throw new IllegalStateException(
                    "El resultado ya fue validado por un supervisor."
            );
        }


        // =============================================
        // NO PERMITIR AUTO-VALIDACIÓN
        //
        // El mismo usuario que registró el resultado
        // no debe aprobar su propio trabajo.
        // =============================================

        if (resultado.getUsuarioRegistro() != null
                && resultado.getUsuarioRegistro().getId() != null
                && Objects.equals(

                resultado
                        .getUsuarioRegistro()
                        .getId(),

                supervisor.getId()
        )) {

            throw new IllegalStateException(
                    "El usuario que registró el resultado "
                            + "no puede validar su propio resultado."
            );
        }


        // =============================================
        // VALIDAR RESULTADO
        // =============================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        resultado.marcarValidado(
                supervisor,
                ahora
        );


        // =============================================
        // GUARDAR
        // =============================================

        ResultadoLaboratorio guardado =
                resultadoLaboratorioRepository
                        .saveAndFlush(
                                resultado
                        );


        // =============================================
        // RESPUESTA
        // =============================================

        return new ResultadoValidado(

                guardado.getId(),

                orden.getId(),

                detalle.getId(),

                MENSAJE_VALIDADO
        );
    }

    // =====================================================
// RNF-024
// AUTORIZAR CORRECCIÓN DE RESULTADO PUBLICADO
// =====================================================
//
// Únicamente un Laboratorista marcado como
// Supervisor de Laboratorio puede autorizar
// la modificación de un resultado ya publicado.
//
// Flujo:
//
// PUBLICADO
//      ↓
// Supervisor autoriza
//      ↓
// Resultado deja de estar publicado
//      ↓
// Validación anterior queda anulada
//      ↓
// Orden vuelve a EN_PROCESO si estaba COMPLETADA
//      ↓
// Laboratorista corrige
//      ↓
// Supervisor valida nuevamente
//      ↓
// Se publica nuevamente
//
// =====================================================

    @Transactional
    public CorreccionAutorizada autorizarCorreccionResultadoPublicado(

            Integer idResultadoLaboratorio,

            String motivo
    ) {

        // =================================================
        // ID
        // =================================================

        validarId(
                idResultadoLaboratorio,
                "resultado de laboratorio"
        );


        // =================================================
        // MOTIVO
        // =================================================

        String motivoLimpio =
                limpiarObligatorio(
                        motivo,
                        "Debe indicar el motivo de la corrección."
                );


        if (motivoLimpio.length() > 1000) {

            throw new IllegalArgumentException(
                    "El motivo de la corrección no puede superar "
                            + "los 1000 caracteres."
            );
        }


        // =================================================
        // SUPERVISOR AUTENTICADO
        // =================================================
        //
        // obtenerLaboratoristaActual() ya comprueba
        // que el usuario pertenece al módulo de Laboratorio.
        //
        // Después comprobamos expresamente que sea Supervisor.
        //
        // =================================================

        Usuario supervisor =
                laboratorioService
                        .obtenerLaboratoristaActual();


        if (supervisor == null
                || supervisor.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Supervisor "
                            + "de Laboratorio autenticado."
            );
        }


        if (!Boolean.TRUE.equals(
                supervisor.getSupervisorLaboratorio()
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene autorización "
                            + "para corregir resultados publicados."
            );
        }


        // =================================================
        // RESULTADO
        // =================================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findById(
                                idResultadoLaboratorio
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El resultado de laboratorio no existe."
                                        )
                        );


        // =================================================
        // DEBE ESTAR PUBLICADO
        // =================================================

        if (!resultado.estaPublicado()) {

            throw new IllegalStateException(
                    "Solamente puede autorizarse la corrección "
                            + "de un resultado que ya fue publicado."
            );
        }


        // =================================================
        // RELACIÓN RESULTADO -> DETALLE
        // =================================================

        if (resultado.getDetalleOrdenLaboratorio() == null
                || resultado
                .getDetalleOrdenLaboratorio()
                .getId() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a un examen válido."
            );
        }


        DetalleOrdenLaboratorio detalle =
                resultado
                        .getDetalleOrdenLaboratorio();


        // =================================================
        // RELACIÓN DETALLE -> ORDEN
        // =================================================

        if (detalle.getOrdenLaboratorio() == null
                || detalle
                .getOrdenLaboratorio()
                .getId() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a una orden válida."
            );
        }


        OrdenLaboratorio orden =
                detalle
                        .getOrdenLaboratorio();


        // =================================================
        // ORDEN SEGURA
        // =================================================
        //
        // Esta búsqueda vuelve a comprobar el contexto
        // del Laboratorista autenticado y la sucursal.
        //
        // No usamos directamente validarOrdenParaResultados()
        // todavía porque una orden completamente publicada
        // puede estar en estado COMPLETADA.
        //
        // =================================================

        OrdenLaboratorio ordenSegura =
                laboratorioService
                        .buscarOrdenSegura(
                                orden.getId()
                        );


        if (!Objects.equals(
                orden.getId(),
                ordenSegura.getId()
        )) {

            throw new IllegalStateException(
                    "La orden del resultado no es válida."
            );
        }


        // =================================================
        // PAGO
        // =================================================

        validarPagoAprobado(
                ordenSegura.getId()
        );


        // =================================================
        // ESTADO DE LA ORDEN
        // =================================================
        //
        // Para una corrección solamente aceptamos:
        //
        // EN_PROCESO:
        // todavía existen otros exámenes pendientes.
        //
        // COMPLETADA:
        // todos habían sido publicados y debemos
        // reabrirla para procesar la corrección.
        //
        // =================================================

        OrdenLaboratorio.EstadoOrdenLaboratorio estadoActual =
                ordenSegura.getEstado();


        if (estadoActual == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee un estado válido."
            );
        }


        if (estadoActual
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .PENDIENTE) {

            throw new IllegalStateException(
                    "No puede corregirse un resultado publicado "
                            + "desde una orden pendiente de pago."
            );
        }


        if (estadoActual
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .CANCELADA) {

            throw new IllegalStateException(
                    "No puede corregirse un resultado de una "
                            + "orden cancelada."
            );
        }


        if (estadoActual
                != OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .EN_PROCESO

                && estadoActual
                != OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .COMPLETADA) {

            throw new IllegalStateException(
                    "El estado actual de la orden no permite "
                            + "autorizar una corrección."
            );
        }


        // =================================================
        // SI TODAVÍA ESTÁ EN PROCESO
        // =================================================
        //
        // En este caso sí podemos reutilizar toda la
        // validación central de resultados.
        //
        // =================================================

        if (estadoActual
                == OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .EN_PROCESO) {

            laboratorioService
                    .validarOrdenParaResultados(
                            ordenSegura
                    );
        }


        // =================================================
        // AUTORIZAR CORRECCIÓN
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        resultado.autorizarCorreccion(
                supervisor,
                ahora,
                motivoLimpio
        );


        // =================================================
        // REABRIR ORDEN COMPLETADA
        // =================================================
        //
        // Como ahora existe al menos un resultado que ya
        // no está publicado, la orden tampoco puede seguir
        // figurando como COMPLETADA.
        //
        // =================================================

        boolean ordenReabierta =
                estadoActual
                        == OrdenLaboratorio
                        .EstadoOrdenLaboratorio
                        .COMPLETADA;


        if (ordenReabierta) {

            ordenSegura.setEstado(
                    OrdenLaboratorio
                            .EstadoOrdenLaboratorio
                            .EN_PROCESO
            );


            ordenLaboratorioRepository
                    .saveAndFlush(
                            ordenSegura
                    );
        }


        // =================================================
        // GUARDAR RESULTADO
        // =================================================

        ResultadoLaboratorio guardado =
                resultadoLaboratorioRepository
                        .saveAndFlush(
                                resultado
                        );


        // =================================================
        // RESPUESTA
        // =================================================

        return new CorreccionAutorizada(

                guardado.getId(),

                ordenSegura.getId(),

                detalle.getId(),

                ordenReabierta,

                motivoLimpio,

                MENSAJE_CORRECCION_AUTORIZADA
        );
    }

    // =====================================================
// CU-09
// PUBLICAR RESULTADO
// =====================================================
//
// El resultado únicamente puede publicarse cuando:
//
// - Pertenece a una orden válida.
// - La orden está EN_PROCESO.
// - Es una orden interna.
// - El pago continúa aprobado.
// - Existe un Laboratorista autenticado.
// - El resultado fue validado previamente.
// - El resultado todavía no está publicado.
//
// Después de publicar se verifica si todos los
// exámenes de la orden ya tienen resultado publicado.
// =====================================================

    @Transactional
    public ResultadoPublicado publicarResultado(
            Integer idResultadoLaboratorio
    ) {

        // =================================================
        // ID
        // =================================================

        validarId(
                idResultadoLaboratorio,
                "resultado de laboratorio"
        );


        // =================================================
        // LABORATORISTA AUTENTICADO
        // =================================================

        Usuario laboratorista =
                laboratorioService
                        .obtenerLaboratoristaActual();


        if (laboratorista == null
                || laboratorista.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Laboratorista autenticado."
            );
        }


        // =================================================
        // RESULTADO
        // =================================================

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findById(
                                idResultadoLaboratorio
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El resultado de laboratorio no existe."
                                        )
                        );


        // =================================================
        // DETALLE
        // =================================================

        if (resultado.getDetalleOrdenLaboratorio() == null
                || resultado
                .getDetalleOrdenLaboratorio()
                .getId() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a un examen válido."
            );
        }


        DetalleOrdenLaboratorio detalle =
                resultado
                        .getDetalleOrdenLaboratorio();


        // =================================================
        // ORDEN
        // =================================================

        if (detalle.getOrdenLaboratorio() == null
                || detalle
                .getOrdenLaboratorio()
                .getId() == null) {

            throw new IllegalStateException(
                    "El resultado no pertenece a una orden válida."
            );
        }


        OrdenLaboratorio orden =
                detalle
                        .getOrdenLaboratorio();


        // =================================================
        // SEGURIDAD DE LA ORDEN
        // =================================================
        //
        // Reutilizamos la misma validación central que ya
        // protege:
        //
        // - Laboratorista.
        // - Sucursal.
        // - Orden interna.
        // - Estado EN_PROCESO.
        //
        // =================================================

        laboratorioService
                .validarOrdenParaResultados(
                        orden
                );


        // =================================================
        // PAGO
        // =================================================

        validarPagoAprobado(
                orden.getId()
        );


        // =================================================
        // NO PERMITIR DOBLE PUBLICACIÓN
        // =================================================

        if (resultado.estaPublicado()) {

            throw new IllegalStateException(
                    "El resultado ya se encuentra publicado."
            );
        }


        // =================================================
        // VALIDACIÓN DE SUPERVISOR OBLIGATORIA
        // =================================================

        if (!resultado.estaValidado()) {

            throw new IllegalStateException(
                    "El resultado debe contar con validación "
                            + "de supervisor antes de publicarse."
            );
        }


        // =================================================
        // PUBLICAR
        // =================================================

        OffsetDateTime ahora =
                OffsetDateTime.now(
                        zonaHoraria
                );


        resultado.publicar(
                laboratorista,
                ahora
        );


        // =================================================
        // GUARDAR RESULTADO
        // =================================================

        ResultadoLaboratorio guardado =
                resultadoLaboratorioRepository
                        .saveAndFlush(
                                resultado
                        );


        // =================================================
        // COMPLETAR ORDEN SI TODOS ESTÁN PUBLICADOS
        // =================================================

        boolean ordenCompletada =
                completarOrdenSiCorresponde(
                        orden
                );


        // =================================================
        // RESPUESTA
        // =================================================

        return new ResultadoPublicado(

                guardado.getId(),

                orden.getId(),

                detalle.getId(),

                ordenCompletada,

                MENSAJE_PUBLICADO
        );
    }

    // =====================================================
// COMPLETAR ORDEN SI CORRESPONDE
// =====================================================
//
// Una orden se considera COMPLETADA cuando:
//
// cantidad de exámenes solicitados
//              ==
// cantidad de resultados publicados.
//
// Si todavía falta aunque sea un examen,
// la orden continúa EN_PROCESO.
// =====================================================

    private boolean completarOrdenSiCorresponde(
            OrdenLaboratorio orden
    ) {

        if (orden == null
                || orden.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar la orden de laboratorio."
            );
        }


        Integer idOrden =
                orden.getId();


        // =================================================
        // TOTAL DE EXÁMENES
        // =================================================

        long cantidadExamenes =
                detalleOrdenLaboratorioRepository
                        .countByOrdenLaboratorio_Id(
                                idOrden
                        );


        if (cantidadExamenes <= 0) {

            throw new IllegalStateException(
                    "La orden de laboratorio no contiene exámenes."
            );
        }


        // =================================================
        // RESULTADOS PUBLICADOS
        // =================================================
        //
        // Utilizamos findAll() por ahora para no modificar
        // ResultadoLaboratorioRepository en este mismo paso.
        //
        // En el siguiente ajuste podremos convertir esto
        // en una consulta COUNT directa si lo necesitamos.
        // =================================================

        long cantidadPublicados =
                resultadoLaboratorioRepository
                        .findAll()
                        .stream()
                        .filter(
                                resultado ->
                                        resultado != null
                                                && resultado.estaPublicado()
                                                && resultado
                                                .getDetalleOrdenLaboratorio()
                                                != null
                                                && resultado
                                                .getDetalleOrdenLaboratorio()
                                                .getOrdenLaboratorio()
                                                != null
                                                && Objects.equals(

                                                resultado
                                                        .getDetalleOrdenLaboratorio()
                                                        .getOrdenLaboratorio()
                                                        .getId(),

                                                idOrden
                                        )
                        )
                        .count();


        // =================================================
        // TODAVÍA FALTAN
        // =================================================

        if (cantidadPublicados
                < cantidadExamenes) {

            return false;
        }


        // =================================================
        // DEFENSA ANTE INCONSISTENCIA
        // =================================================

        if (cantidadPublicados
                > cantidadExamenes) {

            throw new IllegalStateException(
                    "La cantidad de resultados publicados "
                            + "no coincide con los exámenes solicitados."
            );
        }


        // =================================================
        // COMPLETAR ORDEN
        // =================================================

        orden.setEstado(
                OrdenLaboratorio
                        .EstadoOrdenLaboratorio
                        .COMPLETADA
        );


        ordenLaboratorioRepository
                .saveAndFlush(
                        orden
                );


        return true;
    }


    // =====================================================
    // DETALLE SEGURO
    // =====================================================

    private DetalleOrdenLaboratorio buscarDetalleSeguro(

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio
    ) {

        return detalleOrdenLaboratorioRepository
                .findByIdAndOrdenLaboratorio_Id(

                        idDetalleOrdenLaboratorio,

                        idOrdenLaboratorio
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El examen indicado no pertenece "
                                                + "a la orden de laboratorio."
                                )
                );
    }


    // =====================================================
    // PAGO APROBADO
    // =====================================================

    private void validarPagoAprobado(
            Integer idOrdenLaboratorio
    ) {

        boolean pagoAprobado =
                pagoRepository
                        .existsByOrdenLaboratorio_IdAndEstado(

                                idOrdenLaboratorio,

                                Pago.ESTADO_APROBADO
                        );


        if (!pagoAprobado) {

            throw new IllegalStateException(
                    "El pago debe estar completado antes de proceder "
                            + "con la toma de muestras."
            );
        }
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
    // TEXTO OBLIGATORIO
    // =====================================================

    private String limpiarObligatorio(

            String valor,

            String mensaje
    ) {

        if (valor == null
                || valor.trim().isBlank()) {

            throw new IllegalArgumentException(
                    mensaje
            );
        }


        return valor.trim();
    }


    // =====================================================
    // TEXTO OPCIONAL
    // =====================================================

    private String limpiarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.trim().isBlank()) {

            return null;
        }


        return valor.trim();
    }


    // =====================================================
    // CONTEXTO DEL FORMULARIO
    // =====================================================

    public record ContextoResultadoLaboratorio(

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio,

            Integer idResultadoLaboratorio,

            String numeroOrden,

            String nombreExamen,

            String unidadReferencia,

            String rangoReferencia,

            String valorResultado,

            String unidadResultado,

            LocalDateTime fechaResultado,

            boolean fueraRango,

            String notasResultado,

            boolean validadoSupervisor,

            boolean publicado

    ) {
    }


    // =====================================================
    // RESULTADO DE GUARDADO
    // =====================================================

    public record ResultadoGuardado(

            Integer idResultadoLaboratorio,

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio,

            String nombreExamen,

            boolean fueraRango,

            String mensaje,

            String mensajeAlerta

    ) {
    }


    // =====================================================
    // RESULTADO DE VALIDACIÓN
    // =====================================================

    public record ResultadoValidado(

            Integer idResultadoLaboratorio,

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio,

            String mensaje

    ) {
    }

    // =====================================================
// RESULTADO DE PUBLICACIÓN
// =====================================================

    public record ResultadoPublicado(

            Integer idResultadoLaboratorio,

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio,

            boolean ordenCompletada,

            String mensaje

    ) {
    }

    // =====================================================
// RNF-024
// RESULTADO DE AUTORIZACIÓN DE CORRECCIÓN
// =====================================================

    public record CorreccionAutorizada(

            Integer idResultadoLaboratorio,

            Integer idOrdenLaboratorio,

            Integer idDetalleOrdenLaboratorio,

            boolean ordenReabierta,

            String motivo,

            String mensaje

    ) {
    }
}