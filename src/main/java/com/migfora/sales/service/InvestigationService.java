package com.migfora.sales.service;

import com.migfora.sales.dto.InvestigationDtos.*;
import com.migfora.sales.dto.ValidationResult;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.Investigation.*;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.InvestigationRepository;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.runner.ReconTaskDispatcher;
import com.migfora.sales.validator.ReconDependencyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:30 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationService {


    private final InvestigationRepository investigationRepository;
    private final CompanyRepository companyRepository;
    private final ReconTaskRepository reconTaskRepository;
    private final ReconDependencyValidator dependencyValidator;
    private final ReconTaskDispatcher dispatcher;

    @Qualifier("reconTaskExecutor")
    private final TaskExecutor taskExecutor;

    // ── Create investigation session ──────────────────────────────────────────

    // ── Create investigation session ──────────────────────────────────────────

    @Transactional
    public InvestigationSummaryResponse create(CreateInvestigationRequest request,
                                               String triggeredBy) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AuthException("Company not found."));

        Investigation investigation = Investigation.builder()
                .domain(request.domain())
                .company(company)
                .triggeredBy(triggeredBy)
                .status(InvestigationStatus.OPEN)
                .build();

        Investigation saved = investigationRepository.save(investigation);

        log.info("Investigation session created | id={} domain={} company={} by={}",
                saved.getId(), saved.getDomain(), company.getId(), triggeredBy);

        return toSummaryResponse(saved);
    }

    // ── Run specific tasks ────────────────────────────────────────────────────

    // No @Transactional here — each task saves independently
    public List<ReconTaskResponse> runTasks(Long investigationId,
                                            RunTasksRequest request,
                                            String triggeredBy) {
        Investigation investigation = findById(investigationId);

        if (investigation.getStatus() != InvestigationStatus.OPEN) {
            throw new AuthException("Investigation session is not open.");
        }

        List<ReconTask> existingTasks = reconTaskRepository
                .findByInvestigationId(investigationId);

        String resolvedIp = investigation.getIpAddress();

        List<ReconTaskResponse> results = new java.util.ArrayList<>();

        List<ReconTaskType> ordered = new java.util.ArrayList<>(request.tasks());
        ordered.sort((a, b) -> {
            if (a == ReconTaskType.DNS_LOOKUP) return -1;
            if (b == ReconTaskType.DNS_LOOKUP) return 1;
            return 0;
        });

        for (ReconTaskType type : ordered) {
            ReconTaskResponse response = createAndSaveTask(
                    type, investigation, existingTasks, resolvedIp, triggeredBy
            );
            results.add(response);

            // Refresh existing tasks so next iteration sees current state
            existingTasks = reconTaskRepository.findByInvestigationId(investigationId);
        }

        log.info("Tasks processed | investigationId={} total={} pending={} blocked={} skipped={}",
                investigationId,
                results.size(),
                results.stream().filter(t -> t.status() == ReconTaskStatus.PENDING).count(),
                results.stream().filter(t -> t.status() == ReconTaskStatus.BLOCKED).count(),
                results.stream().filter(t -> t.status() == ReconTaskStatus.SKIPPED).count()
        );

        return results;
    }

    // ── Each task in its own transaction ──────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconTaskResponse createAndSaveTask(ReconTaskType type,
                                               Investigation investigation,
                                               List<ReconTask> existingTasks,
                                               String resolvedIp,
                                               String triggeredBy) {
        ValidationResult validation = dependencyValidator.validate(
                type, existingTasks, resolvedIp
        );

        // Remove old result for this type if re-running
        reconTaskRepository
                .findByInvestigationIdAndType(investigation.getId(), type)
                .ifPresent(reconTaskRepository::delete);

        ReconTask task = ReconTask.builder()
                .type(type)
                .investigation(investigation)
                .triggeredBy(triggeredBy)
                .build();

        if (validation.isBlocked()) {
            task.setStatus(ReconTaskStatus.BLOCKED);
            task.setBlockedReason(validation.message());
            log.warn("Task blocked | type={} reason={}", type, validation.message());

        } else if (validation.isSkipped()) {
            task.setStatus(ReconTaskStatus.SKIPPED);
            task.setBlockedReason(validation.message());
            log.warn("Task skipped | type={} reason={}", type, validation.message());

        } else {
            task.setStatus(ReconTaskStatus.PENDING);

            if (validation.hasCdnWarning()) {
                task.setCdnDetected(true);
                task.setCdnProvider(validation.cdnProvider());
                task.setBlockedReason(validation.message());
                log.warn("CDN warning | type={} cdn={}", type, validation.cdnProvider());
            }
        }

        ReconTask saved = reconTaskRepository.save(task);

        // Dispatch PENDING tasks to runner asynchronously
        if (saved.getStatus() == ReconTaskStatus.PENDING) {
            Long taskId = saved.getId();
            taskExecutor.execute(() -> dispatcher.dispatch(taskId));
            log.info("Task dispatched | id={} type={}", taskId, type);
        }

        return toTaskResponse(saved);
    }

    // ── Run all tasks ─────────────────────────────────────────────────────────

    public List<ReconTaskResponse> runAll(Long investigationId,
                                          RunAllTasksRequest request,
                                          String triggeredBy) {
        List<ReconTaskType> allTasks = new java.util.ArrayList<>(List.of(
                ReconTaskType.DNS_LOOKUP,
                ReconTaskType.WHOIS,
                ReconTaskType.TECH_STACK,
                ReconTaskType.SUBDOMAINS,
                ReconTaskType.SSL_CERT,
                ReconTaskType.PERFORMANCE,
                ReconTaskType.HEADERS
        ));

        if (request.includeShogan())  allTasks.add(ReconTaskType.SHODAN);
        if (request.includeCensys())  allTasks.add(ReconTaskType.CENSYS);
        if (request.includeIpInfo())  allTasks.add(ReconTaskType.IP_INFO);

        return runTasks(investigationId, new RunTasksRequest(allTasks), triggeredBy);
    }

    // ── Get all investigations ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvestigationSummaryResponse> getByCompany(Long companyId,
                                                           Pageable pageable) {
        return investigationRepository.findByCompanyId(companyId, pageable)
                .map(this::toSummaryResponse);
    }

    // ── Get full investigation with all tasks ─────────────────────────────────

    @Transactional(readOnly = true)
    public InvestigationResponse getById(Long id) {
        Investigation investigation = findById(id);
        List<ReconTask> tasks = reconTaskRepository.findByInvestigationId(id);
        return toFullResponse(investigation, tasks);
    }

    // ── Get single task result ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReconTaskResponse getTask(Long investigationId, Long taskId) {
        ReconTask task = reconTaskRepository.findById(taskId)
                .orElseThrow(() -> new AuthException("Task not found."));
        return toTaskResponse(task);
    }

    // ── Close investigation session ───────────────────────────────────────────

    @Transactional
    public InvestigationSummaryResponse close(Long id, String closedBy) {
        Investigation investigation = findById(id);
        investigation.setStatus(InvestigationStatus.CLOSED);
        log.info("Investigation closed | id={} by={}", id, closedBy);
        return toSummaryResponse(investigationRepository.save(investigation));
    }

    // ── Delete investigation ──────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        investigationRepository.deleteById(id);
        log.info("Investigation deleted | id={} by={}", id, deletedBy);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Investigation findById(Long id) {
        return investigationRepository.findById(id)
                .orElseThrow(() -> new AuthException("Investigation not found."));
    }

    private ReconTaskResponse toTaskResponse(ReconTask t) {
        return new ReconTaskResponse(
                t.getId(), t.getType(), t.getStatus(),
                t.getResult(), t.getRawOutput(),
                t.getErrorMessage(), t.getTriggeredBy(),
                t.getCreatedAt(), t.getStartedAt(), t.getCompletedAt()
        );
    }

    private InvestigationSummaryResponse toSummaryResponse(Investigation i) {
        List<ReconTask> tasks = reconTaskRepository.findByInvestigationId(i.getId());
        long completed = tasks.stream()
                .filter(t -> t.getStatus() == ReconTaskStatus.COMPLETED).count();
        long failed = tasks.stream()
                .filter(t -> t.getStatus() == ReconTaskStatus.FAILED).count();

        return new InvestigationSummaryResponse(
                i.getId(), i.getDomain(), i.getIpAddress(),
                i.getStatus(), i.getCompany().getId(),
                i.getCompany().getName(), i.getTriggeredBy(),
                tasks.size(), (int) completed, (int) failed,
                i.getCreatedAt(), i.getUpdatedAt()
        );
    }

    private InvestigationResponse toFullResponse(Investigation i,
                                                 List<ReconTask> tasks) {
        return new InvestigationResponse(
                i.getId(), i.getDomain(), i.getIpAddress(),
                i.getStatus(), i.getCompany().getId(),
                i.getCompany().getName(), i.getTriggeredBy(),
                tasks.stream().map(this::toTaskResponse).collect(Collectors.toList()),
                i.getCreatedAt(), i.getUpdatedAt()
        );
    }
}
