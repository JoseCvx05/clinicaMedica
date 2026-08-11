package com.proyecto.clinicamedica.specification;

import com.proyecto.clinicamedica.dto.TipoFiltroUsuario;
import com.proyecto.clinicamedica.dto.UsuarioBusquedaDTO;
import com.proyecto.clinicamedica.entity.Usuario;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * =========================================================
 * SPECIFICATION: BÚSQUEDA DE USUARIOS
 * =========================================================
 *
 * Construye dinámicamente los filtros utilizados por
 * el listado de usuarios del CU-01.
 *
 * Permite combinar:
 *
 * - Criterio principal.
 * - Rol.
 * - Sucursal.
 * - Estado.
 *
 * El cifrado y generación de hashes NO se realiza aquí.
 *
 * Para DPI y NIT, la capa Service enviará el HMAC ya
 * generado mediante el parámetro criterioProtegido.
 *
 * =========================================================
 */
public final class UsuarioSpecification {

    /**
     * Evita crear instancias de esta clase utilitaria.
     */
    private UsuarioSpecification() {
    }


    // =====================================================
    // SPECIFICATION PRINCIPAL
    // =====================================================

    public static Specification<Usuario> conFiltros(
            UsuarioBusquedaDTO busqueda,
            String criterioProtegido
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();


            if (busqueda == null) {

                return criteriaBuilder.conjunction();
            }


            // =================================================
            // FILTRO PRINCIPAL
            // =================================================

            String criterio =
                    normalizarTexto(
                            busqueda.getCriterio()
                    );


            if (busqueda.getTipoFiltro() != null
                    && criterio != null) {

                Predicate filtroPrincipal =
                        crearFiltroPrincipal(
                                busqueda.getTipoFiltro(),
                                criterio,
                                criterioProtegido,
                                root,
                                criteriaBuilder
                        );


                if (filtroPrincipal != null) {

                    predicates.add(
                            filtroPrincipal
                    );
                }
            }


            // =================================================
            // FILTRO COMPLEMENTARIO: ROL
            // =================================================

            if (busqueda.getIdRol() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.join(
                                                "rol",
                                                JoinType.INNER
                                        )
                                        .get("id"),
                                busqueda.getIdRol()
                        )
                );
            }


            // =================================================
            // FILTRO COMPLEMENTARIO: SUCURSAL
            // =================================================

            if (busqueda.getIdSucursal() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.join(
                                                "sucursal",
                                                JoinType.LEFT
                                        )
                                        .get("id"),
                                busqueda.getIdSucursal()
                        )
                );
            }


            // =================================================
            // FILTRO COMPLEMENTARIO: ESTADO
            // =================================================

            if (busqueda.getActivo() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("activo"),
                                busqueda.getActivo()
                        )
                );
            }


            // =================================================
            // COMBINAR CON AND
            // =================================================

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }


    // =====================================================
    // FILTRO PRINCIPAL
    // =====================================================

    private static Predicate crearFiltroPrincipal(
            TipoFiltroUsuario tipoFiltro,
            String criterio,
            String criterioProtegido,
            jakarta.persistence.criteria.Root<Usuario> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {

        return switch (tipoFiltro) {

            // =================================================
            // ID
            // =================================================

            case ID ->
                    buscarPorId(
                            criterio,
                            root,
                            criteriaBuilder
                    );


            // =================================================
            // NOMBRE
            // =================================================

            case NOMBRE ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get(
                                            "nombreCompleto"
                                    )
                            ),
                            contiene(
                                    criterio
                            )
                    );


            // =================================================
            // CORREO
            // =================================================

            case CORREO ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get(
                                            "correoElectronico"
                                    )
                            ),
                            contiene(
                                    criterio
                            )
                    );


            // =================================================
            // USUARIO
            // =================================================

            case USUARIO ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get(
                                            "nombreUsuario"
                                    )
                            ),
                            contiene(
                                    criterio
                            )
                    );


            // =================================================
            // ROL
            // =================================================

            case ROL ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "rol",
                                                    JoinType.INNER
                                            )
                                            .get(
                                                    "nombre"
                                            )
                            ),
                            contiene(
                                    criterio
                            )
                    );


            // =================================================
            // SUCURSAL
            // =================================================

            case SUCURSAL ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "sucursal",
                                                    JoinType.LEFT
                                            )
                                            .get(
                                                    "nombre"
                                            )
                            ),
                            contiene(
                                    criterio
                            )
                    );


            // =================================================
            // DPI
            // =================================================
            //
            // Nunca:
            //
            // dpiCifrado LIKE ...
            //
            // El Service convierte primero:
            //
            // DPI → HMAC-SHA-256 → criterioProtegido
            //
            // =================================================

            case DPI ->
                    buscarPorHash(
                            "dpiHash",
                            criterioProtegido,
                            root,
                            criteriaBuilder
                    );


            // =================================================
            // NIT
            // =================================================

            case NIT ->
                    buscarPorHash(
                            "nitHash",
                            criterioProtegido,
                            root,
                            criteriaBuilder
                    );
        };
    }


    // =====================================================
    // BÚSQUEDA POR ID
    // =====================================================

    private static Predicate buscarPorId(
            String criterio,
            jakarta.persistence.criteria.Root<Usuario> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {

        try {

            Integer id =
                    Integer.valueOf(
                            criterio
                    );


            return criteriaBuilder.equal(
                    root.get("id"),
                    id
            );

        } catch (NumberFormatException ex) {

            /*
             * Si se intenta buscar un ID como:
             *
             * ABC
             *
             * no lanzamos una excepción al usuario.
             * Simplemente generamos una condición imposible.
             */
            return criteriaBuilder.disjunction();
        }
    }


    // =====================================================
    // BÚSQUEDA POR HASH
    // =====================================================

    private static Predicate buscarPorHash(
            String campoHash,
            String criterioProtegido,
            jakarta.persistence.criteria.Root<Usuario> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {

        if (criterioProtegido == null
                || criterioProtegido.isBlank()) {

            /*
             * No permitimos que una búsqueda de DPI/NIT
             * accidentalmente ignore el criterio.
             */
            return criteriaBuilder.disjunction();
        }


        return criteriaBuilder.equal(
                root.get(
                        campoHash
                ),
                criterioProtegido
        );
    }


    // =====================================================
    // NORMALIZAR TEXTO
    // =====================================================

    private static String normalizarTexto(
            String valor
    ) {

        if (valor == null) {
            return null;
        }


        String texto =
                valor.trim();


        if (texto.isEmpty()) {
            return null;
        }


        return texto.toLowerCase(
                Locale.ROOT
        );
    }


    // =====================================================
    // CONTIENE
    // =====================================================

    private static String contiene(
            String criterio
    ) {

        return "%"
                + criterio
                + "%";
    }
}