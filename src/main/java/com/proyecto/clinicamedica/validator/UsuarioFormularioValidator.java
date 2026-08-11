package com.proyecto.clinicamedica.validator;

import com.proyecto.clinicamedica.dto.ResultadoValidacionUsuario;
import com.proyecto.clinicamedica.dto.UsuarioFormularioDTO;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * =========================================================
 * VALIDADOR: FORMULARIO DE USUARIO
 * =========================================================
 *
 * Centraliza las validaciones de formato y obligatoriedad
 * correspondientes al CU-01.
 *
 * Se utiliza tanto para:
 *
 * - Crear usuario.
 * - Editar usuario.
 *
 * IMPORTANTE:
 *
 * Este validador NO consulta la base de datos.
 *
 * Las validaciones que requieren persistencia se harán
 * posteriormente en UsuarioMantenimientoServiceImpl:
 *
 * - Usuario duplicado.
 * - Correo duplicado.
 * - DPI duplicado.
 * - NIT duplicado.
 * - Existencia de Rol.
 * - Existencia de Sucursal.
 * - Existencia de Especialidad.
 *
 * =========================================================
 */
@Component
public class UsuarioFormularioValidator {

    // =====================================================
    // EXPRESIONES REGULARES
    // =====================================================

    private static final Pattern SOLO_NUMEROS =
            Pattern.compile("^[0-9]+$");


    private static final Pattern ALFANUMERICO =
            Pattern.compile("^[A-Za-z0-9]+$");


    private static final Pattern EMAIL =
            Pattern.compile(
                    "^[A-Za-z0-9._%+-]+"
                            + "@"
                            + "[A-Za-z0-9.-]+"
                            + "\\."
                            + "[A-Za-z]{2,}$"
            );


    private static final Pattern TIENE_MAYUSCULA =
            Pattern.compile(".*[A-Z].*");


    private static final Pattern TIENE_MINUSCULA =
            Pattern.compile(".*[a-z].*");


    private static final Pattern TIENE_NUMERO =
            Pattern.compile(".*[0-9].*");


    private static final Pattern TIENE_ESPECIAL =
            Pattern.compile(
                    ".*[^A-Za-z0-9].*"
            );


    // =====================================================
    // VALIDAR CREACIÓN
    // =====================================================

    /**
     * Valida el formulario para FA01 - Crear Usuario.
     *
     * @param dto datos ingresados
     * @param rolEsMedico indica si el rol seleccionado
     *                    corresponde a Médico
     *
     * @return resultado con todos los errores encontrados
     */
    public ResultadoValidacionUsuario validarCreacion(
            UsuarioFormularioDTO dto,
            boolean rolEsMedico
    ) {

        ResultadoValidacionUsuario resultado =
                new ResultadoValidacionUsuario();


        if (dto == null) {

            resultado.agregarError(
                    "formulario",
                    "No se recibieron los datos del usuario."
            );

            return resultado;
        }


        // =================================================
        // VALIDACIONES COMUNES
        // =================================================

        validarNombre(
                dto,
                resultado
        );


        validarCorreo(
                dto,
                resultado
        );


        validarNombreUsuario(
                dto,
                resultado
        );


        validarDpi(
                dto,
                resultado
        );


        validarTelefono(
                dto,
                resultado
        );


        validarNit(
                dto,
                resultado
        );


        validarNumeroSeguro(
                dto,
                resultado
        );


        validarRol(
                dto,
                resultado
        );


        validarEstado(
                dto,
                resultado
        );


        // =================================================
        // CREACIÓN: SUCURSAL OBLIGATORIA
        // =================================================

        validarSucursalCreacion(
                dto,
                resultado
        );


        // =================================================
        // CREACIÓN: CONTRASEÑA OBLIGATORIA
        // =================================================

        validarContrasenaCreacion(
                dto,
                resultado
        );


        // =================================================
        // MÉDICO: ESPECIALIDAD OBLIGATORIA
        // =================================================

        validarEspecialidad(
                dto,
                rolEsMedico,
                resultado
        );


        return resultado;
    }


    // =====================================================
    // VALIDAR EDICIÓN
    // =====================================================

    /**
     * Valida FA04 - Editar Usuario.
     *
     * Diferencias respecto a creación:
     *
     * - Sucursal puede quedar vacía.
     * - Contraseña puede quedar vacía para conservar
     *   la contraseña actual.
     */
    public ResultadoValidacionUsuario validarEdicion(
            UsuarioFormularioDTO dto,
            boolean rolEsMedico
    ) {

        ResultadoValidacionUsuario resultado =
                new ResultadoValidacionUsuario();


        if (dto == null) {

            resultado.agregarError(
                    "formulario",
                    "No se recibieron los datos del usuario."
            );

            return resultado;
        }


        // =================================================
        // VALIDACIONES COMUNES
        // =================================================

        validarNombre(
                dto,
                resultado
        );


        validarCorreo(
                dto,
                resultado
        );


        validarNombreUsuario(
                dto,
                resultado
        );


        validarDpi(
                dto,
                resultado
        );


        validarTelefono(
                dto,
                resultado
        );


        validarNit(
                dto,
                resultado
        );


        validarNumeroSeguro(
                dto,
                resultado
        );


        validarRol(
                dto,
                resultado
        );


        validarEstado(
                dto,
                resultado
        );


        // =================================================
        // EDICIÓN:
        // SUCURSAL ES OPCIONAL
        // =================================================


        // =================================================
        // EDICIÓN:
        // CONTRASEÑA SOLO SE VALIDA SI SE INGRESA
        // =================================================

        validarContrasenaEdicion(
                dto,
                resultado
        );


        // =================================================
        // MÉDICO
        // =================================================

        validarEspecialidad(
                dto,
                rolEsMedico,
                resultado
        );


        return resultado;
    }


    // =====================================================
    // RN-CU01-04
    // NOMBRE
    // =====================================================

    private void validarNombre(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String nombre =
                normalizar(
                        dto.getNombreCompleto()
                );


        // =================================================
        // OBLIGATORIO
        // =================================================

        if (nombre == null) {

            resultado.agregarError(
                    "nombreCompleto",
                    "El campo Nombre es obligatorio."
            );

            return;
        }


        // =================================================
        // LONGITUD 10 - 100
        // =================================================

        int longitud =
                nombre.length();


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
    // CORREO ELECTRÓNICO
    // =====================================================

    private void validarCorreo(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String correo =
                normalizar(
                        dto.getCorreoElectronico()
                );


        /*
         * El correo forma parte del formulario del CU-01
         * y la columna usuario.correo_electronico es
         * obligatoria en nuestro modelo.
         */
        if (correo == null) {

            resultado.agregarError(
                    "correoElectronico",
                    "El campo Correo electrónico es obligatorio."
            );

            return;
        }


        if (!EMAIL.matcher(
                correo
        ).matches()) {

            /*
             * Utilizamos el mismo mensaje de formato de
             * correo definido por las reglas del sistema.
             */
            resultado.agregarError(
                    "correoElectronico",

                    "El formato del correo electrónico no es válido. "
                            + "Ejemplo: usuario@dominio.com"
            );
        }
    }


    // =====================================================
    // RN-CU01-05
    // NOMBRE DE USUARIO
    // =====================================================

    private void validarNombreUsuario(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String usuario =
                normalizar(
                        dto.getNombreUsuario()
                );


        // =================================================
        // OBLIGATORIO
        // =================================================

        if (usuario == null) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El campo Usuario es obligatorio."
            );

            return;
        }


        // =================================================
        // MÍNIMO 8
        // =================================================

        if (usuario.length() < 8) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El usuario debe contener al menos 8 caracteres."
            );

            return;
        }


        // =================================================
        // MÁXIMO 9
        // =================================================

        if (usuario.length() > 9) {

            resultado.agregarError(
                    "nombreUsuario",
                    "El usuario no puede exceder los 9 caracteres."
            );

            return;
        }


        // =================================================
        // SOLO ALFANUMÉRICO
        // =================================================

        if (!ALFANUMERICO
                .matcher(
                        usuario
                )
                .matches()) {

            resultado.agregarError(
                    "nombreUsuario",

                    "El usuario debe contener únicamente "
                            + "caracteres alfanuméricos."
            );
        }
    }


    // =====================================================
    // RN-CU01-07
    // DPI OPCIONAL
    // =====================================================

    private void validarDpi(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String dpi =
                normalizar(
                        dto.getDpi()
                );


        // =================================================
        // OPCIONAL
        // =================================================

        if (dpi == null) {
            return;
        }


        // =================================================
        // EXACTAMENTE 13 CARACTERES
        // =================================================

        if (dpi.length() != 13) {

            resultado.agregarError(
                    "dpi",

                    "El DPI debe contener exactamente 13 dígitos. "
                            + "Usted ingresó "
                            + dpi.length()
                            + " dígitos."
            );

            return;
        }


        // =================================================
        // SOLO NÚMEROS
        // =================================================

        if (!SOLO_NUMEROS
                .matcher(
                        dpi
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
    // RN-CU01-08
    // TELÉFONO OPCIONAL
    // =====================================================

    private void validarTelefono(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String telefono =
                normalizar(
                        dto.getTelefono()
                );


        if (telefono == null) {
            return;
        }


        /*
         * El requisito dice exactamente 8 dígitos.
         *
         * Por eso cualquier incumplimiento utiliza
         * el mismo mensaje.
         */
        if (telefono.length() != 8
                || !SOLO_NUMEROS
                .matcher(
                        telefono
                )
                .matches()) {

            resultado.agregarError(
                    "telefono",

                    "El teléfono debe contener exactamente 8 dígitos."
            );
        }
    }


    // =====================================================
    // RN-CU01-11
    // NIT OPCIONAL
    // RN-GLOBAL-002
    // =====================================================

    private void validarNit(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String nit =
                normalizar(
                        dto.getNit()
                );


        // =================================================
        // OPCIONAL EN CU-01
        // =================================================

        if (nit == null) {
            return;
        }


        // =================================================
        // LONGITUD 8 - 9
        // =================================================

        if (nit.length() < 8
                || nit.length() > 9) {

            resultado.agregarError(
                    "nit",

                    "El NIT debe contener entre 8 y 9 caracteres. "
                            + "Usted ingresó "
                            + nit.length()
                            + " caracteres."
            );

            return;
        }


        // =================================================
        // ALFANUMÉRICO
        // =================================================

        if (!ALFANUMERICO
                .matcher(
                        nit
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
    // RN-CU01-12
    // NÚMERO DE SEGURO
    // =====================================================

    private void validarNumeroSeguro(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String numeroSeguro =
                normalizar(
                        dto.getNumeroSeguro()
                );


        if (numeroSeguro == null) {
            return;
        }


        int longitud =
                numeroSeguro.length();


        if (longitud < 5
                || longitud > 50) {

            resultado.agregarError(
                    "numeroSeguro",

                    "El número de seguro debe contener "
                            + "entre 5 y 50 caracteres."
            );
        }
    }


    // =====================================================
    // RN-CU01-03 / RN-CU01-09
    // ROL
    // =====================================================

    private void validarRol(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        if (dto.getIdRol() == null) {

            resultado.agregarError(
                    "idRol",

                    "Debe seleccionar un rol para el usuario."
            );
        }
    }


    // =====================================================
    // RN-CU01-06 / RN-CU01-13
    // SUCURSAL
    // =====================================================

    private void validarSucursalCreacion(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        if (dto.getIdSucursal() == null) {

            resultado.agregarError(
                    "idSucursal",

                    "Debe seleccionar una sucursal para el usuario."
            );
        }
    }


    // =====================================================
    // RN-CU01-10
    // ESTADO
    // =====================================================

    private void validarEstado(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        if (dto.getActivo() == null) {

            resultado.agregarError(
                    "activo",

                    "Debe seleccionar un estado para el usuario."
            );
        }
    }


    // =====================================================
    // RN-CU01-14
    // ESPECIALIDAD
    // =====================================================

    private void validarEspecialidad(
            UsuarioFormularioDTO dto,
            boolean rolEsMedico,
            ResultadoValidacionUsuario resultado
    ) {

        /*
         * La especialidad únicamente se exige cuando
         * el rol corresponde a Médico.
         */
        if (rolEsMedico
                && dto.getIdEspecialidad() == null) {

            resultado.agregarError(
                    "idEspecialidad",

                    "Debe seleccionar una especialidad para el médico."
            );
        }
    }


    // =====================================================
    // RNF-015
    // CONTRASEÑA EN CREACIÓN
    // =====================================================

    private void validarContrasenaCreacion(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String contrasena =
                dto.getContrasena();


        // =================================================
        // OBLIGATORIA EN CREACIÓN
        // =================================================

        if (contrasena == null
                || contrasena.isBlank()) {

            resultado.agregarError(
                    "contrasena",

                    "La contraseña es obligatoria al crear un usuario."
            );

            return;
        }


        validarPoliticaContrasena(
                contrasena,
                resultado
        );
    }


    // =====================================================
    // CONTRASEÑA EN EDICIÓN
    // =====================================================

    private void validarContrasenaEdicion(
            UsuarioFormularioDTO dto,
            ResultadoValidacionUsuario resultado
    ) {

        String contrasena =
                dto.getContrasena();


        /*
         * FA04 permite dejar Nueva Contraseña vacía.
         * En ese caso se conserva el BCrypt actual.
         */
        if (contrasena == null
                || contrasena.isBlank()) {

            return;
        }


        validarPoliticaContrasena(
                contrasena,
                resultado
        );
    }


    // =====================================================
    // POLÍTICA DE CONTRASEÑA
    // =====================================================

    private void validarPoliticaContrasena(
            String contrasena,
            ResultadoValidacionUsuario resultado
    ) {

        // =================================================
        // MÍNIMO 12 CARACTERES
        // =================================================

        if (contrasena.length() < 12) {

            resultado.agregarError(
                    "contrasena",

                    "La contraseña debe contener al menos 12 caracteres."
            );

            return;
        }


        // =================================================
        // COMBINACIÓN
        // =================================================

        boolean tieneMayuscula =
                TIENE_MAYUSCULA
                        .matcher(
                                contrasena
                        )
                        .matches();


        boolean tieneMinuscula =
                TIENE_MINUSCULA
                        .matcher(
                                contrasena
                        )
                        .matches();


        boolean tieneNumero =
                TIENE_NUMERO
                        .matcher(
                                contrasena
                        )
                        .matches();


        boolean tieneEspecial =
                TIENE_ESPECIAL
                        .matcher(
                                contrasena
                        )
                        .matches();


        if (!tieneMayuscula
                || !tieneMinuscula
                || !tieneNumero
                || !tieneEspecial) {

            resultado.agregarError(
                    "contrasena",

                    "La contraseña debe incluir al menos "
                            + "una letra mayúscula, una letra minúscula, "
                            + "un número y un carácter especial."
            );
        }
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private String normalizar(
            String valor
    ) {

        if (valor == null) {
            return null;
        }


        String resultado =
                valor.trim();


        return resultado.isEmpty()
                ? null
                : resultado;
    }
}