package com.proyecto.clinicamedica.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * =========================================================
 * ENTIDAD: CIE-10
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Catálogo de diagnósticos CIE-10 utilizado durante
 * la consulta médica.
 *
 * El código CIE-10 es opcional dentro de la consulta,
 * pero cuando se selecciona debe provenir de este catálogo.
 *
 * RNF-004:
 * El autocompletado del catálogo debe responder
 * en menos de 500 ms.
 *
 * =========================================================
 */
@Entity
@Table(name = "cie10")
@Getter
@Setter
@NoArgsConstructor
public class Cie10 {


    // =====================================================
    // IDENTIFICADOR
    // =====================================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "id")
    private Integer id;


    // =====================================================
    // CÓDIGO
    // =====================================================

    @Column(
            name = "codigo",
            nullable = false,
            length = 10
    )
    private String codigo;


    // =====================================================
    // DESCRIPCIÓN
    // =====================================================

    @Column(
            name = "descripcion",
            nullable = false,
            length = 500
    )
    private String descripcion;


    // =====================================================
    // ESTADO
    // =====================================================

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo = true;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void prePersist() {

        if (activo == null) {
            activo = true;
        }

        normalizar();
    }


    // =====================================================
    // PRE UPDATE
    // =====================================================

    @PreUpdate
    protected void preUpdate() {

        normalizar();
    }


    // =====================================================
    // NORMALIZAR
    // =====================================================

    private void normalizar() {

        if (codigo != null) {

            codigo =
                    codigo
                            .trim()
                            .toUpperCase();
        }

        if (descripcion != null) {

            descripcion =
                    descripcion.trim();
        }
    }
}