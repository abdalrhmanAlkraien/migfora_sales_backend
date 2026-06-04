package com.migfora.sales.controller;

import com.migfora.sales.dto.PlatformDtos.*;
import com.migfora.sales.service.CompanyPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:06 PM
 */

@RestController
@RequiredArgsConstructor
@Tag(name = "Company Platforms",
        description = "Manage digital platforms per company")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class CompanyPlatformController {

    private final CompanyPlatformService platformService;

    @Operation(summary = "Add platform to company")
    @PostMapping("/api/v1/companies/{companyId}/platforms")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformResponse create(
            @PathVariable Long companyId,
            @Valid @RequestBody CreatePlatformRequest request) {
        return platformService.create(companyId, request);
    }

    @Operation(summary = "List all platforms for a company")
    @GetMapping("/api/v1/companies/{companyId}/platforms")
    public List<PlatformResponse> getByCompany(
            @PathVariable Long companyId) {
        return platformService.getByCompany(companyId);
    }

    @Operation(summary = "Get platform by ID")
    @GetMapping("/api/v1/platforms/{id}")
    public PlatformResponse getById(@PathVariable Long id) {
        return platformService.getById(id);
    }

    @Operation(summary = "Update platform")
    @PatchMapping("/api/v1/platforms/{id}")
    public PlatformResponse update(
            @PathVariable Long id,
            @RequestBody UpdatePlatformRequest request) {
        return platformService.update(id, request);
    }

    @Operation(summary = "Delete platform")
    @DeleteMapping("/api/v1/platforms/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        platformService.delete(id);
    }
}
