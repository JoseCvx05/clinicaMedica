
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Despacho;

import com.proyecto.clinicamedica.entity.DetalleRecetaMedica;

import com.proyecto.clinicamedica.repository.DetalleDespachoRepository;
import com.proyecto.clinicamedica.repository.DetalleRecetaMedicaRepository;

import java.util.List;
import com.proyecto.clinicamedica.entity.RecetaMedica;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.DespachoRepository;
import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * =========================================================
 * SERVICE: PACIENTE NO ADQUIERE MEDICAMENTOS
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * FA03:
 *
 * Permite registrar cuando el paciente decide
 * no adquirir los medicamentos de su receta.
 *
 * REGLAS:
 *
 * - Solo un Farmaceutico autorizado.
 * - La receta debe pertenecer a su sucursal.
 * - La receta debe estar vigente.
 * - La consulta debe estar finalizada.
 * - El estado sera NO_ADQUIRIDO.
 * - El monto total sera Q0.00.
 * - No se registran detalles entregados.
 * - No se descuenta inventario.
 * - Se registra auditoria.
 * - Se protege contra reenvios duplicados.
 *
 * IMPORTANTE:
 *
 * Este servicio no modifica la receta medica.
 * Tampoco registra pagos.
 *
 * =========================================================
 */
@Service
public class FarmaciaNoAdquiridoService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final FarmaciaSeguridadService
            seguridadService;

    private final FarmaciaRecetaValidacionService
            recetaValidacionService;

    private final RecetaMedicaRepository
            recetaRepository;

    private final DespachoRepository
            despachoRepository;

    private final AuditoriaService
            auditoriaService;


    private final FarmaciaFirmaRecetaValidacionService
            firmaRecetaValidacionService;

    private final DetalleRecetaMedicaRepository
            detalleRecetaRepository;

    private final DetalleDespachoRepository
            detalleDespachoRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaNoAdquiridoService(
            FarmaciaSeguridadService seguridadService,
            FarmaciaRecetaValidacionService recetaValidacionService,
            RecetaMedicaRepository recetaRepository,
            DespachoRepository despachoRepository,
            AuditoriaService auditoriaService,
            FarmaciaFirmaRecetaValidacionService firmaRecetaValidacionService,
            DetalleRecetaMedicaRepository detalleRecetaRepository,
            DetalleDespachoRepository detalleDespachoRepository
    ) {

        this.seguridadService =
                seguridadService;

        this.recetaValidacionService =
                recetaValidacionService;

        this.recetaRepository =
                recetaRepository;

        this.despachoRepository =
                despachoRepository;

        this.auditoriaService =
                auditoriaService;

        this.firmaRecetaValidacionService =
                firmaRecetaValidacionService;

        this.detalleRecetaRepository =
                detalleRecetaRepository;

        this.detalleDespachoRepository =
                detalleDespachoRepository;
    }


    // =====================================================
    // RESULTADO
    // =====================================================

    public record ResultadoNoAdquirido(
            Integer idDespacho,
            Integer idReceta,
            boolean operacionRepetida
    ) {
    }


    // =====================================================
    // REGISTRAR NO ADQUIRIDO
    // =====================================================

    @Transactional
    public ResultadoNoAdquirido registrarNoAdquirido(
            Integer idReceta,
            UUID claveOperacion,
            String observaciones
    ) {


        // -------------------------------------------------
        // VALIDAR RECETA
        // -------------------------------------------------

        if (idReceta == null || idReceta <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una receta medica valida."
            );
        }


        // -------------------------------------------------
        // VALIDAR CLAVE DE OPERACION
        // -------------------------------------------------

        if (claveOperacion == null) {

            throw new IllegalArgumentException(
                    "La clave de operacion es obligatoria."
            );
        }


        // -------------------------------------------------
        // NORMALIZAR OBSERVACIONES
        // -------------------------------------------------

        String observacionesLimpias =
                normalizarTexto(observaciones);


        if (observacionesLimpias != null
                && observacionesLimpias.length() > 1000) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar "
                            + "los 1000 caracteres."
            );
        }


        // -------------------------------------------------
        // FARMACEUTICO AUTENTICADO
        // -------------------------------------------------

        Usuario farmaceutico =
                seguridadService
                        .obtenerFarmaceuticoAutorizado();


        Integer idSucursal =
                farmaceutico
                        .getSucursal()
                        .getId();


        // -------------------------------------------------
        // BLOQUEAR RECETA
        // -------------------------------------------------
        //
        // Utilizamos el mismo bloqueo empleado
        // por el servicio de despacho confirmado.
        //
        // -------------------------------------------------

        RecetaMedica receta =
                recetaRepository
                        .findWithLockById(idReceta)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "La receta medica no existe."
                                )
                        );


        // -------------------------------------------------
        // VALIDAR ACCESO A LA RECETA
        // -------------------------------------------------

        seguridadService.validarAccesoReceta(
                receta
        );


        // -------------------------------------------------
        // VERIFICAR SI YA EXISTE LA OPERACION
        // -------------------------------------------------

        Despacho anterior =
                despachoRepository
                        .findByClaveOperacion(
                                claveOperacion
                        )
                        .orElse(null);


        if (anterior != null) {

            return recuperarOperacionAnterior(
                    anterior,
                    receta,
                    farmaceutico,
                    idSucursal,
                    observacionesLimpias
            );
        }


        // -------------------------------------------------
        // CU-11 - FA03
        // EVITAR NO_ADQUIRIDO DUPLICADO POR RECETA
        // -------------------------------------------------
        //
        // El UUID protege contra el reenvio de una
        // misma operacion.
        //
        // Sin embargo, alguien podria generar otro
        // UUID y tratar de registrar nuevamente
        // NO_ADQUIRIDO sobre la misma receta.
        //
        // Por seguridad, permitimos solamente un
        // registro NO_ADQUIRIDO por receta.
        //
        // La verificacion se realiza despues de
        // recuperar operaciones existentes para
        // conservar la idempotencia original.
        //
        // IMPORTANTE:
        //
        // - No modifica inventario.
        // - No elimina registros anteriores.
        // - No modifica la receta.
        // - No afecta los despachos confirmados.
        // - Se ejecuta con la receta ya bloqueada.
        //
        // -------------------------------------------------

        boolean noAdquiridoRegistrado =
                despachoRepository
                        .existsByRecetaMedica_IdAndEstado(
                                receta.getId(),
                                Despacho.EstadoDespacho.NO_ADQUIRIDO
                        );


        // -------------------------------------------------
        // RECHAZAR UNA SEGUNDA DECISION
        // -------------------------------------------------

        if (noAdquiridoRegistrado) {

            throw new IllegalStateException(
                    "Esta receta ya tiene registrada "
                            + "la decision NO_ADQUIRIDO. "
                            + "No se permite registrar "
                            + "nuevamente con otra clave "
                            + "de operacion."
            );
        }



        // -------------------------------------------------
        // VALIDAR RECETA VIGENTE
        // -------------------------------------------------
        //
        // Una nueva operacion requiere una receta
        // activa, vigente y con consulta finalizada.
        //
        // -------------------------------------------------

        recetaValidacionService
                .validarParaDespacho(
                        receta
                );

        // -------------------------------------------------
        // OBTENER DETALLES ORIGINALES DE LA RECETA
        // -------------------------------------------------
        //
        // Necesitamos los medicamentos de la receta
        // para validar su firma digital y comprobar
        // si aun existen cantidades pendientes.
        //
        // -------------------------------------------------

        List<DetalleRecetaMedica> detalles =
                detalleRecetaRepository
                        .findByRecetaMedica_IdOrderByIdAsc(
                                receta.getId()
                        );


        if (detalles == null || detalles.isEmpty()) {

            throw new IllegalStateException(
                    "La receta médica no contiene medicamentos."
            );
        }


        // -------------------------------------------------
        // VALIDAR FIRMA DIGITAL DE LA RECETA
        // -------------------------------------------------
        //
        // Se utiliza el mismo servicio de seguridad
        // implementado para el despacho confirmado.
        //
        // No se permite registrar NO_ADQUIRIDO
        // sobre una receta cuyo contenido firmado
        // haya sido alterado.
        //
        // -------------------------------------------------

        firmaRecetaValidacionService.validar(
                receta,
                detalles
        );


        // -------------------------------------------------
        // VALIDAR MEDICAMENTOS PENDIENTES
        // -------------------------------------------------
        //
        // Impide registrar NO_ADQUIRIDO sobre una
        // receta que ya fue despachada completamente.
        //
        // Esta validacion se ejecuta en el backend
        // y no depende de los controles del HTML.
        //
        // -------------------------------------------------

        validarMedicamentosPendientes(
                detalles
        );

        // -------------------------------------------------
        // CREAR REGISTRO NO ADQUIRIDO
        // -------------------------------------------------

        Despacho despacho =
                new Despacho(
                        receta,
                        farmaceutico,
                        farmaceutico.getSucursal(),
                        Despacho.EstadoDespacho.NO_ADQUIRIDO,
                        new BigDecimal("0.00"),
                        observacionesLimpias,
                        claveOperacion
                );


        // -------------------------------------------------
        // GUARDAR EN POSTGRESQL
        // -------------------------------------------------

        despacho =
                despachoRepository
                        .saveAndFlush(
                                despacho
                        );


        // -------------------------------------------------
        // NO CREAR DETALLES DE DESPACHO
        // -------------------------------------------------
        //
        // El paciente no adquirio medicamentos.
        //
        // Por lo tanto:
        //
        // - No se guarda DetalleDespacho.
        // - No se modifica InventarioFarmacia.
        // - No se genera un pago.
        //
        // -------------------------------------------------


        // -------------------------------------------------
        // REGISTRAR AUDITORIA
        // -------------------------------------------------

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "despacho",

                        String.valueOf(
                                despacho.getId()
                        ),

                        AccionAuditoria.CREAR,

                        farmaceutico.getId(),

                        farmaceutico.getNombreUsuario(),

                        null,

                        Map.of(
                                "recetaId",
                                receta.getId(),

                                "sucursalId",
                                idSucursal,

                                "estado",
                                "NO_ADQUIRIDO",

                                "montoTotal",
                                "0.00",

                                "claveOperacion",
                                claveOperacion.toString()
                        ),

                        null
                )
        );


        // -------------------------------------------------
        // RESULTADO EXITOSO
        // -------------------------------------------------

        return new ResultadoNoAdquirido(
                despacho.getId(),
                receta.getId(),
                false
        );
    }


    // =====================================================
    // RECUPERAR OPERACION ANTERIOR
    // =====================================================
    //
    // Si el formulario se envia nuevamente con
    // la misma clave, no creamos otro registro.
    //
    // La clave solamente puede reutilizarse cuando
    // todos los datos corresponden a la misma operacion.
    //
    // =====================================================

    private ResultadoNoAdquirido recuperarOperacionAnterior(
            Despacho anterior,
            RecetaMedica receta,
            Usuario farmaceutico,
            Integer idSucursal,
            String observaciones
    ) {


        // -------------------------------------------------
        // VALIDAR MISMO ESTADO
        // -------------------------------------------------

        if (!anterior.noFueAdquirido()) {

            throw new IllegalStateException(
                    "La clave de operacion ya fue utilizada "
                            + "para un despacho diferente."
            );
        }


        // -------------------------------------------------
        // VALIDAR MISMA RECETA
        // -------------------------------------------------

        if (!Objects.equals(
                anterior.getRecetaMedica().getId(),
                receta.getId()
        )) {

            throw new IllegalStateException(
                    "La clave de operacion corresponde "
                            + "a otra receta medica."
            );
        }


        // -------------------------------------------------
        // VALIDAR MISMO FARMACEUTICO
        // -------------------------------------------------

        if (!Objects.equals(
                anterior.getFarmaceutico().getId(),
                farmaceutico.getId()
        )) {

            throw new IllegalStateException(
                    "La operacion pertenece "
                            + "a otro farmaceutico."
            );
        }


        // -------------------------------------------------
        // VALIDAR MISMA SUCURSAL
        // -------------------------------------------------

        if (!Objects.equals(
                anterior.getSucursal().getId(),
                idSucursal
        )) {

            throw new IllegalStateException(
                    "La operacion pertenece "
                            + "a otra sucursal."
            );
        }


        // -------------------------------------------------
        // VALIDAR OBSERVACIONES
        // -------------------------------------------------

        if (!Objects.equals(
                normalizarTexto(
                        anterior.getObservaciones()
                ),
                observaciones
        )) {

            throw new IllegalStateException(
                    "La clave de operacion ya existe "
                            + "con observaciones diferentes."
            );
        }


        // -------------------------------------------------
        // DEVOLVER RESULTADO EXISTENTE
        // -------------------------------------------------
        //
        // No volvemos a guardar ni auditar.
        //
        // -------------------------------------------------

        return new ResultadoNoAdquirido(
                anterior.getId(),
                anterior.getRecetaMedica().getId(),
                true
        );
    }


    // =====================================================
    // VALIDAR MEDICAMENTOS PENDIENTES
    // =====================================================
    //
    // CU-11 - FA03
    //
    // Verifica que la receta aun tenga al menos
    // una unidad pendiente de entregar.
    //
    // REGLAS:
    //
    // - Las cantidades prescritas deben ser validas.
    // - Se consultan las cantidades ya entregadas.
    // - Solo cuentan despachos CONFIRMADOS.
    // - No se aceptan cantidades inconsistentes.
    // - Si todo fue entregado, no permite NO_ADQUIRIDO.
    //
    // IMPORTANTE:
    //
    // Este metodo solo consulta informacion.
    // No modifica inventario ni despachos.
    //
    // =====================================================

    private void validarMedicamentosPendientes(
            List<DetalleRecetaMedica> detalles
    ) {

        // -------------------------------------------------
        // CONTROL DE UNIDADES PENDIENTES
        // -------------------------------------------------

        boolean existeCantidadPendiente = false;


        // -------------------------------------------------
        // RECORRER MEDICAMENTOS DE LA RECETA
        // -------------------------------------------------

        for (DetalleRecetaMedica detalle : detalles) {


            // ---------------------------------------------
            // VALIDAR DETALLE Y CANTIDAD PRESCRITA
            // ---------------------------------------------

            if (detalle == null
                    || detalle.getId() == null
                    || detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {

                throw new IllegalStateException(
                        "La receta contiene un detalle inválido."
                );
            }


            // ---------------------------------------------
            // CONSULTAR UNIDADES YA ENTREGADAS
            // ---------------------------------------------

            Long entregadas =
                    detalleDespachoRepository
                            .sumarCantidadEntregadaPorDetalleYEstado(
                                    detalle.getId(),
                                    Despacho.EstadoDespacho.CONFIRMADO
                            );


            // ---------------------------------------------
            // NORMALIZAR RESULTADO
            // ---------------------------------------------

            long cantidadEntregada =
                    entregadas == null
                            ? 0L
                            : entregadas;


            // ---------------------------------------------
            // VALIDAR CONSISTENCIA DEL HISTORIAL
            // ---------------------------------------------

            if (cantidadEntregada < 0
                    || cantidadEntregada > detalle.getCantidad()) {

                throw new IllegalStateException(
                        "El historial de cantidades entregadas "
                                + "de la receta es inconsistente."
                );
            }


            // ---------------------------------------------
            // COMPROBAR UNIDADES PENDIENTES
            // ---------------------------------------------

            if (cantidadEntregada < detalle.getCantidad()) {

                existeCantidadPendiente = true;
            }
        }


        // -------------------------------------------------
        // RECHAZAR RECETA COMPLETAMENTE DESPACHADA
        // -------------------------------------------------

        if (!existeCantidadPendiente) {

            throw new IllegalStateException(
                    "La receta ya fue despachada completamente. "
                            + "No quedan medicamentos pendientes "
                            + "para registrar NO_ADQUIRIDO."
            );
        }
    }


    // =====================================================
    // NORMALIZAR TEXTO
    // =====================================================

    private String normalizarTexto(
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
}
