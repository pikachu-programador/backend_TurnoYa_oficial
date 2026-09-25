package com.turnoya.turnoyabackend.security;

import com.turnoya.turnoyabackend.entity.Paciente;
import com.turnoya.turnoyabackend.entity.PersonalSalud;
import com.turnoya.turnoyabackend.entity.RolUsuario;
import com.turnoya.turnoyabackend.entity.Turno;
import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.exception.ForbiddenException;
import com.turnoya.turnoyabackend.exception.InvalidCredentialsException;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.repository.PacienteRepository;
import com.turnoya.turnoyabackend.repository.PersonalSaludRepository;
import com.turnoya.turnoyabackend.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Lee el usuario logueado desde el SecurityContext para que los servicios
 * sepan quién hace la petición.
 */
@Service
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalSaludRepository personalSaludRepository;

    public UsuarioAutenticadoService(UsuarioRepository usuarioRepository,
                                     PacienteRepository pacienteRepository,
                                     PersonalSaludRepository personalSaludRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.personalSaludRepository = personalSaludRepository;
    }

    public String obtenerEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new InvalidCredentialsException("Debes iniciar sesión para realizar esta acción");
        }
        return auth.getName();
    }

    public Usuario obtenerUsuario() {
        String email = obtenerEmail();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario " + email));
    }

    public Paciente obtenerPaciente() {
        return pacienteRepository.findByUsuarioEmail(obtenerEmail())
                .orElseThrow(() -> new ForbiddenException("Esta acción solo está disponible para pacientes"));
    }

    public PersonalSalud obtenerMedico() {
        return personalSaludRepository.findByUsuarioEmail(obtenerEmail())
                .orElseThrow(() -> new ForbiddenException("Esta acción solo está disponible para médicos"));
    }

    public void verificarEsDuenoDelTurno(Turno turno) {
        Paciente paciente = obtenerPaciente();
        if (!turno.getPaciente().getId().equals(paciente.getId())) {
            throw new ForbiddenException("Este turno pertenece a otro paciente");
        }
    }

    /**
     * Médicos y administradores pueden ver cualquier turno; un paciente solo los suyos.
     */
    public void verificarPuedeVerTurno(Turno turno) {
        Usuario usuario = obtenerUsuario();
        boolean esPaciente = usuario.getRol() == RolUsuario.PACIENTE;
        if (esPaciente && !turno.getPaciente().getUsuario().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Este turno pertenece a otro paciente");
        }
    }
}