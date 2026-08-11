document.addEventListener("DOMContentLoaded", function () {

    // =====================================================
    // ELEMENTOS DEL FORMULARIO
    // =====================================================

    const formulario =
        document.getElementById("formLogin");

    const campoUsuario =
        document.getElementById("nombreUsuario");

    const campoContrasena =
        document.getElementById("contrasena");

    const btnMostrarContrasena =
        document.getElementById("btnMostrarContrasena");

    const btnLogin =
        document.getElementById("btnLogin");

    const textoBtnLogin =
        document.getElementById("textoBtnLogin");

    const cargandoLogin =
        document.getElementById("cargandoLogin");

    const mensajeLogin =
        document.getElementById("mensajeLogin");

    const errorNombreUsuario =
        document.getElementById("errorNombreUsuario");

    const errorContrasena =
        document.getElementById("errorContrasena");

    const contenedorBloqueo =
        document.getElementById("contenedorBloqueo");

    const mensajeBloqueo =
        document.getElementById("mensajeBloqueo");

    const contadorBloqueo =
        document.getElementById("contadorBloqueo");


    // =====================================================
    // VARIABLES
    // =====================================================

    let intervaloBloqueo = null;

    let solicitudEnProceso = false;


    // =====================================================
    // MOSTRAR / OCULTAR CONTRASEÑA
    // =====================================================

    btnMostrarContrasena.addEventListener(
        "click",
        function () {

            const mostrando =
                campoContrasena.type === "text";

            if (mostrando) {

                campoContrasena.type = "password";

                btnMostrarContrasena.textContent =
                    "Mostrar";

                btnMostrarContrasena.setAttribute(
                    "aria-label",
                    "Mostrar contraseña"
                );

            } else {

                campoContrasena.type = "text";

                btnMostrarContrasena.textContent =
                    "Ocultar";

                btnMostrarContrasena.setAttribute(
                    "aria-label",
                    "Ocultar contraseña"
                );
            }
        }
    );


    // =====================================================
    // LIMPIAR ERROR AL ESCRIBIR
    // =====================================================

    campoUsuario.addEventListener(
        "input",
        function () {

            limpiarErrorCampo(
                campoUsuario,
                errorNombreUsuario
            );
        }
    );


    campoContrasena.addEventListener(
        "input",
        function () {

            limpiarErrorCampo(
                campoContrasena,
                errorContrasena
            );
        }
    );


    // =====================================================
    // SUBMIT
    // =====================================================

    formulario.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();


            if (solicitudEnProceso) {
                return;
            }


            limpiarMensajes();


            // =================================================
            // VALIDACIÓN DEL FRONTEND
            // =================================================

            const formularioValido =
                validarFormulario();


            if (!formularioValido) {
                return;
            }


            const datos = {

                nombreUsuario:
                    campoUsuario.value.trim(),

                contrasena:
                campoContrasena.value
            };


            iniciarCarga();


            try {

                const respuesta =
                    await fetch(
                        "/api/public/login",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify(datos)
                        }
                    );


                let contenido = null;


                try {

                    contenido =
                        await respuesta.json();

                } catch (error) {

                    throw new Error(
                        "Respuesta inválida del servidor."
                    );
                }


                // =================================================
                // HTTP 400 - VALIDACIÓN DEL BACKEND
                // =================================================

                if (respuesta.status === 400) {

                    manejarErrorValidacion(
                        contenido
                    );

                    return;
                }


                // =================================================
                // FA06 / FA07
                // =================================================

                if (respuesta.status === 401) {

                    manejarNoAutorizado(
                        contenido
                    );

                    return;
                }


                // =================================================
                // FA09
                // =================================================

                if (respuesta.status === 403) {

                    manejarRolNoAutorizado(
                        contenido
                    );

                    return;
                }


                // =================================================
                // OTRO ERROR HTTP
                // =================================================

                if (!respuesta.ok) {

                    mostrarMensaje(
                        contenido?.mensaje
                        || "Ocurrió un error al iniciar sesión.",
                        "error"
                    );

                    return;
                }


                // =================================================
                // LOGIN EXITOSO
                // =================================================

                if (
                    contenido.estado
                    === "AUTENTICADO"
                ) {

                    manejarLoginExitoso(
                        contenido
                    );

                    return;
                }


                mostrarMensaje(
                    "No fue posible completar el inicio de sesión.",
                    "error"
                );


            } catch (error) {

                /*
                 * FA08:
                 *
                 * Fallo de conexión con el servidor.
                 */
                mostrarMensaje(
                    "No se pudo conectar con el servidor. "
                    + "Intente de nuevo más tarde.",
                    "error"
                );

            } finally {

                /*
                 * Si la cuenta entró en bloqueo,
                 * mantenerFormularioBloqueado() será quien
                 * controle los campos.
                 */
                if (!contenedorBloqueo.hidden) {

                    solicitudEnProceso = false;

                    return;
                }


                finalizarCarga();
            }
        }
    );


    // =====================================================
    // VALIDAR FORMULARIO
    // =====================================================

    function validarFormulario() {

        let valido = true;


        const nombreUsuario =
            campoUsuario.value.trim();

        const contrasena =
            campoContrasena.value;


        // =================================================
        // USUARIO
        // =================================================

        if (nombreUsuario === "") {

            mostrarErrorCampo(
                campoUsuario,
                errorNombreUsuario,
                "El nombre de usuario es obligatorio."
            );

            valido = false;
        }


        // =================================================
        // CONTRASEÑA
        // =================================================

        if (contrasena.trim() === "") {

            mostrarErrorCampo(
                campoContrasena,
                errorContrasena,
                "La contraseña es obligatoria."
            );

            valido = false;
        }


        return valido;
    }


    // =====================================================
    // MANEJAR ERROR DE VALIDACIÓN BACKEND
    // =====================================================

    function manejarErrorValidacion(
        contenido
    ) {

        if (
            contenido?.campo
            === "nombreUsuario"
        ) {

            mostrarErrorCampo(
                campoUsuario,
                errorNombreUsuario,
                contenido.mensaje
            );

            campoUsuario.focus();

            return;
        }


        if (
            contenido?.campo
            === "contrasena"
        ) {

            mostrarErrorCampo(
                campoContrasena,
                errorContrasena,
                contenido.mensaje
            );

            campoContrasena.focus();

            return;
        }


        mostrarMensaje(
            contenido?.mensaje
            || "Revise la información ingresada.",
            "error"
        );
    }


    // =====================================================
    // FA06 / FA07
    // =====================================================

    function manejarNoAutorizado(
        contenido
    ) {

        if (
            contenido.estado
            === "CUENTA_BLOQUEADA"
        ) {

            iniciarBloqueo(
                contenido
            );

            return;
        }


        if (
            contenido.estado
            === "CREDENCIALES_INCORRECTAS"
        ) {

            mostrarMensaje(
                contenido.mensaje,
                "error"
            );

            campoContrasena.value = "";

            campoContrasena.focus();

            return;
        }


        mostrarMensaje(
            contenido.mensaje
            || "No fue posible iniciar sesión.",
            "error"
        );
    }


    // =====================================================
    // FA09
    // =====================================================

    function manejarRolNoAutorizado(
        contenido
    ) {

        mostrarMensaje(
            contenido.mensaje
            || "Este acceso es exclusivo para pacientes.",
            "warning"
        );

        campoContrasena.value = "";

        campoUsuario.focus();
    }


    // =====================================================
    // LOGIN EXITOSO
    // =====================================================

    function manejarLoginExitoso(
        contenido
    ) {

        mostrarMensaje(
            contenido.mensaje
            || "Inicio de sesión exitoso.",
            "success"
        );


        /*
         * Ya no permitimos otro submit durante
         * la redirección.
         */
        solicitudEnProceso = true;

        campoUsuario.disabled = true;
        campoContrasena.disabled = true;
        btnMostrarContrasena.disabled = true;
        btnLogin.disabled = true;


        const redireccion =
            contenido.redireccion
            || "/paciente/dashboard";


        setTimeout(
            function () {

                window.location.href =
                    redireccion;

            },
            700
        );
    }


    // =====================================================
    // FA07 - INICIAR BLOQUEO
    // =====================================================

    function iniciarBloqueo(
        contenido
    ) {

        mostrarMensaje(
            contenido.mensaje
            || "Cuenta bloqueada temporalmente. "
            + "Intente de nuevo en 15 minutos.",
            "warning"
        );


        contenedorBloqueo.hidden =
            false;


        mensajeBloqueo.textContent =
            contenido.mensaje
            || "Cuenta bloqueada temporalmente. "
            + "Intente de nuevo en 15 minutos.";


        mantenerFormularioBloqueado();


        campoContrasena.value =
            "";


        if (intervaloBloqueo !== null) {

            clearInterval(
                intervaloBloqueo
            );
        }


        const bloqueadoHasta =
            contenido.bloqueadoHasta;


        if (!bloqueadoHasta) {

            contadorBloqueo.textContent =
                "15:00";

            return;
        }


        actualizarContadorBloqueo(
            bloqueadoHasta
        );


        intervaloBloqueo =
            setInterval(
                function () {

                    actualizarContadorBloqueo(
                        bloqueadoHasta
                    );

                },
                1000
            );
    }


    // =====================================================
    // CONTADOR DE BLOQUEO
    // =====================================================

    function actualizarContadorBloqueo(
        bloqueadoHasta
    ) {

        const fechaFinal =
            new Date(
                bloqueadoHasta
            );


        const ahora =
            new Date();


        let diferencia =
            fechaFinal.getTime()
            - ahora.getTime();


        // =================================================
        // BLOQUEO FINALIZADO
        // =================================================

        if (
            Number.isNaN(
                fechaFinal.getTime()
            )
        ) {

            contadorBloqueo.textContent =
                "15:00";

            return;
        }


        if (diferencia <= 0) {

            finalizarBloqueo();

            return;
        }


        const segundosTotales =
            Math.ceil(
                diferencia / 1000
            );


        const minutos =
            Math.floor(
                segundosTotales / 60
            );


        const segundos =
            segundosTotales % 60;


        contadorBloqueo.textContent =
            String(minutos)
                .padStart(2, "0")
            + ":"
            + String(segundos)
                .padStart(2, "0");
    }


    // =====================================================
    // MANTENER CAMPOS BLOQUEADOS
    // =====================================================

    function mantenerFormularioBloqueado() {

        solicitudEnProceso =
            false;

        campoUsuario.disabled =
            true;

        campoContrasena.disabled =
            true;

        btnMostrarContrasena.disabled =
            true;

        btnLogin.disabled =
            true;


        textoBtnLogin.hidden =
            false;

        cargandoLogin.hidden =
            true;
    }


    // =====================================================
    // FINALIZAR BLOQUEO
    // =====================================================

    function finalizarBloqueo() {

        if (intervaloBloqueo !== null) {

            clearInterval(
                intervaloBloqueo
            );

            intervaloBloqueo =
                null;
        }


        contenedorBloqueo.hidden =
            true;


        campoUsuario.disabled =
            false;

        campoContrasena.disabled =
            false;

        btnMostrarContrasena.disabled =
            false;

        btnLogin.disabled =
            false;


        solicitudEnProceso =
            false;


        mostrarMensaje(
            "El bloqueo temporal ha finalizado. "
            + "Puede intentar iniciar sesión nuevamente.",
            "success"
        );


        campoContrasena.focus();
    }


    // =====================================================
    // INICIAR CARGA
    // =====================================================

    function iniciarCarga() {

        solicitudEnProceso =
            true;


        btnLogin.disabled =
            true;

        campoUsuario.disabled =
            true;

        campoContrasena.disabled =
            true;

        btnMostrarContrasena.disabled =
            true;


        textoBtnLogin.hidden =
            true;

        cargandoLogin.hidden =
            false;
    }


    // =====================================================
    // FINALIZAR CARGA
    // =====================================================

    function finalizarCarga() {

        solicitudEnProceso =
            false;


        btnLogin.disabled =
            false;

        campoUsuario.disabled =
            false;

        campoContrasena.disabled =
            false;

        btnMostrarContrasena.disabled =
            false;


        textoBtnLogin.hidden =
            false;

        cargandoLogin.hidden =
            true;
    }


    // =====================================================
    // MOSTRAR MENSAJE
    // =====================================================

    function mostrarMensaje(
        mensaje,
        tipo
    ) {

        mensajeLogin.textContent =
            mensaje;

        mensajeLogin.className =
            "login-message " + tipo;

        mensajeLogin.hidden =
            false;
    }


    // =====================================================
    // LIMPIAR MENSAJES
    // =====================================================

    function limpiarMensajes() {

        mensajeLogin.hidden =
            true;

        mensajeLogin.textContent =
            "";

        mensajeLogin.className =
            "login-message";


        limpiarErrorCampo(
            campoUsuario,
            errorNombreUsuario
        );


        limpiarErrorCampo(
            campoContrasena,
            errorContrasena
        );
    }


    // =====================================================
    // MOSTRAR ERROR DE CAMPO
    // =====================================================

    function mostrarErrorCampo(
        campo,
        contenedorError,
        mensaje
    ) {

        campo.classList.add(
            "input-error"
        );

        contenedorError.textContent =
            mensaje;
    }


    // =====================================================
    // LIMPIAR ERROR DE CAMPO
    // =====================================================

    function limpiarErrorCampo(
        campo,
        contenedorError
    ) {

        campo.classList.remove(
            "input-error"
        );

        contenedorError.textContent =
            "";
    }

});