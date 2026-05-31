package com.migfora.sales.controller;

import com.migfora.sales.dto.CompanyDtos.*;
import com.migfora.sales.enitty.Company;
import com.migfora.sales.service.CompanyService;
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
 * @Time: 3:17 PM
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Company management")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Create a new company")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @Valid @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return companyService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Get all companies with search and filter")
    @GetMapping
    public Page<CompanyResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Company.CompanyStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return companyService.getAll(search, status, pageable);
    }

    @Operation(summary = "Get company by ID")
    @GetMapping("/{id}")
    public CompanyResponse getById(@PathVariable Long id) {
        return companyService.getById(id);
    }

    @Operation(summary = "Update company")
    @PatchMapping("/{id}")
    public CompanyResponse update(
            @PathVariable Long id,
            @RequestBody UpdateCompanyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return companyService.update(id, request, jwt.getSubject());
    }

    @Operation(summary = "Delete company")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        companyService.delete(id, jwt.getSubject());
    }
}
