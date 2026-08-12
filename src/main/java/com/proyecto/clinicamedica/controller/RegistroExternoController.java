package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;
import com.proyecto.clinicamedica.service.RegistroExternoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * =========================================================
 * CONTROLLER: REGISTRO DE USUARIOS EXTERNOS
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad:
 *
 * - Mostrar el formulario público.
 * - Recibir los datos del paciente.
 * - Delegar el registro al Service.
 * - Mostrar errores de validación.
 * - Redirigir al login cuando el registro termina.
 *
 * NO contiene reglas de negocio.
 * =========================================================
 */
@Controller
public class RegistroExternoController {


    // =====================================================
    // DEPENDENCIA
    // =====================================================

    private final RegistroExternoService
            registroExternoService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public RegistroExternoController(
            RegistroExternoService registroExternoService
    ) {

        this.registroExternoService =
                registroExternoService;
    }


    // =====================================================
    // MOSTRAR FORMULARIO
    // =====================================================

    @GetMapping("/registro")
    public String mostrarFormulario(
            Model model
    ) {

        if (!model.containsAttribute(
                "registro"
        )) {

            model.addAttribute(
                    "registro",
                    new RegistroExternoDTO()
            );
        }


        if (!model.containsAttribute(
                "resultadoValidacion"
        )) {

            model.addAttribute(
                    "resultadoValidacion",
                    new ResultadoValidacionRegistroExterno()
            );
        }


        return "registro/registro";
    }


    // =====================================================
    // PROCESAR REGISTRO
    // =====================================================

    @PostMapping("/registro")
    public String registrar(
            @ModelAttribute("registro")
            RegistroExternoDTO formulario,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        // =================================================
        // 1. DELEGAR AL SERVICIO
        // =================================================

        ResultadoValidacionRegistroExterno resultado =
                registroExternoService.registrar(
                        formulario
                );


        // =================================================
        // 2. FA02 / FA03 / FA04
        // =================================================

        if (resultado.tieneErrores()) {

            /*
             * Nunca devolvemos nuevamente la contraseña
             * escrita por el paciente al navegador.
             */
            formulario.setContrasena(
                    null
            );


            model.addAttribute(
                    "registro",
                    formulario
            );


            model.addAttribute(
                    "resultadoValidacion",
                    resultado
            );


            return "registro/registro";
        }


        // =================================================
        // 3. REGISTRO EXITOSO
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",

                "¡Registro exitoso! Su cuenta ha sido creada. "
                        + "Ahora puede iniciar sesión con sus credenciales."
        );


        // =================================================
        // 4. REDIRECCIÓN A LOGIN
        // =================================================

        return "redirect:/login";
    }
}