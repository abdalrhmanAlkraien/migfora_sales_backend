package com.migfora.sales.dto;

import com.migfora.sales.entity.Investigation.InvestigationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:28 PM
 */
@NoArgsConstructor
public class InvestigationDtos {


    public record CreateInvestigationRequest(
            @NotNull Long companyId,
            @NotBlank String domain
    ) {}

    public record InvestigationResponse(
            Long id,
            String domain,
            String ipAddress,
            InvestigationStatus status,
            Long companyId,
            String companyName,
            String triggeredBy,
            String dnsRecords,
            String whoisData,
            String techStack,
            String openPorts,
            String subdomains,
            String sslInfo,
            String performanceMetrics,
            String rawFindings,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {}

    public record InvestigationSummaryResponse(
            Long id,
            String domain,
            String ipAddress,
            InvestigationStatus status,
            Long companyId,
            String companyName,
            String triggeredBy,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {}
}
