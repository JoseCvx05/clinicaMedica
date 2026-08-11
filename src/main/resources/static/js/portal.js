/**
 * =========================================================
 * CU-00 - PORTAL WEB
 * =========================================================
 *
 * Funcionalidades:
 *
 * - Abrir modal "Verificar Registro".
 * - Cerrar modal.
 * - FA02: cancelar operación.
 * - Contador X/13 del DPI.
 * - Validación RN-GLOBAL-001.
 * - Enviar DPI al backend.
 * - Mostrar "Verificando...".
 * - Procesar paciente registrado.
 * - FA03: paciente no registrado.
 * - FA04: DPI de usuario interno.
 * - FA05: error de conexión.
 * =========================================================
 */

document.addEventListener("DOMContentLoaded", function () {

    // =====================================================
    // MODAL
    // =====================================================

    const modal =
        document.getElementById("modalVerificacionDpi");

    const btnAbrirEncabezado =
        document.getElementById("btnAbrirVerificacionDpi");

    const btnAbrirInicio =
        document.getElementById("btnAgendarCitaInicio");

    const btnAbrirServicios =
        document.getElementById("btnAgendarCitaServicios");

    const btnCerrar =
        document.getElementById("btnCerrarModalDpi");

    const btnCancelar =
        document.getElementById("btnCancelarVerificacionDpi");


    // =====================================================
    // FORMULARIO
    // =====================================================

    const formulario =
        document.getElementById("formVerificacionDpi");

    const campoDpi =
        document.getElementById("dpi");

    const contadorDpi =
        document.getElementById("contadorDpi");

    const mensajeErrorDpi =
        document.getElementById("mensajeErrorDpi");

    const mensajeResultado =
        document.getElementById("mensajeResultadoDpi");

    const estadoCarga =
        document.getElementById("estadoCargaDpi");

    const btnVerificar =
        document.getElementById("btnVerificarDpi");


    // =====================================================
    // CONSTANTES
    // =====================================================

    const LONGITUD_DPI = 13;

    const MENSAJE_DPI_OBLIGATORIO =
        "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.";

    const MENSAJE_DPI_NUMERICO =
        "El DPI debe contener únicamente números. " +
        "No se permiten letras ni caracteres especiales.";

    const MENSAJE_ERROR_CONEXION =
        "No se pudo conectar con el servidor. Intente de nuevo más tarde.";


    // =====================================================
    // CONTROL DE PETICIÓN
    // =====================================================

    /*
     * Nos permite cancelar una petición si el usuario
     * selecciona Cancelar mientras se está verificando.
     */
    let controladorPeticion = null;


    // =====================================================
    // ABRIR MODAL
    // =====================================================

    function abrirModal() {

        limpiarFormulario();

        modal.classList.add("modal-visible");

        modal.setAttribute(
            "aria-hidden",
            "false"
        );

        document.body.classList.add(
            "modal-abierto"
        );

        /*
         * Coloca el cursor directamente en el campo DPI.
         */
        setTimeout(function () {
            campoDpi.focus();
        }, 100);
    }


    // =====================================================
    // CERRAR MODAL
    // =====================================================

    function cerrarModal() {

        /*
         * Si existe una petición en curso, se cancela.
         */
        if (controladorPeticion !== null) {

            controladorPeticion.abort();

            controladorPeticion = null;
        }

        modal.classList.remove(
            "modal-visible"
        );

        modal.setAttribute(
            "aria-hidden",
            "true"
        );

        document.body.classList.remove(
            "modal-abierto"
        );

        limpiarFormulario();
    }


    // =====================================================
    // LIMPIAR FORMULARIO
    // =====================================================

    function limpiarFormulario() {

        formulario.reset();

        mensajeErrorDpi.textContent = "";
        mensajeResultado.textContent = "";

        mensajeResultado.className =
            "mensaje-resultado";

        contadorDpi.textContent =
            "0/13 dígitos";

        estadoCarga.hidden = true;

        campoDpi.disabled = false;
        btnVerificar.disabled = false;
        btnCancelar.disabled = false;

        btnVerificar.textContent =
            "Verificar DPI";
    }


    // =====================================================
    // CONTADOR DEL DPI
    // =====================================================

    campoDpi.addEventListener(
        "input",
        function () {

            /*
             * No eliminamos automáticamente letras ni
             * caracteres especiales.
             *
             * RN-GLOBAL-001 exige mostrar un mensaje
             * específico cuando se ingresan.
             */

            const cantidad =
                campoDpi.value.length;

            contadorDpi.textContent =
                cantidad
                + "/"
                + LONGITUD_DPI
                + " dígitos";


            /*
             * Al modificar el campo quitamos errores
             * anteriores para permitir corregirlo.
             */
            mensajeErrorDpi.textContent = "";

            campoDpi.classList.remove(
                "campo-invalido"
            );
        }
    );


    // =====================================================
    // VALIDACIÓN FRONTEND
    // RN-GLOBAL-001
    // =====================================================

    function validarDpi(dpi) {

        // -------------------------------------------------
        // REGLA 1 - OBLIGATORIO
        // -------------------------------------------------

        if (dpi === null || dpi.trim() === "") {

            return MENSAJE_DPI_OBLIGATORIO;
        }


        // -------------------------------------------------
        // REGLA 2 - EXACTAMENTE 13 CARACTERES
        // -------------------------------------------------

        if (dpi.length !== LONGITUD_DPI) {

            return (
                "El DPI debe contener exactamente 13 dígitos. "
                + "Usted ingresó "
                + dpi.length
                + " dígitos."
            );
        }


        // -------------------------------------------------
        // REGLA 3 - ÚNICAMENTE NÚMEROS
        // -------------------------------------------------

        const expresionDpi =
            /^[0-9]{13}$/;

        if (!expresionDpi.test(dpi)) {

            return MENSAJE_DPI_NUMERICO;
        }


        return null;
    }


    // =====================================================
    // MOSTRAR ERROR DE DPI
    // =====================================================

    function mostrarErrorDpi(mensaje) {

        mensajeErrorDpi.textContent =
            mensaje;

        campoDpi.classList.add(
            "campo-invalido"
        );

        campoDpi.focus();
    }


    // =====================================================
    // ESTADO DE CARGA
    // Paso 7 del flujo normal
    // =====================================================

    function iniciarCarga() {

        estadoCarga.hidden = false;

        campoDpi.disabled = true;

        btnVerificar.disabled = true;

        btnVerificar.textContent =
            "Verificando...";
    }


    function finalizarCarga() {

        estadoCarga.hidden = true;

        campoDpi.disabled = false;

        btnVerificar.disabled = false;

        btnVerificar.textContent =
            "Verificar DPI";
    }


    // =====================================================
    // MENSAJE DE RESULTADO
    // =====================================================

    function mostrarResultado(
        mensaje,
        tipo
    ) {

        mensajeResultado.textContent =
            mensaje;

        mensajeResultado.className =
            "mensaje-resultado "
            + tipo;
    }


    // =====================================================
    // VERIFICAR DPI
    // =====================================================

    formulario.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            mensajeErrorDpi.textContent = "";
            mensajeResultado.textContent = "";

            campoDpi.classList.remove(
                "campo-invalido"
            );


            const dpi =
                campoDpi.value.trim();


            // =================================================
            // FA01 - VALIDACIÓN FRONTEND
            // =================================================

            const errorValidacion =
                validarDpi(dpi);

            if (errorValidacion !== null) {

                mostrarErrorDpi(
                    errorValidacion
                );

                return;
            }


            // =================================================
            // PASO 7 - INDICADOR "VERIFICANDO..."
            // =================================================

            iniciarCarga();


            /*
             * Creamos un controlador para poder cancelar
             * la petición desde FA02.
             */
            controladorPeticion =
                new AbortController();


            try {

                // =============================================
                // PETICIÓN AL BACKEND
                // =============================================

                const respuesta =
                    await fetch(
                        "/api/public/verificar-dpi",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body: JSON.stringify({
                                dpi: dpi
                            }),

                            signal:
                            controladorPeticion.signal
                        }
                    );


                // =============================================
                // FA01 - ERROR DE VALIDACIÓN BACKEND
                // =============================================

                if (!respuesta.ok) {

                    let errorBackend;

                    try {

                        errorBackend =
                            await respuesta.json();

                    } catch (errorJson) {

                        throw new Error(
                            "RESPUESTA_SERVIDOR_INVALIDA"
                        );
                    }


                    if (
                        respuesta.status === 400
                        && errorBackend.campo === "dpi"
                    ) {

                        mostrarErrorDpi(
                            errorBackend.mensaje
                            ?? "El DPI ingresado no es válido."
                        );

                        return;
                    }


                    /*
                     * El servidor respondió, pero ocurrió
                     * otro error controlado.
                     */
                    mostrarResultado(
                        errorBackend.mensaje
                        ?? "No fue posible procesar la solicitud.",
                        "resultado-error"
                    );

                    return;
                }


                // =============================================
                // RESPUESTA CORRECTA
                // =============================================

                const resultado =
                    await respuesta.json();


                // =============================================
                // FLUJO NORMAL
                // PACIENTE REGISTRADO
                // =============================================

                if (
                    resultado.estado
                    === "PACIENTE_REGISTRADO"
                ) {

                    mostrarResultado(
                        resultado.mensaje,
                        "resultado-exito"
                    );


                    if (resultado.redireccion) {

                        setTimeout(function () {

                            window.location.href =
                                resultado.redireccion;

                        }, 1200);
                    }

                    return;
                }


                // =============================================
                // FA03 - NO REGISTRADO
                // =============================================

                if (
                    resultado.estado
                    === "NO_REGISTRADO"
                ) {

                    mostrarResultado(
                        resultado.mensaje,
                        "resultado-informacion"
                    );


                    if (resultado.redireccion) {

                        setTimeout(function () {

                            window.location.href =
                                resultado.redireccion;

                        }, 1500);
                    }

                    return;
                }


                // =============================================
                // FA04 - USUARIO INTERNO
                // =============================================

                if (
                    resultado.estado
                    === "USUARIO_INTERNO"
                ) {

                    mostrarResultado(
                        resultado.mensaje,
                        "resultado-error"
                    );

                    /*
                     * FA04:
                     * permanece en el diálogo.
                     */
                    campoDpi.value = "";

                    contadorDpi.textContent =
                        "0/13 dígitos";

                    setTimeout(function () {
                        campoDpi.focus();
                    }, 100);

                    return;
                }


                // =============================================
                // RESPUESTA DESCONOCIDA
                // =============================================

                mostrarResultado(
                    "No fue posible determinar el estado del registro. "
                    + "Intente nuevamente.",
                    "resultado-error"
                );

            } catch (error) {

                // =============================================
                // FA02 - PETICIÓN CANCELADA
                // =============================================

                if (error.name === "AbortError") {

                    /*
                     * La cancelación fue voluntaria.
                     * No mostramos un error.
                     */
                    return;
                }


                // =============================================
                // FA05 - ERROR DE CONEXIÓN
                // =============================================

                mostrarResultado(
                    MENSAJE_ERROR_CONEXION,
                    "resultado-error"
                );

            } finally {

                controladorPeticion = null;

                finalizarCarga();
            }
        }
    );


    // =====================================================
    // BOTONES PARA ABRIR EL MODAL
    // =====================================================

    btnAbrirEncabezado?.addEventListener(
        "click",
        abrirModal
    );

    btnAbrirInicio?.addEventListener(
        "click",
        abrirModal
    );

    btnAbrirServicios?.addEventListener(
        "click",
        abrirModal
    );


    // =====================================================
    // CERRAR
    // =====================================================

    btnCerrar?.addEventListener(
        "click",
        cerrarModal
    );


    // =====================================================
    // FA02 - CANCELAR OPERACIÓN
    // =====================================================

    btnCancelar?.addEventListener(
        "click",
        function () {

            cerrarModal();
        }
    );


    // =====================================================
    // CERRAR CON TECLA ESC
    // =====================================================

    document.addEventListener(
        "keydown",
        function (event) {

            if (
                event.key === "Escape"
                && modal.classList.contains(
                    "modal-visible"
                )
            ) {

                cerrarModal();
            }
        }
    );


    // =====================================================
    // CERRAR AL HACER CLIC FUERA DEL CONTENIDO
    // =====================================================

    modal.addEventListener(
        "click",
        function (event) {

            if (event.target === modal) {

                cerrarModal();
            }
        }
    );

});