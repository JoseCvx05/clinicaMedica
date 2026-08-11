package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.VerificacionDpiRequest;
import com.proyecto.clinicamedica.dto.VerificacionDpiResponse;
import com.proyecto.clinicamedica.service.VerificacionPacienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * =========================================================
 * CONTROLADOR: VERIFICACIÓN DE PACIENTE
 * =========================================================
 *
 * Expone el endpoint público utilizado por el modal
 * "Verificar Registro" del CU-00.
 *
 * Responsabilidades:
 *
 * - Recibir la solicitud HTTP.
 * - Ejecutar las validaciones del DTO.
 * - Delegar la lógica al Service.
 * - Retornar la respuesta HTTP.
 *
 * NO contiene lógica de negocio.
 *
 * Aplica:
 *
 * - MVC.
 * - SRP.
 * - DIP.
 * =========================================================
 */
@RestController
@RequestMapping("/api/public")
public class VerificacionPacienteController {

    private final VerificacionPacienteService
            verificacionPacienteService;


    /**
     * Inyección de dependencias por constructor.
     */
    public VerificacionPacienteController(
            VerificacionPacienteService
                    verificacionPacienteService
    ) {
        this.verificacionPacienteService =
                verificacionPacienteService;
    }


    /**
     * =====================================================
     * POST /api/public/verificar-dpi
     * =====================================================
     *
     * Recibe el DPI ingresado en el modal.
     *
     * @Valid ejecutará @DpiValido antes de entrar
     * en la lógica del servicio.
     *
     * Si el DPI es válido:
     *
     * - Paciente registrado.
     * - No registrado.
     * - Usuario interno.
     *
     * serán resueltos por VerificacionPacienteService.
     */
    @PostMapping("/verificar-dpi")
    public ResponseEntity<VerificacionDpiResponse>
    verificarDpi(
            @Valid
            @RequestBody
            VerificacionDpiRequest request
    ) {

        VerificacionDpiResponse response =
                verificacionPacienteService.verificar(
                        request.getDpi()
                );

        return ResponseEntity.ok(response);
    }
}