
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.SucursalEspecialidad;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * =========================================================
 * SERVICIO: CONFIGURACIÓN DE SEDES Y ESPECIALIDADES
 * =========================================================
 *
 * CU-13 - Configuración de Sedes y Especialidades.
 *
 * Responsabilidades:
 *
 * - Consultar asignaciones registradas.
 * - Permitir búsqueda por ID.
 * - Validar sucursales activas.
 * - Validar especialidades activas.
 * - Evitar combinaciones duplicadas.
 * - Registrar nuevas asignaciones.
 * - Mantener el precio de consulta.
 * - Registrar auditoría de creación.
 *
 * IMPORTANTE:
 *
 * Las operaciones de desactivación y reactivación
 * se incorporarán después de revisar cómo afectan
 * a las citas existentes y los pagos pendientes.
 *
 * =========================================================
 */
@Service
public class SucursalEspecialidadService {


    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final SucursalService sucursalService;

    private final EspecialidadService especialidadService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    // =====================================================
// CU-13 - PROTECCIÓN DE CITAS EXISTENTES
// =====================================================

    private final CitaRepository citaRepository;
    // =====================================================
    // CONSTRUCTOR
    // =====================================================


    public SucursalEspecialidadService(
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            SucursalService sucursalService,
            EspecialidadService especialidadService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService,
            CitaRepository citaRepository
    ) {

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.sucursalService =
                sucursalService;

        this.especialidadService =
                especialidadService;

        this.usuarioActualService =
                usuarioActualService;

        this.auditoriaService =
                auditoriaService;

        this.citaRepository =
                citaRepository;
    }


    // =====================================================
    // LISTAR ASIGNACIONES
    // =====================================================
    //
    // Incluye activas e inactivas para que el
    // administrador pueda consultar su estado.
    //
    // =====================================================

    @Transactional(readOnly = true)
    public Page<ResumenAsignacion> listar(
            Pageable pageable
    ) {

        if (pageable == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la paginación."
            );
        }

        return sucursalEspecialidadRepository
                .findAll(pageable)
                .map(this::convertirResumen);
    }


    // =====================================================
    // BUSCAR ASIGNACIÓN POR ID
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<ResumenAsignacion> buscarPorId(
            Integer id
    ) {

        if (id == null || id <= 0) {

            return Optional.empty();
        }

        return sucursalEspecialidadRepository
                .findById(id)
                .map(this::convertirResumen);
    }


    // =====================================================
    // CREAR ASIGNACIÓN
    // =====================================================

    @Transactional
    public SucursalEspecialidad asignar(
            Integer idSucursal,
            Integer idEspecialidad,
            BigDecimal precioConsulta
    ) {

        // ================================================
        // SEGURIDAD - ADMINISTRADOR AUTENTICADO
        // ================================================

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (administrador == null
                || !Boolean.TRUE.equals(
                administrador.getActivo()
        )
                || administrador.getRol() == null
                || !"Administrador".equalsIgnoreCase(
                administrador.getRol().getNombre()
        )) {

            throw new IllegalStateException(
                    "No tiene permisos para realizar esta operación."
            );
        }


        // ================================================
        // VALIDAR SEDE OBLIGATORIA Y ACTIVA
        // ================================================

        if (idSucursal == null || idSucursal <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sede."
            );
        }

        Sucursal sucursal =
                sucursalService
                        .buscarActivaPorId(idSucursal)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La sede seleccionada no existe o está inactiva."
                                )
                        );


        // ================================================
        // VALIDAR ESPECIALIDAD OBLIGATORIA Y ACTIVA
        // ================================================

        if (idEspecialidad == null || idEspecialidad <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad."
            );
        }

        Especialidad especialidad =
                especialidadService
                        .buscarActivaPorId(idEspecialidad)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La especialidad seleccionada no existe o está inactiva."
                                )
                        );


        // ================================================
        // VALIDAR PRECIO DE CONSULTA
        // ================================================
        //
        // La entidad exige precio_consulta NOT NULL.
        // No asignamos precios inventados ni modificamos
        // los precios de relaciones existentes.
        //
        // ================================================

        if (precioConsulta == null
                || precioConsulta.compareTo(BigDecimal.ZERO) <= 0
                || precioConsulta.scale() > 2
                || precioConsulta.compareTo(
                new BigDecimal("99999999.99")
        ) > 0) {

            throw new IllegalArgumentException(
                    "Debe indicar un precio de consulta válido, "
                            + "mayor que cero y con máximo dos decimales."
            );
        }


        // ================================================
        // EVITAR DUPLICADOS, INCLUSO INACTIVOS
        // ================================================

        boolean existe =
                sucursalEspecialidadRepository
                        .findBySucursal_IdAndEspecialidad_Id(
                                idSucursal,
                                idEspecialidad
                        )
                        .isPresent();

        if (existe) {

            throw new IllegalArgumentException(
                    "Esta combinación de sede y especialidad "
                            + "ya existe en el sistema."
            );
        }


        // ================================================
        // CREAR RELACIÓN
        // ================================================

        SucursalEspecialidad asignacion =
                new SucursalEspecialidad();

        asignacion.setSucursal(sucursal);

        asignacion.setEspecialidad(especialidad);

        asignacion.setPrecioConsulta(precioConsulta);

        asignacion.setActivo(true);


        // ================================================
        // GUARDAR EN POSTGRESQL
        // ================================================
        //
        // La restricción UNIQUE de la base de datos
        // también debe proteger contra dos solicitudes
        // simultáneas con la misma combinación.
        //
        // ================================================

        SucursalEspecialidad guardada;

        try {

            guardada = sucursalEspecialidadRepository
                    .saveAndFlush(asignacion);

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalStateException(
                    "No fue posible registrar la asignación. "
                            + "Verifique si la combinación ya existe.",
                    ex
            );
        }


        // ================================================
        // AUDITORÍA
        // ================================================

        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "idSucursal",
                idSucursal
        );

        valoresNuevos.put(
                "idEspecialidad",
                idEspecialidad
        );

        valoresNuevos.put(
                "precioConsulta",
                precioConsulta
        );

        valoresNuevos.put(
                "activo",
                true
        );

        auditoriaService.registrar(
                new RegistroAuditoria(

                        "sucursal_especialidad",

                        String.valueOf(
                                guardada.getId()
                        ),

                        AccionAuditoria.CREAR,

                        administrador.getId(),

                        administrador.getNombreUsuario(),

                        null,

                        valoresNuevos,

                        null
                )
        );


        return guardada;
    }



    // =====================================================
    // CU-13 - FA02: ELIMINAR ASIGNACIÓN
    // =====================================================
    //
    // Se realiza una desactivación lógica.
    //
    // No se elimina físicamente la asignación,
    // para conservar el historial del sistema.
    //
    // Antes de desactivar se verifica que no
    // existan citas pendientes o en atención.
    // =====================================================

    @Transactional
    public void deleteBranchSpecialty(Integer id) {

        // =================================================
        // 1. VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una asignación válida."
            );
        }

        // =================================================
        // 2. VALIDAR ADMINISTRADOR
        // =================================================

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (administrador == null
                || !Boolean.TRUE.equals(administrador.getActivo())
                || administrador.getRol() == null
                || administrador.getRol().getNombre() == null
                || !"Administrador".equalsIgnoreCase(
                administrador.getRol().getNombre().trim()
        )) {

            throw new IllegalStateException(
                    "No tiene permisos para eliminar asignaciones."
            );
        }

        // =================================================
        // 3. OBTENER ASIGNACIÓN CON BLOQUEO
        // =================================================

        SucursalEspecialidad asignacion =
                sucursalEspecialidadRepository
                        .buscarParaDesactivarConBloqueo(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La asignación seleccionada no existe."
                                )
                        );

        // =================================================
        // 4. VERIFICAR ESTADO
        // =================================================

        if (!Boolean.TRUE.equals(asignacion.getActivo())) {

            throw new IllegalStateException(
                    "La asignación ya se encuentra inactiva."
            );
        }

        Integer idSucursal =
                asignacion.getSucursal().getId();

        Integer idEspecialidad =
                asignacion.getEspecialidad().getId();

        // =================================================
        // 5. PROTEGER CITAS EXISTENTES
        // =================================================

        boolean existenCitas =
                citaRepository
                        .existeCitaNoCerradaEnSucursalEspecialidad(
                                idSucursal,
                                idEspecialidad
                        );

        if (existenCitas) {

            throw new IllegalStateException(
                    "No es posible eliminar esta asignación "
                            + "porque existen citas pendientes, "
                            + "pagadas o en proceso de atención. "
                            + "Debe conservarse activa para "
                            + "no afectar las citas y sus pagos."
            );
        }

        // =================================================
        // 6. PREPARAR AUDITORÍA
        // =================================================

        Map<String, Object> valoresAnteriores =
                new LinkedHashMap<>();

        valoresAnteriores.put("idSucursal", idSucursal);
        valoresAnteriores.put("idEspecialidad", idEspecialidad);
        valoresAnteriores.put("activo", true);

        // =================================================
        // 7. DESACTIVAR SIN BORRAR
        // =================================================

        asignacion.setActivo(false);

        sucursalEspecialidadRepository
                .saveAndFlush(asignacion);

        // =================================================
        // 8. AUDITORÍA
        // =================================================

        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put("idSucursal", idSucursal);
        valoresNuevos.put("idEspecialidad", idEspecialidad);
        valoresNuevos.put("activo", false);

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal_especialidad",
                        String.valueOf(asignacion.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                )
        );
    }



    // =====================================================
    // CU-13 - REACTIVAR ASIGNACIÓN
    // =====================================================
    //
    // Permite volver a habilitar una combinación de
    // sede y especialidad previamente desactivada.
    //
    // SEGURIDAD:
    //
    // - Solo un Administrador activo puede reactivarla.
    // - La sede debe continuar activa.
    // - La especialidad debe continuar activa.
    // - No se crea una asignación duplicada.
    // - Se conserva el precio registrado.
    // - Se utiliza bloqueo de PostgreSQL.
    // - Se registra la operación en auditoría.
    //
    // No modifica citas ni pagos existentes.
    // =====================================================

    @Transactional
    public void reactivarAsignacion(Integer id) {

        // =================================================
        // 1. VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una asignación válida."
            );
        }

        // =================================================
        // 2. VALIDAR ADMINISTRADOR
        // =================================================

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (administrador == null
                || !Boolean.TRUE.equals(
                administrador.getActivo()
        )
                || administrador.getRol() == null
                || administrador.getRol().getNombre() == null
                || !"Administrador".equalsIgnoreCase(
                administrador.getRol()
                        .getNombre()
                        .trim()
        )) {

            throw new IllegalStateException(
                    "No tiene permisos para reactivar asignaciones."
            );
        }

        // =================================================
        // 3. OBTENER ASIGNACIÓN CON BLOQUEO
        // =================================================

        SucursalEspecialidad asignacion =
                sucursalEspecialidadRepository
                        .buscarParaDesactivarConBloqueo(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La asignación seleccionada no existe."
                                )
                        );

        // =================================================
        // 4. VALIDAR QUE ESTÉ INACTIVA
        // =================================================

        if (Boolean.TRUE.equals(asignacion.getActivo())) {

            throw new IllegalStateException(
                    "La asignación ya se encuentra activa."
            );
        }

        // =================================================
        // 5. OBTENER SEDE Y ESPECIALIDAD
        // =================================================

        Integer idSucursal =
                asignacion.getSucursal().getId();

        Integer idEspecialidad =
                asignacion.getEspecialidad().getId();

        // =================================================
        // 6. VALIDAR SEDE ACTIVA
        // =================================================

        if (sucursalService
                .buscarActivaPorId(idSucursal)
                .isEmpty()) {

            throw new IllegalStateException(
                    "No es posible reactivar la asignación "
                            + "porque la sede está inactiva "
                            + "o ya no existe."
            );
        }

        // =================================================
        // 7. VALIDAR ESPECIALIDAD ACTIVA
        // =================================================

        if (especialidadService
                .buscarActivaPorId(idEspecialidad)
                .isEmpty()) {

            throw new IllegalStateException(
                    "No es posible reactivar la asignación "
                            + "porque la especialidad está inactiva "
                            + "o ya no existe."
            );
        }

        // =================================================
        // 8. PREPARAR AUDITORÍA ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                new LinkedHashMap<>();

        valoresAnteriores.put(
                "idSucursal",
                idSucursal
        );

        valoresAnteriores.put(
                "idEspecialidad",
                idEspecialidad
        );

        valoresAnteriores.put(
                "precioConsulta",
                asignacion.getPrecioConsulta()
        );

        valoresAnteriores.put(
                "activo",
                false
        );

        // =================================================
        // 9. REACTIVAR ASIGNACIÓN
        // =================================================
        //
        // Conservamos el ID y el precio original.
        // No insertamos un registro nuevo.
        // =================================================

        asignacion.setActivo(true);

        sucursalEspecialidadRepository
                .saveAndFlush(asignacion);

        // =================================================
        // 10. PREPARAR AUDITORÍA NUEVA
        // =================================================

        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "idSucursal",
                idSucursal
        );

        valoresNuevos.put(
                "idEspecialidad",
                idEspecialidad
        );

        valoresNuevos.put(
                "precioConsulta",
                asignacion.getPrecioConsulta()
        );

        valoresNuevos.put(
                "activo",
                true
        );

        // =================================================
        // 11. REGISTRAR AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal_especialidad",
                        String.valueOf(asignacion.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                )
        );
    }


    // =====================================================
    // CONVERTIR ENTIDAD A RESUMEN
    // =====================================================
    //
    // Evita enviar relaciones JPA LAZY
    // directamente a las vistas Thymeleaf.
    //
    // =====================================================

    private ResumenAsignacion convertirResumen(
            SucursalEspecialidad asignacion
    ) {

        return new ResumenAsignacion(

                asignacion.getId(),

                asignacion.getSucursal().getNombre(),

                asignacion.getEspecialidad().getNombre(),

                asignacion.getActivo(),

                asignacion.getPrecioConsulta()
        );
    }


    // =====================================================
    // DTO PARA EL LISTADO
    // =====================================================

    public record ResumenAsignacion(

            Integer id,

            String sucursal,

            String especialidad,

            Boolean activo,

            BigDecimal precioConsulta

    ) {
    }


    // =====================================================
    // CU-14 - CATÁLOGO 9
    // ACTUALIZAR PRECIO DE CONSULTA
    // =====================================================
    //
    // SEGURIDAD:
    //
    // - Solo Administrador activo.
    // - Precio obligatorio y mayor que cero.
    // - Máximo dos decimales.
    // - Bloqueo pesimista en PostgreSQL.
    // - Protege citas antiguas pendientes de pago.
    // - Conserva los precios acordados de citas nuevas.
    // - Registra auditoría del cambio.
    //
    // No modifica citas ni pagos históricos.
    //
    // =====================================================

    @Transactional
    public void actualizarPrecioConsulta(
            Integer id,
            BigDecimal nuevoPrecio
    ) {

        // =================================================
        // 1. VALIDAR ADMINISTRADOR
        // =================================================

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        if (administrador == null
                || !Boolean.TRUE.equals(administrador.getActivo())
                || administrador.getRol() == null
                || administrador.getRol().getNombre() == null
                || !"Administrador".equalsIgnoreCase(
                administrador.getRol().getNombre().trim()
        )) {

            throw new IllegalStateException(
                    "No tiene permisos para modificar tarifas."
            );
        }

        // =================================================
        // 2. VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una asignación válida."
            );
        }

        // =================================================
        // 3. VALIDAR NUEVO PRECIO
        // =================================================

        if (nuevoPrecio == null
                || nuevoPrecio.compareTo(BigDecimal.ZERO) <= 0
                || nuevoPrecio.scale() > 2
                || nuevoPrecio.compareTo(
                new BigDecimal("99999999.99")
        ) > 0) {

            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero, "
                            + "con máximo dos decimales."
            );
        }

        // =================================================
        // 4. BLOQUEAR ASIGNACIÓN
        // =================================================

        SucursalEspecialidad asignacion =
                sucursalEspecialidadRepository
                        .buscarParaDesactivarConBloqueo(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La asignación no existe."
                                )
                        );

        // =================================================
        // 5. VALIDAR ESTADO
        // =================================================

        if (!Boolean.TRUE.equals(asignacion.getActivo())) {

            throw new IllegalStateException(
                    "No puede modificar el precio "
                            + "de una asignación inactiva."
            );
        }

        Integer idSucursal =
                asignacion.getSucursal().getId();

        Integer idEspecialidad =
                asignacion.getEspecialidad().getId();

        // =================================================
        // 6. VALIDAR SUCURSAL Y ESPECIALIDAD ACTIVAS
        // =================================================

        if (sucursalService.buscarActivaPorId(idSucursal)
                .isEmpty()
                || especialidadService
                .buscarActivaPorId(idEspecialidad)
                .isEmpty()) {

            throw new IllegalStateException(
                    "La sucursal o especialidad "
                            + "ya no se encuentra activa."
            );
        }

        // =================================================
        // 7. VALIDAR SI EL PRECIO CAMBIÓ
        // =================================================

        BigDecimal precioAnterior =
                asignacion.getPrecioConsulta();

        if (precioAnterior != null
                && precioAnterior.compareTo(nuevoPrecio) == 0) {

            throw new IllegalArgumentException(
                    "El nuevo precio es igual al actual."
            );
        }

        // =================================================
        // 8. PROTEGER CITAS ANTIGUAS
        // =================================================
        //
        // Si existe una cita pendiente de pago sin
        // precio acordado, no permitimos cambiar
        // la tarifa de esa sucursal y especialidad.
        //
        // Ejemplo: cita antigua #13.
        //
        // =================================================

        boolean existenCitasSinPrecio =
                citaRepository
                        .existsBySucursal_IdAndEspecialidad_IdAndPrecioConsultaAcordadoIsNullAndEstadoCita_NombreIgnoreCase(
                                idSucursal,
                                idEspecialidad,
                                "Pendiente de pago"
                        );

        if (existenCitasSinPrecio) {

            throw new IllegalStateException(
                    "No es posible modificar esta tarifa "
                            + "porque existen citas antiguas "
                            + "pendientes de pago sin precio "
                            + "acordado registrado."
            );
        }

        // =================================================
        // 9. PREPARAR AUDITORÍA ANTERIOR
        // =================================================

        Map<String, Object> valoresAnteriores =
                new LinkedHashMap<>();

        valoresAnteriores.put(
                "idSucursal",
                idSucursal
        );

        valoresAnteriores.put(
                "idEspecialidad",
                idEspecialidad
        );

        valoresAnteriores.put(
                "precioConsulta",
                precioAnterior
        );

        // =================================================
        // 10. ACTUALIZAR PRECIO
        // =================================================

        asignacion.setPrecioConsulta(nuevoPrecio);

        sucursalEspecialidadRepository
                .saveAndFlush(asignacion);

        // =================================================
        // 11. PREPARAR AUDITORÍA NUEVA
        // =================================================

        Map<String, Object> valoresNuevos =
                new LinkedHashMap<>();

        valoresNuevos.put(
                "idSucursal",
                idSucursal
        );

        valoresNuevos.put(
                "idEspecialidad",
                idEspecialidad
        );

        valoresNuevos.put(
                "precioConsulta",
                nuevoPrecio
        );

        // =================================================
        // 12. REGISTRAR AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal_especialidad",
                        String.valueOf(asignacion.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        valoresNuevos,
                        null
                )
        );
    }


}