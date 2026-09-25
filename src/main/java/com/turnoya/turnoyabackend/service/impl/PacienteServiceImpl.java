package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.paciente.PacienteResponse;
import com.turnoya.turnoyabackend.dto.paciente.PacienteUpdateRequest;
import com.turnoya.turnoyabackend.entity.Paciente;
import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.mapper.PacienteMapper;
import com.turnoya.turnoyabackend.repository.PacienteRepository;
import com.turnoya.turnoyabackend.security.UsuarioAutenticadoService;
import com.turnoya.turnoyabackend.service.PacienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PacienteMapper pacienteMapper;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public PacienteServiceImpl(PacienteRepository pacienteRepository,
                               PacienteMapper pacienteMapper,
                               UsuarioAutenticadoService usuarioAutenticadoService) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteMapper = pacienteMapper;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Override
    public PacienteResponse obtenerMiPerfil() {
        return pacienteMapper.toResponse(usuarioAutenticadoService.obtenerPaciente());
    }

    @Override
    @Transactional
    public PacienteResponse actualizarMiPerfil(PacienteUpdateRequest request) {
        Paciente paciente = usuarioAutenticadoService.obtenerPaciente();
        Usuario usuario = paciente.getUsuario();
        if (request.nombre() != null) {
            usuario.setNombre(request.nombre());
        }
        if (request.telefono() != null) {
            usuario.setTelefono(request.telefono());
        }
        return pacienteMapper.toResponse(paciente);
    }

    @Override
    public List<PacienteResponse> listar() {
        return pacienteRepository.findAll().stream()
                .map(pacienteMapper::toResponse)
                .toList();
    }

    @Override
    public PacienteResponse obtenerPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el paciente con id " + id));
        return pacienteMapper.toResponse(paciente);
    }
}
