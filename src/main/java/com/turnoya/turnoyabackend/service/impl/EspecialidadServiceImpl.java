package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.especialidad.EspecialidadRequest;
import com.turnoya.turnoyabackend.dto.especialidad.EspecialidadResponse;
import com.turnoya.turnoyabackend.entity.Especialidad;
import com.turnoya.turnoyabackend.exception.DuplicateResourceException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.mapper.EspecialidadMapper;
import com.turnoya.turnoyabackend.repository.EspecialidadRepository;
import com.turnoya.turnoyabackend.service.EspecialidadService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final EspecialidadMapper especialidadMapper;

    public EspecialidadServiceImpl(EspecialidadRepository especialidadRepository,
                                   EspecialidadMapper especialidadMapper) {
        this.especialidadRepository = especialidadRepository;
        this.especialidadMapper = especialidadMapper;
    }

    @Override
    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        validarNombreDisponible(request.nombre());
        Especialidad especialidad = especialidadRepository.save(especialidadMapper.toEntity(request));
        return especialidadMapper.toResponse(especialidad);
    }

    @Override
    public List<EspecialidadResponse> listar() {
        return especialidadRepository.findAll(Sort.by("nombre")).stream()
                .map(especialidadMapper::toResponse)
                .toList();
    }

    @Override
    public EspecialidadResponse obtenerPorId(Long id) {
        return especialidadMapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public EspecialidadResponse actualizar(Long id, EspecialidadRequest request) {
        Especialidad especialidad = buscar(id);
        if (!especialidad.getNombre().equalsIgnoreCase(request.nombre())) {
            validarNombreDisponible(request.nombre());
        }
        especialidadMapper.updateEntity(especialidad, request);
        return especialidadMapper.toResponse(especialidad);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        especialidadRepository.delete(buscar(id));
    }

    private Especialidad buscar(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la especialidad con id " + id));
    }

    private void validarNombreDisponible(String nombre) {
        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe la especialidad " + nombre);
        }
    }
}
