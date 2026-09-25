package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.turno.TurnoCreateRequest;
import com.turnoya.turnoyabackend.dto.turno.TurnoEstadoUpdateRequest;
import com.turnoya.turnoyabackend.dto.turno.TurnoResponse;
import com.turnoya.turnoyabackend.entity.CentroSalud;
import com.turnoya.turnoyabackend.entity.Especialidad;
import com.turnoya.turnoyabackend.entity.EstadoTurno;
import com.turnoya.turnoyabackend.entity.Paciente;
import com.turnoya.turnoyabackend.entity.RolUsuario;
import com.turnoya.turnoyabackend.entity.Turno;
import com.turnoya.turnoyabackend.event.TipoEventoTurno;
import com.turnoya.turnoyabackend.event.TurnoEventPublisher;
import com.turnoya.turnoyabackend.exception.CentroSinCapacidadException;
import com.turnoya.turnoyabackend.exception.InvalidOperationException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.exception.TurnoConflictException;
import com.turnoya.turnoyabackend.mapper.TurnoMapper;
import com.turnoya.turnoyabackend.repository.CentroSaludRepository;
import com.turnoya.turnoyabackend.repository.EspecialidadRepository;
import com.turnoya.turnoyabackend.repository.TurnoRepository;
import com.turnoya.turnoyabackend.security.UsuarioAutenticadoService;
import com.turnoya.turnoyabackend.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TurnoServiceImpl implements TurnoService {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS =
            List.of(EstadoTurno.RESERVADO, EstadoTurno.EN_ESPERA, EstadoTurno.EN_ATENCION);
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TurnoRepository turnoRepository;
    private final CentroSaludRepository centroSaludRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TurnoMapper turnoMapper;
    private final TurnoEventPublisher turnoEventPublisher;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public TurnoServiceImpl(TurnoRepository turnoRepository,
                            CentroSaludRepository centroSaludRepository,
                            EspecialidadRepository especialidadRepository,
                            TurnoMapper turnoMapper,
                            TurnoEventPublisher turnoEventPublisher,
                            UsuarioAutenticadoService usuarioAutenticadoService) {
        this.turnoRepository = turnoRepository;
        this.centroSaludRepository = centroSaludRepository;
        this.especialidadRepository = especialidadRepository;
        this.turnoMapper = turnoMapper;
        this.turnoEventPublisher = turnoEventPublisher;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Override
    @Transactional
    public TurnoResponse reservar(TurnoCreateRequest request) {
        Paciente paciente = usuarioAutenticadoService.obtenerPaciente();
        CentroSalud centro = centroSaludRepository.findById(request.centroId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el centro de salud con id " + request.centroId()));
        Especialidad especialidad = especialidadRepository.findById(request.especialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la especialidad con id " + request.especialidadId()));

        validarCentroOfreceEspecialidad(centro, especialidad);
        validarSinTurnoActivo(paciente, especialidad);
        validarCapacidad(centro, request.fechaHora());

        Turno turno = Turno.builder()
                .paciente(paciente)
                .centroSalud(centro)
                .especialidad(especialidad)
                .fechaHora(request.fechaHora())
                .estado(EstadoTurno.RESERVADO)
                .prioridad(0)
                .build();
        turnoRepository.save(turno);

        turnoEventPublisher.publicar(turno, TipoEventoTurno.CREACION);
        return turnoMapper.toResponse(turno);
    }

    @Override
    public List<TurnoResponse> listarMisTurnos() {
        Paciente paciente = usuarioAutenticadoService.obtenerPaciente();
        return turnoRepository.findByPacienteIdOrderByFechaHoraDesc(paciente.getId()).stream()
                .map(turnoMapper::toResponse)
                .toList();
    }

    @Override
    public TurnoResponse obtenerPorId(Long id) {
        Turno turno = buscar(id);
        usuarioAutenticadoService.verificarPuedeVerTurno(turno);
        return turnoMapper.toResponse(turno);
    }

    @Override
    @Transactional
    public TurnoResponse cancelar(Long id) {
        Turno turno = buscar(id);
        usuarioAutenticadoService.verificarEsDuenoDelTurno(turno);
        if (turno.getEstado() != EstadoTurno.RESERVADO && turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new TurnoConflictException("Solo puedes cancelar turnos reservados o en espera");
        }
        turno.setEstado(EstadoTurno.CANCELADO);
        turnoEventPublisher.publicar(turno, TipoEventoTurno.CAMBIO_ESTADO);
        return turnoMapper.toResponse(turno);
    }

    @Override
    @Transactional
    public TurnoResponse cambiarEstado(Long id, TurnoEstadoUpdateRequest request) {
        Turno turno = buscar(id);
        validarCambioDeEstado(turno, request.estado());

        boolean esMedico = usuarioAutenticadoService.obtenerUsuario().getRol() == RolUsuario.MEDICO;
        if (esMedico && turno.getMedico() == null) {
            turno.setMedico(usuarioAutenticadoService.obtenerMedico());
        }
        turno.setEstado(request.estado());

        turnoEventPublisher.publicar(turno, TipoEventoTurno.CAMBIO_ESTADO);
        return turnoMapper.toResponse(turno);
    }

    private Turno buscar(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con id " + id));
    }

    private void validarCentroOfreceEspecialidad(CentroSalud centro, Especialidad especialidad) {
        boolean laOfrece = centro.getEspecialidades().stream()
                .anyMatch(e -> e.getId().equals(especialidad.getId()));
        if (!laOfrece) {
            throw new InvalidOperationException(
                    "El centro " + centro.getNombre() + " no atiende la especialidad " + especialidad.getNombre());
        }
    }

    private void validarSinTurnoActivo(Paciente paciente, Especialidad especialidad) {
        if (turnoRepository.existsByPacienteIdAndEspecialidadIdAndEstadoIn(
                paciente.getId(), especialidad.getId(), ESTADOS_ACTIVOS)) {
            throw new TurnoConflictException("Ya tienes un turno activo para " + especialidad.getNombre());
        }
    }

    private void validarCapacidad(CentroSalud centro, LocalDateTime fechaHora) {
        LocalDateTime inicioDia = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusNanos(1);
        long turnosDelDia = turnoRepository.countByCentroSaludIdAndFechaHoraBetweenAndEstadoNot(
                centro.getId(), inicioDia, finDia, EstadoTurno.CANCELADO);
        if (turnosDelDia >= centro.getCapacidadDiaria()) {
            throw new CentroSinCapacidadException("El centro " + centro.getNombre()
                    + " ya no tiene cupos para el " + fechaHora.format(FORMATO_DIA));
        }
    }

    private void validarCambioDeEstado(Turno turno, EstadoTurno nuevoEstado) {
        EstadoTurno actual = turno.getEstado();
        if (actual == EstadoTurno.ATENDIDO || actual == EstadoTurno.CANCELADO) {
            throw new TurnoConflictException("El turno ya está " + actual + " y no se puede modificar");
        }
        if (actual == nuevoEstado) {
            throw new InvalidOperationException("El turno ya está en estado " + actual);
        }
    }
}
