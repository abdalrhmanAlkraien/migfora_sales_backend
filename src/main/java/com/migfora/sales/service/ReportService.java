package com.migfora.sales.service;

import com.migfora.sales.entity.Company;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.repository.InvestigationRepository;
import com.migfora.sales.dto.ReportDtos.*;
import com.migfora.sales.entity.Report;
import com.migfora.sales.entity.Report.ReportStatus;
import com.migfora.sales.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:34 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {


    private final ReportRepository reportRepository;
    private final CompanyRepository companyRepository;
    private final InvestigationRepository investigationRepository;

    @Transactional
    public ReportResponse create(CreateReportRequest request, String generatedBy) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AuthException("Company not found."));

        Investigation investigation = investigationRepository
                .findById(request.investigationId())
                .orElseThrow(() -> new AuthException("Investigation not found."));

        Report report = Report.builder()
                .type(request.type())
                .company(company)
                .investigation(investigation)
                .generatedBy(generatedBy)
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report created | id={} type={} company={} by={}",
                saved.getId(), saved.getType(), company.getId(), generatedBy);

        // TODO Phase 4 — trigger Bedrock RAG pipeline here
        // bedrockReportService.generate(saved.getId());

        return toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getByCompany(Long companyId, Pageable pageable) {
        return reportRepository.findByCompanyId(companyId, pageable)
                .map(r -> toResponse(r, null));
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        Report report = findById(id);
        // TODO Phase 4 — generate presigned S3 URL here
        String downloadUrl = report.getS3Key() != null
                ? "/api/v1/reports/" + id + "/download"
                : null;
        return toResponse(report, downloadUrl);
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        reportRepository.deleteById(id);
        log.info("Report deleted | id={} by={}", id, deletedBy);
    }

    private Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new AuthException("Report not found."));
    }

    private ReportResponse toResponse(Report r, String downloadUrl) {
        return new ReportResponse(
                r.getId(), r.getType(), r.getStatus(),
                r.getCompany().getId(), r.getCompany().getName(),
                r.getInvestigation() != null ? r.getInvestigation().getId() : null,
                r.getS3Key(), downloadUrl, r.getGeneratedBy(),
                r.getErrorMessage(), r.getCreatedAt(), r.getGeneratedAt()
        );
    }
}
