package com.turnoya.turnoyabackend.controller;

import com.turnoya.turnoyabackend.dto.turno.ColaItemResponse;
import com.turnoya.turnoyabackend.dto.turno.TurnoResponse;
import com.turnoya.turnoyabackend.service.ColaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/centros/{centroId}/especialidades/{especialidadId}/cola")
public class ColaController {

    private final ColaService colaService;

    public ColaController(ColaService colaService) {
        this.colaService = colaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<ColaItemResponse>> verCola(@PathVariable("centroId") Long centroId,
                                                          @PathVariable("especialidadId") Long especialidadId) {
        return ResponseEntity.ok(colaService.obtenerCola(centroId, especialidadId));
    }

    @PostMapping("/siguiente")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<TurnoResponse> llamarSiguiente(@PathVariable("centroId") Long centroId,
                                                         @PathVariable("especialidadId") Long especialidadId) {
        return ResponseEntity.ok(colaService.llamarSiguiente(centroId, especialidadId));
    }
}
