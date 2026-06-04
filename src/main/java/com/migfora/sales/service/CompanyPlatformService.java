package com.migfora.sales.service;

import com.migfora.sales.dto.PlatformDtos.*;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.CompanyPlatform;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:03 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyPlatformService {


    private final CompanyPlatformRepository platformRepository;
    private final CompanyRepository         companyRepository;
    private final InvestigationRepository   investigationRepository;
    private final ReportRepository          reportRepository;

    @Transactional
    public PlatformResponse create(Long companyId,
                                   CreatePlatformRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found: " + companyId));

        CompanyPlatform platform = CompanyPlatform.builder()
                .company(company)
                .type(request.type())
                .name(request.name())
                .url(request.url())
                .domain(extractDomain(request.domain(), request.url()))
                .bundleId(request.bundleId())
                .appStoreUrl(request.appStoreUrl())
                .playStoreUrl(request.playStoreUrl())
                .description(request.description())
                .technology(request.technology())
                .hostingProvider(request.hostingProvider())
                .notes(request.notes())
                .status(CompanyPlatform.PlatformStatus.ACTIVE)
                .build();

        platform = platformRepository.save(platform);
        log.info("Platform created | id={} company={} type={} name={}",
                platform.getId(), companyId, platform.getType(), platform.getName());
        return toResponse(platform);
    }

    @Transactional(readOnly = true)
    public List<PlatformResponse> getByCompany(Long companyId) {
        return platformRepository
                .findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public PlatformResponse update(Long id, UpdatePlatformRequest request) {
        CompanyPlatform platform = findById(id);

        if (request.type()            != null) platform.setType(request.type());
        if (request.name()            != null) platform.setName(request.name());
        if (request.url()             != null) platform.setUrl(request.url());
        if (request.domain()          != null) platform.setDomain(request.domain());
        if (request.bundleId()        != null) platform.setBundleId(request.bundleId());
        if (request.appStoreUrl()     != null) platform.setAppStoreUrl(request.appStoreUrl());
        if (request.playStoreUrl()    != null) platform.setPlayStoreUrl(request.playStoreUrl());
        if (request.description()     != null) platform.setDescription(request.description());
        if (request.status()          != null) platform.setStatus(request.status());
        if (request.technology()      != null) platform.setTechnology(request.technology());
        if (request.hostingProvider() != null) platform.setHostingProvider(request.hostingProvider());
        if (request.notes()           != null) platform.setNotes(request.notes());

        platform = platformRepository.save(platform);
        log.info("Platform updated | id={}", id);
        return toResponse(platform);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        platformRepository.deleteById(id);
        log.info("Platform deleted | id={}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CompanyPlatform findById(Long id) {
        return platformRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Platform not found: " + id));
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

    private PlatformResponse toResponse(CompanyPlatform p) {
        long investigationsCount = investigationRepository
                .countByPlatformId(p.getId());

        long reportsCount = reportRepository
                .countByPlatformId(p.getId());

        return new PlatformResponse(
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
                investigationsCount,
                reportsCount,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
