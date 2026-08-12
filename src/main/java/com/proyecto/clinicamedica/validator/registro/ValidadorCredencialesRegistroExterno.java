package com.proyecto.clinicamedica.validator.registro;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;


/**
 * =========================================================
 * VALIDADOR: CREDENCIALES DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Valida exclusivamente:
 *
 * - Correo electrónico.
 * - Nombre de usuario.
 * - Contraseña.
 *
 * No consulta base de datos.
 * Los duplicados se validarán en otro componente.
 * =========================================================
 */
@Component
public class ValidadorCredencialesRegistroExterno
        implements ValidadorRegistroExterno {


    private static final Pattern FORMATO_CORREO =
            Pattern.compile(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            );


    private static final Pattern ALFANUMERICO =
            Pattern.compile(
                    "^[A-Za-z0-9]+$"
            );


    // =====================================================
    // ORDEN
    // =====================================================

    @Override
    public int orden() {

        /*
         * Se ejecuta después del validador
         * de datos personales.
         */
        return 20;
    }


    // =====================================================
    // VALIDAR
    // =====================================================

    @Override
    public void validar(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        if (formulario == null
                || resultado == null) {

            return;
        }


        validarCorreo(
                formulario.getCorreoElectronico(),
                resultado
        );


        validarNombreUsuario(
                formulario.getNombreUsuario(),
                resultado
        );


        validarContrasena(
                formulario.getContrasena(),
                resultado
        );
    }


    // =====================================================
    // RN-CU02-04 - CORREO ELECTRÓNICO
    // =====================================================

    private void validarCorreo(
            String correo,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        correo
                );


        /*
         * La regla establece que el correo es obligatorio,
         * pero proporciona un único mensaje para
         * formato inválido.
         *
         * Un correo vacío también incumple el formato.
         */
        if (limpio.isEmpty()
                || !FORMATO_CORREO
                .matcher(limpio)
                .matches()) {

            resultado.agregarError(
                    "correoElectronico",

                    "El formato del correo electrónico no es válido. "
                            + "Ejemplo: usuario@dominio.com"
            );
        }
    }


    // =====================================================
    // RN-CU02-05 - NOMBRE DE USUARIO
    // =====================================================

    private void validarNombreUsuario(
            String nombreUsuario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        nombreUsuario
                );


        // =================================================
        // MÍNIMO 8
        // =================================================

        if (limpio.length() < 8) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El usuario debe contener al menos 8 caracteres."
            );

            return;
        }


        // =================================================
        // MÁXIMO 9
        // =================================================

        if (limpio.length() > 9) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El usuario no puede exceder los 9 caracteres."
            );

            return;
        }


        // =================================================
        // ALFANUMÉRICO
        // =================================================

        if (!ALFANUMERICO
                .matcher(limpio)
                .matches()) {

            /*
             * CU-02 exige que sea alfanumérico,
             * aunque no proporciona texto exacto
             * para este caso.
             */
            resultado.agregarError(
                    "nombreUsuario",
                    "El usuario debe contener únicamente caracteres alfanuméricos."
            );
        }
    }


    // =====================================================
    // RN-CU02-06 - CONTRASEÑA
    // =====================================================

    private void validarContrasena(
            String contrasena,
            ResultadoValidacionRegistroExterno resultado
    ) {

        /*
         * No hacemos trim de la contraseña.
         *
         * Nunca debemos modificar silenciosamente
         * una contraseña escrita por el usuario.
         */
        int longitud =
                contrasena == null
                        ? 0
                        : contrasena.length();


        if (longitud < 12) {

            resultado.agregarError(
                    "contrasena",
                    "La contraseña debe contener al menos 12 caracteres."
            );
        }
    }


    // =====================================================
    // UTILIDAD
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }
}