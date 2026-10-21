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

    @PostMapping
    @Operation(summary = "Cadastra uma nova empresa, organização ou instituição")
    public ResponseEntity<CompanyDTO> create(@RequestBody CompanyDTO dto) {
        return ResponseEntity.ok(companyService.createCompany(dto));
    }
}
