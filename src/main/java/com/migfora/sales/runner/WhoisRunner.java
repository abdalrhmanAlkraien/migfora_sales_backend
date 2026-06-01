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
        String ip = ctx.getResolvedIp();
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            ProcessResult domainWhois = exec(List.of("whois", domain), 20);
            ProcessResult ipWhois = ip != null
                    ? exec(List.of("whois", ip), 20)
                    : ProcessResult.error("No IP available from DNS_LOOKUP");

            Map<String, Object> domainParsed = parseWhois(domainWhois.output());
            Map<String, Object> ipParsed = ip != null
                    ? parseWhois(ipWhois.output()) : Map.of();

            // Extract key fields for context
            String registrantName  = getField(domainParsed, "Registrant Name");
            String registrar       = getField(domainParsed, "whois");
            String domainStatus    = getField(domainParsed, "status");
            String createdDate     = getField(domainParsed, "created");
            String updatedDate     = getField(domainParsed, "changed");
            String dnssec          = getField(domainParsed, "DNSSEC");
            String ipOrg           = getField(ipParsed, "OrgName");
            String ipCountry       = getField(ipParsed, "Country");
            String ipCity          = getField(ipParsed, "City");

            // Extract nameservers
            List<String> nameservers = domainParsed.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("Name Server"))
                    .map(e -> e.getValue().toString().trim())
                    .toList();

            // Build structured response
            Map<String, Object> structured = new LinkedHashMap<>();
            structured.put("domain",         domain);
            structured.put("ip",             ip);
            structured.put("registrantName", registrantName);
            structured.put("registrar",      registrar);
            structured.put("status",         domainStatus);
            structured.put("createdDate",    createdDate);
            structured.put("updatedDate",    updatedDate);
            structured.put("nameservers",    nameservers);
            structured.put("dnssec",         dnssec);
            structured.put("ipOrg",          ipOrg);
            structured.put("ipCountry",      ipCountry);
            structured.put("ipCity",         ipCity);
            structured.put("domainWhois",    domainParsed);
            structured.put("ipWhois",        ipParsed);

            String rawOutput = "=== whois domain ===\n" + domainWhois.output()
                    + "\n=== whois IP ===\n"
                    + (ip != null ? ipWhois.output() : "skipped — no IP");

            // Write to InvestigationContext
            contextService.writeWhoisData(
                    task.getInvestigation().getId(),
                    toJson(structured)
            );

            markCompleted(task, toJson(structured), rawOutput);

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private Map<String, Object> parseWhois(String raw) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        if (raw == null) return parsed;

        for (String line : raw.split("\n")) {
            if (line.startsWith("%") || line.startsWith("#") || line.isBlank()) continue;
            if (!line.contains(":")) continue;

            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String key   = parts[0].trim();
                String value = parts[1].trim();
                if (!value.isBlank()) {
                    // Keep first occurrence only — putIfAbsent
                    parsed.putIfAbsent(key, value);
                }
            }
        }
        return parsed;
    }

    private String getField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
