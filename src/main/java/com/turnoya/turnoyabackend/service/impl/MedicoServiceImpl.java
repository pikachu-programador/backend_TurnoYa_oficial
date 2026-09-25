package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.medico.MedicoCreateRequest;
import com.turnoya.turnoyabackend.dto.medico.MedicoResponse;
import com.turnoya.turnoyabackend.entity.Especialidad;
import com.turnoya.turnoyabackend.entity.PersonalSalud;
import com.turnoya.turnoyabackend.entity.RolUsuario;
import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.event.UsuarioRegistradoEvent;
import com.turnoya.turnoyabackend.exception.DuplicateResourceException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.mapper.MedicoMapper;
import com.turnoya.turnoyabackend.repository.EspecialidadRepository;
import com.turnoya.turnoyabackend.repository.PersonalSaludRepository;
import com.turnoya.turnoyabackend.repository.UsuarioRepository;
import com.turnoya.turnoyabackend.service.MedicoService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MedicoServiceImpl implements MedicoService {

    private final PersonalSaludRepository personalSaludRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final MedicoMapper medicoMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MedicoServiceImpl(PersonalSaludRepository personalSaludRepository,
                             UsuarioRepository usuarioRepository,
                             EspecialidadRepository especialidadRepository,
                             PasswordEncoder passwordEncoder,
                             MedicoMapper medicoMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.personalSaludRepository = personalSaludRepository;
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.medicoMapper = medicoMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MedicoResponse crear(MedicoCreateRequest request) {
        validarDatosUnicos(request);

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(RolUsuario.MEDICO)
                .build();

        PersonalSalud medico = PersonalSalud.builder()
                .usuario(usuario)
                .colegiatura(request.colegiatura())
                .especialidades(buscarEspecialidades(request.especialidadIds()))
                .build();
        personalSaludRepository.save(medico);

        eventPublisher.publishEvent(new UsuarioRegistradoEvent(this, usuario.getId()));
        return medicoMapper.toResponse(medico);
    }

    @Override
    public List<MedicoResponse> listar(Long especialidadId) {
        List<PersonalSalud> medicos = especialidadId == null
                ? personalSaludRepository.findAll()
                : personalSaludRepository.findByEspecialidadesId(especialidadId);
        return medicos.stream()
                .map(medicoMapper::toResponse)
                .toList();
    }

    @Override
    public MedicoResponse obtenerPorId(Long id) {
        PersonalSalud medico = personalSaludRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el médico con id " + id));
        return medicoMapper.toResponse(medico);
    }

    private void validarDatosUnicos(MedicoCreateRequest request) {
        if (usuarioRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email " + request.email());
        }
        if (personalSaludRepository.existsByColegiatura(request.colegiatura())) {
            throw new DuplicateResourceException("Ya existe un médico con la colegiatura " + request.colegiatura());
        }
    }

    private Set<Especialidad> buscarEspecialidades(Set<Long> ids) {
        List<Especialidad> encontradas = especialidadRepository.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            throw new ResourceNotFoundException("Una o más especialidades indicadas no existen");
        }
        return new HashSet<>(encontradas);
    }
}
