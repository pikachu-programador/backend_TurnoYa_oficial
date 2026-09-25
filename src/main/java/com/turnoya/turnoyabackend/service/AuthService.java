package com.turnoya.turnoyabackend.service;

import com.turnoya.turnoyabackend.dto.auth.AuthResponse;
import com.turnoya.turnoyabackend.dto.auth.LoginRequest;
import com.turnoya.turnoyabackend.dto.auth.RefreshTokenRequest;
import com.turnoya.turnoyabackend.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse registrar(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refrescarToken(RefreshTokenRequest request);
}
