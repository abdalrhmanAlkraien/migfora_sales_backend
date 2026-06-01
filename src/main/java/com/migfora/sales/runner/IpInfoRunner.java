package com.migfora.sales.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 5:03 PM
 */

public class IpInfoRunner extends BaseRunner {


    private final InvestigationContextService contextService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public IpInfoRunner(ReconTaskRepository reconTaskRepository,
                        InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.IP_INFO;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        // Read IP from shared context written by DNS_LOOKUP
        String ip = ctx.getResolvedIp();
        markRunning(task);

        if (ip == null || ip.isBlank()) {
            markFailed(task, "No IP in context — DNS_LOOKUP must run first");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/" + ip + "/json"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                markFailed(task, "ipinfo.io returned status " + response.statusCode());
                return;
            }

            JsonNode json = objectMapper.readTree(response.body());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ip",       getField(json, "ip"));
            result.put("hostname", getField(json, "hostname"));
            result.put("city",     getField(json, "city"));
            result.put("region",   getField(json, "region"));
            result.put("country",  getField(json, "country"));
            result.put("org",      getField(json, "org"));
            result.put("postal",   getField(json, "postal"));
            result.put("timezone", getField(json, "timezone"));

            // Parse ASN from org field (format: "AS12345 Company Name")
            String org = getField(json, "org");
            String asn = org != null && org.startsWith("AS")
                    ? org.split(" ")[0] : null;

            contextService.writeIpInfo(
                    task.getInvestigation().getId(),
                    getField(json, "country"),
                    getField(json, "city"),
                    org,
                    asn,
                    null
            );

            markCompleted(task, toJson(result), response.body());

        } catch (Exception ex) {
            markFailed(task, "ipinfo.io request failed: " + ex.getMessage());
        }
    }

    private String getField(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : null;
    }
}
