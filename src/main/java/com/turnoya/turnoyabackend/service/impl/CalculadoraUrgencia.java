package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.entity.NivelUrgencia;
import com.turnoya.turnoyabackend.entity.Sintoma;
import com.turnoya.turnoyabackend.exception.InvalidTriageLevelException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calcula el nivel de urgencia a partir de los síntomas: se toma el síntoma
 * más grave y, si hay 3 o más síntomas moderados, se sube un nivel.
 */
@Component
public class CalculadoraUrgencia {

    private static final int SINTOMAS_PARA_SUBIR_NIVEL = 3;
    private static final int GRAVEDAD_MODERADA = 2;

    public NivelUrgencia calcular(List<Sintoma> sintomas) {
        if (sintomas == null || sintomas.isEmpty()) {
            throw new InvalidTriageLevelException("Debes indicar al menos un síntoma para calcular la urgencia");
        }

        int gravedad = sintomas.stream()
                .mapToInt(Sintoma::getGravedad)
                .max()
                .orElse(1);

        long sintomasModerados = sintomas.stream()
                .distinct()
                .filter(sintoma -> sintoma.getGravedad() >= GRAVEDAD_MODERADA)
                .count();
        if (sintomasModerados >= SINTOMAS_PARA_SUBIR_NIVEL) {
            gravedad++;
        }

        return NivelUrgencia.desdePrioridad(gravedad);
    }
}
