package com.proyecto.clinicamedica.validator.registro;

import com.proyecto.clinicamedica.dto.RegistroExternoDTO;
import com.proyecto.clinicamedica.dto.ResultadoValidacionRegistroExterno;

import com.proyecto.clinicamedica.repository.UsuarioRepository;

import com.proyecto.clinicamedica.service.HashService;

import org.springframework.stereotype.Component;

import java.util.Locale;


/**
 * =========================================================
 * VALIDADOR: DUPLICADOS DE REGISTRO EXTERNO
 * =========================================================
 *
 * CU-02 Registro de Usuarios Externos.
 *
 * Responsabilidad:
 *
 * - FA02: DPI duplicado.
 * - FA03: correo duplicado.
 * - RN-CU02-05: usuario duplicado.
 * - Evitar NIT duplicado antes de llegar a PostgreSQL.
 *
 * No valida formatos.
 * Los formatos son responsabilidad de los validadores
 * anteriores.
 * =========================================================
 */
@Component
public class ValidadorDuplicadosRegistroExterno
        implements ValidadorRegistroExterno {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final HashService hashService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ValidadorDuplicadosRegistroExterno(
            UsuarioRepository usuarioRepository,
            HashService hashService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.hashService =
                hashService;
    }


    // =====================================================
    // ORDEN
    // =====================================================

    @Override
    public int orden() {

        /*
         * Se ejecuta después de:
         *
         * 10 -> Datos personales.
         * 20 -> Credenciales.
         *
         * Primero comprobamos formato y después hacemos
         * consultas a PostgreSQL.
         */
        return 30;
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


        validarDpiDuplicado(
                formulario,
                resultado
        );


        validarCorreoDuplicado(
                formulario,
                resultado
        );


        validarUsuarioDuplicado(
                formulario,
                resultado
        );


        validarNitDuplicado(
                formulario,
                resultado
        );
    }


    // =====================================================
    // FA02 - DPI YA REGISTRADO
    // =====================================================

    private void validarDpiDuplicado(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        /*
         * Si el DPI ya falló por obligatorio,
         * longitud o formato, no consultamos la BD.
         */
        if (resultado.tieneError(
                "dpi"
        )) {

            return;
        }


        String dpi =
                formulario
                        .getDpi()
                        .trim();


        /*
         * Nunca buscamos DPI en texto plano.
         *
         * Generamos el mismo HMAC determinístico
         * utilizado para almacenar/buscar usuarios.
         */
        String dpiHash =
                hashService.generarHash(
                        dpi
                );


        if (usuarioRepository
                .existsByDpiHash(
                        dpiHash
                )) {

            resultado.agregarError(
                    "dpi",

                    "Ya existe una cuenta registrada con este "
                            + "número de DPI. Si ya tiene cuenta, "
                            + "inicie sesión."
            );
        }
    }


    // =====================================================
    // FA03 - CORREO YA REGISTRADO
    // =====================================================

    private void validarCorreoDuplicado(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        if (resultado.tieneError(
                "correoElectronico"
        )) {

            return;
        }


        String correo =
                formulario
                        .getCorreoElectronico()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        if (usuarioRepository
                .existsByCorreoElectronicoIgnoreCase(
                        correo
                )) {

            resultado.agregarError(
                    "correoElectronico",

                    "Ya existe una cuenta registrada "
                            + "con este correo electrónico."
            );
        }
    }


    // =====================================================
    // RN-CU02-05 - USUARIO ÚNICO
    // =====================================================

    private void validarUsuarioDuplicado(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        if (resultado.tieneError(
                "nombreUsuario"
        )) {

            return;
        }


        String nombreUsuario =
                formulario
                        .getNombreUsuario()
                        .trim();


        if (usuarioRepository
                .existsByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El nombre de usuario ya se encuentra registrado."
            );
        }
    }


    // =====================================================
    // NIT DUPLICADO
    // =====================================================

    private void validarNitDuplicado(
            RegistroExternoDTO formulario,
            ResultadoValidacionRegistroExterno resultado
    ) {

        if (resultado.tieneError(
                "nit"
        )) {

            return;
        }


        String nit =
                formulario
                        .getNit()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );


        String nitHash =
                hashService.generarHash(
                        nit
                );


        if (usuarioRepository
                .existsByNitHash(
                        nitHash
                )) {

            /*
             * El documento de CU-02 no proporciona
             * un mensaje específico para NIT duplicado.
             *
             * Lo controlamos para evitar que una
             * restricción única termine produciendo
             * un error 500.
             */
            resultado.agregarError(
                    "nit",
                    "Ya existe una cuenta registrada con este NIT."
            );
        }
    }
}