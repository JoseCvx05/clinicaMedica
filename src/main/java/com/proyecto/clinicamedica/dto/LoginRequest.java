package com.proyecto.clinicamedica.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * =========================================================
 * DTO: INICIO DE SESIÓN
 * =========================================================
 *
 * Recibe las credenciales ingresadas por el paciente
 * durante el CU-00.
 *
 * La contraseña solamente existe durante la solicitud.
 * Nunca se almacena en texto plano.
 * =========================================================
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    /**
     * Nombre de usuario.
     */
    @NotBlank(
            message = "El nombre de usuario es obligatorio."
    )
    private String nombreUsuario;


    /**
     * Contraseña ingresada.
     *
     * Se comparará posteriormente contra
     * usuario.contrasenaHash mediante PasswordEncoder.
     */
    @NotBlank(
            message = "La contraseña es obligatoria."
    )
    private String contrasena;
}