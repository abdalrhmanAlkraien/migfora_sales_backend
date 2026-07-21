package com.migfora.sales.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/07/2026
 * @Time: 6:58 AM
 */
public class IndustryLookupDtos {

    public record CreateIndustryRequest(
            @NotBlank String name,
            String nameAr,
            String description
    ) {}

    public record UpdateIndustryRequest(
            String name,
            String nameAr,
            String description,
            Boolean active
    ) {}

    public record IndustryResponse(
            Long id,
            String name,
            String nameAr,
            String description,
            boolean active,
            LocalDateTime createdAt
    ) {}
}
