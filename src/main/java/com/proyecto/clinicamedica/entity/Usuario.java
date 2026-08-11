package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * =========================================================
 * ENTIDAD: USUARIO
 * =========================================================
 *
 * Representa tanto usuarios internos como externos
 * del Sistema Informático Hospitalario.
 *
 * Ejemplos de roles:
 * - Paciente
 * - Médico
 * - Enfermero
 * - Recepcionista
 * - Administrador
 *
 * SEGURIDAD:
 *
 * - La contraseña se almacena únicamente como hash.
 * - El DPI NO se almacena en texto plano.
 * - El NIT NO se almacena en texto plano.
 * - DPI y NIT utilizan:
 *
 *      valor cifrado -> recuperación controlada
 *      hash/HMAC     -> búsqueda y duplicidad
 *
 * La lógica criptográfica NO pertenece a esta entidad.
 * =========================================================
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // DATOS GENERALES
    // =====================================================

    @Column(
            name = "nombre_completo",
            nullable = false,
            length = 100
    )
    private String nombreCompleto;


    @Column(
            name = "correo_electronico",
            nullable = false,
            length = 150
    )
    private String correoElectronico;


    @Column(
            name = "nombre_usuario",
            nullable = false,
            length = 9
    )
    private String nombreUsuario;


    /**
     * Contiene únicamente el hash de la contraseña.
     *
     * Nunca se almacena la contraseña original.
     */
    @Column(
            name = "contrasena_hash",
            nullable = false,
            length = 255
    )
    private String contrasenaHash;


    // =====================================================
    // DPI PROTEGIDO
    // =====================================================

    /**
     * DPI cifrado mediante el servicio criptográfico.
     *
     * No se utilizará para búsquedas.
     */
    @Column(
            name = "dpi_cifrado",
            columnDefinition = "TEXT"
    )
    private String dpiCifrado;


    /**
     * HMAC-SHA-256 del DPI normalizado.
     *
     * Se utilizará para:
     * - verificar registro;
     * - detectar duplicados;
     * - buscar usuario por DPI.
     */
    @Column(
            name = "dpi_hash",
            length = 64
    )
    private String dpiHash;


    // =====================================================
    // CONTACTO
    // =====================================================

    @Column(
            name = "telefono",
            length = 8
    )
    private String telefono;


    // =====================================================
    // NIT PROTEGIDO
    // =====================================================

    @Column(
            name = "nit_cifrado",
            columnDefinition = "TEXT"
    )
    private String nitCifrado;


    @Column(
            name = "nit_hash",
            length = 64
    )
    private String nitHash;


    @Column(
            name = "numero_seguro",
            length = 50
    )
    private String numeroSeguro;


    // =====================================================
    // RELACIONES
    // =====================================================

    /**
     * Todo usuario debe poseer un rol.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "id_rol",
            nullable = false
    )
    private Rol rol;


    /**
     * La sucursal puede ser nula dependiendo del
     * tipo de usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;


    /**
     * La especialidad principalmente aplica
     * a usuarios con rol Médico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;


    // =====================================================
    // ESTADO
    // =====================================================

    /**
     * Borrado lógico.
     *
     * true  = usuario activo
     * false = usuario inactivo
     */
    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo = true;


    // =====================================================
    // SEGURIDAD DE LOGIN
    // =====================================================

    /**
     * Número de intentos fallidos consecutivos.
     *
     * CU-00 permite máximo 5 intentos.
     */
    @Column(
            name = "intentos_fallidos_login",
            nullable = false
    )
    private Short intentosFallidosLogin = 0;


    /**
     * Fecha hasta la cual permanece bloqueada
     * temporalmente la cuenta.
     *
     * CU-00 establece bloqueo de 15 minutos.
     */
    @Column(name = "fecha_bloqueo_hasta")
    private OffsetDateTime fechaBloqueoHasta;


    // =====================================================
    // AUDITORÍA
    // =====================================================

    @Column(
            name = "fecha_creacion",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime fechaCreacion;


    /**
     * Usuario que creó el registro.
     *
     * Puede ser null, por ejemplo durante ciertos
     * registros externos o datos iniciales.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;


    @Column(name = "fecha_modificacion")
    private OffsetDateTime fechaModificacion;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modificado_por")
    private Usuario modificadoPor;


    // =====================================================
    // EVENTOS JPA
    // =====================================================

    /**
     * Valores iniciales antes de insertar.
     */
    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }

        if (intentosFallidosLogin == null) {
            intentosFallidosLogin = 0;
        }

        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
    }


    /**
     * Actualiza automáticamente la fecha de modificación.
     */
    @PreUpdate
    public void preUpdate() {

        fechaModificacion = OffsetDateTime.now();
    }
}