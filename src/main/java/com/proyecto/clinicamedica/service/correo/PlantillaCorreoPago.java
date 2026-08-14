package com.proyecto.clinicamedica.service.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;


/**
 * =========================================================
 * PLANTILLA DE CORREO: COMPROBANTE DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Construye:
 *
 * - Asunto del comprobante.
 * - Cuerpo del correo.
 * - Pie automático RN-GLOBAL-006.
 *
 * =========================================================
 */
@Component
public class PlantillaCorreoPago {


    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );


    private final String nombreHospital;

    private final String telefonoHospital;


    public PlantillaCorreoPago(

            @Value("${hospital.nombre}")
            String nombreHospital,

            @Value("${hospital.telefono}")
            String telefonoHospital
    ) {

        this.nombreHospital =
                nombreHospital;

        this.telefonoHospital =
                telefonoHospital;
    }


    // =====================================================
    // ASUNTO RN-CU04-05
    // =====================================================

    public String construirAsunto() {

        return "Comprobante de Pago - Cita Médica - Hospital "
                + nombreHospital;
    }


    // =====================================================
    // CUERPO DEL COMPROBANTE
    // =====================================================

    public String construirCuerpo(

            String nombrePaciente,

            String numeroTransaccion,

            String medico,

            String especialidad,

            String sucursal,

            OffsetDateTime fechaHoraCita,

            BigDecimal monto,

            String formaPago,

            OffsetDateTime fechaHoraPago
    ) {

        String fechaCita =
                fechaHoraCita == null
                        ? "No disponible"
                        : fechaHoraCita.format(
                        FORMATO_FECHA
                );


        String fechaPago =
                fechaHoraPago == null
                        ? "No disponible"
                        : fechaHoraPago.format(
                        FORMATO_FECHA
                );


        String montoTexto =
                monto == null
                        ? "Q0.00"
                        : "Q"
                        + monto.setScale(2)
                        .toPlainString();


        return """
                Estimado(a) %s,

                su pago ha sido procesado exitosamente y su cita médica ha sido confirmada.

                COMPROBANTE DE PAGO

                Número de transacción: %s
                Paciente: %s
                Médico: %s
                Especialidad: %s
                Sede: %s
                Fecha y hora de la cita: %s
                Monto pagado: %s
                Forma de pago: %s
                Fecha y hora de la transacción: %s

                Servicio: Consulta médica - %s

                Este es un correo automático del Sistema Informático Hospitalario.
                No responda a este mensaje.
                Para consultas, comuníquese al teléfono %s.
                """
                .formatted(
                        nombrePaciente,
                        numeroTransaccion,
                        nombrePaciente,
                        medico,
                        especialidad,
                        sucursal,
                        fechaCita,
                        montoTexto,
                        formaPago,
                        fechaPago,
                        especialidad,
                        telefonoHospital
                );
    }
}