package com.migfora.sales.dto;

import com.migfora.sales.entity.CompanyPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:03 PM
 */
public class PlatformDtos {


    public record CreatePlatformRequest(
            @NotNull CompanyPlatform.PlatformType type,
            @NotBlank String name,
            String url,
            String domain,
            String bundleId,
            String appStoreUrl,
            String playStoreUrl,
            String description,
            String technology,
            String hostingProvider,
            String notes
    ) {}

    public record UpdatePlatformRequest(
            CompanyPlatform.PlatformType type,
            String name,
            String url,
            String domain,
            String bundleId,
            String appStoreUrl,
            String playStoreUrl,
            String description,
            CompanyPlatform.PlatformStatus status,
            String technology,
            String hostingProvider,
            String notes
    ) {}

    public record PlatformResponse(
            Long id,
            Long companyId,
            String companyName,
            CompanyPlatform.PlatformType type,
            String name,
            String url,
            String domain,
            String bundleId,
            String appStoreUrl,
            String playStoreUrl,
            String description,
            CompanyPlatform.PlatformStatus status,
            String technology,
            String hostingProvider,
            String notes,
            long investigationsCount,
            long reportsCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
