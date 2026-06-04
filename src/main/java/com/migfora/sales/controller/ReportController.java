package com.migfora.sales.controller;

import com.migfora.sales.dto.ReportDtos.*;
import com.migfora.sales.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:35 PM
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation per investigation")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Generate a report (TECHNICAL_OVERVIEW or SALES_ROADMAP)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return reportService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Get all reports for a company")
    @GetMapping("/platform/{platformId}")
    public Page<ReportListResponse> getByPlatform(
            @PathVariable Long platformId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return reportService.getByPlatform(platformId, pageable);
    }

    @Operation(summary = "Get all reports for a company")
    @GetMapping("/company/{companyId}")
    public Page<ReportListResponse> getByCompany(
            @PathVariable Long companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return reportService.getByCompany(companyId, pageable);
    }

    @Operation(summary = "Get report by ID with presigned S3 download URL")
    @GetMapping("/{id}")
    public ReportResponse getById(@PathVariable Long id) {
        return reportService.getById(id);
    }

    @Operation(summary = "Delete report and remove from S3")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        reportService.delete(id, jwt.getSubject());
    }
}
