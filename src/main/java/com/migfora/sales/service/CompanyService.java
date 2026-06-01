package com.migfora.sales.service;

import com.migfora.sales.dto.CompanyDtos.*;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Company.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.ContactRepository;
import com.migfora.sales.repository.InvestigationRepository;
import com.migfora.sales.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:15 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {


    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final InvestigationRepository investigationRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, String createdBy) {
        if (request.domain() != null && companyRepository.existsByDomain(request.domain())) {
            throw new AuthException("A company with this domain already exists.");
        }

        Company company = Company.builder()
                .name(request.name())
                .domain(request.domain())
                .industry(request.industry())
                .country(request.country())
                .city(request.city())
                .website(request.website())
                .size(request.size())
                .notes(request.notes())
                .createdBy(createdBy)
                .status(request.status() != null ? request.status() : CompanyStatus.PROSPECT)
                .build();

        Company saved = companyRepository.save(company);
        log.info("Company created | id={} name={} by={}", saved.getId(), saved.getName(), createdBy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAll(String search,
                                        CompanyStatus status,
                                        Pageable pageable) {
        String statusStr = status != null ? status.name() : null;

        // Strip sort from pageable — our native query handles ORDER BY
        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return companyRepository.search(search, statusStr, unsorted)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CompanyResponse update(Long id, UpdateCompanyRequest request, String updatedBy) {
        Company company = findById(id);

        if (request.name()     != null) company.setName(request.name());
        if (request.domain()   != null) company.setDomain(request.domain());
        if (request.industry() != null) company.setIndustry(request.industry());
        if (request.country()  != null) company.setCountry(request.country());
        if (request.city()     != null) company.setCity(request.city());
        if (request.website()  != null) company.setWebsite(request.website());
        if (request.size()     != null) company.setSize(request.size());
        if (request.notes()    != null) company.setNotes(request.notes());
        if (request.status()   != null) company.setStatus(request.status());

        log.info("Company updated | id={} by={}", id, updatedBy);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        companyRepository.deleteById(id);
        log.info("Company deleted | id={} by={}", id, deletedBy);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new AuthException("Company not found."));
    }

    public CompanyResponse toResponse(Company c) {
        return new CompanyResponse(
                c.getId(), c.getName(), c.getDomain(),
                c.getIndustry(), c.getCountry(), c.getCity(),
                c.getWebsite(), c.getSize(), c.getNotes(),
                c.getCreatedBy(), c.getStatus(),
                investigationRepository.countByCompanyId(c.getId()),
                contactRepository.countByCompanyId(c.getId()),
                reportRepository.countByCompanyId(c.getId()),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
