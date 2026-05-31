package com.migfora.sales.dto;

import com.migfora.sales.entity.Report.ReportStatus;
import com.migfora.sales.entity.Report.ReportType;
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
            @NotNull Long companyId,
            @NotNull Long investigationId,
            @NotNull ReportType type
    ) {}

    public record ReportResponse(
            Long id,
            ReportType type,
            ReportStatus status,
            Long companyId,
            String companyName,
            Long investigationId,
            String s3Key,
            String downloadUrl,
            String generatedBy,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime generatedAt
    ) {}
}
