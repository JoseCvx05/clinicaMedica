/**
 * =========================================================
 * JAVASCRIPT - CU-03 AGENDAR CITAS
 * =========================================================
 *
 * Funciones actuales:
 *
 * - Seleccionar fecha.
 * - Consultar disponibilidad en tiempo real.
 * - Mostrar horarios disponibles.
 * - Seleccionar horario.
 * - Enviar fecha/hora al backend.
 * - Mostrar contador de reserva temporal.
 *
 * =========================================================
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        // =================================================
        // PASO 4 - FECHA Y HORA
        // =================================================

        inicializarCalendario();


        // =================================================
        // PASO 5 - CONTADOR DE RESERVA
        // =================================================

        inicializarContadorReserva();
    }
);


// =========================================================
// PASO 4 - CALENDARIO Y HORARIOS
// =========================================================

function inicializarCalendario() {

    const fechaInput =
        document.getElementById(
            "fechaCita"
        );


    /*
     * Si no estamos en el Paso 4,
     * este elemento no existe.
     */
    if (!fechaInput) {

        return;
    }


    const contenedor =
        document.getElementById(
            "contenedorHorarios"
        );


    const mensaje =
        document.getElementById(
            "mensajeHorarios"
        );


    const error =
        document.getElementById(
            "errorDisponibilidad"
        );


    const campoInicio =
        document.getElementById(
            "fechaHoraInicio"
        );


    const campoFin =
        document.getElementById(
            "fechaHoraFin"
        );


    const botonContinuar =
        document.getElementById(
            "btnContinuarHorario"
        );


    // =====================================================
    // FECHA MÍNIMA = HOY
    // =====================================================

    establecerFechaMinima(
        fechaInput
    );


    // =====================================================
    // CAMBIO DE FECHA
    // =====================================================

    fechaInput.addEventListener(
        "change",
        async function () {

            limpiarSeleccionHorario(
                campoInicio,
                campoFin,
                botonContinuar
            );


            contenedor.innerHTML =
                "";


            error.classList.add(
                "d-none"
            );


            error.textContent =
                "";


            if (!fechaInput.value) {

                mensaje.textContent =
                    "Seleccione una fecha.";

                return;
            }


            // =============================================
            // MOSTRAR CARGANDO
            // =============================================

            mensaje.innerHTML =
                `
                <div class="d-flex align-items-center">
                    <div
                        class="spinner-border spinner-border-sm me-2"
                        role="status">
                    </div>

                    <span>
                        Consultando disponibilidad...
                    </span>
                </div>
                `;


            try {

                const horarios =
                    await consultarDisponibilidad(
                        fechaInput.value
                    );


                mostrarHorarios(
                    horarios,
                    contenedor,
                    mensaje,
                    campoInicio,
                    campoFin,
                    botonContinuar
                );


            } catch (e) {

                console.error(
                    "Error al consultar disponibilidad:",
                    e
                );


                mensaje.textContent =
                    "";


                error.textContent =
                    "No fue posible consultar los horarios disponibles.";


                error.classList.remove(
                    "d-none"
                );
            }
        }
    );
}


// =========================================================
// ESTABLECER FECHA MÍNIMA
// =========================================================

function establecerFechaMinima(
    fechaInput
) {

    const hoy =
        new Date();


    const anio =
        hoy.getFullYear();


    const mes =
        String(
            hoy.getMonth() + 1
        )
            .padStart(
                2,
                "0"
            );


    const dia =
        String(
            hoy.getDate()
        )
            .padStart(
                2,
                "0"
            );


    fechaInput.min =
        `${anio}-${mes}-${dia}`;
}


// =========================================================
// OBTENER RUTA BASE SEGÚN EL TIPO DE AGENDAMIENTO
// =========================================================

function obtenerRutaBaseCitas() {

    const rutaActual =
        window.location.pathname;


    console.log(
        "Ruta actual:",
        rutaActual
    );


    if (rutaActual.startsWith(
        "/interno/recepcion/"
    )) {

        return "/interno/recepcion/citas";
    }


    return "/paciente/citas";
}


// =========================================================
// CONSULTAR DISPONIBILIDAD
// =========================================================

async function consultarDisponibilidad(
    fecha
) {

    const rutaBase =
        obtenerRutaBaseCitas();


    const url =
        `${rutaBase}/disponibilidad?fecha=${
            encodeURIComponent(
                fecha
            )
        }`;


    console.log(
        "Consultando disponibilidad en:",
        url
    );


    const respuesta =
        await fetch(
            url,
            {
                method: "GET",

                headers: {
                    "Accept":
                        "application/json"
                }
            }
        );


    console.log(
        "Estado HTTP:",
        respuesta.status
    );


    if (!respuesta.ok) {

        const cuerpoError =
            await respuesta.text();


        console.error(
            "Respuesta del servidor:",
            cuerpoError
        );


        throw new Error(
            "El servidor respondió HTTP "
            + respuesta.status
        );
    }


    const horarios =
        await respuesta.json();


    console.log(
        "Horarios recibidos:",
        horarios
    );


    return horarios;
}


// =========================================================
// MOSTRAR HORARIOS
// =========================================================

function mostrarHorarios(
    horarios,
    contenedor,
    mensaje,
    campoInicio,
    campoFin,
    botonContinuar
) {

    contenedor.innerHTML =
        "";


    // =====================================================
    // SIN HORARIOS
    // =====================================================

    if (!horarios
        || horarios.length === 0) {

        mensaje.textContent =
            "No hay horarios disponibles para la fecha seleccionada.";

        return;
    }


    mensaje.textContent =
        "Seleccione un horario:";


    // =====================================================
    // CREAR BOTONES
    // =====================================================

    horarios.forEach(
        function (horario) {

            const boton =
                document.createElement(
                    "button"
                );


            boton.type =
                "button";


            boton.className =
                "btn btn-outline-primary horario-btn";


            boton.textContent =
                formatearHora(
                    horario.inicio
                );


            boton.dataset.inicio =
                horario.inicio;


            boton.dataset.fin =
                horario.fin;


            // =============================================
            // SELECCIONAR HORARIO
            // =============================================

            boton.addEventListener(
                "click",
                function () {

                    document
                        .querySelectorAll(
                            ".horario-btn"
                        )
                        .forEach(
                            function (item) {

                                item.classList.remove(
                                    "seleccionado"
                                );
                            }
                        );


                    boton.classList.add(
                        "seleccionado"
                    );


                    campoInicio.value =
                        boton.dataset.inicio;


                    campoFin.value =
                        boton.dataset.fin;


                    botonContinuar.disabled =
                        false;
                }
            );


            contenedor.appendChild(
                boton
            );
        }
    );
}


// =========================================================
// FORMATEAR HORA
// =========================================================

function formatearHora(
    valor
) {

    const fecha =
        new Date(
            valor
        );


    return fecha.toLocaleTimeString(
        "es-GT",
        {
            hour:
                "2-digit",

            minute:
                "2-digit",

            hour12:
                true
        }
    );
}


// =========================================================
// LIMPIAR HORARIO SELECCIONADO
// =========================================================

function limpiarSeleccionHorario(
    campoInicio,
    campoFin,
    botonContinuar
) {

    campoInicio.value =
        "";


    campoFin.value =
        "";


    botonContinuar.disabled =
        true;
}


// =========================================================
// PASO 5 - CONTADOR DE RESERVA TEMPORAL
// =========================================================

function inicializarContadorReserva() {

    const expiracionInput =
        document.getElementById(
            "fechaExpiracionReserva"
        );


    const contadorReserva =
        document.getElementById(
            "contadorReserva"
        );


    /*
     * Si no estamos en Paso 5,
     * estos elementos no existen.
     */
    if (!expiracionInput
        || !contadorReserva
        || !expiracionInput.value) {

        return;
    }


    const expiracion =
        new Date(
            expiracionInput.value
        );


    // =====================================================
    // ACTUALIZAR CONTADOR
    // =====================================================

    const actualizarContador =
        function () {

            const ahora =
                new Date();


            const diferencia =
                expiracion.getTime()
                - ahora.getTime();


            // =============================================
            // EXPIRÓ
            // =============================================

            if (diferencia <= 0) {

                contadorReserva.textContent =
                    "00:00";


                contadorReserva.classList.add(
                    "text-danger"
                );


                // =============================================
                // FA03
                // =============================================
                //
                // No liberamos únicamente desde JavaScript.
                // El backend comprobará si la reserva
                // realmente expiró.
                // =============================================

                const rutaBase =
                    obtenerRutaBaseCitas();


                window.location.href =
                    `${rutaBase}/agendar/reserva-expirada`;


                return false;
            }


            // =============================================
            // CALCULAR TIEMPO
            // =============================================

            const segundosTotales =
                Math.floor(
                    diferencia / 1000
                );


            const minutos =
                Math.floor(
                    segundosTotales / 60
                );


            const segundos =
                segundosTotales % 60;


            contadorReserva.textContent =
                String(
                    minutos
                )
                    .padStart(
                        2,
                        "0"
                    )

                + ":"

                + String(
                    segundos
                )
                    .padStart(
                        2,
                        "0"
                    );


            return true;
        };


    // =====================================================
    // PRIMERA ACTUALIZACIÓN
    // =====================================================

    const vigente =
        actualizarContador();


    if (!vigente) {

        return;
    }


    // =====================================================
    // ACTUALIZAR CADA SEGUNDO
    // =====================================================

    const intervalo =
        setInterval(
            function () {

                const continua =
                    actualizarContador();


                if (!continua) {

                    clearInterval(
                        intervalo
                    );
                }

            },
            1000
        );
}