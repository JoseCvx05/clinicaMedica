document.addEventListener("DOMContentLoaded", function () {

    // =====================================================
    // ELEMENTOS
    // =====================================================

    const formulario =
        document.getElementById("formLoginInterno");

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

    const mensajeSesionExpirada =
        document.getElementById("mensajeSesionExpirada");

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
    // ESTADO
    // =====================================================

    let solicitudEnProceso = false;

    let intervaloBloqueo = null;


    // =====================================================
    // RN-GLOBAL-007
    // SESIÓN EXPIRADA
    // =====================================================

    /*
     * Posteriormente Spring Security podrá redirigir a:
     *
     * /login-interno?sesion=expirada
     *
     * y este bloque mostrará el mensaje correspondiente.
     */
    const parametros =
        new URLSearchParams(
            window.location.search
        );


    if (
        parametros.get("sesion")
        === "expirada"
    ) {

        mensajeSesionExpirada.hidden =
            false;
    }


    // =====================================================
    // MOSTRAR / OCULTAR CONTRASEÑA
    // =====================================================

    btnMostrarContrasena.addEventListener(
        "click",
        function () {

            const estaVisible =
                campoContrasena.type === "text";


            if (estaVisible) {

                campoContrasena.type =
                    "password";

                btnMostrarContrasena.textContent =
                    "Mostrar";

                btnMostrarContrasena.setAttribute(
                    "aria-label",
                    "Mostrar contraseña"
                );

            } else {

                campoContrasena.type =
                    "text";

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


            if (!validarFormulario()) {
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
                        "/api/interno/login",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify(
                                    datos
                                )
                        }
                    );


                let contenido = null;


                try {

                    contenido =
                        await respuesta.json();

                } catch (error) {

                    throw new Error(
                        "El servidor devolvió una respuesta inválida."
                    );
                }


                // =================================================
                // VALIDACIONES DEL BACKEND
                // =================================================

                if (respuesta.status === 400) {

                    manejarErrorValidacion(
                        contenido
                    );

                    return;
                }


                // =================================================
                // RN-GLOBAL-007
                // CREDENCIALES / BLOQUEO
                // =================================================

                if (respuesta.status === 401) {

                    manejarNoAutorizado(
                        contenido
                    );

                    return;
                }


                // =================================================
                // USUARIO NO INTERNO
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
                        || "No fue posible iniciar sesión.",
                        "error"
                    );

                    return;
                }


                // =================================================
                // LOGIN CORRECTO
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
                 * Error de comunicación con el backend.
                 */
                mostrarMensaje(
                    "No se pudo conectar con el servidor. "
                    + "Intente de nuevo más tarde.",
                    "error"
                );

            } finally {

                /*
                 * Si entró en estado de bloqueo,
                 * no debemos volver a habilitar
                 * los controles.
                 */
                if (!contenedorBloqueo.hidden) {

                    solicitudEnProceso =
                        false;

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


        const usuario =
            campoUsuario.value.trim();

        const contrasena =
            campoContrasena.value;


        // =================================================
        // USUARIO OBLIGATORIO
        // =================================================

        if (usuario === "") {

            mostrarErrorCampo(
                campoUsuario,
                errorNombreUsuario,
                "El nombre de usuario es obligatorio."
            );

            valido = false;
        }


        // =================================================
        // CONTRASEÑA OBLIGATORIA
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
    // ERROR DE VALIDACIÓN DEL BACKEND
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
    // RN-GLOBAL-007
    // CREDENCIALES INCORRECTAS / BLOQUEO
    // =====================================================

    function manejarNoAutorizado(
        contenido
    ) {

        if (
            contenido?.estado
            === "CUENTA_BLOQUEADA"
        ) {

            iniciarBloqueo(
                contenido
            );

            return;
        }


        if (
            contenido?.estado
            === "CREDENCIALES_INCORRECTAS"
        ) {

            mostrarMensaje(
                contenido.mensaje,
                "error"
            );


            campoContrasena.value =
                "";


            campoContrasena.focus();

            return;
        }


        mostrarMensaje(
            contenido?.mensaje
            || "No fue posible iniciar sesión.",
            "error"
        );
    }


    // =====================================================
    // USUARIO NO PERTENECE AL PERSONAL INTERNO
    // =====================================================

    function manejarRolNoAutorizado(
        contenido
    ) {

        mostrarMensaje(
            contenido?.mensaje
            || "Este acceso es exclusivo para personal interno del hospital.",
            "warning"
        );


        campoContrasena.value =
            "";


        campoUsuario.focus();
    }


    // =====================================================
    // LOGIN EXITOSO
    // =====================================================

    function manejarLoginExitoso(
        contenido
    ) {

        mostrarMensaje(
            contenido?.mensaje
            || "Inicio de sesión exitoso.",
            "success"
        );


        solicitudEnProceso =
            true;


        campoUsuario.disabled =
            true;

        campoContrasena.disabled =
            true;

        btnMostrarContrasena.disabled =
            true;

        btnLogin.disabled =
            true;


        const redireccion =
            contenido?.redireccion
            || "/interno/dashboard";


        setTimeout(
            function () {

                window.location.href =
                    redireccion;

            },
            700
        );
    }


    // =====================================================
    // BLOQUEO TEMPORAL
    // =====================================================

    function iniciarBloqueo(
        contenido
    ) {

        const mensaje =
            contenido?.mensaje
            || "Su cuenta ha sido bloqueada temporalmente "
            + "por múltiples intentos fallidos. "
            + "Contacte al administrador del sistema.";


        mostrarMensaje(
            mensaje,
            "warning"
        );


        mensajeBloqueo.textContent =
            mensaje;


        contenedorBloqueo.hidden =
            false;


        campoContrasena.value =
            "";


        mantenerFormularioBloqueado();


        if (intervaloBloqueo !== null) {

            clearInterval(
                intervaloBloqueo
            );
        }


        const bloqueadoHasta =
            contenido?.bloqueadoHasta;


        if (!bloqueadoHasta) {

            contadorBloqueo.textContent =
                "--:--";

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
    // ACTUALIZAR CONTADOR
    // =====================================================

    function actualizarContadorBloqueo(
        bloqueadoHasta
    ) {

        const fechaFinal =
            new Date(
                bloqueadoHasta
            );


        if (
            Number.isNaN(
                fechaFinal.getTime()
            )
        ) {

            contadorBloqueo.textContent =
                "--:--";

            return;
        }


        const ahora =
            new Date();


        const diferencia =
            fechaFinal.getTime()
            - ahora.getTime();


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
    // BLOQUEAR FORMULARIO
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
    // CARGANDO
    // =====================================================

    function iniciarCarga() {

        solicitudEnProceso =
            true;


        campoUsuario.disabled =
            true;

        campoContrasena.disabled =
            true;

        btnMostrarContrasena.disabled =
            true;

        btnLogin.disabled =
            true;


        textoBtnLogin.hidden =
            true;

        cargandoLogin.hidden =
            false;
    }


    function finalizarCarga() {

        solicitudEnProceso =
            false;


        campoUsuario.disabled =
            false;

        campoContrasena.disabled =
            false;

        btnMostrarContrasena.disabled =
            false;

        btnLogin.disabled =
            false;


        textoBtnLogin.hidden =
            false;

        cargandoLogin.hidden =
            true;
    }


    // =====================================================
    // MENSAJES
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
    // ERRORES DE CAMPOS
    // =====================================================

    function mostrarErrorCampo(
        campo,
        contenedor,
        mensaje
    ) {

        campo.classList.add(
            "input-error"
        );

        contenedor.textContent =
            mensaje;
    }


    function limpiarErrorCampo(
        campo,
        contenedor
    ) {

        campo.classList.remove(
            "input-error"
        );

        contenedor.textContent =
            "";
    }

});