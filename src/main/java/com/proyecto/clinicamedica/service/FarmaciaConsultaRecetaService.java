
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.entity.Cita;
import com.proyecto.clinicamedica.entity.RecetaMedica;

import com.proyecto.clinicamedica.repository.RecetaMedicaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * SERVICE: CONSULTA DE RECETAS PARA FARMACIA
 * =========================================================
 *
 * CU-11 - Despacho de Medicamentos.
 *
 * Permite buscar recetas:
 *
 * - Por ID de receta.
 * - Por ID de consulta medica.
 *
 * SEGURIDAD:
 *
 * - Usuario Farmaceutico autenticado.
 * - Usuario, rol y sucursal activos.
 * - Solo recetas de la sucursal autorizada.
 * - Solo recetas activas.
 *
 * VIGENCIA:
 *
 * - Las recetas vencidas pueden mostrarse.
 * - No se permite despacharlas.
 * - La validacion final se hara en el Service
 *   transaccional de Despacho.
 *
 * =========================================================
 */
@Service
@Transactional(readOnly = true)
public class FarmaciaConsultaRecetaService {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final RecetaMedicaRepository recetaRepository;

    private final FarmaciaSeguridadService
            farmaciaSeguridadService;

    private final FarmaciaRecetaValidacionService
            recetaValidacionService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FarmaciaConsultaRecetaService(
            RecetaMedicaRepository recetaRepository,
            FarmaciaSeguridadService farmaciaSeguridadService,
            FarmaciaRecetaValidacionService recetaValidacionService
    ) {

        this.recetaRepository = recetaRepository;

        this.farmaciaSeguridadService =
                farmaciaSeguridadService;

        this.recetaValidacionService =
                recetaValidacionService;
    }


    // =====================================================
    // DTO: RESUMEN DE RECETA
    // =====================================================
    //
    // Transporta solo la informacion que necesita
    // la pantalla de busqueda.
    //
    // =====================================================

    public record ResumenReceta(

            Integer idReceta,

            Integer idConsulta,

            OffsetDateTime fechaEmision,

            boolean vigente,

            long diasTranscurridos,

            String notas

    ) {
    }


    // =====================================================
    // BUSCAR POR ID DE RECETA
    // =====================================================

    public List<ResumenReceta> buscarPorIdReceta(
            Integer idReceta
    ) {

        // -------------------------------------------------
        // VALIDAR CRITERIO
        // -------------------------------------------------

        validarIdentificador(idReceta);


        // -------------------------------------------------
        // OBTENER SUCURSAL AUTORIZADA
        // -------------------------------------------------

        Integer idSucursal =
                farmaciaSeguridadService
                        .obtenerIdSucursalAutorizada();


        // -------------------------------------------------
        // CONSULTAR RECETA ACTIVA
        // -------------------------------------------------

        return recetaRepository
                .findByIdAndActivoTrue(idReceta)
                .filter(
                        receta -> perteneceASucursal(
                                receta,
                                idSucursal
                        )
                )
                .map(this::crearResumen)
                .map(List::of)
                .orElseGet(List::of);
    }


    // =====================================================
    // BUSCAR POR ID DE CONSULTA
    // =====================================================

    public List<ResumenReceta> buscarPorIdConsulta(
            Integer idConsulta
    ) {

        // -------------------------------------------------
        // VALIDAR CRITERIO
        // -------------------------------------------------

        validarIdentificador(idConsulta);


        // -------------------------------------------------
        // OBTENER SUCURSAL AUTORIZADA
        // -------------------------------------------------

        Integer idSucursal =
                farmaciaSeguridadService
                        .obtenerIdSucursalAutorizada();


        // -------------------------------------------------
        // CONSULTAR RECETAS ACTIVAS
        // -------------------------------------------------

        return recetaRepository
                .findByConsultaMedica_IdAndActivoTrueOrderByFechaEmisionDesc(
                        idConsulta
                )
                .stream()
                .filter(
                        receta -> perteneceASucursal(
                                receta,
                                idSucursal
                        )
                )
                .map(this::crearResumen)
                .toList();
    }


    // =====================================================
    // VALIDAR IDENTIFICADOR EN BACKEND
    // =====================================================

    private void validarIdentificador(
            Integer identificador
    ) {

        if (identificador == null
                || identificador <= 0) {

            throw new IllegalArgumentException(
                    "Debe ingresar un identificador válido mayor a cero."
            );
        }
    }


    // =====================================================
    // VALIDAR SUCURSAL DE LA RECETA
    // =====================================================
    //
    // La sucursal autorizada se obtiene del usuario
    // autenticado, no del formulario HTML.
    //
    // No se muestran recetas de otras sucursales.
    //
    // =====================================================

    private boolean perteneceASucursal(
            RecetaMedica receta,
            Integer idSucursalAutorizada
    ) {

        if (receta == null
                || receta.getCita() == null) {

            return false;
        }

        Cita cita = receta.getCita();

        if (cita.getSucursal() == null
                || cita.getSucursal().getId() == null) {

            return false;
        }

        return Objects.equals(
                idSucursalAutorizada,
                cita.getSucursal().getId()
        );
    }


    // =====================================================
    // CONSTRUIR RESUMEN
    // =====================================================

    private ResumenReceta crearResumen(
            RecetaMedica receta
    ) {

        // -------------------------------------------------
        // REVALIDAR SEGURIDAD
        // -------------------------------------------------

        farmaciaSeguridadService
                .validarAccesoReceta(receta);


        // -------------------------------------------------
        // CONSULTAR VIGENCIA DESDE BACKEND
        // -------------------------------------------------

        FarmaciaRecetaValidacionService.VigenciaReceta
                vigencia =
                recetaValidacionService
                        .consultarVigencia(receta);


        // -------------------------------------------------
        // VALIDAR CONSULTA MEDICA
        // -------------------------------------------------

        if (receta.getConsultaMedica() == null
                || receta.getConsultaMedica().getId() == null) {

            throw new IllegalStateException(
                    "La receta no tiene una consulta médica válida."
            );
        }


        // -------------------------------------------------
        // RETORNAR DATOS
        // -------------------------------------------------

        return new ResumenReceta(

                receta.getId(),

                receta.getConsultaMedica().getId(),

                receta.getFechaEmision(),

                vigencia.vigente(),

                vigencia.diasTranscurridos(),

                receta.getNotas()
        );
    }
}
