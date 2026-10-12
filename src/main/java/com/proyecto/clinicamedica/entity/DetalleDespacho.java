
package com.proyecto.clinicamedica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * =========================================================
 * ENTIDAD: DETALLE DE DESPACHO
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Registra cada medicamento entregado en farmacia.
 *
 * Conserva:
 *
 * - Despacho al que pertenece.
 * - Detalle original de la receta.
 * - Medicamento realmente entregado.
 * - Cantidad entregada.
 * - Sustitución y su justificación.
 * - Precio unitario y subtotal.
 * - Datos históricos de los medicamentos.
 *
 * IMPORTANTE:
 *
 * La verificación de stock, los permisos,
 * las cantidades acumuladas y el descuento
 * de inventario corresponden al Service.
 *
 * =========================================================
 */
@Entity
@Table(
        name = "detalle_despacho",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_detalle_despacho_receta",
                        columnNames = {
                                "id_despacho",
                                "id_detalle_receta_medica"
                        }
                )
        }
)
public class DetalleDespacho {

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
    // DESPACHO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_despacho",
            nullable = false,
            updatable = false
    )
    private Despacho despacho;


    // =====================================================
    // DETALLE ORIGINAL DE LA RECETA
    // =====================================================

    @ManyToOne(
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
    // MEDICAMENTO REALMENTE ENTREGADO
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_medicamento_entregado",
            nullable = false,
            updatable = false
    )
    private Medicamento medicamentoEntregado;


    // =====================================================
    // CANTIDAD ENTREGADA
    // =====================================================

    @Column(
            name = "cantidad_entregada",
            nullable = false,
            updatable = false
    )
    private Integer cantidadEntregada;


    // =====================================================
    // RAZÓN DE SUSTITUCIÓN - FA02
    // =====================================================

    @Column(
            name = "razon_sustitucion",
            length = 1000,
            updatable = false
    )
    private String razonSustitucion;


    // =====================================================
    // SNAPSHOT: MEDICAMENTO ORIGINAL
    // =====================================================

    @Column(
            name = "nombre_medicamento_original",
            nullable = false,
            length = 200,
            updatable = false
    )
    private String nombreMedicamentoOriginal;


    // =====================================================
    // SNAPSHOT: MEDICAMENTO ENTREGADO
    // =====================================================

    @Column(
            name = "nombre_medicamento_entregado",
            nullable = false,
            length = 200,
            updatable = false
    )
    private String nombreMedicamentoEntregado;


    // =====================================================
    // SNAPSHOT: UNIDAD ENTREGADA
    // =====================================================

    @Column(
            name = "unidad",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String unidad;


    // =====================================================
    // PRECIO UNITARIO HISTÓRICO
    // =====================================================

    @Column(
            name = "precio_unitario",
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal precioUnitario;


    // =====================================================
    // SUBTOTAL
    // =====================================================

    @Column(
            name = "subtotal",
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal subtotal;


    // =====================================================
    // CONSTRUCTOR VACÍO PARA JPA
    // =====================================================

    public DetalleDespacho() {
    }


    // =====================================================
    // CONSTRUCTOR DE DOMINIO
    // =====================================================

    public DetalleDespacho(
            Despacho despacho,
            DetalleRecetaMedica detalleRecetaMedica,
            Medicamento medicamentoEntregado,
            Integer cantidadEntregada,
            String razonSustitucion
    ) {

        this.despacho = despacho;
        this.detalleRecetaMedica = detalleRecetaMedica;
        this.medicamentoEntregado = medicamentoEntregado;
        this.cantidadEntregada = cantidadEntregada;
        this.razonSustitucion = razonSustitucion;
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    private void prepararCreacion() {

        normalizar();

        validar();

        copiarDatosHistoricos();
    }


    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private void normalizar() {

        if (razonSustitucion != null) {

            razonSustitucion =
                    razonSustitucion.trim();

            if (razonSustitucion.isBlank()) {

                razonSustitucion = null;
            }
        }
    }


    // =====================================================
    // IDENTIFICAR SUSTITUCIÓN - FA02
    // =====================================================

    public boolean esSustitucion() {

        if (detalleRecetaMedica == null
                || detalleRecetaMedica.getMedicamento() == null
                || medicamentoEntregado == null) {

            return false;
        }

        return !Objects.equals(
                detalleRecetaMedica
                        .getMedicamento()
                        .getId(),
                medicamentoEntregado.getId()
        );
    }


    // =====================================================
    // VALIDACIONES BACKEND
    // =====================================================

    public void validar() {

        // -------------------------------------------------
        // DESPACHO OBLIGATORIO
        // -------------------------------------------------

        if (despacho == null
                || despacho.getId() == null) {

            throw new IllegalStateException(
                    "El detalle debe pertenecer a un despacho guardado."
            );
        }


        // -------------------------------------------------
        // DESPACHO CONFIRMADO
        // -------------------------------------------------

        if (!despacho.estaConfirmado()) {

            throw new IllegalStateException(
                    "Solo pueden registrarse medicamentos en un despacho confirmado."
            );
        }


        // -------------------------------------------------
        // DETALLE DE RECETA OBLIGATORIO
        // -------------------------------------------------

        if (detalleRecetaMedica == null
                || detalleRecetaMedica.getId() == null
                || detalleRecetaMedica.getRecetaMedica() == null
                || detalleRecetaMedica.getRecetaMedica().getId() == null) {

            throw new IllegalStateException(
                    "Debe seleccionar un medicamento de una receta válida."
            );
        }


        // -------------------------------------------------
        // MISMA RECETA MÉDICA
        // -------------------------------------------------

        if (despacho.getRecetaMedica() == null
                || !Objects.equals(
                despacho.getRecetaMedica().getId(),
                detalleRecetaMedica
                        .getRecetaMedica()
                        .getId()
        )) {

            throw new IllegalStateException(
                    "El medicamento seleccionado no pertenece a la receta del despacho."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ORIGINAL VÁLIDO
        // -------------------------------------------------

        if (detalleRecetaMedica.getMedicamento() == null
                || detalleRecetaMedica.getMedicamento().getId() == null) {

            throw new IllegalStateException(
                    "El medicamento original de la receta no es válido."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ENTREGADO OBLIGATORIO
        // -------------------------------------------------

        if (medicamentoEntregado == null
                || medicamentoEntregado.getId() == null) {

            throw new IllegalStateException(
                    "Debe seleccionar el medicamento que será entregado."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ACTIVO
        // -------------------------------------------------

        if (!medicamentoEntregado.estaActivo()) {

            throw new IllegalStateException(
                    "No se puede despachar un medicamento inactivo."
            );
        }


        // -------------------------------------------------
        // VALIDACIÓN DEL CATÁLOGO
        // -------------------------------------------------

        medicamentoEntregado.validar();


        // -------------------------------------------------
        // CANTIDAD MAYOR A CERO
        // -------------------------------------------------

        if (cantidadEntregada == null
                || cantidadEntregada <= 0) {

            throw new IllegalStateException(
                    "La cantidad a despachar debe ser mayor a cero."
            );
        }


        // -------------------------------------------------
        // NO SUPERAR CANTIDAD ORIGINAL EN UNA LÍNEA
        // -------------------------------------------------

        if (detalleRecetaMedica.getCantidad() == null
                || cantidadEntregada
                > detalleRecetaMedica.getCantidad()) {

            throw new IllegalStateException(
                    "La cantidad a despachar supera la cantidad prescrita."
            );
        }


        // -------------------------------------------------
        // SUSTITUCIÓN - FA02
        // -------------------------------------------------

        if (esSustitucion()) {

            if (razonSustitucion == null
                    || razonSustitucion.isBlank()) {

                throw new IllegalStateException(
                        "Debe ingresar la razón de sustitución del medicamento."
                );
            }

        } else {

            if (razonSustitucion != null) {

                throw new IllegalStateException(
                        "No debe registrar una sustitución cuando se entrega el medicamento original."
                );
            }
        }


        // -------------------------------------------------
        // LONGITUD DE LA JUSTIFICACIÓN
        // -------------------------------------------------

        if (razonSustitucion != null
                && razonSustitucion.length() > 1000) {

            throw new IllegalStateException(
                    "La razón de sustitución no puede superar los 1000 caracteres."
            );
        }
    }


    // =====================================================
    // COPIAR INFORMACIÓN HISTÓRICA
    // =====================================================

    private void copiarDatosHistoricos() {

        // -------------------------------------------------
        // MEDICAMENTO ORIGINAL
        // -------------------------------------------------

        nombreMedicamentoOriginal =
                detalleRecetaMedica
                        .getNombreMedicamento();


        if (nombreMedicamentoOriginal == null
                || nombreMedicamentoOriginal.isBlank()) {

            throw new IllegalStateException(
                    "El medicamento original no tiene nombre histórico válido."
            );
        }


        // -------------------------------------------------
        // MEDICAMENTO ENTREGADO
        // -------------------------------------------------

        nombreMedicamentoEntregado =
                medicamentoEntregado.getNombre();


        unidad =
                medicamentoEntregado.getUnidad();


        // -------------------------------------------------
        // PRECIO CALCULADO EN BACKEND
        // -------------------------------------------------

        if (esSustitucion()) {

            // Para la alternativa se utiliza su precio
            // actual registrado en el catálogo.

            precioUnitario =
                    medicamentoEntregado.getPrecio();

        } else {

            // Para el medicamento original conservamos
            // el precio histórico de la receta.

            precioUnitario =
                    detalleRecetaMedica.getPrecioUnitario();
        }


        if (precioUnitario == null
                || precioUnitario.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalStateException(
                    "El medicamento no tiene un precio válido."
            );
        }


        if (precioUnitario
                .stripTrailingZeros()
                .scale() > 2) {

            throw new IllegalStateException(
                    "El precio del medicamento debe tener como máximo dos decimales."
            );
        }


        // -------------------------------------------------
        // SUBTOTAL CALCULADO EN BACKEND
        // -------------------------------------------------

        subtotal =
                precioUnitario.multiply(
                        BigDecimal.valueOf(
                                cantidadEntregada
                        )
                );


        // -------------------------------------------------
        // EVITAR MONTOS FUERA DEL LÍMITE DE COLUMNA
        // -------------------------------------------------

        if (subtotal.compareTo(
                new BigDecimal("9999999999.99")
        ) > 0) {

            throw new IllegalStateException(
                    "El subtotal del medicamento supera el monto máximo permitido."
            );
        }
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }


    public Despacho getDespacho() {
        return despacho;
    }


    public DetalleRecetaMedica getDetalleRecetaMedica() {
        return detalleRecetaMedica;
    }


    public Medicamento getMedicamentoEntregado() {
        return medicamentoEntregado;
    }


    public Integer getCantidadEntregada() {
        return cantidadEntregada;
    }


    public String getRazonSustitucion() {
        return razonSustitucion;
    }


    public String getNombreMedicamentoOriginal() {
        return nombreMedicamentoOriginal;
    }


    public String getNombreMedicamentoEntregado() {
        return nombreMedicamentoEntregado;
    }


    public String getUnidad() {
        return unidad;
    }


    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }


    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
