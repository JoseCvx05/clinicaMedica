package com.proyecto.clinicamedica.service.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * PLANTILLA DE CORREO: REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad exclusiva:
 *
 * - Construir el asunto.
 * - Construir el cuerpo del correo de bienvenida.
 *
 * Tanto el primer envío como los reintentos utilizan
 * exactamente esta misma plantilla.
 * =========================================================
 */
@Component
public class PlantillaCorreoRegistroExterno {


    private final String nombreHospital;

    private final String telefonoHospital;


    public PlantillaCorreoRegistroExterno(

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
    // ASUNTO CU-02
    // =====================================================

    public String construirAsunto() {

        return "Bienvenido al Sistema de Citas - Hospital "
                + nombreHospital;
    }


    // =====================================================
    // CUERPO CU-02 + RN-GLOBAL-006
    // =====================================================

    public String construirCuerpo(
            String nombreCompleto
    ) {

        return """
                Estimado(a) %s,

                su registro ha sido completado exitosamente.
                Ya puede agendar sus citas médicas a través de nuestro portal.

                Paciente: %s
                Servicio: Registro de usuario externo

                Este es un correo automático del Sistema Informático Hospitalario.
                No responda a este mensaje.
                Para consultas, comuníquese al teléfono %s.
                """
                .formatted(
                        nombreCompleto,
                        nombreCompleto,
                        telefonoHospital
                );
    }
}