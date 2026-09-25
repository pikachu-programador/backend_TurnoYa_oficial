package com.turnoya.turnoyabackend.controller;

import com.turnoya.turnoyabackend.dto.auth.AuthResponse;
import com.turnoya.turnoyabackend.dto.auth.LoginRequest;
import com.turnoya.turnoyabackend.dto.auth.RefreshTokenRequest;
import com.turnoya.turnoyabackend.dto.auth.RegisterRequest;
import com.turnoya.turnoyabackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refrescarToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refrescarToken(request));
    }
}
