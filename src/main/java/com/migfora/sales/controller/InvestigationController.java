package com.migfora.sales.controller;

import com.migfora.sales.dto.DashboardDtos;
import com.migfora.sales.dto.InvestigationContextDtos.*;
import com.migfora.sales.dto.InvestigationDtos.*;
import com.migfora.sales.service.DashboardService;
import com.migfora.sales.service.InvestigationContextService;
import com.migfora.sales.service.InvestigationService;
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

import java.util.List;

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
    private final InvestigationContextService investigationContextService;
    private final DashboardService dashboardService;

    @Operation(summary = "Create a new investigation session for a company")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestigationSummaryResponse create(
            @Valid @RequestBody CreateInvestigationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return investigationService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Run specific recon tasks — pick what you want")
    @PostMapping("/{id}/run")
    public List<ReconTaskResponse> runTasks(
            @PathVariable Long id,
            @Valid @RequestBody RunTasksRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return investigationService.runTasks(id, request, jwt.getSubject());
    }

    @Operation(summary = "Run all recon tasks at once")
    @PostMapping("/{id}/run-all")
    public List<ReconTaskResponse> runAll(
            @PathVariable Long id,
            @RequestBody RunAllTasksRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return investigationService.runAll(id, request, jwt.getSubject());
    }

    @Operation(summary = "Get all investigation sessions for a company")
    @GetMapping("/company/{companyId}")
    public Page<InvestigationSummaryResponse> getByCompany(
            @PathVariable Long companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return investigationService.getByCompany(companyId, pageable);
    }

    @Operation(summary = "Get full investigation with all task results")
    @GetMapping("/{id}")
    public InvestigationResponse getById(@PathVariable Long id) {
        return investigationService.getById(id);
    }

    @Operation(summary = "Get a single task result")
    @GetMapping("/{id}/tasks/{taskId}")
    public ReconTaskResponse getTask(
            @PathVariable Long id,
            @PathVariable Long taskId) {
        return investigationService.getTask(id, taskId);
    }

    @Operation(summary = "Close investigation session")
    @PatchMapping("/{id}/close")
    public InvestigationSummaryResponse close(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return investigationService.close(id, jwt.getSubject());
    }

    @Operation(summary = "Delete investigation session")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        investigationService.delete(id, jwt.getSubject());
    }

    @Operation(summary = "Get shared context for this investigation session")
    @GetMapping("/{id}/context")
    public InvestigationContextResponse getContext(@PathVariable Long id) {
        return investigationContextService.getContext(id);
    }

    @Operation(summary = "Get all available recon task types with dependencies")
    @GetMapping("/tasks/lookup")
    public List<ReconTaskLookupResponse> getTaskLookup() {
        return investigationService.getTaskLookup();
    }

    @Operation(summary = "Check if a task can run based on current investigation context")
    @PostMapping("/{id}/tasks/check")
    public ResponseEntity<TaskReadinessResponse> checkTaskReadiness(
            @PathVariable Long id,
            @RequestBody TaskReadinessRequest request) {
        return ResponseEntity.ok(investigationService.checkTaskReadiness(id, request));
    }

    @GetMapping("/{id}/tasks")
    public List<ReconTaskResponse> getAllTasks(@PathVariable Long id) {
        return investigationService.getAllTasks(id);
    }
}
