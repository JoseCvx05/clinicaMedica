package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.OrdenLaboratorio;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.entity.OrdenLaboratorio.EstadoOrdenLaboratorio;
import com.proyecto.clinicamedica.repository.OrdenLaboratorioRepository;
import com.proyecto.clinicamedica.dto.laboratorio.OrdenLaboratorioDetalleDTO;
import com.proyecto.clinicamedica.entity.DetalleOrdenLaboratorio;

import com.proyecto.clinicamedica.repository.DetalleOrdenLaboratorioRepository;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * =========================================================
 * SERVICIO: LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Responsabilidades:
 *
 * - Validar Laboratorista autenticado.
 * - Validar usuario activo.
 * - Validar rol Laboratorista.
 * - Validar sucursal.
 * - Listar órdenes únicamente de su sucursal.
 * - Evitar acceso a órdenes de otra sucursal.
 * - Obtener órdenes con bloqueo para operaciones críticas.
 * - Validar autorización de Supervisor de Laboratorio.
 * - Diferenciar órdenes internas y externas.
 *
 * =========================================================
 */
@Service
public class LaboratorioService {


    // =====================================================
    // ROL
    // =====================================================

    private static final String ROL_LABORATORISTA =
            "Laboratorista";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final OrdenLaboratorioRepository
            ordenLaboratorioRepository;

    private final UsuarioActualService
            usuarioActualService;


    private final DetalleOrdenLaboratorioRepository
            detalleOrdenLaboratorioRepository;
    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LaboratorioService(

            OrdenLaboratorioRepository ordenLaboratorioRepository,

            DetalleOrdenLaboratorioRepository
                    detalleOrdenLaboratorioRepository,

            UsuarioActualService usuarioActualService
    ) {

        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.detalleOrdenLaboratorioRepository =
                detalleOrdenLaboratorioRepository;

        this.usuarioActualService =
                usuarioActualService;
    }


    // =====================================================
    // LISTAR ÓRDENES DE LA SUCURSAL
    // =====================================================
    //
    // CU-09 PASO 1:
    //
    // El personal de Laboratorio visualiza las órdenes
    // registradas correspondientes a su sucursal.
    //
    // También se incluyen órdenes externas porque deben
    // mostrarse identificadas como "Externa".
    //
    // =====================================================

    @Transactional(
            readOnly = true
    )
    public List<OrdenLaboratorio>
    listarOrdenesSucursalActual() {

        Usuario laboratorista =
                validarLaboratoristaActual();


        Integer idSucursal =
                obtenerIdSucursal(
                        laboratorista
                );


        return ordenLaboratorioRepository
                .findByCita_Sucursal_IdOrderByFechaHoraCreacionDesc(
                        idSucursal
                );
    }
    // =====================================================
// CU-09
// LISTAR / FILTRAR ÓRDENES
// =====================================================
//
// Permite filtrar por:
//
// - Estado.
// - Nombre del paciente.
// - Nombre del Médico.
//
// Siempre utiliza la sucursal del Laboratorista
// autenticado.
//
// Nunca recibe idSucursal desde el navegador.
// =====================================================

    @Transactional(
            readOnly = true
    )
    public List<OrdenLaboratorio>
    filtrarOrdenes(

            String estado,

            String paciente,

            String medico
    ) {

        Usuario laboratorista =
                validarLaboratoristaActual();


        Integer idSucursal =
                obtenerIdSucursal(
                        laboratorista
                );


        // =================================================
        // ESTADO
        // =================================================

        EstadoOrdenLaboratorio estadoOrden =
                convertirEstado(
                        estado
                );


        // =================================================
        // TEXTO DE BÚSQUEDA
        // =================================================

        String pacienteLimpio =
                limpiarFiltro(
                        paciente,
                        "paciente"
                );


        String medicoLimpio =
                limpiarFiltro(
                        medico,
                        "médico"
                );


        // =================================================
        // CONSULTA
        // =================================================

        return ordenLaboratorioRepository
                .filtrarOrdenesLaboratorio(

                        idSucursal,

                        estadoOrden,

                        pacienteLimpio,

                        medicoLimpio
                );
    }


    // =====================================================
    // BUSCAR ORDEN SEGURA
    // =====================================================
    //
    // Utilizado para consultas de solo lectura.
    //
    // Impide modificar el ID de la URL y abrir una orden
    // perteneciente a otra sucursal.
    //
    // =====================================================

    @Transactional(
            readOnly = true
    )
    public OrdenLaboratorio buscarOrdenSegura(
            Integer idOrden
    ) {

        validarIdOrden(
                idOrden
        );


        Usuario laboratorista =
                validarLaboratoristaActual();


        Integer idSucursal =
                obtenerIdSucursal(
                        laboratorista
                );


        return ordenLaboratorioRepository
                .findByIdAndCita_Sucursal_Id(

                        idOrden,

                        idSucursal
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "La orden de laboratorio no existe "
                                                + "o pertenece a otra sucursal."
                                )
                );
    }
    // =====================================================
// CU-09
// OBTENER DETALLE COMPLETO DE ORDEN
// =====================================================
//
// PASO 2:
//
// Al seleccionar una orden se muestra:
//
// - Número de orden.
// - Paciente.
// - Médico.
// - Estado.
// - Monto total.
// - Indicador de orden externa.
// - Observaciones.
// - Exámenes solicitados.
// - Precio individual.
//
// Seguridad:
//
// La orden primero se obtiene mediante buscarOrdenSegura(),
// por lo que un Laboratorista no puede abrir una orden
// perteneciente a otra sucursal.
// =====================================================

    @Transactional(
            readOnly = true
    )
    public OrdenLaboratorioDetalleDTO obtenerDetalleOrden(
            Integer idOrden
    ) {

        // =================================================
        // ORDEN SEGURA
        // =================================================

        OrdenLaboratorio orden =
                buscarOrdenSegura(
                        idOrden
                );


        // =================================================
        // VALIDAR RELACIONES PRINCIPALES
        // =================================================

        if (orden.getCita() == null
                || orden.getCita().getPaciente() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee "
                            + "un paciente válido."
            );
        }


        if (orden.getMedico() == null) {

            throw new IllegalStateException(
                    "La orden de laboratorio no posee "
                            + "un Médico solicitante válido."
            );
        }


        // =================================================
        // DETALLES / EXÁMENES
        // =================================================

        List<DetalleOrdenLaboratorio> detalles =
                detalleOrdenLaboratorioRepository
                        .findByOrdenLaboratorio_IdOrderByNombreExamenAsc(
                                orden.getId()
                        );


        if (detalles == null
                || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La orden de laboratorio no contiene "
                            + "exámenes registrados."
            );
        }


        // =================================================
        // CONSTRUIR EXÁMENES DEL DTO
        // =================================================

        List<OrdenLaboratorioDetalleDTO.ExamenOrdenDTO>
                examenes =

                detalles
                        .stream()

                        .map(
                                detalle ->
                                        new OrdenLaboratorioDetalleDTO
                                                .ExamenOrdenDTO(

                                                detalle.getId(),

                                                detalle.getNombreExamen(),

                                                detalle.getPrecioExamen(),

                                                detalle.getUnidad(),

                                                detalle.getRangoReferencia()
                                        )
                        )

                        .toList();


        // =================================================
        // MONTO TOTAL
        // =================================================
        //
        // IMPORTANTE:
        //
        // Se utiliza precio_examen almacenado en el detalle,
        // NO el precio actual del catálogo.
        //
        // Así se conserva el valor histórico de la orden.
        // =================================================

        BigDecimal montoTotal =
                detalles
                        .stream()

                        .map(
                                DetalleOrdenLaboratorio
                                        ::getPrecioExamen
                        )

                        .filter(
                                precio -> precio != null
                        )

                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        // =================================================
        // DTO FINAL
        // =================================================

        return new OrdenLaboratorioDetalleDTO(

                orden.getId(),

                orden.getNumeroOrden(),

                orden.getCita()
                        .getPaciente()
                        .getNombreCompleto(),

                orden.getMedico()
                        .getNombreCompleto(),

                orden.getEstado()
                        .name(),

                orden.isOrdenExterna(),

                orden.getObservaciones(),

                orden.getFechaHoraCreacion(),

                montoTotal,

                List.copyOf(
                        examenes
                )
        );
    }


    // =====================================================
    // BUSCAR ORDEN SEGURA CON BLOQUEO
    // =====================================================
    //
    // Utilizado cuando posteriormente hagamos:
    //
    // - cambio a EN_PROCESO;
    // - registro de resultados;
    // - validación;
    // - publicación;
    // - cambio a COMPLETADA.
    //
    // =====================================================

    @Transactional
    public OrdenLaboratorio
    buscarOrdenSeguraConBloqueo(
            Integer idOrden
    ) {

        validarIdOrden(
                idOrden
        );


        Usuario laboratorista =
                validarLaboratoristaActual();


        Integer idSucursal =
                obtenerIdSucursal(
                        laboratorista
                );


        return ordenLaboratorioRepository
                .buscarPorIdYSucursalConBloqueo(

                        idOrden,

                        idSucursal
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "La orden de laboratorio no existe "
                                                + "o pertenece a otra sucursal."
                                )
                );
    }


    // =====================================================
    // OBTENER LABORATORISTA ACTUAL
    // =====================================================
    //
    // Otros servicios de CU-09 podrán reutilizar esta
    // validación sin confiar en IDs enviados desde HTML.
    //
    // =====================================================

    @Transactional(
            readOnly = true
    )
    public Usuario obtenerLaboratoristaActual() {

        return validarLaboratoristaActual();
    }


    // =====================================================
    // OBTENER SUPERVISOR ACTUAL
    // =====================================================
    //
    // RN-CU09-02:
    //
    // La validación previa a publicación requiere una
    // autorización de supervisor.
    //
    // En el proyecto actual no existe un rol adicional
    // "Supervisor de Laboratorio".
    //
    // Por eso se exige:
    //
    // - usuario activo;
    // - rol Laboratorista;
    // - supervisorLaboratorio = true.
    //
    // =====================================================

    @Transactional(
            readOnly = true
    )
    public Usuario obtenerSupervisorLaboratorioActual() {

        Usuario laboratorista =
                validarLaboratoristaActual();


        if (!Boolean.TRUE.equals(
                laboratorista
                        .getSupervisorLaboratorio()
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado no posee autorización "
                            + "de Supervisor de Laboratorio."
            );
        }


        return laboratorista;
    }


    // =====================================================
    // VALIDAR ORDEN INTERNA
    // =====================================================
    //
    // Las órdenes externas deben mostrarse en CU-09,
    // pero NO deben procesarse dentro del laboratorio.
    //
    // =====================================================

    public void validarOrdenInterna(
            OrdenLaboratorio orden
    ) {

        if (orden == null
                || orden.getId() == null) {

            throw new IllegalArgumentException(
                    "La orden de laboratorio no es válida."
            );
        }


        if (orden.isOrdenExterna()) {

            throw new IllegalStateException(
                    "La orden seleccionada es externa y no puede "
                            + "ser procesada por el laboratorio interno."
            );
        }
    }


    // =====================================================
    // VALIDAR ORDEN PARA REGISTRO DE RESULTADOS
    // =====================================================
    //
    // RN-CU09-01:
    //
    // El pago debe estar completado antes de la toma
    // de muestras.
    //
    // En nuestro modelo, después del pago la orden deberá
    // encontrarse en EN_PROCESO.
    //
    // =====================================================

    public void validarOrdenParaResultados(
            OrdenLaboratorio orden
    ) {

        validarOrdenInterna(
                orden
        );


        if (orden.getEstado()
                != OrdenLaboratorio
                .EstadoOrdenLaboratorio
                .EN_PROCESO) {

            throw new IllegalStateException(
                    "El pago debe estar completado antes de proceder "
                            + "con la toma de muestras."
            );
        }
    }


    // =====================================================
    // VALIDAR LABORATORISTA ACTUAL
    // =====================================================

    private Usuario validarLaboratoristaActual() {

        Usuario usuario =
                usuarioActualService
                        .obtenerUsuarioActual();


        // -------------------------------------------------
        // USUARIO
        // -------------------------------------------------

        if (usuario == null
                || usuario.getId() == null) {

            throw new IllegalStateException(
                    "No fue posible identificar al Laboratorista autenticado."
            );
        }


        // -------------------------------------------------
        // ACTIVO
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(
                usuario.getActivo()
        )) {

            throw new IllegalStateException(
                    "El usuario autenticado se encuentra inactivo."
            );
        }


        // -------------------------------------------------
        // ROL
        // -------------------------------------------------

        if (usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || !ROL_LABORATORISTA
                .equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                                .trim()
                )) {

            throw new IllegalStateException(
                    "El usuario autenticado no tiene permisos "
                            + "para acceder al módulo de Laboratorio."
            );
        }


        // -------------------------------------------------
        // SUCURSAL
        // -------------------------------------------------

        obtenerIdSucursal(
                usuario
        );


        return usuario;
    }


    // =====================================================
    // OBTENER SUCURSAL
    // =====================================================

    private Integer obtenerIdSucursal(
            Usuario usuario
    ) {

        if (usuario == null
                || usuario.getSucursal() == null
                || usuario.getSucursal().getId() == null
                || usuario.getSucursal().getId() <= 0) {

            throw new IllegalStateException(
                    "No fue posible identificar la sucursal "
                            + "del Laboratorista autenticado."
            );
        }


        return usuario
                .getSucursal()
                .getId();
    }

    // =====================================================
// CONVERTIR ESTADO
// =====================================================

    private EstadoOrdenLaboratorio convertirEstado(
            String estado
    ) {

        if (estado == null
                || estado.isBlank()) {

            return null;
        }


        String valor =
                estado
                        .trim()
                        .toUpperCase();


        try {

            return EstadoOrdenLaboratorio
                    .valueOf(
                            valor
                    );

        } catch (
                IllegalArgumentException ex
        ) {

            throw new IllegalArgumentException(
                    "El estado de la orden de laboratorio no es válido."
            );
        }
    }


// =====================================================
// LIMPIAR FILTRO DE TEXTO
// =====================================================

    private String limpiarFiltro(

            String valor,

            String nombreCampo
    ) {

        /*
         * IMPORTANTE:
         *
         * No devolvemos null.
         *
         * PostgreSQL + Hibernate puede interpretar un parámetro
         * String nulo dentro de LOWER / CONCAT como bytea.
         *
         * Una cadena vacía produce:
         *
         * LIKE '%%'
         *
         * que equivale a no aplicar filtro.
         */
        if (valor == null
                || valor.isBlank()) {

            return "";
        }


        String limpio =
                valor.trim();


        if (limpio.length() > 100) {

            throw new IllegalArgumentException(
                    "El filtro de "
                            + nombreCampo
                            + " no puede superar los 100 caracteres."
            );
        }


        return limpio;
    }


    // =====================================================
    // VALIDAR ID
    // =====================================================

    private void validarIdOrden(
            Integer idOrden
    ) {

        if (idOrden == null
                || idOrden <= 0) {

            throw new IllegalArgumentException(
                    "El identificador de la orden de laboratorio "
                            + "no es válido."
            );
        }
    }
}