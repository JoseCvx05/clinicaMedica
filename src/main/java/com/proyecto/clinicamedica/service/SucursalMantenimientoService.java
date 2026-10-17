
package com.proyecto.clinicamedica.service;

import com.proyecto.clinicamedica.dto.AccionAuditoria;
import com.proyecto.clinicamedica.dto.RegistroAuditoria;

import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;

import com.proyecto.clinicamedica.repository.SucursalRepository;
import com.proyecto.clinicamedica.repository.UsuarioRepository;
import com.proyecto.clinicamedica.repository.CitaRepository;
import com.proyecto.clinicamedica.repository.InventarioFarmaciaRepository;
import com.proyecto.clinicamedica.repository.SucursalEspecialidadRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================
 * CU-14 - MANTENIMIENTO DE SUCURSALES
 * =========================================================
 *
 * Responsabilidades:
 *
 * - Crear sucursales.
 * - Actualizar sucursales existentes.
 * - Desactivar sucursales mediante borrado lógico.
 * - Validar campos obligatorios.
 * - Validar longitudes máximas.
 * - Validar teléfono de 8 dígitos.
 * - Evitar nombres activos duplicados.
 * - Verificar permisos específicos.
 * - Proteger sucursales con usuarios activos.
 * - Proteger citas pendientes o en atención.
 * - Proteger inventarios con existencias.
 * - Proteger asignaciones activas de especialidades.
 * - Registrar auditoría.
 *
 * IMPORTANTE:
 *
 * La eliminación es lógica.
 *
 * NO se eliminan físicamente registros.
 *
 * Se conservan las referencias históricas
 * utilizadas por los diferentes módulos.
 *
 * Este servicio no modifica directamente
 * médicos, citas, inventarios ni asignaciones
 * de especialidades.
 *
 * =========================================================
 */
@Service
public class SucursalMantenimientoService {

    // =====================================================
    // CONSTANTES
    // =====================================================

    private static final int MAX_NOMBRE = 100;

    private static final int MAX_DIRECCION = 500;

    private static final int MAX_DESCRIPCION = 250;

    private static final int LONGITUD_TELEFONO = 8;

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final SucursalRepository sucursalRepository;

    private final PermisoCatalogoService permisoCatalogoService;

    private final UsuarioActualService usuarioActualService;

    private final AuditoriaService auditoriaService;

    // =====================================================
    // CU-14 - DEPENDENCIAS DE PROTECCIÓN
    // =====================================================

    private final UsuarioRepository usuarioRepository;

    private final CitaRepository citaRepository;

    private final InventarioFarmaciaRepository
            inventarioFarmaciaRepository;

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public SucursalMantenimientoService(
            SucursalRepository sucursalRepository,
            PermisoCatalogoService permisoCatalogoService,
            UsuarioActualService usuarioActualService,
            AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            InventarioFarmaciaRepository inventarioFarmaciaRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository
    ) {

        this.sucursalRepository = sucursalRepository;

        this.permisoCatalogoService = permisoCatalogoService;

        this.usuarioActualService = usuarioActualService;

        this.auditoriaService = auditoriaService;

        this.usuarioRepository = usuarioRepository;

        this.citaRepository = citaRepository;

        this.inventarioFarmaciaRepository =
                inventarioFarmaciaRepository;

        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;
    }

    // =====================================================
    // CU-14 - CREAR SUCURSAL
    // =====================================================

    @Transactional
    public Sucursal crear(
            String nombre,
            String telefono,
            String direccion,
            String descripcion
    ) {

        // =================================================
        // VERIFICAR AUTORIZACIÓN
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/create"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR DATOS
        // =================================================

        String nombreNormalizado =
                validarNombre(nombre);

        String telefonoNormalizado =
                validarTelefono(telefono);

        String direccionNormalizada =
                validarTextoOpcional(
                        direccion,
                        MAX_DIRECCION,
                        "La dirección"
                );

        String descripcionNormalizada =
                validarTextoOpcional(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        // =================================================
        // EVITAR DUPLICADOS ACTIVOS
        // =================================================

        if (sucursalRepository
                .existsByNombreIgnoreCaseAndActivoTrue(
                        nombreNormalizado
                )) {

            throw new IllegalArgumentException(
                    "Ya existe una sucursal activa con el nombre "
                            + nombreNormalizado
                            + "."
            );
        }

        // =================================================
        // CONSTRUIR SUCURSAL
        // =================================================

        Sucursal sucursal = new Sucursal();

        sucursal.setNombre(nombreNormalizado);

        sucursal.setTelefono(telefonoNormalizado);

        sucursal.setDireccion(direccionNormalizada);

        sucursal.setDescripcion(descripcionNormalizada);

        sucursal.setActivo(true);

        // =================================================
        // GUARDAR EN POSTGRESQL
        // =================================================

        Sucursal guardada =
                sucursalRepository.saveAndFlush(
                        sucursal
                );

        // =================================================
        // REGISTRAR AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.CREAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        null,
                        crearSnapshot(guardada),
                        null
                )
        );

        return guardada;
    }

    // =====================================================
    // CU-14 - ACTUALIZAR SUCURSAL
    // =====================================================

    @Transactional
    public Sucursal actualizar(
            Integer id,
            String nombre,
            String telefono,
            String direccion,
            String descripcion
    ) {

        // =================================================
        // VERIFICAR AUTORIZACIÓN
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/update"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal válida."
            );
        }

        // =================================================
        // BUSCAR SUCURSAL EXISTENTE
        // =================================================

        Sucursal sucursal =
                sucursalRepository.findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La sucursal seleccionada no existe."
                                )
                        );

        // =================================================
        // VALIDAR NUEVOS DATOS
        // =================================================

        String nombreNormalizado =
                validarNombre(nombre);

        String telefonoNormalizado =
                validarTelefono(telefono);

        String direccionNormalizada =
                validarTextoOpcional(
                        direccion,
                        MAX_DIRECCION,
                        "La dirección"
                );

        String descripcionNormalizada =
                validarTextoOpcional(
                        descripcion,
                        MAX_DESCRIPCION,
                        "La descripción"
                );

        // =================================================
        // COMPROBAR DUPLICADOS DURANTE EDICIÓN
        // =================================================
        //
        // Una sucursal puede conservar su propio nombre.
        //
        // El problema existe solamente cuando otra
        // sucursal ACTIVA utiliza ese mismo nombre.
        //
        // Las sucursales inactivas conservan su historial.
        // Su posible reactivación requerirá validaciones
        // independientes.
        //
        // =================================================

        if (Boolean.TRUE.equals(sucursal.getActivo())) {

            sucursalRepository
                    .findByNombreIgnoreCaseAndActivoTrue(
                            nombreNormalizado
                    )
                    .ifPresent(existente -> {

                        if (!existente.getId().equals(id)) {

                            throw new IllegalArgumentException(
                                    "Ya existe una sucursal activa con el nombre "
                                            + nombreNormalizado
                                            + "."
                            );
                        }
                    });
        }

        // =================================================
        // GUARDAR VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(sucursal);

        // =================================================
        // ACTUALIZAR CAMPOS PERMITIDOS
        // =================================================
        //
        // No modificamos el ID ni el estado activo.
        //
        // La desactivación se maneja mediante una
        // operación independiente y protegida.
        //
        // =================================================

        sucursal.setNombre(nombreNormalizado);

        sucursal.setTelefono(telefonoNormalizado);

        sucursal.setDireccion(direccionNormalizada);

        sucursal.setDescripcion(descripcionNormalizada);

        // =================================================
        // GUARDAR CAMBIOS
        // =================================================

        Sucursal guardada =
                sucursalRepository.saveAndFlush(
                        sucursal
                );

        // =================================================
        // REGISTRAR AUDITORÍA
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.ACTUALIZAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        crearSnapshot(guardada),
                        null
                )
        );

        return guardada;
    }

    // =====================================================
    // CU-14 - DESACTIVAR SUCURSAL
    // =====================================================
    //
    // Elimina lógicamente una sucursal.
    //
    // REGLAS:
    //
    // 1. Verificar permiso branch/delete.
    // 2. Validar que la sucursal exista.
    // 3. Bloquear la sucursal durante la transacción.
    // 4. Comprobar que continúe activa.
    // 5. Verificar que no tenga usuarios activos.
    // 6. Verificar que no tenga citas no cerradas.
    // 7. Verificar que no tenga stock disponible.
    // 8. Verificar que no tenga especialidades activas.
    // 9. Cambiar únicamente su estado a inactivo.
    // 10. Registrar auditoría.
    //
    // =====================================================

    @Transactional
    public void desactivar(Integer id) {

        // =================================================
        // VERIFICAR AUTORIZACIÓN
        // =================================================

        permisoCatalogoService.exigirPermiso(
                "branch/delete"
        );

        Usuario administrador =
                usuarioActualService.obtenerUsuarioActual();

        // =================================================
        // VALIDAR IDENTIFICADOR
        // =================================================

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal válida."
            );
        }

        // =================================================
        // BLOQUEAR SUCURSAL
        // =================================================
        //
        // Impide que dos administradores desactiven
        // simultáneamente la misma sucursal.
        //
        // =================================================

        Sucursal sucursal =
                sucursalRepository
                        .buscarPorIdConBloqueo(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "La sucursal seleccionada no existe."
                                )
                        );

        // =================================================
        // VERIFICAR ESTADO ACTUAL
        // =================================================

        if (!Boolean.TRUE.equals(sucursal.getActivo())) {

            throw new IllegalStateException(
                    "La sucursal ya se encuentra inactiva."
            );
        }

        // =================================================
        // PROTEGER USUARIOS ACTIVOS
        // =================================================
        //
        // Incluye médicos, enfermeros, recepcionistas,
        // cajeros y otros usuarios asociados a la sede.
        //
        // =================================================

        boolean tieneUsuariosActivos =
                usuarioRepository
                        .existsBySucursal_IdAndActivoTrue(id);

        if (tieneUsuariosActivos) {

            throw new IllegalStateException(
                    "No se puede desactivar la sucursal "
                            + "porque tiene usuarios activos asignados."
            );
        }

        // =================================================
        // PROTEGER CITAS NO CERRADAS
        // =================================================
        //
        // No permite desactivar mientras existan citas
        // pendientes, pagadas o en proceso de atención.
        //
        // Las citas históricas permanecen registradas.
        //
        // =================================================

        boolean tieneCitasNoCerradas =
                citaRepository
                        .existeCitaNoCerradaEnSucursal(id);

        if (tieneCitasNoCerradas) {

            throw new IllegalStateException(
                    "No se puede desactivar la sucursal "
                            + "porque tiene citas pendientes "
                            + "o en proceso de atención."
            );
        }

        // =================================================
        // PROTEGER INVENTARIO CON EXISTENCIAS
        // =================================================
        //
        // Impide cerrar una sede que todavía conserva
        // medicamentos disponibles en su inventario.
        //
        // =================================================

        boolean tieneInventarioDisponible =
                inventarioFarmaciaRepository
                        .existsBySucursal_IdAndCantidadDisponibleGreaterThan(
                                id,
                                0
                        );

        if (tieneInventarioDisponible) {

            throw new IllegalStateException(
                    "No se puede desactivar la sucursal "
                            + "porque todavía tiene medicamentos "
                            + "disponibles en inventario."
            );
        }

        // =================================================
        // PROTEGER ESPECIALIDADES ASIGNADAS
        // =================================================
        //
        // Las asignaciones activas deberán resolverse
        // antes desde el mantenimiento del CU-13.
        //
        // =================================================

        boolean tieneEspecialidadesActivas =
                sucursalEspecialidadRepository
                        .existsBySucursal_IdAndActivoTrue(id);

        if (tieneEspecialidadesActivas) {

            throw new IllegalStateException(
                    "No se puede desactivar la sucursal "
                            + "porque tiene especialidades activas "
                            + "asignadas. Primero debe resolver "
                            + "esas asignaciones en el CU-13."
            );
        }

        // =================================================
        // GUARDAR VALORES ANTERIORES
        // =================================================

        Map<String, Object> valoresAnteriores =
                crearSnapshot(sucursal);

        // =================================================
        // DESACTIVACIÓN LÓGICA
        // =================================================
        //
        // NO utilizamos delete().
        //
        // La sucursal permanece en PostgreSQL.
        //
        // Se conservan sus referencias históricas.
        //
        // =================================================

        sucursal.setActivo(false);

        Sucursal guardada =
                sucursalRepository.saveAndFlush(
                        sucursal
                );

        // =================================================
        // REGISTRAR AUDITORÍA
        // =================================================
        //
        // La auditoría forma parte de la transacción.
        //
        // Si se produce un error durante el registro
        // de auditoría, la transacción debe revertirse.
        //
        // =================================================

        auditoriaService.registrar(
                new RegistroAuditoria(
                        "sucursal",
                        String.valueOf(guardada.getId()),
                        AccionAuditoria.ELIMINAR,
                        administrador.getId(),
                        administrador.getNombreUsuario(),
                        valoresAnteriores,
                        crearSnapshot(guardada),
                        null
                )
        );
    }

    // =====================================================
    // VALIDAR NOMBRE
    // =====================================================

    private String validarNombre(
            String nombre
    ) {

        if (nombre == null || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de la sucursal es obligatorio."
            );
        }

        String nombreNormalizado =
                nombre.trim()
                        .replaceAll("\\s+", " ");

        if (nombreNormalizado.length() > MAX_NOMBRE) {

            throw new IllegalArgumentException(
                    "El nombre de la sucursal no puede exceder "
                            + MAX_NOMBRE
                            + " caracteres."
            );
        }

        return nombreNormalizado;
    }

    // =====================================================
    // VALIDAR TELÉFONO
    // =====================================================

    private String validarTelefono(
            String telefono
    ) {

        // El teléfono es opcional.
        if (telefono == null || telefono.isBlank()) {

            return null;
        }

        String telefonoNormalizado = telefono.trim();

        // Debe contener exactamente 8 dígitos numéricos.
        if (telefonoNormalizado.length() != LONGITUD_TELEFONO
                || !telefonoNormalizado.matches("[0-9]{8}")) {

            throw new IllegalArgumentException(
                    "El teléfono debe contener exactamente 8 dígitos numéricos."
            );
        }

        return telefonoNormalizado;
    }

    // =====================================================
    // VALIDAR TEXTO OPCIONAL
    // =====================================================
    //
    // Utilizado para dirección y descripción.
    //
    // Un campo opcional vacío se almacena como null.
    //
    // =====================================================

    private String validarTextoOpcional(
            String valor,
            int longitudMaxima,
            String nombreCampo
    ) {

        if (valor == null || valor.isBlank()) {

            return null;
        }

        String normalizado = valor.trim();

        if (normalizado.length() > longitudMaxima) {

            throw new IllegalArgumentException(
                    nombreCampo
                            + " no puede exceder los "
                            + longitudMaxima
                            + " caracteres."
            );
        }

        return normalizado;
    }

    // =====================================================
    // CONSTRUIR SNAPSHOT DE AUDITORÍA
    // =====================================================
    //
    // Se guardan los campos permitidos.
    //
    // No contiene contraseñas, DPI, NIT ni tokens.
    //
    // LinkedHashMap admite valores null en los campos
    // opcionales, como teléfono o descripción.
    //
    // =====================================================

    private Map<String, Object> crearSnapshot(
            Sucursal sucursal
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "nombre",
                sucursal.getNombre()
        );

        snapshot.put(
                "telefono",
                sucursal.getTelefono()
        );

        snapshot.put(
                "direccion",
                sucursal.getDireccion()
        );

        snapshot.put(
                "descripcion",
                sucursal.getDescripcion()
        );

        snapshot.put(
                "activo",
                sucursal.getActivo()
        );

        return snapshot;
    }
}
