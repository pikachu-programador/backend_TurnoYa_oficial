package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.medico.MedicoCreateRequest;
import com.turnoya.turnoyabackend.dto.medico.MedicoResponse;

import java.util.List;

public interface MedicoService {

    MedicoResponse crear(MedicoCreateRequest request);

    List<MedicoResponse> listar(Long especialidadId);

    MedicoResponse obtenerPorId(Long id);
}
