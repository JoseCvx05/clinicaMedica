package com.proyecto.clinicamedica.mapper;

import com.proyecto.clinicamedica.model.EstadoConsultaMedica;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


/**
 * =========================================================
 * CONVERTER: ESTADO CONSULTA MÉDICA
 * =========================================================
 *
 * Convierte entre:
 *
 * Java:
 * EstadoConsultaMedica.EN_CURSO
 *
 * PostgreSQL:
 * "En curso"
 *
 * =========================================================
 */
@Converter
public class EstadoConsultaMedicaConverter
        implements AttributeConverter<
        EstadoConsultaMedica,
        String> {


    @Override
    public String convertToDatabaseColumn(
            EstadoConsultaMedica estado
    ) {

        if (estado == null) {

            return null;
        }

        return estado
                .getValorBaseDatos();
    }


    @Override
    public EstadoConsultaMedica convertToEntityAttribute(
            String valor
    ) {

        return EstadoConsultaMedica
                .desdeValorBaseDatos(
                        valor
                );
    }
}