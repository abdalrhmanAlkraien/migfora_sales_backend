package com.migfora.sales.dto;

import com.migfora.sales.entity.Investigation.InvestigationStatus;
import com.migfora.sales.entity.ReconTask.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:28 PM
 */
@NoArgsConstructor
public class InvestigationDtos {


    // ── Requests ──────────────────────────────────────────────────────────────

    public record CreateInvestigationRequest(
            @NotNull Long companyId,
            @NotBlank String domain
    ) {}

    public record RunTasksRequest(
            @NotEmpty List<ReconTaskType> tasks
    ) {}

    public record RunAllTasksRequest(
            boolean includeShogan,
            boolean includeCensys,
            boolean includeIpInfo
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record ReconTaskResponse(
            Long id,
            ReconTaskType type,
            ReconTaskStatus status,
            Object result,
//            Object rawOutput,
            String errorMessage,
            String triggeredBy,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {}

    public record InvestigationResponse(
            Long id,
            String domain,
            String ipAddress,
            InvestigationStatus status,
            Long companyId,
            String companyName,
            String triggeredBy,
            List<ReconTaskResponse> tasks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record InvestigationSummaryResponse(
            Long id,
            String domain,
            String ipAddress,
            InvestigationStatus status,
            Long companyId,
            String companyName,
            String triggeredBy,
            int totalTasks,
            int completedTasks,
            int failedTasks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ReconTaskLookupResponse(
            ReconTaskType type,
            String description,
            String tool,
            ReconTaskType dependsOn,       // null means no dependency
            boolean requiresIp,            // needs IP from DNS_LOOKUP
            boolean requiresDomain,        // needs domain reachability
            boolean externalApi            // needs API key
    ) {}

    public record TaskReadinessRequest(
            @NotNull ReconTaskType taskType
    ) {}

    public record TaskReadinessResponse(
            ReconTaskType taskType,
            boolean canRun,
            String reason,
            boolean cdnWarning,
            String cdnProvider,
            String resolvedIp
    ) {}

    public record SubdomainScanResult(
            Integer totalScanned,
            Integer flaggedCount,
            List<SubdomainDetail> flagged,
            List<SubdomainDetail> subdomains
    ) {}

    public record SubdomainDetail(
            String subdomain,
            String type,
            String ip,
            Boolean cdnDetected,
            Boolean sameIpAsMain,
            Integer statusCode,
            String server,
            String poweredBy,
            Boolean https,
            Boolean reachable,
            String cors,
            String allowedMethods,
            List<String> flags
    ) {}
}
