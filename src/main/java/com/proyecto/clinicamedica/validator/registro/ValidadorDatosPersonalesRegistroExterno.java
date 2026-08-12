package com.proyecto.clinicamedica.validator.registro;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;


/**
 * =========================================================
 * VALIDADOR: DATOS PERSONALES
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad:
 *
 * - Nombre completo.
 * - DPI.
 * - NIT.
 * - Teléfono.
 * - Número de seguro médico.
 *
 * No valida:
 *
 * - Correo.
 * - Nombre de usuario.
 * - Contraseña.
 * - Duplicados.
 *
 * =========================================================
 */
@Component
public class ValidadorDatosPersonalesRegistroExterno
        implements ValidadorRegistroExterno {


    // =====================================================
    // PATRONES
    // =====================================================

    private static final Pattern SOLO_NUMEROS =
            Pattern.compile(
                    "^\\d+$"
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
         * Primero validamos estructura y datos personales.
         */
        return 10;
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


        validarNombre(
                formulario.getNombreCompleto(),
                resultado
        );


        validarDpi(
                formulario.getDpi(),
                resultado
        );


        validarNit(
                formulario.getNit(),
                resultado
        );


        validarTelefono(
                formulario.getTelefono(),
                resultado
        );


        validarNumeroSeguro(
                formulario.getNumeroSeguro(),
                resultado
        );
    }


    // =====================================================
    // RN-CU02-01 - NOMBRE COMPLETO
    // =====================================================

    private void validarNombre(
            String nombre,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        nombre
                );


        int longitud =
                limpio.length();


        /*
         * El documento establece que Nombre es obligatorio
         * y debe tener entre 10 y 100 caracteres.
         *
         * No define un mensaje independiente para vacío,
         * por lo que utilizamos el mensaje oficial de
         * longitud también cuando la longitud es 0.
         */
        if (longitud < 10
                || longitud > 100) {

            resultado.agregarError(
                    "nombreCompleto",

                    "El nombre debe contener entre 10 y 100 "
                            + "caracteres. Usted ingresó "
                            + longitud
                            + " caracteres."
            );
        }
    }


    // =====================================================
    // RN-GLOBAL-001 - DPI
    // =====================================================

    private void validarDpi(
            String dpi,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        dpi
                );


        // =================================================
        // OBLIGATORIO
        // =================================================

        if (limpio.isEmpty()) {

            resultado.agregarError(
                    "dpi",

                    "El campo DPI es obligatorio. "
                            + "Por favor, ingrese su número de DPI."
            );

            return;
        }


        // =================================================
        // EXACTAMENTE 13 DÍGITOS
        // =================================================

        if (limpio.length() != 13) {

            resultado.agregarError(
                    "dpi",

                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + limpio.length()
                            + " dígitos."
            );

            return;
        }


        // =================================================
        // SOLO NÚMEROS
        // =================================================

        if (!SOLO_NUMEROS
                .matcher(
                        limpio
                )
                .matches()) {

            resultado.agregarError(
                    "dpi",

                    "El DPI debe contener únicamente números. "
                            + "No se permiten letras ni caracteres especiales."
            );
        }
    }


    // =====================================================
    // RN-GLOBAL-002 - NIT
    // =====================================================

    private void validarNit(
            String nit,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        nit
                );


        // =================================================
        // OBLIGATORIO
        // =================================================

        if (limpio.isEmpty()) {

            resultado.agregarError(
                    "nit",
                    "El campo NIT es obligatorio."
            );

            return;
        }


        // =================================================
        // 8 A 9 CARACTERES
        // =================================================

        if (limpio.length() < 8
                || limpio.length() > 9) {

            resultado.agregarError(
                    "nit",

                    "El NIT debe contener entre 8 y 9 caracteres. "
                            + "Usted ingresó "
                            + limpio.length()
                            + " caracteres."
            );

            return;
        }


        // =================================================
        // ALFANUMÉRICO
        // =================================================

        if (!ALFANUMERICO
                .matcher(
                        limpio
                )
                .matches()) {

            resultado.agregarError(
                    "nit",

                    "El NIT debe contener únicamente "
                            + "caracteres alfanuméricos."
            );
        }
    }


    // =====================================================
    // RN-CU02-02 - TELÉFONO
    // =====================================================

    private void validarTelefono(
            String telefono,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        telefono
                );


        /*
         * CU-02 indica:
         *
         * - obligatorio;
         * - exactamente 8 dígitos;
         * - numérico.
         *
         * El documento proporciona un único mensaje.
         */
        if (limpio.length() != 8
                || !SOLO_NUMEROS
                .matcher(
                        limpio
                )
                .matches()) {

            resultado.agregarError(
                    "telefono",

                    "El número de teléfono debe contener "
                            + "exactamente 8 dígitos numéricos."
            );
        }
    }


    // =====================================================
    // RN-CU02-03 - SEGURO MÉDICO
    // =====================================================

    private void validarNumeroSeguro(
            String numeroSeguro,
            ResultadoValidacionRegistroExterno resultado
    ) {

        String limpio =
                limpiar(
                        numeroSeguro
                );


        // Es opcional.
        if (limpio.isEmpty()) {

            return;
        }


        if (limpio.length() < 5
                || limpio.length() > 50) {

            /*
             * CU-02 establece el rango 5-50 pero no
             * proporciona texto exacto para el error.
             *
             * Este mensaje es una decisión de
             * implementación para poder informar al usuario.
             */
            resultado.agregarError(
                    "numeroSeguro",

                    "El número de seguro médico debe contener "
                            + "entre 5 y 50 caracteres."
            );
        }
    }


    // =====================================================
    // LIMPIAR
    // =====================================================

    private String limpiar(
            String valor
    ) {

        return valor == null
                ? ""
                : valor.trim();
    }
}