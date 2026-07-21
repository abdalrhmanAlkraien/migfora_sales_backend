package com.migfora.sales.controller;

import com.migfora.sales.dto.IndustryLookupDtos.*;
import com.migfora.sales.service.IndustryLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/07/2026
 * @Time: 7:00 AM
 */
@RestController
@RequestMapping("/api/v1/industries")
@RequiredArgsConstructor
@Tag(name = "Industries", description = "Industry lookup for company classification")
public class IndustryLookupController {

    private final IndustryLookupService industryService;

    @Operation(summary = "Get all active industries — for dropdown")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public List<IndustryResponse> getAll() {
        return industryService.getAll();
    }

    @Operation(summary = "Get industry by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public IndustryResponse getById(@PathVariable Long id) {
        return industryService.getById(id);
    }

    @Operation(summary = "Create industry — admin only")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public IndustryResponse create(
            @Valid @RequestBody CreateIndustryRequest request) {
        return industryService.create(request);
    }

    @Operation(summary = "Update industry — admin only")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public IndustryResponse update(
            @PathVariable Long id,
            @RequestBody UpdateIndustryRequest request) {
        return industryService.update(id, request);
    }

    @Operation(summary = "Deactivate industry — admin only")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public void delete(@PathVariable Long id) {
        industryService.delete(id);
    }

    @Operation(summary = "Get all active industries — paginated")
    @GetMapping("/pageable")
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public Page<IndustryResponse> getAllPageable(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return industryService.getAllPageable(pageable);
    }
}