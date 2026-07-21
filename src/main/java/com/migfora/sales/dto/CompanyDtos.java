package com.migfora.sales.dto;

import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Company.CompanyStatus;
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
            @NotNull Long industryId,
            String country,
            String city,
            String website,
            String size,
            CompanyStatus status,
            Company.LeadSource leadSource,    // ← new
            String linkedinUrl,       // ← new

            @NotNull
            @Size(min = 1, message = "At least one platform is required")
            List<PlatformDtos.CreatePlatformRequest> platforms,
            List<String> notes

    ) {
    }

    public record UpdateCompanyRequest(
            String name,
            String domain,
            Long industryId,
            String country,
            String city,
            String website,
            String size,
            String notes,
            CompanyStatus status,
            Company.LeadSource leadSource,    // ← new
            String linkedinUrl      // ← new
    ) {
    }

    public record CompanyResponse(
            Long id,
            String name,
            String domain,
            String website,
            String linkedinUrl,
            String size,
            Long industryId,              // ← new
            String industryName,          // ← new
            String country,
            String city,
            String notes,
            String createdBy,
            Company.CompanyStatus status,
            Company.LeadSource leadSource,
            List<PlatformDtos.PlatformResponse> platforms,
            long investigationsCount,
            long contactsCount,
            long reportsCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
