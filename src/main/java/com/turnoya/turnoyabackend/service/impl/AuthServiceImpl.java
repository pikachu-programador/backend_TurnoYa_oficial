package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.auth.AuthResponse;
import com.turnoya.turnoyabackend.dto.auth.LoginRequest;
import com.turnoya.turnoyabackend.dto.auth.RefreshTokenRequest;
import com.turnoya.turnoyabackend.dto.auth.RegisterRequest;
import com.turnoya.turnoyabackend.entity.Paciente;
import com.turnoya.turnoyabackend.entity.RolUsuario;
import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.event.UsuarioRegistradoEvent;
import com.turnoya.turnoyabackend.exception.DuplicateResourceException;
import com.turnoya.turnoyabackend.exception.InvalidCredentialsException;
import com.turnoya.turnoyabackend.mapper.UsuarioMapper;
import com.turnoya.turnoyabackend.repository.PacienteRepository;
import com.turnoya.turnoyabackend.repository.UsuarioRepository;
import com.turnoya.turnoyabackend.security.JwtService;
import com.turnoya.turnoyabackend.service.AuthService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioMapper usuarioMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PacienteRepository pacienteRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           UsuarioMapper usuarioMapper,
                           ApplicationEventPublisher eventPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioMapper = usuarioMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        validarDatosUnicos(request);

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(RolUsuario.PACIENTE)
                .build();

        Paciente paciente = Paciente.builder()
                .usuario(usuario)
                .dni(request.dni())
                .fechaNacimiento(request.fechaNacimiento())
                .build();
        pacienteRepository.save(paciente);

        eventPublisher.publishEvent(new UsuarioRegistradoEvent(this, usuario.getId()));
        return construirRespuesta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Email o contraseña incorrectos"));
        return construirRespuesta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refrescarToken(RefreshTokenRequest request) {
        String email = jwtService.validarRefreshToken(request.refreshToken());
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("El usuario del token ya no existe"));
        return construirRespuesta(usuario);
    }

    private void validarDatosUnicos(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email " + request.email());
        }
        if (pacienteRepository.existsByDni(request.dni())) {
            throw new DuplicateResourceException("Ya existe un paciente con el DNI " + request.dni());
        }
    }

    private AuthResponse construirRespuesta(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                TOKEN_TYPE,
                jwtService.getExpirationMs(),
                usuarioMapper.toResponse(usuario)
        );
    }
}
