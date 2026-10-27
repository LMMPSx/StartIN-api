package com.startin.api.controller;

import com.startin.api.dto.company.CompanyDTO;
import com.startin.api.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Empresas & Organizações", description = "Endpoints para gerenciamento de organizações parceiras e contratantes")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as empresas cadastradas")
    public ResponseEntity<List<CompanyDTO>> list() {
        return ResponseEntity.ok(companyService.listAll());
    }

    @GetMapping("/default")
    @Operation(summary = "Busca o perfil da empresa padrão")
    public ResponseEntity<CompanyDTO> getDefault() {
        CompanyDTO dto = companyService.getDefaultCompany();
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca perfil da empresa por ID")
    public ResponseEntity<CompanyDTO> getById(@PathVariable Long id) {
        return companyService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Busca perfil da empresa pelo ID do usuário")
    public ResponseEntity<CompanyDTO> getByUserId(@PathVariable Long userId) {
        return companyService.getByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova empresa, organização ou instituição")
    public ResponseEntity<CompanyDTO> create(@RequestBody CompanyDTO dto) {
        return ResponseEntity.ok(companyService.createCompany(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados do perfil da empresa")
    public ResponseEntity<CompanyDTO> update(@PathVariable Long id, @RequestBody CompanyDTO dto) {
        return ResponseEntity.ok(companyService.updateCompany(id, dto));
    }
}
