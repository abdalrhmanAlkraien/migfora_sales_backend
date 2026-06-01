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
 * @Time: 4:59 PM
 */
@Component
@Slf4j
public class WhoisRunner extends BaseRunner {


    private final InvestigationContextService contextService;

    public WhoisRunner(ReconTaskRepository reconTaskRepository,
                       InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.WHOIS;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        // Read IP from shared context — written by DNS_LOOKUP
        String ip = ctx.getResolvedIp();
        String domain = task.getInvestigation().getDomain();

        markRunning(task);

        try {
            // Run whois on both domain and IP
            ProcessResult domainWhois = exec(List.of("whois", domain), 20);
            ProcessResult ipWhois     = ip != null
                    ? exec(List.of("whois", ip), 20)
                    : ProcessResult.error("No IP available from DNS_LOOKUP");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("domain",       domain);
            result.put("ip",           ip);
            result.put("domainWhois",  parseWhois(domainWhois.output()));
            result.put("ipWhois",      ip != null
                    ? parseWhois(ipWhois.output()) : "No IP resolved");

            String rawOutput = "=== whois domain ===\n" + domainWhois.output()
                    + "\n=== whois IP ===\n"
                    + (ip != null ? ipWhois.output() : "skipped — no IP");

            // Write to context
            contextService.writeWhoisData(
                    task.getInvestigation().getId(),
                    toJson(result)
            );

            markCompleted(task, toJson(result), rawOutput);

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private Map<String, String> parseWhois(String raw) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (raw == null) return parsed;

        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (line.contains(":") && !line.startsWith("%")
                    && !line.startsWith("#")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key   = parts[0].trim();
                    String value = parts[1].trim();
                    if (!value.isBlank()) {
                        parsed.putIfAbsent(key, value);
                    }
                }
            }
        }
        return parsed;
    }
}
