package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


/**
 * =========================================================
 * REPOSITORY: PAGO
 * =========================================================
 *
 * CU-04 Pago en Línea con Tarjeta.
 *
 * Responsabilidades:
 *
 * - Consultar pagos por idempotency key.
 * - Detectar pagos aprobados de una cita.
 * - Recuperar información para el comprobante.
 *
 * =========================================================
 */
@Repository
public interface PagoRepository
        extends JpaRepository<Pago, Integer> {


    // =====================================================
    // IDEMPOTENCIA
    // =====================================================

    /**
     * Permite detectar si una solicitud con el mismo UUID
     * ya fue procesada anteriormente.
     */
    Optional<Pago> findByIdempotencyKey(
            UUID idempotencyKey
    );


    // =====================================================
    // ¿CITA YA PAGADA?
    // =====================================================

    /**
     * Evita volver a procesar una cita que ya posee
     * un pago aprobado.
     */
    boolean existsByCita_IdAndEstado(
            Integer idCita,
            String estado
    );


    // =====================================================
    // PAGO APROBADO DE UNA CITA
    // =====================================================

    Optional<Pago>
    findFirstByCita_IdAndEstadoOrderByFechaCreacionDesc(
            Integer idCita,
            String estado
    );


    // =====================================================
    // COMPROBANTE DEL PACIENTE
    // =====================================================

    /**
     * Recupera el pago junto con toda la información
     * necesaria para construir la pantalla de comprobante.
     *
     * También valida que la cita pertenezca al paciente
     * autenticado.
     */
    @Query("""
        SELECT p
        FROM Pago p

        JOIN FETCH p.cita c
        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        JOIN FETCH c.sucursal
        JOIN FETCH c.especialidad
        JOIN FETCH c.estadoCita

        JOIN FETCH p.formaPago

        WHERE p.numeroTransaccion = :numeroTransaccion
          AND c.paciente.id = :idPaciente
          AND p.estado = 'APROBADO'
          AND p.ordenLaboratorio IS NULL
        """)
    Optional<Pago> buscarComprobante(
            @Param("numeroTransaccion")
            String numeroTransaccion,

            @Param("idPaciente")
            Integer idPaciente
    );
    @Query("""
    SELECT p
    FROM Pago p

    JOIN FETCH p.cita c
    JOIN FETCH c.paciente
    JOIN FETCH c.medico
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.estadoCita

    JOIN FETCH p.formaPago

    WHERE p.id = :idPago
      AND p.estado = 'APROBADO'
      AND p.ordenLaboratorio IS NULL
    """)
    Optional<Pago> buscarParaNotificacion(
            @Param("idPago")
            Integer idPago
    );
    // =====================================================
// CU-06 - COMPROBANTE DE CAJA
// =====================================================

    /**
     * Recupera un pago aprobado para mostrar o reimprimir
     * el comprobante desde el módulo de Caja.
     *
     * A diferencia de buscarComprobante(), aquí no se filtra
     * por paciente autenticado porque el actor es un Cajero.
     *
     * La seguridad de acceso se controla en:
     *
     * /interno/caja/**
     *
     * mediante Spring Security.
     *
     * RNF-033:
     * El comprobante puede consultarse y reimprimirse
     * tantas veces como sea necesario.
     */
    // =====================================================
// CU-06
// COMPROBANTE DE CONSULTA
// =====================================================
//
// IMPORTANTE:
//
// Solo recupera pagos que NO estén asociados
// a una orden de laboratorio.
//
// Esto evita mostrar un pago de laboratorio
// como si fuera una consulta médica.
// =====================================================

    @Query("""
       SELECT p
       FROM Pago p

       JOIN FETCH p.cita c
       JOIN FETCH c.paciente
       JOIN FETCH c.medico
       JOIN FETCH c.sucursal
       JOIN FETCH c.especialidad
       JOIN FETCH c.estadoCita

       JOIN FETCH p.formaPago

       LEFT JOIN FETCH p.cajero

       WHERE p.numeroTransaccion = :numeroTransaccion
         AND p.estado = 'APROBADO'
         AND p.ordenLaboratorio IS NULL
       """)
    Optional<Pago> buscarComprobanteCaja(

            @Param("numeroTransaccion")
            String numeroTransaccion
    );

    // =====================================================
// CU-09 / CU-16
// COMPROBANTE DE LABORATORIO
// =====================================================
//
// Recupera exclusivamente pagos asociados
// a una OrdenLaboratorio.
//
// Carga:
//
// - Pago.
// - Orden.
// - Cita.
// - Paciente.
// - Médico solicitante.
// - Sucursal.
// - Forma de pago.
// - Cajero.
//
// Los exámenes serán obtenidos después mediante
// DetalleOrdenLaboratorioRepository.
// =====================================================

    @Query("""
       SELECT p
       FROM Pago p

       JOIN FETCH p.ordenLaboratorio o

       JOIN FETCH o.cita c
       JOIN FETCH c.paciente

       JOIN FETCH o.medico
       JOIN FETCH c.sucursal

       JOIN FETCH p.formaPago

       LEFT JOIN FETCH p.cajero

       WHERE p.numeroTransaccion = :numeroTransaccion
         AND p.estado = 'APROBADO'
       """)
    Optional<Pago> buscarComprobanteLaboratorio(

            @Param("numeroTransaccion")
            String numeroTransaccion
    );

    // =====================================================
// CU-09 / CU-16
// ¿ORDEN DE LABORATORIO YA PAGADA?
// =====================================================
//
// Comprueba si existe un pago APROBADO asociado
// específicamente a una orden de laboratorio.
//
// IMPORTANTE:
//
// No utiliza id_cita porque una misma cita puede tener:
//
// - pago de consulta;
// - pago de laboratorio.
//
// =====================================================

    boolean existsByOrdenLaboratorio_IdAndEstado(
            Integer idOrdenLaboratorio,
            String estado
    );


// =====================================================
// CU-09 / CU-16
// OBTENER PAGO APROBADO DE LA ORDEN
// =====================================================
//
// Permite recuperar el último pago aprobado asociado
// específicamente a la orden de laboratorio.
//
// Se utilizará posteriormente para:
//
// - validar el pago;
// - mostrar comprobante;
// - obtener número de transacción;
// - forma de pago;
// - fecha/hora;
// - cajero.
//
// =====================================================

    Optional<Pago>
    findFirstByOrdenLaboratorio_IdAndEstadoOrderByFechaCreacionDesc(
            Integer idOrdenLaboratorio,
            String estado
    );
}