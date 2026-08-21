package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.caja.CajaBusquedaDTO;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.SucursalEspecialidad;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;


/**
 * =========================================================
 * SERVICIO: COBRO DE CONSULTA EN CAJA
 * =========================================================
 *
 * CU-06 Cobro de Consulta en Caja.
 *
 * En esta primera etapa maneja:
 *
 * - Búsqueda por número de cita.
 * - Búsqueda por DPI.
 * - Validaciones RN-GLOBAL-001.
 * - RN-CU06-01.
 * - Obtención segura del precio de consulta.
 *
 * Posteriormente también coordinará:
 *
 * - Procesamiento del medio de pago.
 * - Persistencia del Pago.
 * - Cambio de estado de la Cita.
 * - Generación del comprobante.
 *
 * Las reglas específicas de Efectivo y Tarjeta NO se
 * implementan aquí; corresponden a ProcesadorCobroCaja.
 *
 * =========================================================
 */
@Service
public class CobroCajaService {


    // =====================================================
    // MENSAJES
    // =====================================================

    public static final String MENSAJE_BUSQUEDA_OBLIGATORIA =
            "Debe ingresar un número de cita o DPI para buscar.";


    /**
     * Se utiliza el texto de RN-CU06-01 de las reglas
     * consolidadas.
     */
    public static final String MENSAJE_SIN_PENDIENTES =
            "No hay citas pendientes de pago bajo los parámetros indicados.";


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final CitaRepository citaRepository;

    private final UsuarioRepository usuarioRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final HashService hashService;

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public CobroCajaService(

            CitaRepository citaRepository,

            UsuarioRepository usuarioRepository,

            SucursalEspecialidadRepository
                    sucursalEspecialidadRepository,

            HashService hashService,

            @Value("${cita.zona-horaria:America/Guatemala}")
            String zonaHoraria
    ) {

        this.citaRepository =
                citaRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.hashService =
                hashService;

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // BUSCAR
    // =====================================================

    @Transactional(readOnly = true)
    public ResultadoBusquedaCaja buscar(
            CajaBusquedaDTO formulario
    ) {

        if (formulario == null) {

            return ResultadoBusquedaCaja.error(
                    MENSAJE_BUSQUEDA_OBLIGATORIA
            );
        }


        String tipo =
                normalizar(
                        formulario.getTipoBusqueda()
                );


        String valor =
                normalizar(
                        formulario.getValorBusqueda()
                );


        // =================================================
        // DPI VACÍO
        // =================================================

        if (CajaBusquedaDTO.TIPO_DPI
                .equalsIgnoreCase(tipo)
                && valor.isBlank()) {

            return ResultadoBusquedaCaja.error(

                    "El campo DPI es obligatorio. "
                            + "Por favor, ingrese su número de DPI."
            );
        }


        // =================================================
        // BÚSQUEDA VACÍA
        // =================================================

        if (valor.isBlank()) {

            return ResultadoBusquedaCaja.error(
                    MENSAJE_BUSQUEDA_OBLIGATORIA
            );
        }


        // =================================================
        // POR DPI
        // =================================================

        if (CajaBusquedaDTO.TIPO_DPI
                .equalsIgnoreCase(tipo)) {

            return buscarPorDpi(
                    valor
            );
        }


        // =================================================
        // POR NÚMERO DE CITA
        // =================================================

        if (CajaBusquedaDTO.TIPO_CITA
                .equalsIgnoreCase(tipo)) {

            return buscarPorNumero(
                    valor
            );
        }


        return ResultadoBusquedaCaja.error(
                MENSAJE_BUSQUEDA_OBLIGATORIA
        );
    }


    // =====================================================
    // BUSCAR POR NÚMERO
    // =====================================================

    private ResultadoBusquedaCaja buscarPorNumero(
            String valor
    ) {

        Integer idCita;


        try {

            idCita =
                    Integer.valueOf(
                            valor
                    );

        } catch (NumberFormatException ex) {

            return ResultadoBusquedaCaja.sinResultados(
                    MENSAJE_SIN_PENDIENTES
            );
        }


        if (idCita <= 0) {

            return ResultadoBusquedaCaja.sinResultados(
                    MENSAJE_SIN_PENDIENTES
            );
        }


        Cita cita =
                citaRepository
                        .buscarPendienteDePagoParaCajaPorNumero(
                                idCita
                        )
                        .orElse(
                                null
                        );


        if (cita == null) {

            return ResultadoBusquedaCaja.sinResultados(
                    MENSAJE_SIN_PENDIENTES
            );
        }


        return construirResultado(
                cita
        );
    }


    // =====================================================
    // BUSCAR POR DPI
    // =====================================================

    private ResultadoBusquedaCaja buscarPorDpi(
            String dpi
    ) {

        // =================================================
        // SOLO NÚMEROS
        // =================================================

        if (!dpi.matches("\\d+")) {

            return ResultadoBusquedaCaja.error(

                    "El DPI debe contener únicamente números. "
                            + "No se permiten letras ni caracteres especiales."
            );
        }


        // =================================================
        // 13 DÍGITOS
        // =================================================

        if (dpi.length() != 13) {

            return ResultadoBusquedaCaja.error(

                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpi.length()
                            + " dígitos."
            );
        }


        // =================================================
        // PROTEGER DPI
        // =================================================

        String dpiHash =
                hashService
                        .generarHash(
                                dpi
                        );


        Usuario paciente =
                usuarioRepository
                        .findByDpiHash(
                                dpiHash
                        )
                        .orElse(
                                null
                        );


        /*
         * CU-06 no necesita distinguir públicamente entre:
         *
         * - paciente inexistente;
         * - paciente sin citas;
         * - paciente con citas ya pagadas;
         *
         * En todos esos escenarios no hay una cita
         * Pendiente de pago disponible para Caja.
         */
        if (paciente == null) {

            return ResultadoBusquedaCaja.sinResultados(
                    MENSAJE_SIN_PENDIENTES
            );
        }


        List<Cita> citas =
                citaRepository
                        .buscarPendientesDePagoParaCajaPorPaciente(

                                paciente.getId(),

                                OffsetDateTime.now(
                                        zonaHoraria
                                )
                        );


        if (citas.isEmpty()) {

            return ResultadoBusquedaCaja.sinResultados(
                    MENSAJE_SIN_PENDIENTES
            );
        }


        /*
         * El documento habla de mostrar el detalle
         * de la cita en singular.
         *
         * El repositorio ordena las pendientes por fecha,
         * mostrando primero la más próxima.
         *
         * Más adelante podemos mejorar la UX mostrando
         * una selección cuando existan varias pendientes,
         * pero no es requisito explícito de CU-06.
         */
        return construirResultado(
                citas.get(0)
        );
    }


    // =====================================================
    // CONSTRUIR RESULTADO
    // =====================================================

    private ResultadoBusquedaCaja construirResultado(
            Cita cita
    ) {

        // =================================================
        // PRECIO REAL DESDE BD
        // =================================================

        SucursalEspecialidad configuracion =
                sucursalEspecialidadRepository
                        .findBySucursal_IdAndEspecialidad_IdAndActivoTrue(

                                cita.getSucursal()
                                        .getId(),

                                cita.getEspecialidad()
                                        .getId()
                        )
                        .orElse(
                                null
                        );


        if (configuracion == null
                || configuracion.getPrecioConsulta() == null
                || configuracion.getPrecioConsulta()
                .compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return ResultadoBusquedaCaja.error(
                    "No fue posible determinar el monto de la consulta."
            );
        }


        DetalleCobroCaja detalle =
                new DetalleCobroCaja(

                        cita.getId(),

                        cita.getPaciente()
                                .getId(),

                        cita.getPaciente()
                                .getNombreCompleto(),

                        cita.getEspecialidad()
                                .getNombre(),

                        cita.getMedico()
                                .getNombreCompleto(),

                        cita.getSucursal()
                                .getNombre(),

                        convertirAZonaHoraria(
                                cita.getFechaHoraCita()
                        ),

                        configuracion
                                .getPrecioConsulta(),

                        cita.getEstadoCita()
                                .getNombre()
                );


        return ResultadoBusquedaCaja.encontrada(
                detalle
        );
    }


    // =====================================================
    // ZONA HORARIA
    // =====================================================

    /**
     * Evita repetir el problema que corregimos en CU-05:
     * PostgreSQL/JDBC puede representar el mismo instante
     * con offset UTC.
     *
     * Caja siempre muestra hora local de Guatemala.
     */
    private OffsetDateTime convertirAZonaHoraria(
            OffsetDateTime fechaHora
    ) {

        if (fechaHora == null) {

            return null;
        }


        return fechaHora
                .atZoneSameInstant(
                        zonaHoraria
                )
                .toOffsetDateTime();
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private String normalizar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }


    // =====================================================
    // RESULTADOS
    // =====================================================

    public enum TipoResultadoCaja {

        ENCONTRADA,

        SIN_RESULTADOS,

        ERROR_VALIDACION
    }


    public record ResultadoBusquedaCaja(

            TipoResultadoCaja tipo,

            String mensaje,

            DetalleCobroCaja cita

    ) {


        public static ResultadoBusquedaCaja encontrada(
                DetalleCobroCaja cita
        ) {

            return new ResultadoBusquedaCaja(

                    TipoResultadoCaja.ENCONTRADA,

                    null,

                    cita
            );
        }


        public static ResultadoBusquedaCaja sinResultados(
                String mensaje
        ) {

            return new ResultadoBusquedaCaja(

                    TipoResultadoCaja.SIN_RESULTADOS,

                    mensaje,

                    null
            );
        }


        public static ResultadoBusquedaCaja error(
                String mensaje
        ) {

            return new ResultadoBusquedaCaja(

                    TipoResultadoCaja.ERROR_VALIDACION,

                    mensaje,

                    null
            );
        }
    }


    // =====================================================
    // DETALLE PARA LA PANTALLA DE CAJA
    // =====================================================

    public record DetalleCobroCaja(

            Integer numeroCita,

            Integer idPaciente,

            String nombrePaciente,

            String especialidad,

            String medico,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            BigDecimal montoTotal,

            String estado

    ) {
    }
}