package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import com.migfora.sales.validator.ReconDependencyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:58 PM
 */

@Component
@Slf4j
public class DnsLookupRunner extends BaseRunner {


    private final InvestigationContextService contextService;
    private final ReconDependencyValidator cdnDetector;

    public DnsLookupRunner(ReconTaskRepository reconTaskRepository,
                           InvestigationContextService contextService,
                           ReconDependencyValidator cdnDetector) {
        super(reconTaskRepository);
        this.contextService = contextService;
        this.cdnDetector = cdnDetector;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.DNS_LOOKUP;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            // Run dig for A, MX, TXT, NS records
            ProcessResult digA   = exec(List.of("dig", "+short", "A",   domain), 15);
            ProcessResult digMX  = exec(List.of("dig", "+short", "MX",  domain), 15);
            ProcessResult digTXT = exec(List.of("dig", "+short", "TXT", domain), 15);
            ProcessResult digNS  = exec(List.of("dig", "+short", "NS",  domain), 15);

            // Run nslookup as secondary verification
            ProcessResult nslookup = exec(List.of("nslookup", domain), 10);

            // Extract resolved IP from A record
            String resolvedIp = extractFirstIp(digA.output());

            if (resolvedIp == null || resolvedIp.isBlank()) {
                markFailed(task, "NXDOMAIN — domain does not resolve to any IP");
                return;
            }

            // Detect CDN
            String cdnProvider = cdnDetector.detectCdn(resolvedIp);
            boolean cdnDetected = cdnProvider != null;

            // Build structured result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("resolvedIp",  resolvedIp);
            result.put("cdnDetected", cdnDetected);
            result.put("cdnProvider", cdnProvider);
            result.put("aRecords",  parseLines(digA.output()));
            result.put("mxRecords", parseLines(digMX.output()));
            result.put("txtRecords", parseLines(digTXT.output()));
            result.put("nsRecords",  parseLines(digNS.output()));

            String rawOutput = "=== dig A ===\n" + digA.output()
                    + "\n=== dig MX ===\n" + digMX.output()
                    + "\n=== dig TXT ===\n" + digTXT.output()
                    + "\n=== dig NS ===\n" + digNS.output()
                    + "\n=== nslookup ===\n" + nslookup.output();

            // Write to shared investigation context
            contextService.writeDnsData(
                    task.getInvestigation().getId(),
                    resolvedIp,
                    toJson(result.get("aRecords")),
                    toJson(result.get("nsRecords")),
                    cdnDetected,
                    cdnProvider
            );

            if (cdnDetected) {
                log.warn("CDN detected | domain={} ip={} cdn={}",
                        domain, resolvedIp, cdnProvider);
                result.put("warning",
                        "CDN detected — IP belongs to " + cdnProvider +
                                ". Real origin server IP is hidden.");
            }

            markCompleted(task, toJson(result), rawOutput);

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private String extractFirstIp(String digOutput) {
        if (digOutput == null || digOutput.isBlank()) return null;
        Pattern ipPattern = Pattern.compile(
                "\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
        Matcher matcher = ipPattern.matcher(digOutput);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> parseLines(String output) {
        if (output == null || output.isBlank()) return List.of();
        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();
    }
}
