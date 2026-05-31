package com.migfora.sales.dto;

import com.migfora.sales.entity.Company.*;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
            String notes
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
            String domain,
            String industry,
            String country,
            String city,
            String website,
            String size,
            String notes,
            String createdBy,
            CompanyStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
