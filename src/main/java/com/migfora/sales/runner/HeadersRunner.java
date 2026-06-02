package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 5:01 PM
 */
@Component
@Slf4j
public class HeadersRunner extends BaseRunner {


    private final InvestigationContextService contextService;

    public HeadersRunner(ReconTaskRepository reconTaskRepository,
                         InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.HEADERS;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            // Try HTTPS first, fall back to HTTP
            ProcessResult https = exec(
                    List.of("curl", "-I", "-L", "--max-time", "10",
                            "-s", "https://" + domain), 15);

            ProcessResult http = exec(
                    List.of("curl", "-I", "-L", "--max-time", "10",
                            "-s", "http://" + domain), 15);

            String rawHeaders = https.success() ? https.output() : http.output();
            boolean httpsWorks = https.success();

            Map<String, String> headers = parseHeaders(rawHeaders);

            // Extract key security and stack indicators
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("httpsAvailable",     httpsWorks);
            result.put("httpStatusCode",     extractStatusCode(rawHeaders));
            result.put("server",             headers.getOrDefault("Server", null));
            result.put("xPoweredBy",         headers.getOrDefault("X-Powered-By", null));
            result.put("xFrameOptions",      headers.getOrDefault("X-Frame-Options", null));
            result.put("contentSecurityPolicy", headers.getOrDefault("Content-Security-Policy", null));
            result.put("strictTransportSecurity", headers.getOrDefault("Strict-Transport-Security", null));
            result.put("setCookie",          headers.getOrDefault("Set-Cookie", null));
            result.put("via",                headers.getOrDefault("Via", null));
            result.put("cfRay",              headers.getOrDefault("CF-RAY", null));
            result.put("allHeaders",         headers);

            // Write to context — server header helps TECH_STACK runner
            contextService.writeHeadersData(
                    task.getInvestigation().getId(),
                    (String) result.get("server"),
                    (String) result.get("xPoweredBy"),
                    (Integer) result.get("httpStatusCode"),
                    (Boolean) result.get("httpsAvailable"),
                    toJson(result.get("allHeaders"))
            );

            markCompleted(task, toJson(result), rawHeaders);

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private Map<String, String> parseHeaders(String raw) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (raw == null) return headers;
        for (String line : raw.split("\n")) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    headers.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return headers;
    }

    private Integer extractStatusCode(String raw) {
        if (raw == null) return null;
        for (String line : raw.split("\n")) {
            if (line.startsWith("HTTP/")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    try { return Integer.parseInt(parts[1]); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }
}
