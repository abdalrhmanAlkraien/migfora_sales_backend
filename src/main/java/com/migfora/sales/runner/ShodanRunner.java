package com.migfora.sales.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 01/06/2026
 * @Time: 2:41 AM
 */

@Component
@Slf4j
public class ShodanRunner extends BaseRunner{


    private final InvestigationContextService contextService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${recon.shodan.api-key:}")
    private String shodanApiKey;

    public ShodanRunner(ReconTaskRepository reconTaskRepository,
                        InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.SHODAN;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String ip = ctx.getResolvedIp();
        markRunning(task);

        if (shodanApiKey == null || shodanApiKey.isBlank()) {
            markFailed(task, "Shodan API key not configured — add SHODAN_API_KEY to .env");
            return;
        }

        if (ip == null || ip.isBlank()) {
            markFailed(task, "No IP in context — DNS_LOOKUP must run first");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.shodan.io/shodan/host/"
                            + ip + "?key=" + shodanApiKey))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                markFailed(task, "IP not found in Shodan database");
                return;
            }
            if (response.statusCode() != 200) {
                markFailed(task, "Shodan API error: " + response.statusCode());
                return;
            }

            JsonNode json = objectMapper.readTree(response.body());

            // Extract open ports
            List<Integer> ports = new ArrayList<>();
            if (json.has("ports") && json.get("ports").isArray()) {
                json.get("ports").forEach(p -> ports.add(p.asInt()));
            }

            // Extract services/banners
            Map<String, String> services = new LinkedHashMap<>();
            if (json.has("data") && json.get("data").isArray()) {
                for (JsonNode service : json.get("data")) {
                    int port = service.path("port").asInt();
                    String product = service.path("product").asText("");
                    if (!product.isBlank()) {
                        services.put(String.valueOf(port), product);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ip",           ip);
            result.put("organization", json.path("org").asText(null));
            result.put("isp",          json.path("isp").asText(null));
            result.put("country",      json.path("country_name").asText(null));
            result.put("city",         json.path("city").asText(null));
            result.put("openPorts",    ports);
            result.put("services",     services);
            result.put("hostnames",    json.path("hostnames"));
            result.put("tags",         json.path("tags"));
            result.put("lastUpdate",   json.path("last_update").asText(null));

            // Write to context
            contextService.writeOpenPorts(
                    task.getInvestigation().getId(),
                    toJson(ports),
                    toJson(services)
            );

            log.info("Shodan scan complete | ip={} openPorts={}", ip, ports.size());
            markCompleted(task, toJson(result), response.body());

        } catch (Exception ex) {
            markFailed(task, "Shodan request failed: " + ex.getMessage());
        }
    }
}
