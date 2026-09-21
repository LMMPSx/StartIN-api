package com.api.backend.service;

import com.api.backend.dto.StartupRequestDTO;
import com.api.backend.dto.StartupResponseDTO;
import com.api.backend.model.Startup;
import com.api.backend.repository.StartupRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StartupService {

    @Autowired
    private StartupRepository repository;

    public StartupResponseDTO criar(StartupRequestDTO dto) {
        if (repository.existsByCnpj(dto.getCnpj())) {
            throw new IllegalArgumentException("Já existe uma startup cadastrada com este CNPJ.");
        }
        Startup entity = new Startup();
        BeanUtils.copyProperties(dto, entity);
        Startup saved = repository.save(entity);
        return convertToDTO(saved);
    }

    public List<StartupResponseDTO> listarTodas() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StartupResponseDTO buscarPorId(Long id) {
        Startup entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup não encontrada com ID: " + id));
        return convertToDTO(entity);
    }

    public StartupResponseDTO atualizar(Long id, StartupRequestDTO dto) {
        Startup entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup não encontrada com ID: " + id));

        BeanUtils.copyProperties(dto, entity, "id");
        Startup updated = repository.save(entity);
        return convertToDTO(updated);
    }

    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Startup não encontrada com ID: " + id);
        }
        repository.deleteById(id);
    }

    private StartupResponseDTO convertToDTO(Startup entity) {
        StartupResponseDTO dto = new StartupResponseDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}