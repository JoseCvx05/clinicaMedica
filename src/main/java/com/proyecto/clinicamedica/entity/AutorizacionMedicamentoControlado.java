
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * =========================================================
 * ENTIDAD: AUTORIZACION DE MEDICAMENTO CONTROLADO
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * RNF-017:
 *
 * Los medicamentos controlados requieren una
 * autorizacion adicional y registro de auditoria.
 *
 * REGLAS:
 *
 * - Solo el medico tratante puede autorizar.
 * - La autorizacion pertenece a un detalle de receta.
 * - Solo se autorizan medicamentos controlados.
 * - La cantidad autorizada no supera la prescrita.
 * - La fecha es generada por el backend.
 * - No se permiten autorizaciones duplicadas.
 * - El registro es inmutable mediante JPA.
 *
 * IMPORTANTE:
 *
 * El Service verificara adicionalmente:
 *
 * - La identidad autenticada del medico.
 * - El rol MEDICO y su estado activo.
 * - La vigencia maxima de 7 dias.
 * - La sucursal correspondiente.
 * - El registro de auditoria.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "autorizacion_medicamento_controlado",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_autorizacion_controlado_detalle",
                        columnNames = {
                                "id_detalle_receta_medica"
                        }
                )
        }
)
public class AutorizacionMedicamentoControlado {

    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // DETALLE ORIGINAL DE LA RECETA
    // =====================================================

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_detalle_receta_medica",
            nullable = false,
            updatable = false
    )
    private DetalleRecetaMedica detalleRecetaMedica;


    // =====================================================
    // MEDICO QUE AUTORIZA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_medico_autorizador",
            nullable = false,
            updatable = false
    )
    private Usuario medicoAutorizador;


    // =====================================================
    // CANTIDAD AUTORIZADA
    // =====================================================
    //
    // Se toma de la cantidad prescrita.
    // El navegador no define este valor.
    //
    // =====================================================

    @Column(
            name = "cantidad_autorizada",
            nullable = false,
            updatable = false
    )
    private Integer cantidadAutorizada;


    // =====================================================
    // FECHA Y HORA DE AUTORIZACION
    // =====================================================

    @Column(
            name = "fecha_hora_autorizacion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHoraAutorizacion;


    // =====================================================
    // OBSERVACIONES DEL MEDICO
    // =====================================================

    @Column(
            name = "observaciones",
            length = 1000,
            updatable = false
    )
    private String observaciones;


    // =====================================================
    // CONSTRUCTOR PARA JPA
    // =====================================================

    public AutorizacionMedicamentoControlado() {
    }


    // =====================================================
    // CONSTRUCTOR DE DOMINIO
    // =====================================================

    public AutorizacionMedicamentoControlado(
            DetalleRecetaMedica detalleRecetaMedica,
            Usuario medicoAutorizador,
            String observaciones
    ) {

        this.detalleRecetaMedica =
                detalleRecetaMedica;

        this.medicoAutorizador =
                medicoAutorizador;

        this.observaciones =
                observaciones;

        if (detalleRecetaMedica != null) {

            this.cantidadAutorizada =
                    detalleRecetaMedica.getCantidad();
        }

        normalizar();

        validar();
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        if (fechaHoraAutorizacion == null) {

            fechaHoraAutorizacion =
                    OffsetDateTime.now();
        }

        normalizar();

        validar();
    }


    // =====================================================
    // PROTEGER REGISTRO HISTORICO
    // =====================================================
    //
    // Una autorizacion guardada no se puede
    // modificar ni eliminar mediante JPA.
    //
    // La proteccion adicional a nivel de base
    // de datos se evaluara por separado.
    //
    // =====================================================

    @PreUpdate
    @PreRemove
    private void impedirModificacion() {

        throw new IllegalStateException(
                "Las autorizaciones de medicamentos "
                        + "controlados son registros históricos "
                        + "y no pueden modificarse ni eliminarse."
        );
    }


    // =====================================================
    // NORMALIZACION
    // =====================================================

    private void normalizar() {

        if (observaciones != null) {

            observaciones =
                    observaciones.trim();

            if (observaciones.isBlank()) {

                observaciones = null;
            }
        }
    }


    // =====================================================
    // VALIDACIONES EN BACKEND
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // DETALLE DE RECETA OBLIGATORIO
        // -------------------------------------------------

        if (detalleRecetaMedica == null
                || detalleRecetaMedica.getId() == null) {

            throw new IllegalStateException(
                    "Debe seleccionar un detalle de receta válido."
            );
        }


        // -------------------------------------------------
        // RECETA MEDICA VALIDA
        // -------------------------------------------------

        RecetaMedica receta =
                detalleRecetaMedica.getRecetaMedica();

        if (receta == null
                || receta.getId() == null) {

            throw new IllegalStateException(
                    "El medicamento debe pertenecer a una receta médica válida."
            );
        }


        // -------------------------------------------------
        // RECETA ACTIVA
        // -------------------------------------------------

        if (!receta.estaActiva()) {

            throw new IllegalStateException(
                    "No se puede autorizar un medicamento "
                            + "de una receta inactiva."
            );
        }


        // -------------------------------------------------
        // MEDICO AUTORIZADOR OBLIGATORIO
        // -------------------------------------------------

        if (medicoAutorizador == null
                || medicoAutorizador.getId() == null) {

            throw new IllegalStateException(
                    "Debe existir un médico autorizador válido."
            );
        }


        // -------------------------------------------------
        // MISMO MEDICO DE LA RECETA
        // -------------------------------------------------

        if (receta.getMedico() == null
                || receta.getMedico().getId() == null
                || !Objects.equals(
                receta.getMedico().getId(),
                medicoAutorizador.getId()
        )) {

            throw new IllegalStateException(
                    "Solo el médico tratante responsable "
                            + "de la receta puede autorizar "
                            + "este medicamento controlado."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO VALIDO
        // -------------------------------------------------

        Medicamento medicamento =
                detalleRecetaMedica.getMedicamento();

        if (medicamento == null
                || medicamento.getId() == null) {

            throw new IllegalStateException(
                    "El medicamento seleccionado no es válido."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ACTIVO
        // -------------------------------------------------

        if (!medicamento.estaActivo()) {

            throw new IllegalStateException(
                    "No se puede autorizar un medicamento inactivo."
            );
        }


        // -------------------------------------------------
        // DEBE SER MEDICAMENTO CONTROLADO
        // -------------------------------------------------

        if (!medicamento.esControlado()) {

            throw new IllegalStateException(
                    "El medicamento seleccionado "
                            + "no requiere autorización especial."
            );
        }


        // -------------------------------------------------
        // CANTIDAD PRESCRITA VALIDA
        // -------------------------------------------------

        Integer cantidadPrescrita =
                detalleRecetaMedica.getCantidad();

        if (cantidadPrescrita == null
                || cantidadPrescrita <= 0) {

            throw new IllegalStateException(
                    "La cantidad prescrita del medicamento "
                            + "no es válida."
            );
        }


        // -------------------------------------------------
        // CANTIDAD AUTORIZADA VALIDA
        // -------------------------------------------------

        if (cantidadAutorizada == null
                || cantidadAutorizada <= 0
                || cantidadAutorizada > cantidadPrescrita) {

            throw new IllegalStateException(
                    "La cantidad autorizada debe ser mayor "
                            + "a cero y no puede superar "
                            + "la cantidad prescrita."
            );
        }


        // -------------------------------------------------
        // ESTADO DEL MEDICO
        // -------------------------------------------------

        if (!Boolean.TRUE.equals(
                medicoAutorizador.getActivo()
        )) {

            throw new IllegalStateException(
                    "El médico autorizador debe encontrarse activo."
            );
        }


        // -------------------------------------------------
        // OBSERVACIONES
        // -------------------------------------------------

        if (observaciones != null
                && observaciones.length() > 1000) {

            throw new IllegalStateException(
                    "Las observaciones de la autorización "
                            + "no pueden superar los 1000 caracteres."
            );
        }
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public DetalleRecetaMedica getDetalleRecetaMedica() {
        return detalleRecetaMedica;
    }


    public Usuario getMedicoAutorizador() {
        return medicoAutorizador;
    }


    public Integer getCantidadAutorizada() {
        return cantidadAutorizada;
    }


    public OffsetDateTime getFechaHoraAutorizacion() {
        return fechaHoraAutorizacion;
    }


    public String getObservaciones() {
        return observaciones;
    }
}
