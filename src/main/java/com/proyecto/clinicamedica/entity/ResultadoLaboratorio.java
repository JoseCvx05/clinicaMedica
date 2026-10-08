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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;


/**
 * =========================================================
 * ENTIDAD: RESULTADO DE LABORATORIO
 * =========================================================
 *
 * CU-09 - Gestión de Laboratorio.
 *
 * Representa el resultado individual correspondiente
 * a uno de los exámenes solicitados dentro de una
 * orden de laboratorio.
 *
 * Cada DetalleOrdenLaboratorio puede tener únicamente
 * un ResultadoLaboratorio.
 *
 * Permite:
 *
 * - Registrar valor del resultado.
 * - Registrar unidad.
 * - Registrar fecha del resultado.
 * - Marcar manualmente fuera de rango.
 * - Registrar notas.
 * - Identificar al Laboratorista que registró.
 * - Registrar validación previa a publicación.
 * - Publicar individualmente el resultado.
 * - Controlar concurrencia mediante @Version.
 *
 * RN-CU09-02.
 * RNF-024.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "resultado_laboratorio"
)
public class ResultadoLaboratorio {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private Integer id;


    // =====================================================
    // DETALLE DE LA ORDEN
    // =====================================================
    //
    // Un examen solicitado puede tener solamente
    // un resultado registrado.
    //
    // La BD también protege esta relación mediante
    // UNIQUE(id_detalle_orden_laboratorio).
    // =====================================================

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_detalle_orden_laboratorio",
            nullable = false,
            unique = true
    )
    private DetalleOrdenLaboratorio detalleOrdenLaboratorio;


    // =====================================================
    // RESULTADO
    // =====================================================

    @Column(
            name = "valor_resultado",
            nullable = false,
            length = 255
    )
    private String valorResultado;


    @Column(
            name = "unidad_resultado",
            nullable = false,
            length = 100
    )
    private String unidadResultado;


    @Column(
            name = "fecha_resultado",
            nullable = false
    )
    private OffsetDateTime fechaResultado;


    // =====================================================
    // FUERA DE RANGO
    // =====================================================
    //
    // CU-09 FA02:
    //
    // El indicador es manual.
    // No se calcula automáticamente desde el rango.
    // =====================================================

    @Column(
            name = "fuera_rango",
            nullable = false
    )
    private boolean fueraRango = false;


    // =====================================================
    // NOTAS
    // =====================================================

    @Column(
            name = "notas_resultado",
            length = 5000
    )
    private String notasResultado;


    // =====================================================
    // USUARIO QUE REGISTRA
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_usuario_registro",
            nullable = false
    )
    private Usuario usuarioRegistro;


    @Column(
            name = "fecha_hora_registro",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHoraRegistro;


    // =====================================================
    // VALIDACIÓN DE SUPERVISOR
    // =====================================================
    //
    // Antes de publicar un resultado debe existir
    // una validación autorizada.
    //
    // La comprobación de:
    //
    // rol = Laboratorista
    // supervisorLaboratorio = true
    //
    // se realizará posteriormente en el Service.
    // =====================================================

    @Column(
            name = "validado_supervisor",
            nullable = false
    )
    private boolean validadoSupervisor = false;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_usuario_validacion"
    )
    private Usuario usuarioValidacion;


    @Column(
            name = "fecha_hora_validacion"
    )
    private OffsetDateTime fechaHoraValidacion;


    // =====================================================
    // PUBLICACIÓN
    // =====================================================

    @Column(
            name = "publicado",
            nullable = false
    )
    private boolean publicado = false;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_usuario_publicacion"
    )
    private Usuario usuarioPublicacion;


    @Column(
            name = "fecha_hora_publicacion"
    )
    private OffsetDateTime fechaHoraPublicacion;


    // =====================================================
    // ACTUALIZACIÓN
    // =====================================================

    @Column(
            name = "fecha_hora_actualizacion",
            nullable = false
    )
    private OffsetDateTime fechaHoraActualizacion;


    // =====================================================
    // CONCURRENCIA
    // =====================================================

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ResultadoLaboratorio() {
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        OffsetDateTime ahora =
                OffsetDateTime.now();


        if (fechaHoraRegistro == null) {

            fechaHoraRegistro =
                    ahora;
        }


        if (fechaHoraActualizacion == null) {

            fechaHoraActualizacion =
                    ahora;
        }


        normalizar();

        validar();
    }

    // =====================================================
// RNF-024
// AUTORIZACIÓN DE CORRECCIÓN
// =====================================================
//
// Un resultado publicado solamente puede volver
// al flujo de edición cuando un Supervisor de
// Laboratorio autoriza expresamente la corrección.
//
// La validación del rol y del atributo
// supervisorLaboratorio se realiza en el Service.
//
// =====================================================

    @Column(
            name = "correccion_autorizada",
            nullable = false
    )
    private boolean correccionAutorizada = false;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "id_usuario_autorizacion_correccion"
    )
    private Usuario usuarioAutorizacionCorreccion;


    @Column(
            name = "fecha_hora_autorizacion_correccion"
    )
    private OffsetDateTime fechaHoraAutorizacionCorreccion;


    @Column(
            name = "motivo_correccion",
            length = 1000
    )
    private String motivoCorreccion;


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    private void prepararActualizacion() {

        fechaHoraActualizacion =
                OffsetDateTime.now();


        normalizar();

        validar();
    }


    // =====================================================
    // VALIDACIÓN DE DOMINIO
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // DETALLE DE LA ORDEN
        // -------------------------------------------------

        if (detalleOrdenLaboratorio == null
                || detalleOrdenLaboratorio.getId() == null) {

            throw new IllegalStateException(
                    "El resultado debe estar asociado a un examen válido de la orden."
            );
        }


        // -------------------------------------------------
        // VALOR
        // -------------------------------------------------

        if (valorResultado == null
                || valorResultado.isBlank()) {

            throw new IllegalStateException(
                    "El valor del resultado es obligatorio."
            );
        }


        if (valorResultado.length() > 255) {

            throw new IllegalStateException(
                    "El valor del resultado supera la longitud permitida."
            );
        }


        // -------------------------------------------------
        // UNIDAD
        // -------------------------------------------------

        if (unidadResultado == null
                || unidadResultado.isBlank()) {

            throw new IllegalStateException(
                    "La unidad del resultado es obligatoria."
            );
        }


        if (unidadResultado.length() > 100) {

            throw new IllegalStateException(
                    "La unidad del resultado supera la longitud permitida."
            );
        }


        // -------------------------------------------------
        // FECHA DEL RESULTADO
        // -------------------------------------------------

        if (fechaResultado == null) {

            throw new IllegalStateException(
                    "La fecha del resultado es obligatoria."
            );
        }


        // -------------------------------------------------
        // NOTAS
        // -------------------------------------------------

        if (notasResultado != null
                && notasResultado.length() > 5000) {

            throw new IllegalStateException(
                    "Las notas del resultado no pueden superar los 5000 caracteres."
            );
        }


        // -------------------------------------------------
        // USUARIO DE REGISTRO
        // -------------------------------------------------

        if (usuarioRegistro == null
                || usuarioRegistro.getId() == null) {

            throw new IllegalStateException(
                    "El resultado debe registrar al Laboratorista responsable."
            );
        }


        if (fechaHoraRegistro == null) {

            throw new IllegalStateException(
                    "La fecha de registro del resultado es obligatoria."
            );
        }


        // -------------------------------------------------
        // VALIDACIÓN DE SUPERVISOR
        // -------------------------------------------------

        if (!validadoSupervisor) {

            if (usuarioValidacion != null
                    || fechaHoraValidacion != null) {

                throw new IllegalStateException(
                        "Un resultado no validado no puede tener datos de validación."
                );
            }

        } else {

            if (usuarioValidacion == null
                    || usuarioValidacion.getId() == null
                    || fechaHoraValidacion == null) {

                throw new IllegalStateException(
                        "La validación del resultado debe registrar al supervisor y la fecha."
                );
            }
        }


        // -------------------------------------------------
        // PUBLICACIÓN
        // -------------------------------------------------

        if (!publicado) {

            if (usuarioPublicacion != null
                    || fechaHoraPublicacion != null) {

                throw new IllegalStateException(
                        "Un resultado no publicado no puede tener datos de publicación."
                );
            }

        } else {

            if (!validadoSupervisor) {

                throw new IllegalStateException(
                        "El resultado debe estar validado antes de ser publicado."
                );
            }


            if (usuarioValidacion == null
                    || fechaHoraValidacion == null) {

                throw new IllegalStateException(
                        "El resultado publicado debe conservar los datos de validación."
                );
            }


            if (usuarioPublicacion == null
                    || usuarioPublicacion.getId() == null
                    || fechaHoraPublicacion == null) {

                throw new IllegalStateException(
                        "La publicación debe registrar al usuario responsable y la fecha."
                );
            }
        }

        // -------------------------------------------------
// RNF-024
// AUTORIZACIÓN DE CORRECCIÓN
// -------------------------------------------------

        if (motivoCorreccion != null
                && motivoCorreccion.length() > 1000) {

            throw new IllegalStateException(
                    "El motivo de corrección no puede superar "
                            + "los 1000 caracteres."
            );
        }


        if (correccionAutorizada) {

            if (usuarioAutorizacionCorreccion == null
                    || usuarioAutorizacionCorreccion.getId() == null) {

                throw new IllegalStateException(
                        "La corrección autorizada debe registrar "
                                + "al Supervisor responsable."
                );
            }


            if (fechaHoraAutorizacionCorreccion == null) {

                throw new IllegalStateException(
                        "La corrección autorizada debe registrar "
                                + "la fecha y hora de autorización."
                );
            }


            if (motivoCorreccion == null
                    || motivoCorreccion.isBlank()) {

                throw new IllegalStateException(
                        "La corrección autorizada debe registrar "
                                + "un motivo."
                );
            }


            /*
             * Mientras una corrección está abierta,
             * el resultado no puede continuar publicado.
             *
             * Esto evita que el Médico vea información
             * modificada antes de la nueva validación.
             */
            if (publicado) {

                throw new IllegalStateException(
                        "Un resultado en proceso de corrección "
                                + "no puede permanecer publicado."
                );
            }
        }


        // -------------------------------------------------
        // ORDEN TEMPORAL
        // -------------------------------------------------

        if (fechaHoraValidacion != null
                && fechaHoraRegistro != null
                && fechaHoraValidacion.isBefore(
                fechaHoraRegistro
        )) {

            throw new IllegalStateException(
                    "La validación no puede ser anterior al registro del resultado."
            );
        }


        if (fechaHoraPublicacion != null
                && fechaHoraValidacion != null
                && fechaHoraPublicacion.isBefore(
                fechaHoraValidacion
        )) {

            throw new IllegalStateException(
                    "La publicación no puede ser anterior a la validación."
            );
        }
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizar() {

        if (valorResultado != null) {

            valorResultado =
                    valorResultado.trim();
        }


        if (unidadResultado != null) {

            unidadResultado =
                    unidadResultado.trim();
        }


        if (notasResultado != null) {

            notasResultado =
                    notasResultado.trim();


            if (notasResultado.isBlank()) {

                notasResultado = null;
            }
        }
        if (motivoCorreccion != null) {

            motivoCorreccion =
                    motivoCorreccion.trim();


            if (motivoCorreccion.isBlank()) {

                motivoCorreccion = null;
            }
        }
    }


    // =====================================================
    // MÉTODOS DE DOMINIO
    // =====================================================

    public boolean estaValidado() {

        return validadoSupervisor;
    }


    public boolean estaPublicado() {

        return publicado;
    }

    public boolean estaCorreccionAutorizada() {

        return correccionAutorizada;
    }


    public boolean estaFueraDeRango() {

        return fueraRango;
    }


    /**
     * RNF-024.
     *
     * Un resultado publicado ya no puede considerarse
     * editable por el flujo normal.
     *
     * La autorización especial para una corrección será
     * controlada por el Service.
     */
    public boolean puedeEditarNormalmente() {

        return !publicado;
    }
    /**
     * =====================================================
     * RNF-024
     * AUTORIZAR CORRECCIÓN DE RESULTADO PUBLICADO
     * =====================================================
     *
     * La autorización:
     *
     * - solamente aplica a un resultado publicado;
     * - registra Supervisor;
     * - registra fecha;
     * - registra motivo;
     * - retira temporalmente la publicación;
     * - invalida la validación anterior.
     *
     * Después debe ejecutarse nuevamente:
     *
     * corrección
     *      -> validación
     *      -> publicación
     *
     * =====================================================
     */
    public void autorizarCorreccion(

            Usuario supervisor,

            OffsetDateTime fecha,

            String motivo

    ) {

        // =================================================
        // DEBE ESTAR PUBLICADO
        // =================================================

        if (!publicado) {

            throw new IllegalStateException(
                    "Solamente puede autorizarse la corrección "
                            + "de un resultado publicado."
            );
        }


        // =================================================
        // SUPERVISOR
        // =================================================

        if (supervisor == null
                || supervisor.getId() == null) {

            throw new IllegalArgumentException(
                    "El Supervisor que autoriza la corrección "
                            + "no es válido."
            );
        }


        // =================================================
        // FECHA
        // =================================================

        if (fecha == null) {

            throw new IllegalArgumentException(
                    "La fecha de autorización de la corrección "
                            + "es obligatoria."
            );
        }


        // =================================================
        // MOTIVO
        // =================================================

        if (motivo == null
                || motivo.trim().isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el motivo de la corrección."
            );
        }


        String motivoLimpio =
                motivo.trim();


        if (motivoLimpio.length() > 1000) {

            throw new IllegalArgumentException(
                    "El motivo de la corrección no puede superar "
                            + "los 1000 caracteres."
            );
        }


        // =================================================
        // REGISTRAR AUTORIZACIÓN
        // =================================================

        correccionAutorizada =
                true;


        usuarioAutorizacionCorreccion =
                supervisor;


        fechaHoraAutorizacionCorreccion =
                fecha;


        motivoCorreccion =
                motivoLimpio;


        // =================================================
        // RETIRAR PUBLICACIÓN
        // =================================================
        //
        // Mientras se corrige, el Médico no debe visualizar
        // una versión todavía no validada.
        //
        // =================================================

        publicado =
                false;


        usuarioPublicacion =
                null;


        fechaHoraPublicacion =
                null;


        // =================================================
        // INVALIDAR VALIDACIÓN ANTERIOR
        // =================================================
        //
        // Después de modificar el resultado será necesaria
        // una nueva validación de Supervisor.
        //
        // =================================================

        validadoSupervisor =
                false;


        usuarioValidacion =
                null;


        fechaHoraValidacion =
                null;


        validar();
    }


    public void marcarValidado(

            Usuario supervisor,

            OffsetDateTime fecha
    ) {

        if (publicado) {

            throw new IllegalStateException(
                    "Un resultado publicado no puede volver a validarse."
            );
        }


        if (supervisor == null
                || supervisor.getId() == null) {

            throw new IllegalArgumentException(
                    "El supervisor de laboratorio no es válido."
            );
        }


        if (fecha == null) {

            throw new IllegalArgumentException(
                    "La fecha de validación es obligatoria."
            );
        }


        validadoSupervisor =
                true;

        usuarioValidacion =
                supervisor;

        fechaHoraValidacion =
                fecha;


        validar();
    }


    public void publicar(

            Usuario usuario,

            OffsetDateTime fecha
    ) {

        if (publicado) {

            throw new IllegalStateException(
                    "El resultado ya se encuentra publicado."
            );
        }


        if (!validadoSupervisor) {

            throw new IllegalStateException(
                    "El resultado debe estar validado antes de ser publicado."
            );
        }


        if (usuario == null
                || usuario.getId() == null) {

            throw new IllegalArgumentException(
                    "El usuario responsable de la publicación no es válido."
            );
        }


        if (fecha == null) {

            throw new IllegalArgumentException(
                    "La fecha de publicación es obligatoria."
            );
        }


        publicado =
                true;


        usuarioPublicacion =
                usuario;


        fechaHoraPublicacion =
                fecha;


        // =============================================
        // RNF-024
        // CONSUMIR AUTORIZACIÓN DE CORRECCIÓN
        // =============================================

        correccionAutorizada =
                false;


        validar();
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getId() {

        return id;
    }


    public void setId(
            Integer id
    ) {

        this.id = id;
    }


    public DetalleOrdenLaboratorio getDetalleOrdenLaboratorio() {

        return detalleOrdenLaboratorio;
    }


    public void setDetalleOrdenLaboratorio(
            DetalleOrdenLaboratorio detalleOrdenLaboratorio
    ) {

        this.detalleOrdenLaboratorio =
                detalleOrdenLaboratorio;
    }


    public String getValorResultado() {

        return valorResultado;
    }


    public void setValorResultado(
            String valorResultado
    ) {

        this.valorResultado =
                valorResultado;
    }


    public String getUnidadResultado() {

        return unidadResultado;
    }


    public void setUnidadResultado(
            String unidadResultado
    ) {

        this.unidadResultado =
                unidadResultado;
    }


    public OffsetDateTime getFechaResultado() {

        return fechaResultado;
    }


    public void setFechaResultado(
            OffsetDateTime fechaResultado
    ) {

        this.fechaResultado =
                fechaResultado;
    }


    public boolean isFueraRango() {

        return fueraRango;
    }


    public void setFueraRango(
            boolean fueraRango
    ) {

        this.fueraRango =
                fueraRango;
    }


    public String getNotasResultado() {

        return notasResultado;
    }


    public void setNotasResultado(
            String notasResultado
    ) {

        this.notasResultado =
                notasResultado;
    }


    public Usuario getUsuarioRegistro() {

        return usuarioRegistro;
    }


    public void setUsuarioRegistro(
            Usuario usuarioRegistro
    ) {

        this.usuarioRegistro =
                usuarioRegistro;
    }


    public OffsetDateTime getFechaHoraRegistro() {

        return fechaHoraRegistro;
    }


    public void setFechaHoraRegistro(
            OffsetDateTime fechaHoraRegistro
    ) {

        this.fechaHoraRegistro =
                fechaHoraRegistro;
    }


    public boolean isValidadoSupervisor() {

        return validadoSupervisor;
    }


    public void setValidadoSupervisor(
            boolean validadoSupervisor
    ) {

        this.validadoSupervisor =
                validadoSupervisor;
    }


    public Usuario getUsuarioValidacion() {

        return usuarioValidacion;
    }


    public void setUsuarioValidacion(
            Usuario usuarioValidacion
    ) {

        this.usuarioValidacion =
                usuarioValidacion;
    }


    public OffsetDateTime getFechaHoraValidacion() {

        return fechaHoraValidacion;
    }


    public void setFechaHoraValidacion(
            OffsetDateTime fechaHoraValidacion
    ) {

        this.fechaHoraValidacion =
                fechaHoraValidacion;
    }


    public boolean isPublicado() {

        return publicado;
    }


    public void setPublicado(
            boolean publicado
    ) {

        this.publicado =
                publicado;
    }


    public Usuario getUsuarioPublicacion() {

        return usuarioPublicacion;
    }


    public void setUsuarioPublicacion(
            Usuario usuarioPublicacion
    ) {

        this.usuarioPublicacion =
                usuarioPublicacion;
    }


    public OffsetDateTime getFechaHoraPublicacion() {

        return fechaHoraPublicacion;
    }


    public void setFechaHoraPublicacion(
            OffsetDateTime fechaHoraPublicacion
    ) {

        this.fechaHoraPublicacion =
                fechaHoraPublicacion;
    }

        // =====================================================
// RNF-024
// DATOS DE AUTORIZACIÓN DE CORRECCIÓN
// =====================================================

        public boolean isCorreccionAutorizada() {

            return correccionAutorizada;
        }


        public Usuario getUsuarioAutorizacionCorreccion() {

            return usuarioAutorizacionCorreccion;
        }


        public OffsetDateTime getFechaHoraAutorizacionCorreccion() {

            return fechaHoraAutorizacionCorreccion;
        }


        public String getMotivoCorreccion() {

            return motivoCorreccion;
        }

    public OffsetDateTime getFechaHoraActualizacion() {

        return fechaHoraActualizacion;
    }


    public void setFechaHoraActualizacion(
            OffsetDateTime fechaHoraActualizacion
    ) {

        this.fechaHoraActualizacion =
                fechaHoraActualizacion;
    }


    public Long getVersion() {

        return version;
    }


    public void setVersion(
            Long version
    ) {

        this.version =
                version;
    }
}