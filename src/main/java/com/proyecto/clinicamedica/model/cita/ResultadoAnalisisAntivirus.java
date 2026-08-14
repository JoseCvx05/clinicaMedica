package com.proyecto.clinicamedica.dto.cita;

import com.proyecto.clinicamedica.model.cita.EstadoAnalisisAntivirus;

/**
 * =========================================================
 * RESULTADO DEL ANÁLISIS ANTIVIRUS
 * =========================================================
 */
public record ResultadoAnalisisAntivirus(

        EstadoAnalisisAntivirus estado,

        String detalle

) {

    public boolean estaLimpio() {

        return estado
                == EstadoAnalisisAntivirus.LIMPIO;
    }


    public boolean estaInfectado() {

        return estado
                == EstadoAnalisisAntivirus.INFECTADO;
    }


    public boolean tieneError() {

        return estado
                == EstadoAnalisisAntivirus.ERROR;
    }
}