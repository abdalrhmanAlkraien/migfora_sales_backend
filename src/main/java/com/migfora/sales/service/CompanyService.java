package com.migfora.sales.service;

import com.migfora.sales.dto.CompanyDtos.*;
import com.migfora.sales.dto.PlatformDtos;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Company.*;
import com.migfora.sales.entity.CompanyPlatform;
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

import java.util.ArrayList;
import java.util.List;

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
    private final CompanyPlatformService platformService;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, String createdBy) {
        if (request.website() != null && companyRepository.existsByWebsite(request.website())) {
            throw new AuthException("A company with this domain already exists.");
        }

        Company company = Company.builder()
                .name(request.name())
                .industry(request.industry())
                .country(request.country())
                .city(request.city())
                .website(request.website())
                .size(request.size())
                .notes(request.notes())
                .createdBy(createdBy)
                .status(request.status() != null ? request.status() : CompanyStatus.PROSPECT)
                .build();

        // Build platforms and link to company
        List<CompanyPlatform> platforms = request.platforms().stream()
                .map(p -> CompanyPlatform.builder()
                        .company(company)                 // ← link to company
                        .type(p.type())
                        .name(p.name())
                        .url(p.url())
                        .domain(extractDomain(p.domain(), p.url()))
                        .bundleId(p.bundleId())
                        .appStoreUrl(p.appStoreUrl())
                        .playStoreUrl(p.playStoreUrl())
                        .description(p.description())
                        .technology(p.technology())
                        .hostingProvider(p.hostingProvider())
                        .notes(p.notes())
                        .status(CompanyPlatform.PlatformStatus.ACTIVE)
                        .build())
                .toList();

        company.setPlatforms(platforms);

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
        List<PlatformDtos.PlatformResponse> platforms = platformService.getByCompany(c.getId());
        return new CompanyResponse(
                c.getId(), c.getName(), c.getWebsite(), c.getSize(),   // ← company-level fields
                c.getIndustry(), c.getCountry(), c.getCity(),
                c.getNotes(), c.getCreatedBy(), c.getStatus(),
                platforms,
                investigationRepository.countByPlatformIdIn(
                        platforms.stream().map(PlatformDtos.PlatformResponse::id).toList()),
                contactRepository.countByCompanyId(c.getId()),
                reportRepository.countByPlatformIdIn(
                        platforms.stream().map(PlatformDtos.PlatformResponse::id).toList()),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private String extractDomain(String domain, String url) {
        if (domain != null && !domain.isBlank()) return domain;
        if (url == null || url.isBlank()) return null;
        try {
            String d = url.replaceFirst("https?://", "").split("/")[0];
            return d.startsWith("www.") ? d.substring(4) : d;
        } catch (Exception e) {
            return null;
        }
    }
}
