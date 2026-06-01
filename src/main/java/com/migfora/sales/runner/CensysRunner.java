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
public class CensysRunner extends BaseRunner {


    private final InvestigationContextService contextService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${recon.censys.api-id:}")
    private String censysApiId;

    @Value("${recon.censys.api-secret:}")
    private String censysApiSecret;

    public CensysRunner(ReconTaskRepository reconTaskRepository,
                        InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.CENSYS;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String ip = ctx.getResolvedIp();
        markRunning(task);

        if (censysApiId == null || censysApiId.isBlank()) {
            markFailed(task,
                    "Censys API credentials not configured — add CENSYS_API_ID to .env");
            return;
        }

        if (ip == null || ip.isBlank()) {
            markFailed(task, "No IP in context — DNS_LOOKUP must run first");
            return;
        }

        try {
            // Basic auth: apiId:apiSecret base64 encoded
            String credentials = Base64.getEncoder().encodeToString(
                    (censysApiId + ":" + censysApiSecret).getBytes()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://search.censys.io/api/v2/hosts/" + ip))
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                markFailed(task, "IP not found in Censys database");
                return;
            }
            if (response.statusCode() != 200) {
                markFailed(task, "Censys API error: " + response.statusCode());
                return;
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode result = json.path("result");

            // Extract services
            List<Map<String, Object>> services = new ArrayList<>();
            if (result.has("services") && result.get("services").isArray()) {
                for (JsonNode svc : result.get("services")) {
                    Map<String, Object> service = new LinkedHashMap<>();
                    service.put("port",            svc.path("port").asInt());
                    service.put("serviceName",     svc.path("service_name").asText(null));
                    service.put("transportProto",  svc.path("transport_protocol").asText(null));
                    service.put("banner",          svc.path("banner").asText(null));
                    services.add(service);
                }
            }

            Map<String, Object> structured = new LinkedHashMap<>();
            structured.put("ip",            ip);
            structured.put("asn",           result.path("autonomous_system").path("asn").asText(null));
            structured.put("asnName",       result.path("autonomous_system").path("name").asText(null));
            structured.put("country",       result.path("location").path("country").asText(null));
            structured.put("city",          result.path("location").path("city").asText(null));
            structured.put("services",      services);
            structured.put("lastUpdatedAt", result.path("last_updated_at").asText(null));

            log.info("Censys scan complete | ip={} services={}", ip, services.size());
            markCompleted(task, toJson(structured), response.body());

        } catch (Exception ex) {
            markFailed(task, "Censys request failed: " + ex.getMessage());
        }
    }
}
