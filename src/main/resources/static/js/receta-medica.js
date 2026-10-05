document.addEventListener("DOMContentLoaded", function () {

    const lista =
        document.getElementById("listaMedicamentos");

    const plantilla =
        document.getElementById("plantillaMedicamento");

    const botonAgregar =
        document.getElementById("botonAgregar");

    const contador =
        document.getElementById("contadorMedicamentos");

    const formulario =
        document.getElementById("formReceta");


    // =====================================================
    // VALIDACIÓN DE ELEMENTOS
    // =====================================================

    if (!lista) {
        console.error("No se encontró listaMedicamentos");
        return;
    }

    if (!plantilla) {
        console.error("No se encontró plantillaMedicamento");
        return;
    }

    if (!botonAgregar) {
        console.error("No se encontró botonAgregar");
        return;
    }

    if (!formulario) {
        console.error("No se encontró formReceta");
        return;
    }


    // =====================================================
    // ACTUALIZAR NUMERACIÓN
    // =====================================================

    function actualizarNumeracion() {

        const items =
            lista.querySelectorAll(
                ".medicamento-item"
            );


        items.forEach(function (item, indice) {

            const titulo =
                item.querySelector(
                    ".tituloMedicamento"
                );

            const botonEliminar =
                item.querySelector(
                    ".botonEliminar"
                );


            if (titulo) {

                titulo.textContent =
                    "Medicamento "
                    + (indice + 1);
            }


            if (botonEliminar) {

                botonEliminar.style.display =
                    items.length > 1
                        ? "inline-flex"
                        : "none";
            }
        });


        if (contador) {

            contador.textContent =
                items.length
                + (
                    items.length === 1
                        ? " medicamento"
                        : " medicamentos"
                );
        }
    }


    // =====================================================
    // INFORMACIÓN DEL MEDICAMENTO
    // =====================================================

    function actualizarDetalleCatalogo(item) {

        const select =
            item.querySelector(
                ".selectMedicamento"
            );

        const detalle =
            item.querySelector(
                ".detalle-catalogo"
            );

        const controlado =
            item.querySelector(
                ".controlado"
            );


        if (!select || !detalle || !controlado) {
            return;
        }


        const opcion =
            select.options[
                select.selectedIndex
                ];


        if (!opcion || !opcion.value) {

            detalle.textContent = "";

            detalle.classList.remove(
                "visible"
            );

            controlado.classList.remove(
                "visible"
            );

            return;
        }


        const descripcion =
            opcion.dataset.descripcion
            || "Sin descripción";


        const precio =
            opcion.dataset.precio
            || "0.00";


        const unidad =
            opcion.dataset.unidad
            || "";


        detalle.textContent =
            descripcion
            + " | Precio de referencia: Q"
            + precio
            + " por "
            + unidad;


        detalle.classList.add(
            "visible"
        );


        if (
            opcion.dataset.controlado === "true"
        ) {

            controlado.classList.add(
                "visible"
            );

        } else {

            controlado.classList.remove(
                "visible"
            );
        }
    }


    // =====================================================
    // CREAR BLOQUE DE MEDICAMENTO
    // =====================================================

    function agregarMedicamento() {

        const item =
            plantilla
                .content
                .firstElementChild
                .cloneNode(true);


        if (!item) {

            console.error(
                "No fue posible clonar la plantilla."
            );

            return;
        }


        const select =
            item.querySelector(
                ".selectMedicamento"
            );


        const botonEliminar =
            item.querySelector(
                ".botonEliminar"
            );


        // -------------------------------------------------
        // CAMBIO DE MEDICAMENTO
        // -------------------------------------------------

        if (select) {

            select.addEventListener(
                "change",
                function () {

                    actualizarDetalleCatalogo(
                        item
                    );
                }
            );
        }


        // -------------------------------------------------
        // ELIMINAR MEDICAMENTO
        // -------------------------------------------------

        if (botonEliminar) {

            botonEliminar.addEventListener(
                "click",
                function () {

                    const items =
                        lista.querySelectorAll(
                            ".medicamento-item"
                        );


                    if (items.length <= 1) {
                        return;
                    }


                    item.remove();

                    actualizarNumeracion();
                }
            );
        }


        // -------------------------------------------------
        // INSERTAR
        // -------------------------------------------------

        lista.appendChild(
            item
        );


        actualizarNumeracion();
    }


    // =====================================================
    // BOTÓN AGREGAR
    // =====================================================

    botonAgregar.addEventListener(
        "click",
        function () {

            agregarMedicamento();
        }
    );


    // =====================================================
    // PRIMER MEDICAMENTO
    // =====================================================

    agregarMedicamento();


    // =====================================================
    // VALIDAR FORMULARIO
    // =====================================================

    formulario.addEventListener(
        "submit",
        function (evento) {

            const items =
                lista.querySelectorAll(
                    ".medicamento-item"
                );


            if (items.length === 0) {

                evento.preventDefault();

                alert(
                    "Debe registrar al menos un medicamento."
                );

                return;
            }


            let valido = true;


            items.forEach(function (item) {

                const medicamento =
                    item.querySelector(
                        ".selectMedicamento"
                    );

                const cantidad =
                    item.querySelector(
                        ".cantidad"
                    );

                const dosis =
                    item.querySelector(
                        ".dosis"
                    );

                const frecuencia =
                    item.querySelector(
                        ".frecuencia"
                    );

                const duracion =
                    item.querySelector(
                        ".duracion"
                    );


                if (
                    !medicamento
                    ||
                    !medicamento.value
                    ||
                    !cantidad
                    ||
                    Number(cantidad.value) <= 0
                    ||
                    !dosis
                    ||
                    !dosis.value.trim()
                    ||
                    !frecuencia
                    ||
                    !frecuencia.value.trim()
                    ||
                    !duracion
                    ||
                    !duracion.value.trim()
                ) {

                    valido = false;
                }
            });


            if (!valido) {

                evento.preventDefault();

                alert(
                    "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
                );

                return;
            }


            const confirmar =
                window.confirm(
                    "¿Desea guardar esta receta médica?"
                );


            if (!confirmar) {

                evento.preventDefault();
            }
        }
    );


    console.log(
        "Receta médica JavaScript cargado correctamente."
    );
});