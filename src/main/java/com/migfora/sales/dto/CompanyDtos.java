package com.migfora.sales.dto;

import com.migfora.sales.entity.Company.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:13 PM
 */
@NoArgsConstructor
public class CompanyDtos {

    public record CreateCompanyRequest(
            @NotBlank String name,
            String domain,
            String industry,
            String country,
            String city,
            String website,
            String size,
            String notes,
            CompanyStatus status,

            @NotNull
            @Size(min = 1, message = "At least one platform is required")
            List<PlatformDtos.CreatePlatformRequest> platforms

    ) {}

    public record UpdateCompanyRequest(
            String name,
            String domain,
            String industry,
            String country,
            String city,
            String website,
            String size,
            String notes,
            CompanyStatus status
    ) {}

    public record CompanyResponse(
            Long id,
            String name,
            String industry,
            String country,
            String city,
            String website,
            String size,
            String notes,
            String createdBy,
            CompanyStatus status,
            List<PlatformDtos.PlatformResponse> platforms,       // ← included in response
            long investigationsCount,
            long contactsCount,
            long reportsCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
