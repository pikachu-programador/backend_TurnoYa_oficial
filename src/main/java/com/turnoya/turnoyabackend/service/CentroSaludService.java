package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.centro.CentroCercanoResponse;
import com.turnoya.turnoyabackend.dto.centro.CentroSaludRequest;
import com.turnoya.turnoyabackend.dto.centro.CentroSaludResponse;

import java.util.List;

public interface CentroSaludService {

    CentroSaludResponse crear(CentroSaludRequest request);

    List<CentroSaludResponse> listar(Long especialidadId);

    CentroSaludResponse obtenerPorId(Long id);

    CentroSaludResponse actualizar(Long id, CentroSaludRequest request);

    void eliminar(Long id);

    CentroSaludResponse agregarEspecialidad(Long centroId, Long especialidadId);

    List<CentroCercanoResponse> buscarCercanos(double latitud, double longitud, Long especialidadId, double radioKm);
}
