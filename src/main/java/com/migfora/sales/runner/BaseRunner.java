package com.migfora.sales.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskStatus;
import com.migfora.sales.repository.ReconTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:57 PM
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseRunner implements ReconRunner {


    protected final ReconTaskRepository reconTaskRepository;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    // ── Mark task as running ──────────────────────────────────────────────────

    protected void markRunning(ReconTask task) {
        task.setStatus(ReconTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        reconTaskRepository.save(task);
    }

    // ── Mark task as completed ────────────────────────────────────────────────

    protected void markCompleted(ReconTask task, String result, String rawOutput) {
        task.setStatus(ReconTaskStatus.COMPLETED);
        task.setResult(result);
        task.setRawOutput(rawOutput);
        task.setCompletedAt(LocalDateTime.now());
        reconTaskRepository.save(task);
        log.info("Task completed | id={} type={} duration={}ms",
                task.getId(), task.getType(),
                java.time.Duration.between(task.getStartedAt(),
                        task.getCompletedAt()).toMillis());
    }

    // ── Mark task as failed ───────────────────────────────────────────────────

    protected void markFailed(ReconTask task, String error) {
        task.setStatus(ReconTaskStatus.FAILED);
        task.setErrorMessage(error);
        task.setCompletedAt(LocalDateTime.now());
        reconTaskRepository.save(task);
        log.error("Task failed | id={} type={} error={}",
                task.getId(), task.getType(), error);
    }

    // ── Execute shell command ─────────────────────────────────────────────────

    protected ProcessResult exec(List<String> command, int timeoutSeconds) {
        try {
            log.debug("Executing: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ProcessResult.timeout(
                        "Command timed out after " + timeoutSeconds + "s");
            }

            int exitCode = process.exitValue();
            return new ProcessResult(output, exitCode, null);

        } catch (Exception ex) {
            log.error("Command execution failed: {} | error: {}",
                    String.join(" ", command), ex.getMessage());
            return ProcessResult.error(ex.getMessage());
        }
    }

    // ── Safe JSON serialization ───────────────────────────────────────────────

    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ── Result wrapper ────────────────────────────────────────────────────────

    public record ProcessResult(
            String output,
            int exitCode,
            String error
    ) {
        public boolean success() {
            return error == null && exitCode == 0;
        }

        public static ProcessResult timeout(String msg) {
            return new ProcessResult("", -1, msg);
        }

        public static ProcessResult error(String msg) {
            return new ProcessResult("", -1, msg);
        }
    }
}
