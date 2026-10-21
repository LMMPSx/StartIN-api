package com.startin.api.service;

import com.startin.api.dto.company.CompanyDTO;
import com.startin.api.model.Company;
import com.startin.api.model.enums.CompanyType;
import com.startin.api.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<CompanyDTO> listAll() {
        return companyRepository.findAll().stream().map(CompanyDTO::fromEntity).toList();
    }

    public Optional<CompanyDTO> getById(Long id) {
        return companyRepository.findById(id).map(CompanyDTO::fromEntity);
    }

    @Transactional
    public CompanyDTO createCompany(CompanyDTO dto) {
        Company company = new Company();
        company.setName(dto.getName());
        company.setType(CompanyType.fromString(dto.getType()));
        company.setDescription(dto.getDescription());
        company.setTags(dto.getTags());

        Company saved = companyRepository.save(company);
        return CompanyDTO.fromEntity(saved);
    }
}
