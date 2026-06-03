package com.migfora.sales.service;

import com.migfora.sales.dto.ValidationResult;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.dto.InvestigationDtos.ReconTaskResponse;
import com.migfora.sales.dto.PipelineDtos.*;
import com.migfora.sales.entity.*;
import com.migfora.sales.entity.ReconTask.ReconTaskStatus;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.*;
import com.migfora.sales.validator.ReconDependencyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:27 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private final ReconPipelineRepository pipelineRepository;
    private final ReconTaskRepository reconTaskRepository;
    private final InvestigationRepository investigationRepository;
    private final ReconDependencyValidator dependencyValidator;


    // ── Create pipeline template ──────────────────────────────────────────────

    @Transactional
    public PipelineResponse create(CreatePipelineRequest request, String createdBy) {
        if (pipelineRepository.existsByName(request.name())) {
            throw new AuthException("A pipeline with this name already exists.");
        }

        validatePipelineSteps(request.steps());

        ReconPipeline pipeline = ReconPipeline.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(createdBy)
                .isDefault(request.isDefault())
                .build();

        List<PipelineStep> steps = request.steps().stream()
                .map(s -> PipelineStep.builder()
                        .executionOrder(s.executionOrder())
                        .taskType(s.taskType())
                        .stopOnFailure(s.stopOnFailure() != null ? s.stopOnFailure() : true)
                        .continueOnCdn(s.continueOnCdn() != null ? s.continueOnCdn() : true)
                        .notes(s.notes())
                        .pipeline(pipeline)
                        .build())
                .collect(Collectors.toList());

        pipeline.setSteps(steps);

        ReconPipeline saved = pipelineRepository.save(pipeline);
        log.info("Pipeline created | id={} name={} steps={} by={}",
                saved.getId(), saved.getName(), steps.size(), createdBy);

        return toResponse(saved);
    }

    // ── Get all pipelines ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PipelineResponse> getAll() {
        return pipelineRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get pipeline by ID ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PipelineResponse getById(Long id) {
        return toResponse(findById(id));
    }

    // ── Delete pipeline ───────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        pipelineRepository.deleteById(id);
        log.info("Pipeline deleted | id={} by={}", id, deletedBy);
    }

    // ── Run pipeline on investigation ─────────────────────────────────────────

    public PipelineExecutionResponse run(Long investigationId,
                                         Long pipelineId,
                                         String triggeredBy) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new AuthException("Investigation not found."));

        if (investigation.getStatus() != Investigation.InvestigationStatus.OPEN) {
            throw new AuthException("Investigation session is not open.");
        }

        ReconPipeline pipeline = findById(pipelineId);

        List<PipelineStep> steps = pipeline.getSteps().stream()
                .sorted(Comparator.comparingInt(PipelineStep::getExecutionOrder))
                .collect(Collectors.toList());

        log.info("Pipeline execution started | investigationId={} pipeline={} steps={}",
                investigationId, pipeline.getName(), steps.size());

        List<PipelineTaskResult> taskResults = new ArrayList<>();
        boolean pipelineStopped = false;

        for (PipelineStep step : steps) {

            if (pipelineStopped) {
                // Pipeline was stopped by a previous step failure
                taskResults.add(new PipelineTaskResult(
                        step.getExecutionOrder(),
                        step.getTaskType(),
                        ReconTaskStatus.SKIPPED.name(),
                        false, null,
                        "Pipeline stopped by previous step failure",
                        null, true
                ));
                // Still save to DB so user can see the full picture
                saveSkippedTask(step, investigation, triggeredBy,
                        "Pipeline stopped by previous step failure");
                continue;
            }

            // Get current state of tasks for dependency check
            List<ReconTask> existingTasks = reconTaskRepository
                    .findByInvestigationId(investigationId);

            String resolvedIp = investigation.getIpAddress();

            // Validate dependency
            ValidationResult validation = dependencyValidator.validate(
                    step.getTaskType(), existingTasks, resolvedIp
            );

            PipelineTaskResult result;

            if (validation.isBlocked()) {
                // Dependency not met
                result = new PipelineTaskResult(
                        step.getExecutionOrder(),
                        step.getTaskType(),
                        ReconTaskStatus.BLOCKED.name(),
                        false, null,
                        validation.message(),
                        null, false
                );

                saveBlockedTask(step, investigation, triggeredBy, validation.message());

                if (step.getStopOnFailure()) {
                    pipelineStopped = true;
                    log.warn("Pipeline stopped | step={} reason={}",
                            step.getTaskType(), validation.message());
                }

            } else if (validation.isSkipped()) {
                // Dependency failed
                result = new PipelineTaskResult(
                        step.getExecutionOrder(),
                        step.getTaskType(),
                        ReconTaskStatus.SKIPPED.name(),
                        false, null,
                        validation.message(),
                        null, false
                );
                saveSkippedTask(step, investigation, triggeredBy, validation.message());

                if (step.getStopOnFailure()) {
                    pipelineStopped = true;
                    log.warn("Pipeline stopped | step={} reason={}",
                            step.getTaskType(), validation.message());
                }

            } else if (validation.hasCdnWarning() && !step.getContinueOnCdn()) {
                // CDN detected and step is configured to stop on CDN
                result = new PipelineTaskResult(
                        step.getExecutionOrder(),
                        step.getTaskType(),
                        ReconTaskStatus.BLOCKED.name(),
                        true,
                        validation.cdnProvider(),
                        validation.message(),
                        null, true
                );
                saveBlockedTask(step, investigation, triggeredBy, validation.message());
                pipelineStopped = true;
                log.warn("Pipeline stopped — CDN detected | step={} cdn={}",
                        step.getTaskType(), validation.cdnProvider());

            } else {
                // All good — queue the task
                result = new PipelineTaskResult(
                        step.getExecutionOrder(),
                        step.getTaskType(),
                        ReconTaskStatus.PENDING.name(),
                        validation.hasCdnWarning(),
                        validation.cdnProvider(),
                        validation.hasCdnWarning() ? validation.message() : null,
                        null, false
                );
                savePendingTask(step, investigation, triggeredBy, validation);
                log.info("Task queued | step={} order={}",
                        step.getTaskType(), step.getExecutionOrder());
            }

            taskResults.add(result);
        }

        String overallStatus = pipelineStopped ? "PARTIALLY_QUEUED" : "FULLY_QUEUED";

        log.info("Pipeline execution complete | investigationId={} pipeline={} status={}",
                investigationId, pipeline.getName(), overallStatus);

        return new PipelineExecutionResponse(
                investigationId,
                pipeline.getId(),
                pipeline.getName(),
                overallStatus,
                taskResults
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePendingTask(PipelineStep step, Investigation investigation,
                                String triggeredBy, ValidationResult validation) {
        ReconTask task = ReconTask.builder()
                .type(step.getTaskType())
                .investigation(investigation)
                .pipelineStep(step)
                .pipelineId(step.getPipeline().getId())
                .pipelineName(step.getPipeline().getName())
                .executionOrder(step.getExecutionOrder())
                .triggeredBy(triggeredBy)
                .status(ReconTaskStatus.PENDING)
                .cdnDetected(validation.hasCdnWarning())
                .cdnProvider(validation.cdnProvider())
                .blockedReason(validation.hasCdnWarning() ? validation.message() : null)
                .build();
        reconTaskRepository.save(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBlockedTask(PipelineStep step, Investigation investigation,
                                String triggeredBy, String reason) {
        ReconTask task = ReconTask.builder()
                .type(step.getTaskType())
                .investigation(investigation)
                .pipelineStep(step)
                .pipelineId(step.getPipeline().getId())
                .pipelineName(step.getPipeline().getName())
                .executionOrder(step.getExecutionOrder())
                .triggeredBy(triggeredBy)
                .status(ReconTaskStatus.BLOCKED)
                .blockedReason(reason)
                .build();
        reconTaskRepository.save(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSkippedTask(PipelineStep step, Investigation investigation,
                                String triggeredBy, String reason) {
        ReconTask task = ReconTask.builder()
                .type(step.getTaskType())
                .investigation(investigation)
                .pipelineStep(step)
                .pipelineId(step.getPipeline().getId())
                .pipelineName(step.getPipeline().getName())
                .executionOrder(step.getExecutionOrder())
                .triggeredBy(triggeredBy)
                .status(ReconTaskStatus.SKIPPED)
                .blockedReason(reason)
                .build();
        reconTaskRepository.save(task);
    }

    public PipelineValidationResponse validate(CreatePipelineRequest request) {
        List<PipelineStepValidationError> errors = new ArrayList<>();

        List<PipelineStepRequest> steps = request.steps().stream()
                .sorted(Comparator.comparingInt(PipelineStepRequest::executionOrder))
                .collect(Collectors.toList());

        // Track which tasks appear before the current one
        Set<ReconTaskType> seenTasks = new LinkedHashSet<>();

        for (PipelineStepRequest step : steps) {
            ReconTaskType type = step.taskType();
            ReconTaskType dependency = DEPENDS_ON.get(type);

            if (dependency != null && !seenTasks.contains(dependency)) {
                errors.add(new PipelineStepValidationError(
                        step.executionOrder(),
                        type,
                        type.name() + " requires " + dependency.name() +
                                " to run first — add " + dependency.name() +
                                " before order " + step.executionOrder()
                ));
            }

            seenTasks.add(type);
        }

        // Check duplicate orders
        Map<Integer, Long> orderCounts = steps.stream()
                .collect(Collectors.groupingBy(
                        PipelineStepRequest::executionOrder,
                        Collectors.counting()
                ));

        orderCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> errors.add(new PipelineStepValidationError(
                        e.getKey(), null,
                        "Duplicate execution order: " + e.getKey()
                )));

        // Check duplicate task types
        Map<ReconTaskType, Long> typeCounts = steps.stream()
                .collect(Collectors.groupingBy(
                        PipelineStepRequest::taskType,
                        Collectors.counting()
                ));

        typeCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> errors.add(new PipelineStepValidationError(
                        -1, e.getKey(),
                        "Duplicate task type: " + e.getKey().name()
                )));

        return new PipelineValidationResponse(errors.isEmpty(), errors);
    }

    // Dependency map — same as ReconDependencyValidator
    private static final Map<ReconTaskType, ReconTaskType> DEPENDS_ON = Map.ofEntries(
            Map.entry(ReconTaskType.WHOIS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SHODAN, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.CENSYS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.IP_INFO, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.TECH_STACK, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SUBDOMAINS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SSL_CERT, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.PERFORMANCE, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.HEADERS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.DNS_HISTORY, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.DIRECT_IP_SCAN, ReconTaskType.DNS_HISTORY),
            Map.entry(ReconTaskType.SUBDOMAIN_SCAN, ReconTaskType.SUBDOMAINS)
    );

    private void validatePipelineSteps(List<PipelineStepRequest> steps) {
        // Check for duplicate orders
        long distinctOrders = steps.stream()
                .map(PipelineStepRequest::executionOrder)
                .distinct().count();
        if (distinctOrders != steps.size()) {
            throw new AuthException("Pipeline steps must have unique execution orders.");
        }

        // Check for duplicate task types
        long distinctTypes = steps.stream()
                .map(PipelineStepRequest::taskType)
                .distinct().count();
        if (distinctTypes != steps.size()) {
            throw new AuthException("Pipeline steps cannot have duplicate task types.");
        }

        // Warn if DNS_LOOKUP is not first but is present
        boolean hasDns = steps.stream()
                .anyMatch(s -> s.taskType() == ReconTaskType.DNS_LOOKUP);
        boolean dnsIsFirst = steps.stream()
                .filter(s -> s.taskType() == ReconTaskType.DNS_LOOKUP)
                .anyMatch(s -> s.executionOrder() == 1);

        if (hasDns && !dnsIsFirst) {
            throw new AuthException(
                    "DNS_LOOKUP must be at execution order 1 — all other tasks depend on it."
            );
        }
    }

    private ReconPipeline findById(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new AuthException("Pipeline not found."));
    }

    private PipelineResponse toResponse(ReconPipeline p) {
        List<PipelineStepResponse> steps = p.getSteps().stream()
                .sorted(Comparator.comparingInt(PipelineStep::getExecutionOrder))
                .map(s -> new PipelineStepResponse(
                        s.getId(),
                        s.getExecutionOrder(),
                        s.getTaskType(),
                        s.getStopOnFailure(),
                        s.getContinueOnCdn(),
                        s.getNotes()
                ))
                .collect(Collectors.toList());

        return new PipelineResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.isDefault(), p.getCreatedBy(),
                steps, p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
