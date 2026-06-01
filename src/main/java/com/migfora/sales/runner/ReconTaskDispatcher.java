package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskStatus;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 5:04 PM
 */


@Service
@Slf4j
public class ReconTaskDispatcher {

    private final Map<ReconTaskType, ReconRunner> runners;
    private final ReconTaskRepository reconTaskRepository;
    private final InvestigationContextService contextService;

    public ReconTaskDispatcher(List<ReconRunner> runnerList,
                               ReconTaskRepository reconTaskRepository,
                               InvestigationContextService contextService) {
        this.reconTaskRepository = reconTaskRepository;
        this.contextService = contextService;
        // Auto-register all runners by their supported type
        this.runners = runnerList.stream()
                .collect(Collectors.toMap(ReconRunner::supports,
                        Function.identity()));

        log.info("Registered recon runners: {}",
                runners.keySet().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
    }

    public void dispatch(Long taskId) {
        ReconTask task = reconTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException(
                        "Task not found: " + taskId));

        if (task.getStatus() != ReconTaskStatus.PENDING) {
            log.warn("Task not in PENDING state | id={} status={}",
                    taskId, task.getStatus());
            return;
        }

        ReconRunner runner = runners.get(task.getType());

        if (runner == null) {
            log.error("No runner registered for task type: {}", task.getType());
            task.setStatus(ReconTaskStatus.FAILED);
            task.setErrorMessage("No runner registered for: " + task.getType());
            reconTaskRepository.save(task);
            return;
        }

        // Load shared context for this investigation session
        InvestigationContext ctx = contextService
                .getOrCreate(task.getInvestigation().getId());

        log.info("Dispatching task | id={} type={} domain={}",
                taskId, task.getType(),
                task.getInvestigation().getDomain());

        try {
            runner.run(task, ctx);
        } catch (Exception ex) {
            log.error("Runner threw uncaught exception | taskId={} type={} error={}",
                    taskId, task.getType(), ex.getMessage());
            task.setStatus(ReconTaskStatus.FAILED);
            task.setErrorMessage("Runner error: " + ex.getMessage());
            reconTaskRepository.save(task);
        }
    }
}
