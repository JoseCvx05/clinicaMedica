package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.cita.CitaWizardDTO;
import com.proyecto.clinicamedica.dto.cita.ResultadoAnalisisAntivirus;
import com.proyecto.clinicamedica.dto.cita.ResultadoValidacionDocumentoCita;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.exception.DocumentoCitaInvalidoException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * =========================================================
 * SERVICIO: FINALIZACIÓN DE CITA
 * =========================================================
 *
 * CU-03 - Agendar Citas.
 *
 * Coordina la etapa final del agendamiento:
 *
 * - Validar el documento opcional.
 * - Analizar el documento con antivirus.
 * - Confirmar y registrar la cita.
 * - Almacenar el documento si fue proporcionado.
 *
 * =========================================================
 */
@Service
public class FinalizacionCitaService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final ValidacionDocumentoCitaService
            validacionDocumentoCitaService;

    private final AntivirusDocumentoService
            antivirusDocumentoService;

    private final ConfirmacionCitaService
            confirmacionCitaService;

    private final DocumentoCitaService
            documentoCitaService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FinalizacionCitaService(
            ValidacionDocumentoCitaService validacionDocumentoCitaService,
            AntivirusDocumentoService antivirusDocumentoService,
            ConfirmacionCitaService confirmacionCitaService,
            DocumentoCitaService documentoCitaService
    ) {

        this.validacionDocumentoCitaService =
                validacionDocumentoCitaService;

        this.antivirusDocumentoService =
                antivirusDocumentoService;

        this.confirmacionCitaService =
                confirmacionCitaService;

        this.documentoCitaService =
                documentoCitaService;
    }


    // =====================================================
    // FINALIZAR AGENDAMIENTO
    // =====================================================

    @Transactional
    public Cita finalizar(
            Usuario paciente,
            CitaWizardDTO wizard,
            MultipartFile documento
    ) {

        // =================================================
        // VALIDAR DOCUMENTO
        // =================================================

        ResultadoValidacionDocumentoCita validacion =
                validacionDocumentoCitaService
                        .validar(
                                documento
                        );


        if (!validacion.valido()) {

            throw new DocumentoCitaInvalidoException(
                    validacion.mensaje()
            );
        }


        // =================================================
        // COMPROBAR SI EXISTE DOCUMENTO
        // =================================================

        boolean tieneDocumento =
                documento != null
                        && !documento.isEmpty();


        // =================================================
        // ANALIZAR CON ANTIVIRUS
        // =================================================

        if (tieneDocumento) {

            ResultadoAnalisisAntivirus antivirus =
                    antivirusDocumentoService
                            .analizar(
                                    documento
                            );


            // =============================================
            // DOCUMENTO INFECTADO
            // =============================================

            if (antivirus.estaInfectado()) {

                throw new DocumentoCitaInvalidoException(
                        "El documento adjunto fue rechazado "
                                + "porque se detectó contenido potencialmente peligroso."
                );
            }


            // =============================================
            // ERROR DEL ANTIVIRUS
            // =============================================

            if (antivirus.tieneError()) {

                throw new DocumentoCitaInvalidoException(
                        "No fue posible verificar el documento con el "
                                + "servicio antivirus. Intente nuevamente "
                                + "o continúe sin adjuntar el documento."
                );
            }
        }


        // =================================================
        // CONFIRMAR CITA
        // =================================================
        //
        // La cita solamente se registra después de que
        // el documento haya pasado todas las validaciones.
        // =================================================

        Cita cita =
                confirmacionCitaService
                        .confirmar(
                                paciente,
                                wizard
                        );


        // =================================================
        // GUARDAR DOCUMENTO
        // =================================================

        if (tieneDocumento) {

            documentoCitaService
                    .guardar(
                            cita,
                            paciente,
                            documento
                    );
        }


        return cita;
    }
}