package com.migfora.sales.service;

import com.migfora.sales.dto.CompanyDtos.*;
import com.migfora.sales.dto.PlatformDtos;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Company.*;
import com.migfora.sales.entity.CompanyPlatform;
import com.migfora.sales.entity.IndustryLookup;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyPlatformRepository;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.ContactRepository;
import com.migfora.sales.repository.IndustryLookupRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

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
    private final NoteService noteService;
    private final IndustryLookupService industryLookupService;
    private final CompanyPlatformRepository platformRepository;
    private final IndustryLookupRepository industryLookupRepository;
    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, String createdBy) {
        if (request.website() != null && companyRepository.existsByWebsite(request.website())) {
            throw new AuthException("A company with this domain already exists.");
        }

        IndustryLookup industry = industryLookupService
                .validateAndGetById(request.industryId());

        Company company = Company.builder()
                .name(request.name())
                .industry(industry)
                .country(request.country())
                .city(request.city())
                .website(request.website())
                .size(request.size())
                .createdBy(createdBy)
                .status(request.status() != null ? request.status() : CompanyStatus.PROSPECT)
                .linkedinUrl(request.linkedinUrl())      // ← new
                .leadSource(request.leadSource())         // ← new

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

        if (request.notes() != null && !request.notes().isEmpty()) {
            noteService.createBulk(saved, request.notes(), createdBy);  // ← saved has ID now
        }

        log.info("Company created | id={} name={} by={}", saved.getId(), saved.getName(), createdBy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAll(String search,
                                        CompanyStatus status,
                                        List<Long> industryIds,
                                        Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Company> page = companyRepository
                .search(search, statusStr, industryIds, unsorted);

        if (page.isEmpty()) return page.map(c -> toResponse(c));

        List<Long> companyIds = page.getContent()
                .stream().map(Company::getId).toList();

        // Platforms
        Map<Long, List<CompanyPlatform>> platformsByCompany =
                platformRepository.findByCompanyIdInWithCompany(companyIds)
                        .stream()
                        .collect(Collectors.groupingBy(p -> p.getCompany().getId()));

        List<Long> platformIds = platformsByCompany.values().stream()
                .flatMap(List::stream)
                .map(CompanyPlatform::getId)
                .toList();

        // Investigation counts per platform
        Map<Long, Long> investigationByPlatform = platformIds.isEmpty()
                ? Map.of()
                : investigationRepository.countGroupByPlatformId(platformIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()));

        // Investigation counts per company (sum of platforms)
        Map<Long, Long> investigationCounts = platformsByCompany.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToLong(p -> investigationByPlatform
                                        .getOrDefault(p.getId(), 0L))
                                .sum()
                ));

        // Contact counts per company
        Map<Long, Long> contactCounts =
                contactRepository.countGroupByCompanyId(companyIds)
                        .stream()
                        .collect(Collectors.toMap(
                                r -> ((Number) r[0]).longValue(),
                                r -> ((Number) r[1]).longValue()));

        // Report counts per platform
        Map<Long, Long> reportByPlatform = platformIds.isEmpty()
                ? Map.of()
                : reportRepository.countGroupByPlatformId(platformIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()));

        // Report counts per company (sum of platforms)
        Map<Long, Long> reportCounts = platformsByCompany.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToLong(p -> reportByPlatform
                                        .getOrDefault(p.getId(), 0L))
                                .sum()
                ));

        // Industries in bulk
        List<Long> industryIdList = page.getContent().stream()
                .filter(c -> c.getIndustry() != null)
                .map(c -> c.getIndustry().getId())
                .distinct()
                .toList();

        Map<Long, IndustryLookup> industriesById = industryIdList.isEmpty()
                ? Map.of()
                : industryLookupRepository.findAllById(industryIdList)
                .stream()
                .collect(Collectors.toMap(IndustryLookup::getId, i -> i));

        return page.map(c -> toBulkResponse(
                c,
                platformsByCompany.getOrDefault(c.getId(), List.of()),
                investigationCounts.getOrDefault(c.getId(), 0L),
                contactCounts.getOrDefault(c.getId(), 0L),
                reportCounts.getOrDefault(c.getId(), 0L),
                investigationByPlatform,
                reportByPlatform,
                industriesById
        ));
    }

    private CompanyResponse toBulkResponse(Company c,
                                           List<CompanyPlatform> platforms,
                                           long investigationsCount,
                                           long contactsCount,
                                           long reportsCount,
                                           Map<Long, Long> investigationByPlatform,
                                           Map<Long, Long> reportByPlatform,
                                           Map<Long, IndustryLookup> industriesById) {

        List<PlatformDtos.PlatformResponse> platformResponses = platforms.stream()
                .map(p -> new PlatformDtos.PlatformResponse(
                        p.getId(),
                        p.getCompany().getId(),
                        p.getCompany().getName(),
                        p.getType(),
                        p.getName(),
                        p.getUrl(),
                        p.getDomain(),
                        p.getBundleId(),
                        p.getAppStoreUrl(),
                        p.getPlayStoreUrl(),
                        p.getDescription(),
                        p.getStatus(),
                        p.getTechnology(),
                        p.getHostingProvider(),
                        p.getNotes(),
                        investigationByPlatform.getOrDefault(p.getId(), 0L),
                        reportByPlatform.getOrDefault(p.getId(), 0L),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                ))
                .toList();

        IndustryLookup industry = c.getIndustry() != null
                ? industriesById.get(c.getIndustry().getId())
                : null;

        return new CompanyResponse(
                c.getId(), c.getName(), c.getDomain(), c.getWebsite(),
                c.getLinkedinUrl(), c.getSize(),
                industry != null ? industry.getId()   : null,
                industry != null ? industry.getName() : null,
                c.getCountry(), c.getCity(), c.getNotes(),
                c.getCreatedBy(), c.getStatus(), c.getLeadSource(),
                platformResponses,
                investigationsCount,
                contactsCount,
                reportsCount,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CompanyResponse update(Long id, UpdateCompanyRequest request, String updatedBy) {
        Company company = findById(id);

        IndustryLookup industry = industryLookupService
                .validateAndGetById(request.industryId());

        if (request.name()     != null) company.setName(request.name());
        if (request.industryId() != null) company.setIndustry(industry);
        if (request.country()  != null) company.setCountry(request.country());
        if (request.city()     != null) company.setCity(request.city());
        if (request.website()  != null) company.setWebsite(request.website());
        if (request.size()     != null) company.setSize(request.size());
        if (request.notes()    != null) company.setNotes(request.notes());
        if (request.status()   != null) company.setStatus(request.status());
        if (request.linkedinUrl()   != null) company.setLinkedinUrl(request.linkedinUrl());
        if (request.leadSource()   != null) company.setLeadSource(request.leadSource());

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
                c.getId(),
                c.getName(),
                c.getDomain(),
                c.getWebsite(),
                c.getLinkedinUrl(),
                c.getSize(),
                c.getIndustry() != null ? c.getIndustry().getId()   : null,
                c.getIndustry() != null ? c.getIndustry().getName() : null,
                c.getCountry(),
                c.getCity(),
                c.getNotes(),
                c.getCreatedBy(),
                c.getStatus(),
                c.getLeadSource(),
                platforms,
                investigationRepository.countByPlatformIdIn(
                        platforms.stream().map(PlatformDtos.PlatformResponse::id).toList()),
                contactRepository.countByCompanyId(c.getId()),
                reportRepository.countByPlatformIdIn(
                        platforms.stream().map(PlatformDtos.PlatformResponse::id).toList()),
                c.getCreatedAt(),
                c.getUpdatedAt()
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
