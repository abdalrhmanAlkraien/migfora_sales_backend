package com.migfora.sales.controller;

import com.migfora.sales.dto.InvestigationDtos.*;
import com.migfora.sales.service.InvestigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:31 PM
 */
@RestController
@RequestMapping("/api/v1/investigations")
@RequiredArgsConstructor
@Tag(name = "Investigations", description = "Domain/IP investigation per company")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class InvestigationController {


    private final InvestigationService investigationService;

    @Operation(summary = "Trigger a new investigation")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestigationSummaryResponse create(
            @Valid @RequestBody CreateInvestigationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return investigationService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Get all investigations for a company")
    @GetMapping("/company/{companyId}")
    public Page<InvestigationSummaryResponse> getByCompany(
            @PathVariable Long companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return investigationService.getByCompany(companyId, pageable);
    }

    @Operation(summary = "Get full investigation result by ID")
    @GetMapping("/{id}")
    public InvestigationResponse getById(@PathVariable Long id) {
        return investigationService.getById(id);
    }

    @Operation(summary = "Delete investigation")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        investigationService.delete(id, jwt.getSubject());
    }
}
