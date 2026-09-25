package com.turnoya.turnoyabackend.service.impl;

import com.turnoya.turnoyabackend.dto.centro.CentroCercanoResponse;
import com.turnoya.turnoyabackend.dto.centro.CentroSaludRequest;
import com.turnoya.turnoyabackend.dto.centro.CentroSaludResponse;
import com.turnoya.turnoyabackend.entity.CentroSalud;
import com.turnoya.turnoyabackend.entity.Especialidad;
import com.turnoya.turnoyabackend.exception.ResourceNotFoundException;
import com.turnoya.turnoyabackend.mapper.CentroSaludMapper;
import com.turnoya.turnoyabackend.repository.CentroSaludRepository;
import com.turnoya.turnoyabackend.repository.EspecialidadRepository;
import com.turnoya.turnoyabackend.service.CentroSaludService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CentroSaludServiceImpl implements CentroSaludService {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private final CentroSaludRepository centroSaludRepository;
    private final EspecialidadRepository especialidadRepository;
    private final CentroSaludMapper centroSaludMapper;

    public CentroSaludServiceImpl(CentroSaludRepository centroSaludRepository,
                                  EspecialidadRepository especialidadRepository,
                                  CentroSaludMapper centroSaludMapper) {
        this.centroSaludRepository = centroSaludRepository;
        this.especialidadRepository = especialidadRepository;
        this.centroSaludMapper = centroSaludMapper;
    }

    @Override
    @Transactional
    public CentroSaludResponse crear(CentroSaludRequest request) {
        CentroSalud centro = centroSaludMapper.toEntity(request);
        centro.setEspecialidades(buscarEspecialidades(request.especialidadIds()));
        return centroSaludMapper.toResponse(centroSaludRepository.save(centro));
    }

    @Override
    public List<CentroSaludResponse> listar(Long especialidadId) {
        return buscarCentros(especialidadId).stream()
                .map(centroSaludMapper::toResponse)
                .toList();
    }

    @Override
    public CentroSaludResponse obtenerPorId(Long id) {
        return centroSaludMapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public CentroSaludResponse actualizar(Long id, CentroSaludRequest request) {
        CentroSalud centro = buscar(id);
        centroSaludMapper.updateEntity(centro, request);
        if (request.especialidadIds() != null) {
            centro.setEspecialidades(buscarEspecialidades(request.especialidadIds()));
        }
        return centroSaludMapper.toResponse(centro);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        centroSaludRepository.delete(buscar(id));
    }

    @Override
    @Transactional
    public CentroSaludResponse agregarEspecialidad(Long centroId, Long especialidadId) {
        CentroSalud centro = buscar(centroId);
        Especialidad especialidad = especialidadRepository.findById(especialidadId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la especialidad con id " + especialidadId));
        centro.getEspecialidades().add(especialidad);
        return centroSaludMapper.toResponse(centro);
    }

    @Override
    public List<CentroCercanoResponse> buscarCercanos(double latitud, double longitud, Long especialidadId,
                                                      double radioKm) {
        return buscarCentros(especialidadId).stream()
                .map(centro -> centroSaludMapper.toCercanoResponse(centro,
                        calcularDistanciaKm(latitud, longitud, centro.getLatitud(), centro.getLongitud())))
                .filter(centro -> centro.distanciaKm() <= radioKm)
                .sorted(Comparator.comparingDouble(CentroCercanoResponse::distanciaKm))
                .toList();
    }

    private List<CentroSalud> buscarCentros(Long especialidadId) {
        return especialidadId == null
                ? centroSaludRepository.findAll()
                : centroSaludRepository.findByEspecialidadesId(especialidadId);
    }

    private CentroSalud buscar(Long id) {
        return centroSaludRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el centro de salud con id " + id));
    }

    private Set<Especialidad> buscarEspecialidades(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Especialidad> encontradas = especialidadRepository.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            throw new ResourceNotFoundException("Una o más especialidades indicadas no existen");
        }
        return new HashSet<>(encontradas);
    }

    /**
     * Distancia en línea recta entre dos coordenadas (fórmula de Haversine).
     */
    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
