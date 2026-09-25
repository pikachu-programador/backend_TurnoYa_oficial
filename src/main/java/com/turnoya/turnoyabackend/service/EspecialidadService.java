package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.especialidad.EspecialidadRequest;
import com.turnoya.turnoyabackend.dto.especialidad.EspecialidadResponse;

import java.util.List;

public interface EspecialidadService {

    EspecialidadResponse crear(EspecialidadRequest request);

    List<EspecialidadResponse> listar();

    EspecialidadResponse obtenerPorId(Long id);

    EspecialidadResponse actualizar(Long id, EspecialidadRequest request);

    void eliminar(Long id);
}
