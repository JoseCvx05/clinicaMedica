package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * =========================================================
 * ENTIDAD: BITÁCORA DE AUDITORÍA
 * =========================================================
 *
 * Representa el registro histórico de operaciones
 * realizadas dentro del sistema.
 *
 * Para CU-01 permitirá registrar:
 *
 * - Crear usuario.
 * - Actualizar usuario.
 * - Eliminar usuario mediante borrado lógico.
 *
 * RNF-023:
 * Las operaciones CRUD de usuarios deben registrar
 * un log de auditoría inmutable.
 *
 * IMPORTANTE:
 *
 * Esta entidad solamente debe utilizarse para INSERT.
 *
 * La inmutabilidad se protege mediante:
 *
 * 1. @Immutable en Hibernate.
 * 2. Trigger de PostgreSQL contra UPDATE/DELETE/TRUNCATE.
 *
 * Nunca deben guardarse aquí:
 *
 * - Contraseñas.
 * - Hash de contraseña.
 * - DPI cifrado.
 * - DPI hash.
 * - NIT cifrado.
 * - NIT hash.
 * - JWT.
 * =========================================================
 */
@Entity
@Table(name = "bitacora_auditoria")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class BitacoraAuditoria {

    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    // =====================================================
    // REGISTRO AFECTADO
    // =====================================================

    @Column(
            name = "tabla_afectada",
            nullable = false,
            length = 100
    )
    private String tablaAfectada;


    @Column(
            name = "id_registro_afectado",
            length = 50
    )
    private String idRegistroAfectado;


    // =====================================================
    // ACCIÓN
    // =====================================================

    @Column(
            name = "accion",
            nullable = false,
            length = 30
    )
    private String accion;


    // =====================================================
    // USUARIO QUE EJECUTÓ LA OPERACIÓN
    // =====================================================

    @Column(
            name = "id_usuario"
    )
    private Integer idUsuario;


    /**
     * Snapshot del nombre de usuario.
     *
     * Esto permite conservar quién ejecutó la acción
     * aunque posteriormente la cuenta sea desactivada
     * o cambie su nombre.
     */
    @Column(
            name = "nombre_usuario",
            length = 100
    )
    private String nombreUsuario;


    // =====================================================
    // FECHA / HORA
    // =====================================================

    @Column(
            name = "fecha_hora",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaHora;


    // =====================================================
    // SNAPSHOT ANTERIOR
    // =====================================================

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "valores_anteriores",
            columnDefinition = "jsonb",
            updatable = false
    )
    private Map<String, Object> valoresAnteriores;


    // =====================================================
    // SNAPSHOT NUEVO
    // =====================================================

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "valores_nuevos",
            columnDefinition = "jsonb",
            updatable = false
    )
    private Map<String, Object> valoresNuevos;


    // =====================================================
    // DIRECCIÓN IP
    // =====================================================

    @Column(
            name = "direccion_ip",
            length = 45
    )
    private String direccionIp;


    // =====================================================
    // PRE-PERSIST
    // =====================================================

    /**
     * La base de datos también posee DEFAULT CURRENT_TIMESTAMP,
     * pero dejamos igualmente protegida la creación desde JPA.
     */
    @PrePersist
    protected void prePersist() {

        if (fechaHora == null) {

            fechaHora =
                    OffsetDateTime.now();
        }
    }
}