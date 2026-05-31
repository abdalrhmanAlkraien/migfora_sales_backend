package com.migfora.sales.dto;

import com.migfora.sales.entity.ReconTask.ReconTaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:26 PM
 */
@NoArgsConstructor
public class PipelineDtos {

    // ── Requests ──────────────────────────────────────────────────────────────

    public record CreatePipelineRequest(
            @NotBlank String name,
            String description,
            boolean isDefault,
            @NotEmpty List<PipelineStepRequest> steps
    ) {}

    public record PipelineStepRequest(
            @NotNull @Min(1) int executionOrder,
            @NotNull ReconTaskType taskType,
            boolean stopOnFailure,
            boolean continueOnCdn,
            String notes
    ) {}

    public record RunPipelineRequest(
            @NotNull Long pipelineId
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record PipelineStepResponse(
            Long id,
            int executionOrder,
            ReconTaskType taskType,
            boolean stopOnFailure,
            boolean continueOnCdn,
            String notes
    ) {}

    public record PipelineResponse(
            Long id,
            String name,
            String description,
            boolean isDefault,
            String createdBy,
            List<PipelineStepResponse> steps,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record PipelineExecutionResponse(
            Long investigationId,
            Long pipelineId,
            String pipelineName,
            String status,
            List<PipelineTaskResult> taskResults
    ) {}

    public record PipelineTaskResult(
            int executionOrder,
            ReconTaskType taskType,
            String taskStatus,
            boolean cdnDetected,
            String cdnProvider,
            String blockedReason,
            String errorMessage,
            boolean pipelineStopped
    ) {}
}
