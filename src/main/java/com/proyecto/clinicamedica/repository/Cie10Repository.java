package com.proyecto.clinicamedica.repository;

import com.proyecto.clinicamedica.entity.Cie10;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * =========================================================
 * REPOSITORY: CIE-10
 * =========================================================
 *
 * CU-08 - Consulta Médica.
 *
 * Responsabilidades:
 *
 * - Buscar códigos activos.
 * - Buscar un código exacto.
 * - Proporcionar resultados para autocompletado.
 *
 * La tabla PostgreSQL posee índices para:
 *
 * - codigo
 * - descripcion mediante pg_trgm
 *
 * RNF-004:
 * Autocompletado menor a 500 ms.
 *
 * =========================================================
 */
@Repository
public interface Cie10Repository
        extends JpaRepository<Cie10, Integer> {


    // =====================================================
    // CÓDIGO EXACTO ACTIVO
    // =====================================================

    Optional<Cie10> findByCodigoIgnoreCaseAndActivoTrue(
            String codigo
    );


    // =====================================================
    // AUTOCOMPLETADO
    // =====================================================

    /**
     * Busca por:
     *
     * - Inicio del código.
     * - Coincidencia dentro de la descripción.
     *
     * Solo devuelve registros activos.
     *
     * Pageable se utilizará para limitar la cantidad
     * de sugerencias enviadas al navegador.
     */
    @Query(
            value = """
                    SELECT c.*
                    FROM cie10 c
                    
                    WHERE c.activo = TRUE
                    
                      AND (
                            c.codigo ILIKE CONCAT(:texto, '%')
                            
                            OR
                            
                            c.descripcion ILIKE CONCAT('%', :texto, '%')
                          )
                    
                    ORDER BY
                        CASE
                            WHEN c.codigo ILIKE CONCAT(:texto, '%')
                            THEN 0
                            ELSE 1
                        END,
                        c.codigo ASC
                    """,
            nativeQuery = true
    )
    List<Cie10> buscarParaAutocompletado(
            @Param("texto")
            String texto,
            Pageable pageable
    );
}