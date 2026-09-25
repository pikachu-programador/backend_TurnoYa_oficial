package com.turnoya.turnoyabackend.controller;

import com.turnoya.turnoyabackend.dto.usuario.UsuarioResponse;
import com.turnoya.turnoyabackend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerMiPerfil() {
        return ResponseEntity.ok(usuarioService.obtenerPerfilActual());
    }
}