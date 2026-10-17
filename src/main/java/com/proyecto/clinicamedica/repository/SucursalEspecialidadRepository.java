
package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.SucursalEspecialidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: SUCURSAL - ESPECIALIDAD
 * =========================================================
 *
 * CU-03:
 *
 * Consulta las especialidades activas configuradas
 * para una sucursal.
 *
 * CU-04:
 *
 * Permite obtener la configuración exacta de una
 * sucursal y especialidad para determinar de forma
 * segura el precio de la consulta.
 *
 * CU-13:
 *
 * Permite administrar las asignaciones entre sedes
 * y especialidades médicas.
 *
 * También permite detectar combinaciones duplicadas,
 * incluso cuando la asignación está inactiva.
 *
 * =========================================================
 */
@Repository
public interface SucursalEspecialidadRepository
        extends JpaRepository<SucursalEspecialidad, Integer> {


    // =====================================================
    // CU-03 - ESPECIALIDADES DE UNA SUCURSAL
    // =====================================================

    List<SucursalEspecialidad>
    findBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrueOrderByEspecialidad_NombreAsc(
            Integer idSucursal
    );


    // =====================================================
    // CU-03 - ¿POSEE ESPECIALIDADES?
    // =====================================================

    boolean
    existsBySucursal_IdAndActivoTrueAndEspecialidad_ActivoTrue(
            Integer idSucursal
    );


    // =====================================================
    // CU-04 - CONFIGURACIÓN SUCURSAL / ESPECIALIDAD
    // =====================================================

    Optional<SucursalEspecialidad>
    findBySucursal_IdAndEspecialidad_IdAndActivoTrue(
            Integer idSucursal,
            Integer idEspecialidad
    );


    // =====================================================
    // CU-13 - DETECTAR ASIGNACIÓN EXISTENTE
    // =====================================================
    //
    // Busca una combinación exacta entre sucursal
    // y especialidad.
    //
    // IMPORTANTE:
    //
    // No filtramos por activo porque necesitamos
    // detectar también asignaciones inactivas.
    //
    // Esto evita intentar insertar una combinación
    // que ya existe en PostgreSQL.
    //
    // =====================================================

    Optional<SucursalEspecialidad>
    findBySucursal_IdAndEspecialidad_Id(
            Integer idSucursal,
            Integer idEspecialidad
    );


    // =====================================================
    // CU-13 - BLOQUEO PARA DESACTIVACIÓN SEGURA
    // =====================================================
    //
    // Bloquea la asignación mientras se verifica
    // si puede ser desactivada.
    //
    // Evita que dos administradores modifiquen
    // simultáneamente la misma asignación.
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT se
            FROM SucursalEspecialidad se
            WHERE se.id = :id
            """)
    Optional<SucursalEspecialidad> buscarParaDesactivarConBloqueo(

            @Param("id")
            Integer id
    );



    // =====================================================
    // CU-03 / CU-13 - PROTEGER CONFIRMACIÓN DE CITAS
    // =====================================================
    //
    // Busca una asignación activa y bloquea su registro
    // durante la confirmación de una cita.
    //
    // Este bloqueo se compartirá con la desactivación
    // del CU-13 para evitar operaciones simultáneas.
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT se
            FROM SucursalEspecialidad se

            WHERE se.sucursal.id = :idSucursal

              AND se.especialidad.id = :idEspecialidad

              AND se.activo = true
            """)
    Optional<SucursalEspecialidad> buscarActivaParaConfirmarConBloqueo(

            @Param("idSucursal")
            Integer idSucursal,

            @Param("idEspecialidad")
            Integer idEspecialidad
    );



    // =====================================================
    // CU-14 - PROTEGER ESPECIALIDADES ASIGNADAS
    // =====================================================
    //
    // Comprueba si una especialidad tiene al menos
    // una asignación ACTIVA en cualquier sucursal.
    //
    // Se utiliza antes de desactivar una especialidad
    // desde el mantenimiento de catálogos.
    //
    // SEGURIDAD:
    //
    // - Evita desactivar especialidades utilizadas.
    // - Protege configuraciones del CU-03 y CU-13.
    // - No modifica ninguna asignación existente.
    // - No elimina registros históricos.
    //
    // =====================================================

    boolean existsByEspecialidad_IdAndActivoTrue(
            Integer idEspecialidad
    );



    // =====================================================
    // CU-14 - PROTEGER SUCURSALES CON ESPECIALIDADES
    // =====================================================
    //
    // Comprueba si una sucursal tiene asignaciones
    // activas de especialidades médicas.
    //
    // IMPORTANTE:
    //
    // - Consulta todas las especialidades asignadas.
    // - No modifica las relaciones existentes.
    // - No elimina registros históricos.
    // - Protege la configuración realizada en CU-13.
    //
    // Antes de desactivar una sucursal, las asignaciones
    // activas deberán resolverse desde el CU-13.
    //
    // =====================================================

    boolean existsBySucursal_IdAndActivoTrue(
            Integer idSucursal
    );


}
