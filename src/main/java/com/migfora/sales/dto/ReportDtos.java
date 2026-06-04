package com.migfora.sales.dto;

import com.migfora.sales.entity.Report;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:32 PM
 */
@NoArgsConstructor
public class ReportDtos {


    public record CreateReportRequest(
            @NotNull Long platformId,        // ← was companyId
            @NotNull Long investigationId,
            @NotNull Report.ReportType type
    ) {}

    public record ReportResponse(
            Long id,
            Report.ReportType type,
            Report.ReportStatus status,
            Long companyId,
            String companyName,
            Long platformId,                 // ← new
            String platformName,             // ← new
            Long investigationId,
            String title,
            String summary,
            String content,
            String aiProvider,
            String aiModel,
            Integer tokenCount,
            String s3Key,
            String downloadUrl,
            String generatedBy,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime generatedAt
    ) {}

    public record ReportListResponse(
            Long id,
            Report.ReportType type,
            Report.ReportStatus status,
            Long companyId,
            String companyName,
            Long platformId,                 // ← new
            String platformName,             // ← new
            Long investigationId,
            String title,
            String summary,
            String aiProvider,
            String downloadUrl,
            LocalDateTime createdAt,
            LocalDateTime generatedAt
    ) {}
}
