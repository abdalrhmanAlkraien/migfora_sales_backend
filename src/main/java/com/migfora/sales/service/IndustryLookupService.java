package com.migfora.sales.service;

import com.migfora.sales.dto.IndustryLookupDtos.*;
import com.migfora.sales.entity.IndustryLookup;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.IndustryLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/07/2026
 * @Time: 6:59 AM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndustryLookupService {

    private final IndustryLookupRepository industryRepository;

    // ── Get all active industries (for dropdown) ──────────────────────────────

    @Transactional(readOnly = true)
    public List<IndustryResponse> getAll() {
        return industryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<IndustryResponse> getAllPageable(Pageable pageable) {
        return industryRepository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IndustryResponse getById(Long id) {
        return toResponse(findById(id));
    }

    // ── Create — admin only ───────────────────────────────────────────────────

    @Transactional
    public IndustryResponse create(CreateIndustryRequest request) {
        if (industryRepository.existsByNameIgnoreCase(request.name())) {
            throw new AuthException(
                    "Industry already exists: " + request.name());
        }

        IndustryLookup industry = IndustryLookup.builder()
                .name(request.name())
                .nameAr(request.nameAr())
                .description(request.description())
                .active(true)
                .build();

        industry = industryRepository.save(industry);
        log.info("[Industry] Created | id={} name={}", industry.getId(), industry.getName());
        return toResponse(industry);
    }

    // ── Update — admin only ───────────────────────────────────────────────────

    @Transactional
    public IndustryResponse update(Long id, UpdateIndustryRequest request) {
        IndustryLookup industry = findById(id);

        if (request.name()        != null) industry.setName(request.name());
        if (request.nameAr()      != null) industry.setNameAr(request.nameAr());
        if (request.description() != null) industry.setDescription(request.description());
        if (request.active()      != null) industry.setActive(request.active());

        industry = industryRepository.save(industry);
        log.info("[Industry] Updated | id={} name={}", id, industry.getName());
        return toResponse(industry);
    }

    // ── Delete — admin only ───────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        IndustryLookup industry = findById(id);
        industry.setActive(false);   // soft delete
        industryRepository.save(industry);
        log.info("[Industry] Deactivated | id={} name={}", id, industry.getName());
    }

    // ── Validate industry exists (used by CompanyService) ─────────────────────

    public IndustryLookup validateAndGet(String name) {
        return industryRepository.findByNameIgnoreCase(name)
                .filter(IndustryLookup::isActive)
                .orElseThrow(() -> new AuthException(
                        "Invalid industry: '" + name + "'. " +
                                "Use GET /api/v1/industries to see valid options."));
    }

    public IndustryLookup validateAndGetById(Long id) {
        return industryRepository.findById(id)
                .filter(IndustryLookup::isActive)
                .orElseThrow(() -> new AuthException(
                        "Invalid industry ID: " + id +
                                ". Use GET /api/v1/industries to see valid options."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private IndustryLookup findById(Long id) {
        return industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Industry not found: " + id));
    }

    private IndustryResponse toResponse(IndustryLookup i) {
        return new IndustryResponse(
                i.getId(),
                i.getName(),
                i.getNameAr(),
                i.getDescription(),
                i.isActive(),
                i.getCreatedAt()
        );
    }
}
