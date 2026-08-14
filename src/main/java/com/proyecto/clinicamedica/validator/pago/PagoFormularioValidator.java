package com.proyecto.clinicamedica.validator.pago;

import com.proyecto.clinicamedica.dto.pago.PagoFormularioDTO;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * =========================================================
 * VALIDATOR: FORMULARIO DE PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Centraliza las validaciones de:
 *
 * - Número de tarjeta.
 * - Nombre del titular.
 * - Fecha de vencimiento.
 * - CVV.
 * - Idempotency Key.
 *
 * Se mantiene en una sola clase para evitar crear un
 * validador independiente por cada campo.
 *
 * =========================================================
 */
@Component
public class PagoFormularioValidator
        implements Validator {


    // =====================================================
    // MENSAJES CU-04
    // =====================================================

    private static final String MENSAJE_TARJETA =
            "El número de tarjeta debe contener entre 13 y 19 dígitos y ser válido.";


    private static final String MENSAJE_TITULAR =
            "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.";


    private static final String MENSAJE_VENCIMIENTO =
            "La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.";


    private static final String MENSAJE_CVV =
            "El CVV debe contener 3 ó 4 dígitos numéricos.";


    // =====================================================
    // PATRONES
    // =====================================================

    /**
     * RN-CU04-01:
     *
     * únicamente dígitos y longitud 13-19.
     */
    private static final Pattern PATRON_TARJETA =
            Pattern.compile(
                    "^\\d{13,19}$"
            );


    /**
     * RN-CU04-02:
     *
     * únicamente letras Unicode y espacios.
     *
     * Esto permite nombres con:
     *
     * Á É Í Ó Ú Ñ
     *
     * pero rechaza números y caracteres especiales.
     */
    private static final Pattern PATRON_TITULAR =
            Pattern.compile(
                    "^[\\p{L} ]{5,100}$"
            );


    /**
     * MM/AA
     *
     * Mes válido:
     *
     * 01 - 12
     */
    private static final Pattern PATRON_VENCIMIENTO =
            Pattern.compile(
                    "^(0[1-9]|1[0-2])/(\\d{2})$"
            );


    /**
     * CVV de 3 o 4 dígitos.
     */
    private static final Pattern PATRON_CVV =
            Pattern.compile(
                    "^\\d{3,4}$"
            );


    // =====================================================
    // ZONA HORARIA
    // =====================================================

    private final ZoneId zonaHoraria;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public PagoFormularioValidator(

            @Value(
                    "${cita.zona-horaria:America/Guatemala}"
            )
            String zonaHoraria
    ) {

        this.zonaHoraria =
                ZoneId.of(
                        zonaHoraria
                );
    }


    // =====================================================
    // SUPPORTS
    // =====================================================

    @Override
    public boolean supports(
            Class<?> clazz
    ) {

        return PagoFormularioDTO.class
                .isAssignableFrom(
                        clazz
                );
    }


    // =====================================================
    // VALIDAR
    // =====================================================

    @Override
    public void validate(
            Object target,
            Errors errors
    ) {

        PagoFormularioDTO formulario =
                (PagoFormularioDTO) target;


        validarNumeroTarjeta(
                formulario.getNumeroTarjeta(),
                errors
        );


        validarNombreTitular(
                formulario.getNombreTitular(),
                errors
        );


        validarVencimiento(
                formulario.getVencimiento(),
                errors
        );


        validarCvv(
                formulario.getCvv(),
                errors
        );


        validarIdempotencia(
                formulario,
                errors
        );
    }


    // =====================================================
    // RN-CU04-01 - NÚMERO DE TARJETA
    // =====================================================

    private void validarNumeroTarjeta(
            String numeroTarjeta,
            Errors errors
    ) {

        if (numeroTarjeta == null
                || numeroTarjeta.isBlank()) {

            errors.rejectValue(
                    "numeroTarjeta",
                    "pago.numeroTarjeta.invalido",
                    MENSAJE_TARJETA
            );

            return;
        }


        String numero =
                numeroTarjeta.trim();


        // =================================================
        // 13-19 DÍGITOS
        // =================================================

        if (!PATRON_TARJETA
                .matcher(numero)
                .matches()) {

            errors.rejectValue(
                    "numeroTarjeta",
                    "pago.numeroTarjeta.invalido",
                    MENSAJE_TARJETA
            );

            return;
        }


        // =================================================
        // ALGORITMO LUHN
        // =================================================

        if (!cumpleLuhn(
                numero
        )) {

            errors.rejectValue(
                    "numeroTarjeta",
                    "pago.numeroTarjeta.invalido",
                    MENSAJE_TARJETA
            );
        }
    }


    // =====================================================
    // ALGORITMO LUHN
    // =====================================================

    /**
     * Comprueba matemáticamente el dígito de control
     * del número de tarjeta.
     */
    private boolean cumpleLuhn(
            String numero
    ) {

        int suma =
                0;

        boolean duplicar =
                false;


        /*
         * El algoritmo se procesa de derecha a izquierda.
         */
        for (
                int i = numero.length() - 1;
                i >= 0;
                i--
        ) {

            int digito =
                    numero.charAt(i)
                            - '0';


            if (duplicar) {

                digito *=
                        2;


                if (digito > 9) {

                    digito -=
                            9;
                }
            }


            suma +=
                    digito;


            duplicar =
                    !duplicar;
        }


        return suma % 10 == 0;
    }


    // =====================================================
    // RN-CU04-02 - NOMBRE DEL TITULAR
    // =====================================================

    private void validarNombreTitular(
            String nombreTitular,
            Errors errors
    ) {

        if (nombreTitular == null
                || nombreTitular.isBlank()) {

            errors.rejectValue(
                    "nombreTitular",
                    "pago.nombreTitular.invalido",
                    MENSAJE_TITULAR
            );

            return;
        }


        String nombre =
                nombreTitular.trim();


        if (!PATRON_TITULAR
                .matcher(nombre)
                .matches()) {

            errors.rejectValue(
                    "nombreTitular",
                    "pago.nombreTitular.invalido",
                    MENSAJE_TITULAR
            );
        }
    }


    // =====================================================
    // RN-CU04-03 - VENCIMIENTO
    // =====================================================

    private void validarVencimiento(
            String vencimiento,
            Errors errors
    ) {

        if (vencimiento == null
                || vencimiento.isBlank()) {

            errors.rejectValue(
                    "vencimiento",
                    "pago.vencimiento.invalido",
                    MENSAJE_VENCIMIENTO
            );

            return;
        }


        String valor =
                vencimiento.trim();


        Matcher matcher =
                PATRON_VENCIMIENTO
                        .matcher(
                                valor
                        );


        // =================================================
        // FORMATO MM/AA
        // =================================================

        if (!matcher.matches()) {

            errors.rejectValue(
                    "vencimiento",
                    "pago.vencimiento.invalido",
                    MENSAJE_VENCIMIENTO
            );

            return;
        }


        // =================================================
        // CONSTRUIR MES/AÑO
        // =================================================

        int mes =
                Integer.parseInt(
                        matcher.group(1)
                );


        int anioDosDigitos =
                Integer.parseInt(
                        matcher.group(2)
                );


        /*
         * El formato definido por el CU utiliza MM/AA.
         *
         * Para el sistema se interpreta como años
         * comprendidos entre 2000 y 2099.
         */
        int anio =
                2000
                        + anioDosDigitos;


        YearMonth vencimientoTarjeta =
                YearMonth.of(
                        anio,
                        mes
                );


        YearMonth mesActual =
                YearMonth.now(
                        zonaHoraria
                );


        // =================================================
        // TARJETA VENCIDA
        // =================================================
        //
        // Una tarjeta que vence durante el mes actual
        // todavía se considera vigente hasta finalizar
        // dicho mes.
        // =================================================

        if (vencimientoTarjeta
                .isBefore(
                        mesActual
                )) {

            errors.rejectValue(
                    "vencimiento",
                    "pago.vencimiento.invalido",
                    MENSAJE_VENCIMIENTO
            );
        }
    }


    // =====================================================
    // RN-CU04-04 - CVV
    // =====================================================

    private void validarCvv(
            String cvv,
            Errors errors
    ) {

        if (cvv == null
                || cvv.isBlank()) {

            errors.rejectValue(
                    "cvv",
                    "pago.cvv.invalido",
                    MENSAJE_CVV
            );

            return;
        }


        if (!PATRON_CVV
                .matcher(
                        cvv.trim()
                )
                .matches()) {

            errors.rejectValue(
                    "cvv",
                    "pago.cvv.invalido",
                    MENSAJE_CVV
            );
        }
    }


    // =====================================================
    // RNF-016 - IDEMPOTENCIA
    // =====================================================

    private void validarIdempotencia(
            PagoFormularioDTO formulario,
            Errors errors
    ) {

        if (formulario.getIdempotencyKey() == null) {

            /*
             * Este error no corresponde a un campo
             * ingresado directamente por el paciente.
             *
             * Significa que la solicitud perdió o alteró
             * el UUID generado por el servidor.
             */
            errors.reject(
                    "pago.idempotencia.invalida",
                    "No fue posible validar la solicitud de pago. Recargue la página e intente nuevamente."
            );
        }
    }
}