package com.turnoya.turnoyabackend.controller;

import com.turnoya.turnoyabackend.dto.paciente.PacienteResponse;
import com.turnoya.turnoyabackend.dto.paciente.PacienteUpdateRequest;
import com.turnoya.turnoyabackend.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> obtenerMiPerfil() {
        return ResponseEntity.ok(pacienteService.obtenerMiPerfil());
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> actualizarMiPerfil(@Valid @RequestBody PacienteUpdateRequest request) {
        return ResponseEntity.ok(pacienteService.actualizarMiPerfil(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'MEDICO')")
    public ResponseEntity<List<PacienteResponse>> listar() {
        return ResponseEntity.ok(pacienteService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'MEDICO')")
    public ResponseEntity<PacienteResponse> obtenerPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }
}
