package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.triage.TriageRequest;
import com.turnoya.turnoyabackend.dto.triage.TriageResponse;
import com.turnoya.turnoyabackend.entity.EstadoTurno;
import com.turnoya.turnoyabackend.entity.NivelUrgencia;
import com.turnoya.turnoyabackend.entity.Sintoma;
import com.turnoya.turnoyabackend.entity.Triage;
import com.turnoya.turnoyabackend.entity.Turno;
import com.turnoya.turnoyabackend.event.TriageRegistradoEvent;
import com.turnoya.turnoyabackend.exception.DuplicateResourceException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.exception.TurnoConflictException;
import com.turnoya.turnoyabackend.mapper.TriageMapper;
import com.turnoya.turnoyabackend.repository.TriageRepository;
import com.turnoya.turnoyabackend.repository.TurnoRepository;
import com.turnoya.turnoyabackend.security.UsuarioAutenticadoService;
import com.turnoya.turnoyabackend.service.TriageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TriageServiceImpl implements TriageService {

    private final TriageRepository triageRepository;
    private final TurnoRepository turnoRepository;
    private final TriageMapper triageMapper;
    private final CalculadoraUrgencia calculadoraUrgencia;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ApplicationEventPublisher eventPublisher;

    public TriageServiceImpl(TriageRepository triageRepository,
                             TurnoRepository turnoRepository,
                             TriageMapper triageMapper,
                             CalculadoraUrgencia calculadoraUrgencia,
                             UsuarioAutenticadoService usuarioAutenticadoService,
                             ApplicationEventPublisher eventPublisher) {
        this.triageRepository = triageRepository;
        this.turnoRepository = turnoRepository;
        this.triageMapper = triageMapper;
        this.calculadoraUrgencia = calculadoraUrgencia;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TriageResponse registrar(Long turnoId, TriageRequest request) {
        Turno turno = buscarTurno(turnoId);
        usuarioAutenticadoService.verificarEsDuenoDelTurno(turno);
        if (triageRepository.existsByTurnoId(turnoId)) {
            throw new DuplicateResourceException("Este turno ya tiene un triage registrado");
        }
        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            throw new TurnoConflictException("Solo se puede registrar el triage de un turno reservado");
        }

        NivelUrgencia nivel = calculadoraUrgencia.calcular(request.sintomas());
        Triage triage = Triage.builder()
                .turno(turno)
                .sintomas(unirSintomas(request.sintomas()))
                .nivelUrgencia(nivel)
                .build();
        triageRepository.save(triage);

        turno.setTriage(triage);
        turno.setEstado(EstadoTurno.EN_ESPERA);
        turno.setPrioridad(nivel.getPrioridad());

        eventPublisher.publishEvent(new TriageRegistradoEvent(this, turno.getId(),
                turno.getCentroSalud().getId(), turno.getEspecialidad().getId(), nivel));
        return triageMapper.toResponse(triage);
    }

    @Override
    public TriageResponse obtenerPorTurno(Long turnoId) {
        Turno turno = buscarTurno(turnoId);
        usuarioAutenticadoService.verificarPuedeVerTurno(turno);
        Triage triage = triageRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("El turno " + turnoId + " aún no tiene triage"));
        return triageMapper.toResponse(triage);
    }

    private Turno buscarTurno(Long turnoId) {
        return turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con id " + turnoId));
    }

    private String unirSintomas(List<Sintoma> sintomas) {
        return sintomas.stream()
                .distinct()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
