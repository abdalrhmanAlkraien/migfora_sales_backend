package com.migfora.sales.controller;

import com.migfora.sales.dto.PipelineDtos.*;
import com.migfora.sales.service.PipelineService;
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
 * @Date: 31/05/2026
 * @Time: 4:29 PM
 */
@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Recon Pipelines", description = "Build and run ordered recon pipelines")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class PipelineController {


    private final PipelineService pipelineService;

    @Operation(summary = "Create a new recon pipeline template")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PipelineResponse create(
            @Valid @RequestBody CreatePipelineRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pipelineService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Get all pipelines")
    @GetMapping
    public List<PipelineResponse> getAll() {
        return pipelineService.getAll();
    }

    @Operation(summary = "Get pipeline by ID")
    @GetMapping("/{id}")
    public PipelineResponse getById(@PathVariable Long id) {
        return pipelineService.getById(id);
    }

    @Operation(summary = "Run a saved pipeline on an investigation")
    @PostMapping("/{pipelineId}/run")
    public PipelineExecutionResponse run(
            @PathVariable Long pipelineId,
            @Valid @RequestBody RunPipelineRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pipelineService.run(request.investigationId(), pipelineId, jwt.getSubject());
    }

    @Operation(summary = "Delete pipeline")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        pipelineService.delete(id, jwt.getSubject());
    }

    @Operation(summary = "Validate pipeline steps before saving")
    @PostMapping("/validate")
    public ResponseEntity<PipelineValidationResponse> validate(
            @Valid @RequestBody CreatePipelineRequest request) {
        return ResponseEntity.ok(pipelineService.validate(request));
    }
}
