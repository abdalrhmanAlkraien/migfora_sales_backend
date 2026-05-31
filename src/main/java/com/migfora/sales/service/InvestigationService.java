package com.migfora.sales.service;

import com.migfora.sales.dto.InvestigationDtos.*;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.Investigation.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.InvestigationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:30 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationService {


    private final InvestigationRepository investigationRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public InvestigationSummaryResponse create(CreateInvestigationRequest request,
                                               String triggeredBy) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AuthException("Company not found."));

        Investigation investigation = Investigation.builder()
                .domain(request.domain())
                .company(company)
                .triggeredBy(triggeredBy)
                .status(InvestigationStatus.PENDING)
                .build();

        Investigation saved = investigationRepository.save(investigation);
        log.info("Investigation created | id={} domain={} company={} by={}",
                saved.getId(), saved.getDomain(), company.getId(), triggeredBy);

        // TODO Phase 3 — dispatch to SQS recon queue here
        // sqsService.dispatch(saved.getId());

        return toSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<InvestigationSummaryResponse> getByCompany(Long companyId, Pageable pageable) {
        return investigationRepository.findByCompanyId(companyId, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getById(Long id) {
        return toFullResponse(findById(id));
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        investigationRepository.deleteById(id);
        log.info("Investigation deleted | id={} by={}", id, deletedBy);
    }

    private Investigation findById(Long id) {
        return investigationRepository.findById(id)
                .orElseThrow(() -> new AuthException("Investigation not found."));
    }

    private InvestigationSummaryResponse toSummaryResponse(Investigation i) {
        return new InvestigationSummaryResponse(
                i.getId(), i.getDomain(), i.getIpAddress(),
                i.getStatus(), i.getCompany().getId(),
                i.getCompany().getName(), i.getTriggeredBy(),
                i.getCreatedAt(), i.getCompletedAt()
        );
    }

    private InvestigationResponse toFullResponse(Investigation i) {
        return new InvestigationResponse(
                i.getId(), i.getDomain(), i.getIpAddress(),
                i.getStatus(), i.getCompany().getId(),
                i.getCompany().getName(), i.getTriggeredBy(),
                i.getDnsRecords(), i.getWhoisData(), i.getTechStack(),
                i.getOpenPorts(), i.getSubdomains(), i.getSslInfo(),
                i.getPerformanceMetrics(), i.getRawFindings(),
                i.getErrorMessage(), i.getCreatedAt(), i.getCompletedAt()
        );
    }
}
