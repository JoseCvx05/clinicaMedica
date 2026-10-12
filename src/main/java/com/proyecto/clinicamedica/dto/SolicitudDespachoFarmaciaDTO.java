
package com.proyecto.clinicamedica.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * =========================================================
 * DTO: SOLICITUD DE DESPACHO DE FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Recibe exclusivamente la información necesaria
 * para solicitar un despacho.
 *
 * NO recibe:
 *
 * - Precio unitario.
 * - Monto total.
 * - Farmacéutico.
 * - Sucursal.
 * - Stock disponible.
 *
 * Esos valores serán obtenidos y validados
 * directamente en el backend.
 *
 * =========================================================
 */
public class SolicitudDespachoFarmaciaDTO {

    // =====================================================
    // RECETA MÉDICA
    // =====================================================

    @NotNull(message = "Debe seleccionar una receta médica.")
    @Positive(message = "El identificador de receta no es válido.")
    private Integer idReceta;


    // =====================================================
    // CLAVE DE OPERACIÓN
    // =====================================================
    //
    // Permite identificar un intento de despacho
    // y controlar reenvíos de la misma solicitud.
    //
    // =====================================================

    @NotNull(message = "La clave de operación es obligatoria.")
    private UUID claveOperacion;


    // =====================================================
    // MEDICAMENTOS SOLICITADOS
    // =====================================================

    @Valid
    @NotEmpty(message = "Debe seleccionar al menos un medicamento para despachar.")
    private List<LineaDespachoDTO> medicamentos =
            new ArrayList<>();


    // =====================================================
    // OBSERVACIONES
    // =====================================================

    @Size(
            max = 1000,
            message = "Las observaciones no pueden superar los 1000 caracteres."
    )
    private String observaciones;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SolicitudDespachoFarmaciaDTO() {
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Integer getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(Integer idReceta) {
        this.idReceta = idReceta;
    }

    public UUID getClaveOperacion() {
        return claveOperacion;
    }

    public void setClaveOperacion(UUID claveOperacion) {
        this.claveOperacion = claveOperacion;
    }

    public List<LineaDespachoDTO> getMedicamentos() {
        return medicamentos;
    }

    public void setMedicamentos(
            List<LineaDespachoDTO> medicamentos
    ) {
        this.medicamentos = medicamentos;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }


    // =====================================================
    // DTO: MEDICAMENTO SOLICITADO
    // =====================================================
    //
    // Cada línea representa un medicamento que
    // el farmacéutico solicita entregar.
    //
    // La sustitución se identifica comparando
    // el medicamento original de la receta con
    // el medicamento realmente entregado.
    //
    // =====================================================

    public static class LineaDespachoDTO {

        // -------------------------------------------------
        // DETALLE ORIGINAL DE LA RECETA
        // -------------------------------------------------

        @NotNull(message = "Debe seleccionar un detalle de receta.")
        @Positive(message = "El detalle de receta no es válido.")
        private Integer idDetalleReceta;


        // -------------------------------------------------
        // MEDICAMENTO QUE SE ENTREGARÁ
        // -------------------------------------------------

        @NotNull(message = "Debe seleccionar el medicamento a entregar.")
        @Positive(message = "El medicamento seleccionado no es válido.")
        private Integer idMedicamentoEntregado;


        // -------------------------------------------------
        // CANTIDAD SOLICITADA
        // -------------------------------------------------

        @NotNull(message = "La cantidad a despachar es obligatoria.")
        @Positive(message = "La cantidad a despachar debe ser mayor a cero.")
        private Integer cantidadEntregada;


        // -------------------------------------------------
        // JUSTIFICACIÓN DE SUSTITUCIÓN - FA02
        // -------------------------------------------------

        @Size(
                max = 1000,
                message = "La razón de sustitución no puede superar los 1000 caracteres."
        )
        private String razonSustitucion;

        // -------------------------------------------------
// CU-11 FA02 - SOLICITUD DE SUSTITUCIÓN
// -------------------------------------------------
//
// Indica si el farmacéutico marcó la casilla.
//
// El backend verificará que coincida con
// el medicamento realmente entregado.
//
// Por defecto es false para mantener
// compatibles los despachos normales.
// -------------------------------------------------

        private boolean sustitucionSolicitada = false;


        // -------------------------------------------------
        // CONSTRUCTOR
        // -------------------------------------------------

        public LineaDespachoDTO() {
        }


        // -------------------------------------------------
        // GETTERS Y SETTERS
        // -------------------------------------------------

        public Integer getIdDetalleReceta() {
            return idDetalleReceta;
        }

        public void setIdDetalleReceta(Integer idDetalleReceta) {
            this.idDetalleReceta = idDetalleReceta;
        }

        public Integer getIdMedicamentoEntregado() {
            return idMedicamentoEntregado;
        }

        public void setIdMedicamentoEntregado(
                Integer idMedicamentoEntregado
        ) {
            this.idMedicamentoEntregado = idMedicamentoEntregado;
        }

        public Integer getCantidadEntregada() {
            return cantidadEntregada;
        }

        public void setCantidadEntregada(Integer cantidadEntregada) {
            this.cantidadEntregada = cantidadEntregada;
        }

        public String getRazonSustitucion() {
            return razonSustitucion;
        }

        public void setRazonSustitucion(String razonSustitucion) {
            this.razonSustitucion = razonSustitucion;
        }

        // -------------------------------------------------
// CU-11 FA02 - GETTER Y SETTER
// -------------------------------------------------

        public boolean isSustitucionSolicitada() {
            return sustitucionSolicitada;
        }

        public void setSustitucionSolicitada(
                boolean sustitucionSolicitada
        ) {
            this.sustitucionSolicitada = sustitucionSolicitada;
        }
    }
}
