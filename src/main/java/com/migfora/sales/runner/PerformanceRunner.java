package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 5:02 PM
 */
@Component
@Slf4j
public class PerformanceRunner extends BaseRunner {


    private final InvestigationContextService contextService;

    private static final String CURL_FORMAT =
            "time_namelookup:%{time_namelookup}\\n" +
                    "time_connect:%{time_connect}\\n" +
                    "time_appconnect:%{time_appconnect}\\n" +
                    "time_starttransfer:%{time_starttransfer}\\n" +
                    "time_total:%{time_total}\\n" +
                    "http_code:%{http_code}\\n" +
                    "size_download:%{size_download}\\n";

    public PerformanceRunner(ReconTaskRepository reconTaskRepository,
                             InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.PERFORMANCE;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            ProcessResult result = exec(
                    List.of("curl",
                            "-w", CURL_FORMAT,
                            "-o", "/dev/null",
                            "-s",
                            "--max-time", "15",
                            "https://" + domain),
                    20
            );

            Map<String, Double> metrics = parseMetrics(result.output());

            // Convert seconds to milliseconds
            Double ttfb        = toMs(metrics.get("time_starttransfer"));
            Double dnsTime     = toMs(metrics.get("time_namelookup"));
            Double connectTime = toMs(metrics.get("time_connect"));
            Double tlsTime     = toMs(metrics.get("time_appconnect"));
            Double totalTime   = toMs(metrics.get("time_total"));

            Double httpCodeDouble = metrics.get("http_code");
            Double sizeBytesDouble = metrics.get("size_download");

            Integer httpCode = httpCodeDouble != null ? httpCodeDouble.intValue() : null;
            Long sizeBytes   = sizeBytesDouble != null ? sizeBytesDouble.longValue() : null;

            Map<String, Object> structured = new LinkedHashMap<>();
            structured.put("ttfb_ms",        ttfb);
            structured.put("dns_ms",         dnsTime);
            structured.put("connect_ms",     connectTime);
            structured.put("tls_ms",         tlsTime);
            structured.put("total_ms",       totalTime);
            structured.put("http_code",      metrics.get("http_code"));
            structured.put("size_bytes",     metrics.get("size_download"));
            structured.put("rating",         ratePerformance(ttfb));

            contextService.writePerformanceData(
                    task.getInvestigation().getId(),
                    ttfb, dnsTime, connectTime, tlsTime, totalTime,
                    httpCode, sizeBytes
            );

            markCompleted(task, toJson(structured), result.output());

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private Map<String, Double> parseMetrics(String output) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        if (output == null) return metrics;
        Pattern p = Pattern.compile("(\\w+):([\\d.]+)");
        Matcher m = p.matcher(output);
        while (m.find()) {
            try {
                metrics.put(m.group(1), Double.parseDouble(m.group(2)));
            } catch (NumberFormatException ignored) {}
        }
        return metrics;
    }

    private Double toMs(Double seconds) {
        return seconds != null ? Math.round(seconds * 1000.0) / 1.0 : null;
    }

    private String ratePerformance(Double ttfbMs) {
        if (ttfbMs == null) return "UNKNOWN";
        if (ttfbMs < 200)  return "EXCELLENT";
        if (ttfbMs < 500)  return "GOOD";
        if (ttfbMs < 1000) return "AVERAGE";
        if (ttfbMs < 2000) return "SLOW";
        return "VERY_SLOW";
    }
}
