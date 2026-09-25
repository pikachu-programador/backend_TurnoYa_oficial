package com.turnoya.turnoyabackend.controller;

import com.turnoya.turnoyabackend.dto.triage.TriageRequest;
import com.turnoya.turnoyabackend.dto.triage.TriageResponse;
import com.turnoya.turnoyabackend.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/turnos/{turnoId}/triage")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<TriageResponse> registrar(@PathVariable("turnoId") Long turnoId,
                                                    @Valid @RequestBody TriageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triageService.registrar(turnoId, request));
    }

    @GetMapping
    public ResponseEntity<TriageResponse> obtener(@PathVariable("turnoId") Long turnoId) {
        return ResponseEntity.ok(triageService.obtenerPorTurno(turnoId));
    }
}
