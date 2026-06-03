package com.migfora.sales.service;

import com.migfora.sales.config.AiConfig;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.repository.InvestigationRepository;
import com.migfora.sales.dto.ReportDtos.*;
import com.migfora.sales.entity.Report;
import com.migfora.sales.entity.Report.ReportStatus;
import com.migfora.sales.repository.ReportRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:34 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {


    private final ReportRepository             reportRepository;
    private final CompanyRepository            companyRepository;
    private final InvestigationRepository      investigationRepository;
    private final InvestigationContextService  contextService;
    private final LlmService                   llmService;
    private final PdfGenerationService         pdfService;
    private final S3Service                    s3Service;
    private final AiConfig aiConfig;

    @Value("${aws.s3.bucket:migfora-reports}")
    private String s3Bucket;

    @Transactional
    public ReportResponse create(CreateReportRequest request, String generatedBy) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AuthException("Company not found."));

        Investigation investigation = investigationRepository
                .findById(request.investigationId())
                .orElseThrow(() -> new AuthException("Investigation not found."));

        // Create report record as GENERATING
        Report report = Report.builder()
                .type(request.type())
                .company(company)
                .investigation(investigation)
                .generatedBy(generatedBy)
                .status(Report.ReportStatus.GENERATING)
                .language("en")
                .build();

        report = reportRepository.save(report);
        log.info("[Report] Created | id={} type={}", report.getId(), report.getType());
        InvestigationContext ctx = contextService.get(investigation.getId());
        String domain = investigation.getDomain();

        Long reportId = report.getId();
        CompletableFuture.runAsync(() -> generateAsync(reportId, request, domain, ctx));

        return toResponse(report, null);
    }

    @Transactional
    public void generateAsync(Long reportId, CreateReportRequest request,
                              String domain, InvestigationContext ctx) {

        Report report = reportRepository.findById(reportId).orElseThrow();
        try {
            report.setStatus(Report.ReportStatus.GENERATING);
            reportRepository.save(report);

            String prompt = buildPrompt(request.type(), domain, ctx);
            String content = llmService.generate(prompt);
            String summary = extractSummary(content);
            String title = buildTitle(request.type(), domain);

            byte[] pdfBytes = pdfService.generatePdf(title, content, domain);
            String s3Key = buildS3Key(report.getCompany().getId(),
                    report.getInvestigation().getId(), reportId, request.type());
            s3Service.uploadPdf(pdfBytes, s3Key);

            report.setTitle(title);
            report.setContent(content);
            report.setSummary(summary);
            report.setAiProvider(aiConfig.getProvider());
            report.setAiModel(aiConfig.getQubridModel());
            report.setTokenCount(prompt.length() / 4 + content.length() / 4);
            report.setS3Key(s3Key);
            report.setS3Bucket(s3Bucket);
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setGeneratedAt(LocalDateTime.now());
            reportRepository.save(report);

            log.info("[Report] Async complete | id={}", reportId);

        } catch (Exception ex) {
            log.error("[Report] Async failed | id={} error={}", reportId, ex.getMessage());
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(ex.getMessage());
            reportRepository.save(report);
        }
    }

    @Transactional(readOnly = true)
    public Page<ReportListResponse> getByCompany(Long companyId, Pageable pageable) {
        return reportRepository.findByCompanyId(companyId, pageable)
                .map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        Report report = findById(id);
        String downloadUrl = null;
        if (report.getS3Key() != null &&
                report.getStatus() == Report.ReportStatus.COMPLETED) {
            downloadUrl = s3Service.generatePresignedUrl(report.getS3Key());
        }
        return toResponse(report, downloadUrl);
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        Report report = findById(id);
        // Delete from S3 first
        if (report.getS3Key() != null) {
            s3Service.delete(report.getS3Key());
        }
        reportRepository.deleteById(id);
        log.info("[Report] Deleted | id={} by={}", id, deletedBy);
    }

    @PostConstruct
    @Transactional
    public void recoverStuckReports() {
        List<Report> stuck = reportRepository
                .findByStatus(Report.ReportStatus.GENERATING);

        if (!stuck.isEmpty()) {
            log.warn("[Report] Found {} stuck GENERATING reports on startup — marking FAILED",
                    stuck.size());
            stuck.forEach(r -> {
                r.setStatus(Report.ReportStatus.FAILED);
                r.setErrorMessage(
                        "Report generation interrupted — application restarted. Please retry.");
            });
            reportRepository.saveAll(stuck);
        }

        // Also recover PENDING reports that were never picked up
        List<Report> pending = reportRepository
                .findByStatus(Report.ReportStatus.PENDING);
        if (!pending.isEmpty()) {
            log.warn("[Report] Found {} stuck PENDING reports on startup — marking FAILED",
                    pending.size());
            pending.forEach(r -> {
                r.setStatus(Report.ReportStatus.FAILED);
                r.setErrorMessage(
                        "Report generation interrupted — application restarted. Please retry.");
            });
            reportRepository.saveAll(pending);
        }
    }

    // ── S3 key builder ────────────────────────────────────────────────────────

    private String buildS3Key(Long companyId, Long investigationId,
                              Long reportId, Report.ReportType type) {
        return String.format("reports/company-%d/investigation-%d/%d-%s-%s.pdf",
                companyId, investigationId, reportId,
                type.name().toLowerCase(),
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
    }

    // ── Context summary ───────────────────────────────────────────────────────

    private String buildContextSummary(InvestigationContext ctx, String domain) {
        StringBuilder sb = new StringBuilder();
        sb.append("Domain: ").append(domain).append("\n\n");

        if (ctx.getResolvedIp() != null) {
            sb.append("=== DNS & Network ===\n");
            sb.append("Resolved IP: ").append(ctx.getResolvedIp()).append("\n");
            if (ctx.getRealIp() != null)
                sb.append("Real IP: ").append(ctx.getRealIp()).append("\n");
            sb.append("CDN: ").append(ctx.isCdnDetected() ?
                    ctx.getCdnProvider() : "None").append("\n\n");
        }

        if (ctx.getIpOrg() != null) {
            sb.append("=== Hosting ===\n");
            sb.append("Provider: ").append(ctx.getIpOrg()).append("\n");
            sb.append("Location: ").append(ctx.getIpCity())
                    .append(", ").append(ctx.getIpCountry()).append("\n\n");
        }

        if (ctx.getServerHeader() != null) {
            sb.append("=== Web Server ===\n");
            sb.append("Server: ").append(ctx.getServerHeader()).append("\n");
            if (ctx.getPoweredByHeader() != null)
                sb.append("Powered By: ").append(ctx.getPoweredByHeader()).append("\n");
            sb.append("HTTPS: ").append(ctx.isHttpsRedirect()).append("\n\n");
        }

        if (ctx.getSslIssuer() != null) {
            sb.append("=== SSL ===\n");
            sb.append("Issuer: ").append(ctx.getSslIssuer()).append("\n");
            sb.append("Expiry: ").append(ctx.getSslExpiry()).append("\n\n");
        }

        if (ctx.getTtfb() != null) {
            sb.append("=== Performance ===\n");
            sb.append("TTFB: ").append(ctx.getTtfb()).append("ms\n");
            sb.append("Total: ").append(ctx.getTotalTime()).append("ms\n\n");
        }

        if (ctx.getTechDetected() != null) {
            sb.append("=== Tech Stack ===\n");
            sb.append("Detected: ").append(ctx.getTechDetected()).append("\n");
            sb.append("Inferred: ").append(ctx.getTechInferred()).append("\n\n");
        }

        if (ctx.getRealServer() != null) {
            sb.append("=== Direct Scan ===\n");
            sb.append("Real Server: ").append(ctx.getRealServer()).append("\n");
            if (ctx.getRealRuntime() != null)
                sb.append("Runtime: ").append(ctx.getRealRuntime()).append("\n");
            sb.append("Load Balanced: ").append(ctx.isLoadBalanced()).append("\n");
            sb.append("Orchestration: ").append(ctx.getOrchestration()).append("\n\n");
        }

        if (ctx.getSubdomainScanData() != null) {
            sb.append("=== Subdomain Analysis ===\n");
            sb.append(ctx.getSubdomainScanData()).append("\n\n");
        }

        if (ctx.getWhoisData() != null) {
            sb.append("=== WHOIS ===\n");
            sb.append(ctx.getWhoisData()).append("\n\n");
        }

        return sb.toString();
    }

    // ── Prompt builders ───────────────────────────────────────────────────────

    private String buildSystemPrompt(Report.ReportType type) {
        return switch (type) {
            case TECHNICAL_OVERVIEW -> """
                You are a senior cloud architect at MIGFORA, a technology services 
                company specializing in AWS cloud engineering for Saudi Arabia and GCC.
                Generate a professional technical overview report in clear English 
                using markdown. Be factual, insightful, and specific to the data provided.
                """;
            case SALES_ROADMAP -> """
                You are a senior solutions architect at MIGFORA, a technology services 
                company specializing in AWS, DevOps, and software development for 
                Saudi Arabia and GCC clients.
                Generate a strategic sales roadmap in clear English using markdown.
                Identify specific opportunities, reference actual technologies found,
                and give the sales engineer concrete talking points.
                """;
        };
    }

    private String buildUserPrompt(Report.ReportType type,
                                   String domain,
                                   String context) {
        return switch (type) {
            case TECHNICAL_OVERVIEW -> """
                Analyze the following reconnaissance data for %s and generate a 
                Technical Overview Report with these sections:
                
                1. **Executive Summary**
                2. **Infrastructure Overview** (hosting, cloud, CDN, architecture)
                3. **Technology Stack** (frontend, backend, frameworks)
                4. **Security Assessment** (SSL, headers, exposed services)
                5. **Performance Analysis** (TTFB, load times)
                6. **Network Architecture** (IPs, subdomains, CDN)
                7. **Engineering Maturity**
                8. **Key Findings**
                
                Data:
                %s
                """.formatted(domain, context);

            case SALES_ROADMAP -> """
                Based on the reconnaissance data for %s, generate a Sales Roadmap 
                identifying opportunities for MIGFORA with these sections:
                
                1. **Opportunity Summary** (pain points, why they need help)
                2. **AWS Migration Roadmap** (what to migrate and how)
                3. **Infrastructure Improvements** (HA, scaling, redundancy)
                4. **Security Hardening** (specific gaps and fixes)
                5. **Observability & Monitoring** (what's missing)
                6. **DevOps & CI/CD Improvements**
                7. **Performance Optimization**
                8. **Estimated Engagement** (Small/Medium/Large, timeline)
                9. **Sales Talking Points** (3-5 specific points for first meeting)
                10. **Proposed MIGFORA Services**
                
                Data:
                %s
                """.formatted(domain, context);
        };
    }

    private String buildPrompt(Report.ReportType type,
                               String domain,
                               InvestigationContext ctx) {
        StringBuilder prompt = new StringBuilder();

        // ── Role + Task ───────────────────────────────────────────────────────────
        switch (type) {
            case TECHNICAL_OVERVIEW -> prompt.append("""
                You are a senior cloud architect at MIGFORA, a technology services
                company specializing in AWS cloud engineering for Saudi Arabia and GCC.
                
                Analyze the reconnaissance data below for %s and generate a professional
                Technical Overview Report in clear English using markdown.
                Be factual, specific, and draw conclusions from patterns in the data.
                
                Required sections:
                1. **Executive Summary** (3-5 sentences)
                2. **Infrastructure Overview** (hosting, cloud provider, CDN, architecture)
                3. **Technology Stack** (frontend, backend, frameworks, database hints)
                4. **Security Assessment** (SSL, headers, exposed services, vulnerabilities)
                5. **Performance Analysis** (TTFB, load times, optimization opportunities)
                6. **Network Architecture** (IPs, subdomains, CDN setup)
                7. **Engineering Maturity** (based on tech choices, security, architecture)
                8. **Key Findings** (bullet points of most important discoveries)
                
                """.formatted(domain));

            case SALES_ROADMAP -> prompt.append("""
                You are a senior solutions architect at MIGFORA, a technology services
                company specializing in AWS, DevOps, and software development for
                Saudi Arabia and GCC clients.
                
                Based on the reconnaissance data below for %s, generate a strategic
                Sales Roadmap identifying specific opportunities for MIGFORA.
                Reference actual technologies found and give concrete talking points.
                
                Required sections:
                1. **Opportunity Summary** (pain points, why they need help)
                2. **AWS Migration Roadmap** (what to migrate and how)
                3. **Infrastructure Improvements** (HA, scaling, redundancy)
                4. **Security Hardening** (specific gaps and fixes)
                5. **Observability & Monitoring** (what's missing, what to add)
                6. **DevOps & CI/CD Improvements**
                7. **Performance Optimization**
                8. **Estimated Engagement** (Small/Medium/Large, timeline estimate)
                9. **Sales Talking Points** (3-5 specific points for first meeting)
                10. **Proposed MIGFORA Services**
                
                """.formatted(domain));
        }

        // ── Reconnaissance Data ───────────────────────────────────────────────────
        prompt.append("## Reconnaissance Data\n\n");
        prompt.append("**Domain:** ").append(domain).append("\n\n");

        // DNS & Network
        if (ctx.getResolvedIp() != null) {
            prompt.append("**DNS & Network:**\n");
            prompt.append("- Resolved IP: ").append(ctx.getResolvedIp()).append("\n");
            if (ctx.getRealIp() != null)
                prompt.append("- Real IP (behind CDN): ").append(ctx.getRealIp()).append("\n");
            prompt.append("- CDN: ").append(ctx.isCdnDetected()
                    ? ctx.getCdnProvider() : "None").append("\n");
            if (ctx.getNameservers() != null)
                prompt.append("- Nameservers: ").append(ctx.getNameservers()).append("\n");
            prompt.append("\n");
        }

        // Hosting
        if (ctx.getIpOrg() != null) {
            prompt.append("**Hosting & Location:**\n");
            prompt.append("- Provider: ").append(ctx.getIpOrg()).append("\n");
            prompt.append("- Location: ").append(ctx.getIpCity())
                    .append(", ").append(ctx.getIpCountry()).append("\n");
            if (ctx.getIpAsn() != null)
                prompt.append("- ASN: ").append(ctx.getIpAsn()).append("\n");
            prompt.append("\n");
        }

        // Web Server
        if (ctx.getServerHeader() != null) {
            prompt.append("**Web Server:**\n");
            prompt.append("- Server: ").append(ctx.getServerHeader()).append("\n");
            if (ctx.getPoweredByHeader() != null)
                prompt.append("- Powered By: ").append(ctx.getPoweredByHeader()).append("\n");
            prompt.append("- HTTPS: ").append(ctx.isHttpsRedirect()).append("\n");
            if (ctx.getHttpStatusCode() != null)
                prompt.append("- Status: ").append(ctx.getHttpStatusCode()).append("\n");
            prompt.append("\n");
        }

        // SSL
        if (ctx.getSslIssuer() != null) {
            prompt.append("**SSL Certificate:**\n");
            prompt.append("- Issuer: ").append(ctx.getSslIssuer()).append("\n");
            prompt.append("- Expiry: ").append(ctx.getSslExpiry()).append("\n");
            prompt.append("- Valid: ").append(ctx.isSslValid()).append("\n");
            prompt.append("\n");
        }

        // Performance
        if (ctx.getTtfb() != null) {
            prompt.append("**Performance:**\n");
            prompt.append("- TTFB: ").append(ctx.getTtfb()).append("ms\n");
            prompt.append("- Total Load: ").append(ctx.getTotalTime()).append("ms\n");
            prompt.append("- DNS Resolve: ").append(ctx.getDnsResolveTime()).append("ms\n");
            prompt.append("- TLS Time: ").append(ctx.getTlsTime()).append("ms\n");
            if (ctx.getPerformanceSizeBytes() != null)
                prompt.append("- Page Size: ").append(ctx.getPerformanceSizeBytes())
                        .append(" bytes\n");
            prompt.append("\n");
        }

        // Tech Stack
        if (ctx.getTechDetected() != null) {
            prompt.append("**Technology Stack:**\n");
            prompt.append("- Detected: ").append(ctx.getTechDetected()).append("\n");
            prompt.append("- Inferred: ").append(ctx.getTechInferred()).append("\n");
            prompt.append("\n");
        }

        // Direct Scan
        if (ctx.getRealServer() != null) {
            prompt.append("**Direct IP Scan (bypassing CDN):**\n");
            prompt.append("- Real Server: ").append(ctx.getRealServer()).append("\n");
            if (ctx.getRealPoweredBy() != null)
                prompt.append("- Real Powered By: ").append(ctx.getRealPoweredBy()).append("\n");
            if (ctx.getRealRuntime() != null)
                prompt.append("- Runtime: ").append(ctx.getRealRuntime()).append("\n");
            prompt.append("- Load Balanced: ").append(ctx.isLoadBalanced()).append("\n");
            prompt.append("- Orchestration: ").append(ctx.getOrchestration()).append("\n");
            prompt.append("\n");
        }

        // Subdomains
        if (ctx.getSubdomains() != null) {
            prompt.append("**Subdomains:**\n");
            prompt.append(ctx.getSubdomains()).append("\n\n");
        }

        // Subdomain Analysis
        if (ctx.getSubdomainScanData() != null) {
            prompt.append("**Subdomain Analysis:**\n");
            prompt.append(ctx.getSubdomainScanData()).append("\n\n");
        }

        // WHOIS
        if (ctx.getWhoisData() != null) {
            prompt.append("**WHOIS:**\n");
            prompt.append(ctx.getWhoisData()).append("\n\n");
        }

        return prompt.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildTitle(Report.ReportType type, String domain) {
        return switch (type) {
            case TECHNICAL_OVERVIEW -> "Technical Overview — " + domain;
            case SALES_ROADMAP      -> "Sales Roadmap — " + domain;
        };
    }

    private String extractSummary(String content) {
        String[] lines = content.split("\n");
        StringBuilder summary = new StringBuilder();
        boolean foundHeader = false;
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#")) { foundHeader = true; continue; }
            if (!foundHeader || line.isBlank()) continue;
            if (line.startsWith("#")) break;
            summary.append(line).append(" ");
            if (++count >= 3) break;
        }
        return summary.toString().trim();
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
                r.getTitle(), r.getSummary(), r.getContent(),
                r.getAiProvider(), r.getAiModel(), r.getTokenCount(),
                r.getS3Key(), downloadUrl, r.getGeneratedBy(),
                r.getErrorMessage(), r.getCreatedAt(), r.getGeneratedAt()
        );
    }

    private ReportListResponse toListResponse(Report r) {
        String downloadUrl = null;
        if (r.getS3Key() != null &&
                r.getStatus() == Report.ReportStatus.COMPLETED) {
            try {
                downloadUrl = s3Service.generatePresignedUrl(r.getS3Key());
            } catch (Exception ignored) {}
        }
        return new ReportListResponse(
                r.getId(), r.getType(), r.getStatus(),
                r.getCompany().getId(), r.getCompany().getName(),
                r.getInvestigation() != null ? r.getInvestigation().getId() : null,
                r.getTitle(), r.getSummary(),
                r.getAiProvider(), downloadUrl,
                r.getCreatedAt(), r.getGeneratedAt()
        );
    }
}
