package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.turno.ColaItemResponse;
import com.turnoya.turnoyabackend.dto.turno.PosicionColaResponse;
import com.turnoya.turnoyabackend.dto.turno.TurnoResponse;
import com.turnoya.turnoyabackend.entity.EstadoTurno;
import com.turnoya.turnoyabackend.entity.NivelUrgencia;
import com.turnoya.turnoyabackend.entity.PersonalSalud;
import com.turnoya.turnoyabackend.entity.Turno;
import com.turnoya.turnoyabackend.event.TipoEventoTurno;
import com.turnoya.turnoyabackend.event.TurnoEventPublisher;
import com.turnoya.turnoyabackend.exception.ForbiddenException;
import com.turnoya.turnoyabackend.exception.InvalidOperationException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.mapper.TurnoMapper;
import com.turnoya.turnoyabackend.repository.CentroSaludRepository;
import com.turnoya.turnoyabackend.repository.TurnoRepository;
import com.turnoya.turnoyabackend.security.UsuarioAutenticadoService;
import com.turnoya.turnoyabackend.service.ColaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * La cola de un centro y especialidad son los turnos EN_ESPERA ordenados por
 * prioridad (según el triage) y luego por hora de reserva.
 */
@Service
@Transactional(readOnly = true)
public class ColaServiceImpl implements ColaService {

    private final TurnoRepository turnoRepository;
    private final CentroSaludRepository centroSaludRepository;
    private final TurnoMapper turnoMapper;
    private final TurnoEventPublisher turnoEventPublisher;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public ColaServiceImpl(TurnoRepository turnoRepository,
                           CentroSaludRepository centroSaludRepository,
                           TurnoMapper turnoMapper,
                           TurnoEventPublisher turnoEventPublisher,
                           UsuarioAutenticadoService usuarioAutenticadoService) {
        this.turnoRepository = turnoRepository;
        this.centroSaludRepository = centroSaludRepository;
        this.turnoMapper = turnoMapper;
        this.turnoEventPublisher = turnoEventPublisher;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Override
    public List<ColaItemResponse> obtenerCola(Long centroId, Long especialidadId) {
        if (!centroSaludRepository.existsById(centroId)) {
            throw new ResourceNotFoundException("No se encontró el centro de salud con id " + centroId);
        }
        List<Turno> cola = turnoRepository.buscarCola(centroId, especialidadId, EstadoTurno.EN_ESPERA);
        List<ColaItemResponse> respuesta = new ArrayList<>();
        for (int i = 0; i < cola.size(); i++) {
            respuesta.add(turnoMapper.toColaItem(cola.get(i), i + 1));
        }
        return respuesta;
    }

    @Override
    public PosicionColaResponse obtenerPosicion(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con id " + turnoId));
        usuarioAutenticadoService.verificarEsDuenoDelTurno(turno);
        if (turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new InvalidOperationException("Tu turno no está en la cola. Registra tu triage al llegar al centro");
        }

        List<Turno> cola = turnoRepository.buscarCola(
                turno.getCentroSalud().getId(), turno.getEspecialidad().getId(), EstadoTurno.EN_ESPERA);
        int posicion = 1;
        for (Turno enCola : cola) {
            if (enCola.getId().equals(turnoId)) {
                break;
            }
            posicion++;
        }
        NivelUrgencia nivel = turno.getTriage() != null ? turno.getTriage().getNivelUrgencia() : null;
        return new PosicionColaResponse(turnoId, posicion, posicion - 1, nivel);
    }

    @Override
    @Transactional
    public TurnoResponse llamarSiguiente(Long centroId, Long especialidadId) {
        PersonalSalud medico = usuarioAutenticadoService.obtenerMedico();
        boolean atiendeEspecialidad = medico.getEspecialidades().stream()
                .anyMatch(e -> e.getId().equals(especialidadId));
        if (!atiendeEspecialidad) {
            throw new ForbiddenException("No atiendes la especialidad de esta cola");
        }

        List<Turno> cola = turnoRepository.buscarCola(centroId, especialidadId, EstadoTurno.EN_ESPERA);
        if (cola.isEmpty()) {
            throw new ResourceNotFoundException("No hay pacientes en espera en esta cola");
        }

        Turno siguiente = cola.get(0);
        siguiente.setEstado(EstadoTurno.EN_ATENCION);
        siguiente.setMedico(medico);
        turnoEventPublisher.publicar(siguiente, TipoEventoTurno.CAMBIO_ESTADO);
        return turnoMapper.toResponse(siguiente);
    }
}
