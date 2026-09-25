package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.turno.ColaItemResponse;
import com.turnoya.turnoyabackend.dto.turno.PosicionColaResponse;
import com.turnoya.turnoyabackend.dto.turno.TurnoResponse;

import java.util.List;

public interface ColaService {

    List<ColaItemResponse> obtenerCola(Long centroId, Long especialidadId);

    PosicionColaResponse obtenerPosicion(Long turnoId);

    TurnoResponse llamarSiguiente(Long centroId, Long especialidadId);
}
