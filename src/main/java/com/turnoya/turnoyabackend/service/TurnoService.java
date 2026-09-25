package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.turno.TurnoCreateRequest;
import com.turnoya.turnoyabackend.dto.turno.TurnoEstadoUpdateRequest;
import com.turnoya.turnoyabackend.dto.turno.TurnoResponse;

import java.util.List;

public interface TurnoService {

    TurnoResponse reservar(TurnoCreateRequest request);

    List<TurnoResponse> listarMisTurnos();

    TurnoResponse obtenerPorId(Long id);

    TurnoResponse cancelar(Long id);

    TurnoResponse cambiarEstado(Long id, TurnoEstadoUpdateRequest request);
}
