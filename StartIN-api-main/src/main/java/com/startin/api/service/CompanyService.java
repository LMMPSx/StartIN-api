package com.startin.api.service;

import com.startin.api.dto.company.CompanyDTO;
import com.startin.api.model.Company;
import com.startin.api.model.enums.CompanyType;
import com.startin.api.repository.CompanyRepository;
import com.startin.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    public List<CompanyDTO> listAll() {
        return companyRepository.findAll().stream().map(CompanyDTO::fromEntity).toList();
    }

    public Optional<CompanyDTO> getById(Long id) {
        return companyRepository.findById(id).map(CompanyDTO::fromEntity);
    }

    public Optional<CompanyDTO> getByUserId(Long userId) {
        return companyRepository.findByUserId(userId).map(CompanyDTO::fromEntity);
    }

    public CompanyDTO getDefaultCompany() {
        return companyRepository.findAll().stream()
                .findFirst()
                .map(CompanyDTO::fromEntity)
                .orElse(null);
    }

    @Transactional
    public CompanyDTO createCompany(CompanyDTO dto) {
        Company company = new Company();
        if (dto.getUserId() != null) {
            companyRepository.findByUserId(dto.getUserId()).ifPresent(existing -> {
                company.setId(existing.getId());
                company.setUser(existing.getUser());
            });
            if (company.getUser() == null) {
                userRepository.findById(dto.getUserId()).ifPresent(company::setUser);
            }
        }
        company.setName(dto.getName() != null && !dto.getName().isBlank() ? dto.getName() : "Empresa Parceira");
        company.setType(CompanyType.fromString(dto.getType()));
        dto.applyToEntity(company);

        Company saved = companyRepository.save(company);
        return CompanyDTO.fromEntity(saved);
    }

    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseGet(() -> {
                    if (dto.getUserId() != null) {
                        Optional<Company> byUser = companyRepository.findByUserId(dto.getUserId());
                        if (byUser.isPresent()) return byUser.get();
                    }
                    return companyRepository.findAll().stream().findFirst().orElseGet(() -> {
                        Company c = new Company();
                        c.setName("Minha Empresa");
                        c.setType(CompanyType.EMPRESA);
                        return c;
                    });
                });

        if (dto.getUserId() != null && company.getUser() == null) {
            userRepository.findById(dto.getUserId()).ifPresent(company::setUser);
        }

        dto.applyToEntity(company);
        Company saved = companyRepository.save(company);
        return CompanyDTO.fromEntity(saved);
    }
}
