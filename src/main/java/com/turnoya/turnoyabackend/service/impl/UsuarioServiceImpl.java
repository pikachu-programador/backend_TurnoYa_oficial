package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.usuario.UsuarioResponse;
import com.turnoya.turnoyabackend.mapper.UsuarioMapper;
import com.turnoya.turnoyabackend.security.UsuarioAutenticadoService;
import com.turnoya.turnoyabackend.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl(UsuarioAutenticadoService usuarioAutenticadoService, UsuarioMapper usuarioMapper) {
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public UsuarioResponse obtenerPerfilActual() {
        return usuarioMapper.toResponse(usuarioAutenticadoService.obtenerUsuario());
    }
}