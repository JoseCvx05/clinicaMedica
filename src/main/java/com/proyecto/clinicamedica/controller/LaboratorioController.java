package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.service.LaboratorioService;
import com.proyecto.clinicamedica.dto.laboratorio.OrdenLaboratorioDetalleDTO;
import com.proyecto.clinicamedica.service.NotificacionResultadoLaboratorioService;
import com.proyecto.clinicamedica.dto.laboratorio.ResultadoLaboratorioDTO;
import com.proyecto.clinicamedica.service.ResultadoLaboratorioService;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


/**
 * =========================================================
 * CONTROLLER: LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Responsabilidades iniciales:
 *
 * - Mostrar pantalla "Órdenes de Laboratorio".
 * - Aplicar filtros por:
 *
 *      estado
 *      paciente
 *      médico
 *
 * - Delegar validación de Laboratorista al Service.
 * - No aceptar idSucursal desde el navegador.
 * - Registrar resultados.
 * - Permitir validación por Supervisor de Laboratorio.
 *
 * =========================================================
 */
@Controller
@RequestMapping(
        "/interno/laboratorio"
)
public class LaboratorioController {


    // =====================================================
    // VISTA
    // =====================================================

    private static final String VISTA_ORDENES =
            "interno/laboratorio/ordenes";


    private static final String VISTA_DETALLE =
            "interno/laboratorio/detalle-orden";


    private static final String VISTA_RESULTADO =
            "interno/laboratorio/resultado-form";


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final LaboratorioService
            laboratorioService;

    private final ResultadoLaboratorioService
            resultadoLaboratorioService;

    private final NotificacionResultadoLaboratorioService
            notificacionResultadoLaboratorioService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LaboratorioController(
            LaboratorioService laboratorioService,
            ResultadoLaboratorioService resultadoLaboratorioService,
            NotificacionResultadoLaboratorioService
                    notificacionResultadoLaboratorioService

    ) {

        this.laboratorioService =
                laboratorioService;

        this.resultadoLaboratorioService =
                resultadoLaboratorioService;

        this.notificacionResultadoLaboratorioService =
                notificacionResultadoLaboratorioService;
    }


    // =====================================================
    // LISTAR ÓRDENES
    // =====================================================
    //
    // CU-09 PASO 1
    //
    // Pantalla:
    //
    // "Órdenes de Laboratorio"
    //
    // Filtros:
    //
    // - Estado.
    // - Paciente.
    // - Médico.
    //
    // =====================================================

    @GetMapping
    public String mostrarOrdenes(

            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            @RequestParam(
                    name = "paciente",
                    required = false
            )
            String paciente,

            @RequestParam(
                    name = "medico",
                    required = false
            )
            String medico,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // LABORATORISTA AUTENTICADO
            // =============================================

            Usuario laboratorista =
                    laboratorioService
                            .obtenerLaboratoristaActual();


            // =============================================
            // CONSULTAR ÓRDENES
            // =============================================

            List<OrdenLaboratorio> ordenes =
                    laboratorioService
                            .filtrarOrdenes(

                                    estado,

                                    paciente,

                                    medico
                            );


            // =============================================
            // DATOS DEL USUARIO
            // =============================================

            model.addAttribute(
                    "laboratorista",
                    laboratorista
            );


            model.addAttribute(
                    "esSupervisor",
                    Boolean.TRUE.equals(
                            laboratorista
                                    .getSupervisorLaboratorio()
                    )
            );


            // =============================================
            // ÓRDENES
            // =============================================

            model.addAttribute(
                    "ordenes",
                    ordenes
            );


            // =============================================
            // ESTADOS DISPONIBLES
            // =============================================

            model.addAttribute(
                    "estados",
                    OrdenLaboratorio
                            .EstadoOrdenLaboratorio
                            .values()
            );


            // =============================================
            // CONSERVAR FILTROS
            // =============================================

            model.addAttribute(
                    "filtroEstado",
                    estado
            );


            model.addAttribute(
                    "filtroPaciente",
                    paciente
            );


            model.addAttribute(
                    "filtroMedico",
                    medico
            );


            return VISTA_ORDENES;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/dashboard";
        }
    }


    // =====================================================
    // CU-09
    // MOSTRAR DETALLE DE ORDEN
    // =====================================================
    //
    // Paso 2:
    //
    // Al seleccionar una orden se muestra:
    //
    // - Número.
    // - Paciente.
    // - Médico.
    // - Estado.
    // - Monto total.
    // - Indicador Interna / Externa.
    // - Observaciones.
    // - Exámenes solicitados.
    // - Precio individual.
    //
    // Seguridad:
    //
    // El ID recibido desde la URL NO determina por sí solo
    // qué orden puede consultar el usuario.
    //
    // LaboratorioService valida:
    //
    // - Laboratorista autenticado.
    // - Usuario activo.
    // - Rol Laboratorista.
    // - Sucursal.
    // - Orden perteneciente a esa misma sucursal.
    //
    // =====================================================

    @GetMapping(
            "/ordenes/{idOrden}"
    )
    public String mostrarDetalleOrden(

            @PathVariable
            Integer idOrden,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // OBTENER DETALLE SEGURO
            // =============================================

            OrdenLaboratorioDetalleDTO detalle =
                    laboratorioService
                            .obtenerDetalleOrden(
                                    idOrden
                            );


            // =============================================
            // LABORATORISTA ACTUAL
            // =============================================

            Usuario laboratorista =
                    laboratorioService
                            .obtenerLaboratoristaActual();


            // =============================================
            // MODELO
            // =============================================

            model.addAttribute(
                    "detalle",
                    detalle
            );


            model.addAttribute(
                    "laboratorista",
                    laboratorista
            );


            model.addAttribute(
                    "esSupervisor",
                    Boolean.TRUE.equals(
                            laboratorista
                                    .getSupervisorLaboratorio()
                    )
            );


            return VISTA_DETALLE;

        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/laboratorio";
        }
    }


    // =====================================================
    // CU-09
    // MOSTRAR FORMULARIO DE RESULTADO
    // =====================================================

    @GetMapping(
            "/ordenes/{idOrden}/resultados/{idDetalle}"
    )
    public String mostrarResultado(

            @PathVariable("idOrden")
            Integer idOrden,

            @PathVariable("idDetalle")
            Integer idDetalle,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // PREPARAR RESULTADO
            // =============================================

            ResultadoLaboratorioService
                    .ContextoResultadoLaboratorio contexto =

                    resultadoLaboratorioService
                            .prepararResultado(
                                    idOrden,
                                    idDetalle
                            );


            // =============================================
            // LABORATORISTA ACTUAL
            // =============================================
            //
            // Lo enviamos a la vista para poder determinar:
            //
            // - Si es Laboratorista normal.
            // - Si es Supervisor de Laboratorio.
            //
            // La seguridad REAL continúa en el Service.
            // =============================================

            Usuario laboratorista =
                    laboratorioService
                            .obtenerLaboratoristaActual();


            boolean esSupervisor =
                    Boolean.TRUE.equals(
                            laboratorista
                                    .getSupervisorLaboratorio()
                    );


            // =============================================
            // FORMULARIO
            // =============================================

            ResultadoLaboratorioDTO formulario =
                    new ResultadoLaboratorioDTO();


            formulario.setIdOrdenLaboratorio(
                    contexto.idOrdenLaboratorio()
            );

            formulario.setIdDetalleOrdenLaboratorio(
                    contexto.idDetalleOrdenLaboratorio()
            );

            formulario.setValorResultado(
                    contexto.valorResultado()
            );

            formulario.setUnidadResultado(
                    contexto.unidadResultado()
            );

            formulario.setFechaResultado(
                    contexto.fechaResultado()
            );

            formulario.setFueraRango(
                    contexto.fueraRango()
            );

            formulario.setNotasResultado(
                    contexto.notasResultado()
            );


            // =============================================
            // MODELO
            // =============================================

            model.addAttribute(
                    "contextoResultado",
                    contexto
            );

            model.addAttribute(
                    "resultadoLaboratorio",
                    formulario
            );

            model.addAttribute(
                    "laboratorista",
                    laboratorista
            );

            model.addAttribute(
                    "esSupervisor",
                    esSupervisor
            );


            return VISTA_RESULTADO;


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/laboratorio/ordenes/"
                    + idOrden;
        }
    }


    // =====================================================
    // CU-09
    // GUARDAR RESULTADO
    // =====================================================

    @PostMapping(
            "/resultados/guardar"
    )
    public String guardarResultado(

            @ModelAttribute("resultadoLaboratorio")
            ResultadoLaboratorioDTO formulario,

            RedirectAttributes redirectAttributes
    ) {

        Integer idOrden =
                formulario != null
                        ? formulario.getIdOrdenLaboratorio()
                        : null;


        Integer idDetalle =
                formulario != null
                        ? formulario.getIdDetalleOrdenLaboratorio()
                        : null;


        try {

            ResultadoLaboratorioService.ResultadoGuardado resultado =
                    resultadoLaboratorioService
                            .guardarResultado(
                                    formulario
                            );


            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            if (resultado.mensajeAlerta() != null
                    && !resultado.mensajeAlerta().isBlank()) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeAlerta",
                                resultado.mensajeAlerta()
                        );
            }


            return "redirect:/interno/laboratorio/ordenes/"
                    + resultado.idOrdenLaboratorio()
                    + "/resultados/"
                    + resultado.idDetalleOrdenLaboratorio();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            if (idOrden != null
                    && idOrden > 0
                    && idDetalle != null
                    && idDetalle > 0) {

                return "redirect:/interno/laboratorio/ordenes/"
                        + idOrden
                        + "/resultados/"
                        + idDetalle;
            }


            return "redirect:/interno/laboratorio";
        }
    }


    // =====================================================
    // CU-09
    // VALIDAR RESULTADO POR SUPERVISOR
    // =====================================================
    //
    // RN-CU09-02:
    //
    // Antes de publicar el resultado debe existir
    // una validación realizada por un Supervisor
    // de Laboratorio.
    //
    // IMPORTANTE:
    //
    // Aunque la vista oculte este botón para usuarios
    // normales, la autorización REAL se vuelve a verificar
    // dentro de ResultadoLaboratorioService.
    //
    // El navegador únicamente envía el ID del resultado.
    //
    // NO confiamos en:
    //
    // - supervisorLaboratorio enviado desde HTML.
    // - idUsuario enviado desde HTML.
    // - idSucursal enviado desde HTML.
    //
    // =====================================================

    @PostMapping(
            "/resultados/{idResultado}/validar"
    )
    public String validarResultadoSupervisor(

            @PathVariable("idResultado")
            Integer idResultado,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // VALIDACIÓN REAL
            // =============================================

            ResultadoLaboratorioService.ResultadoValidado resultado =
                    resultadoLaboratorioService
                            .validarResultadoSupervisor(
                                    idResultado
                            );


            // =============================================
            // MENSAJE
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            // =============================================
            // REGRESAR AL MISMO EXAMEN
            // =============================================

            return "redirect:/interno/laboratorio/ordenes/"
                    + resultado.idOrdenLaboratorio()
                    + "/resultados/"
                    + resultado.idDetalleOrdenLaboratorio();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            /*
             * En caso de intento inválido no utilizamos IDs
             * adicionales enviados desde el navegador para
             * reconstruir la ruta.
             *
             * Regresamos al módulo seguro de Laboratorio.
             */
            return "redirect:/interno/laboratorio";
        }
    }

    // =====================================================
// RNF-024
// AUTORIZAR CORRECCIÓN DE RESULTADO PUBLICADO
// =====================================================
//
// Un resultado publicado solamente puede volver
// al flujo de edición mediante autorización expresa
// de un Supervisor de Laboratorio.
//
// Seguridad:
//
// - El navegador envía únicamente:
//      idResultado
//      motivo
//
// - NO recibimos:
//      idUsuario
//      idSucursal
//      supervisorLaboratorio
//
// La seguridad real se valida nuevamente dentro de
// ResultadoLaboratorioService.
//
// =====================================================

    @PostMapping(
            "/resultados/{idResultado}/autorizar-correccion"
    )
    public String autorizarCorreccionResultado(

            @PathVariable("idResultado")
            Integer idResultado,

            @RequestParam(
                    name = "motivo"
            )
            String motivo,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // RNF-024
            // AUTORIZAR CORRECCIÓN
            // =============================================

            ResultadoLaboratorioService.CorreccionAutorizada resultado =
                    resultadoLaboratorioService
                            .autorizarCorreccionResultadoPublicado(
                                    idResultado,
                                    motivo
                            );


            // =============================================
            // MENSAJE
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            // =============================================
            // ORDEN REABIERTA
            // =============================================

            if (resultado.ordenReabierta()) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeAlerta",
                                "La orden de laboratorio fue reabierta "
                                        + "a estado En Proceso para realizar "
                                        + "la corrección autorizada."
                        );
            }


            // =============================================
            // ABRIR RESULTADO PARA CORREGIR
            // =============================================
            //
            // Después de la autorización:
            //
            // publicado = false
            // validadoSupervisor = false
            // correccionAutorizada = true
            //
            // Por eso ya puede abrirse nuevamente el
            // formulario normal de resultado.
            //
            // =============================================

            return "redirect:/interno/laboratorio/ordenes/"
                    + resultado.idOrdenLaboratorio()
                    + "/resultados/"
                    + resultado.idDetalleOrdenLaboratorio();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            /*
             * No utilizamos IDs adicionales recibidos
             * desde HTML para reconstruir una ruta.
             *
             * Si la autorización falla regresamos al
             * módulo seguro de Laboratorio.
             */
            return "redirect:/interno/laboratorio";
        }
    }

    // =====================================================
// CU-09
// PUBLICAR RESULTADO
// =====================================================
//
// Publica individualmente un resultado.
//
// La seguridad real se encuentra en
// ResultadoLaboratorioService:
//
// - Laboratorista autenticado.
// - Misma sucursal.
// - Orden interna.
// - Orden EN_PROCESO.
// - Pago aprobado.
// - Validación previa de supervisor.
// - Evita doble publicación.
//
// Si era el último examen pendiente,
// la orden pasa automáticamente a COMPLETADA.
// =====================================================

    @PostMapping(
            "/resultados/{idResultado}/publicar"
    )
    public String publicarResultado(

            @PathVariable("idResultado")
            Integer idResultado,

            RedirectAttributes redirectAttributes
    ) {

        try {

            // =============================================
            // PUBLICAR
            // =============================================

            ResultadoLaboratorioService.ResultadoPublicado resultado =
                    resultadoLaboratorioService
                            .publicarResultado(
                                    idResultado
                            );

            // =============================================
// CU-09 / RNF-007
// NOTIFICAR AL MÉDICO
// =============================================
//
// La publicación ya terminó correctamente.
//
// La notificación trabaja en una transacción
// independiente para que un fallo del correo
// nunca revierta el resultado publicado.
//
// =============================================

            try {

                notificacionResultadoLaboratorioService
                        .enviarNotificacion(
                                resultado.idResultadoLaboratorio()
                        );

            } catch (Exception ex) {

                System.err.println(
                        "El resultado de laboratorio #"
                                + resultado.idResultadoLaboratorio()
                                + " fue publicado correctamente, "
                                + "pero no fue posible procesar "
                                + "la notificación al Médico. "
                                + ex.getMessage()
                );
            }


            // =============================================
            // MENSAJE
            // =============================================

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeExito",
                            resultado.mensaje()
                    );


            // =============================================
            // ORDEN COMPLETADA
            // =============================================
            //
            // Si este era el último examen, NO regresamos
            // al formulario del resultado porque la orden
            // ya dejó de estar EN_PROCESO.
            //
            // Regresamos al detalle de la orden.
            // =============================================

            if (resultado.ordenCompletada()) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensajeExito",
                                resultado.mensaje()
                                        + " Todos los resultados de la orden "
                                        + "fueron publicados. "
                                        + "La orden ha sido completada."
                        );


                return "redirect:/interno/laboratorio/ordenes/"
                        + resultado.idOrdenLaboratorio();
            }


            // =============================================
            // TODAVÍA FALTAN EXÁMENES
            // =============================================
            //
            // Si la orden continúa EN_PROCESO,
            // volvemos al resultado publicado.
            // =============================================

            return "redirect:/interno/laboratorio/ordenes/"
                    + resultado.idOrdenLaboratorio()
                    + "/resultados/"
                    + resultado.idDetalleOrdenLaboratorio();


        } catch (
                IllegalArgumentException
                | IllegalStateException ex
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "mensajeError",
                            ex.getMessage()
                    );


            return "redirect:/interno/laboratorio";
        }
    }
}