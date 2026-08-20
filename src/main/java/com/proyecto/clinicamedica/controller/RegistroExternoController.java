package com.proyecto.clinicamedica.controller;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;
import com.proyecto.clinicamedica.service.RegistroExternoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * =========================================================
 * CONTROLLER: REGISTRO DE USUARIOS EXTERNOS
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * También permite reutilizar CU-02 desde CU-05.
 *
 * Flujo normal:
 *
 * /registro
 *      -> registro exitoso
 *      -> /login
 *
 * Flujo desde recepción:
 *
 * /registro?origen=recepcion
 *      -> registro exitoso
 *      -> /interno/recepcion
 *
 * =========================================================
 */
@Controller
public class RegistroExternoController {


    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final String ORIGEN_RECEPCION =
            "recepcion";


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

            @RequestParam(
                    value = "origen",
                    required = false
            )
            String origen,

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


        // =================================================
        // CONSERVAR ORIGEN
        // =================================================

        model.addAttribute(
                "origen",
                normalizarOrigen(origen)
        );


        return "registro/registro";
    }


    // =====================================================
    // PROCESAR REGISTRO
    // =====================================================

    @PostMapping("/registro")
    public String registrar(

            @ModelAttribute("registro")
            RegistroExternoDTO formulario,

            @RequestParam(
                    value = "origen",
                    required = false
            )
            String origen,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        String origenNormalizado =
                normalizarOrigen(
                        origen
                );


        // =================================================
        // 1. DELEGAR AL SERVICIO
        // =================================================

        ResultadoValidacionRegistroExterno resultado =
                registroExternoService.registrar(
                        formulario
                );


        // =================================================
        // 2. ERRORES DE VALIDACIÓN
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


            /*
             * Conservamos el origen para que, después de
             * corregir los errores, el flujo siga sabiendo
             * si viene desde recepción.
             */
            model.addAttribute(
                    "origen",
                    origenNormalizado
            );


            return "registro/registro";
        }


        // =================================================
        // 3. REGISTRO DESDE RECEPCIÓN - CU-05 FA03
        // =================================================

        if (esOrigenRecepcion(
                origenNormalizado
        )) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",

                    "Paciente registrado exitosamente. "
                            + "Puede continuar con la búsqueda de su cita."
            );


            return "redirect:/interno/recepcion";
        }


        // =================================================
        // 4. REGISTRO EXTERNO NORMAL - CU-02
        // =================================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",

                "¡Registro exitoso! Su cuenta ha sido creada. "
                        + "Ahora puede iniciar sesión con sus credenciales."
        );


        return "redirect:/login";
    }


    // =====================================================
    // ¿VIENE DE RECEPCIÓN?
    // =====================================================

    private boolean esOrigenRecepcion(
            String origen
    ) {

        return ORIGEN_RECEPCION
                .equalsIgnoreCase(
                        origen
                );
    }


    // =====================================================
    // NORMALIZAR ORIGEN
    // =====================================================

    private String normalizarOrigen(
            String origen
    ) {

        if (origen == null) {

            return "";
        }


        String valor =
                origen.trim()
                        .toLowerCase();


        /*
         * Solo aceptamos los orígenes conocidos.
         *
         * Evita utilizar directamente un valor recibido
         * del cliente como URL de redirección.
         */
        if (ORIGEN_RECEPCION.equals(
                valor
        )) {

            return ORIGEN_RECEPCION;
        }


        return "";
    }
}