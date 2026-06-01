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
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 5:03 PM
 */
@Component
@Slf4j
public class SslCertRunner extends BaseRunner {

    private final InvestigationContextService contextService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SslCertRunner(ReconTaskRepository reconTaskRepository,
                         InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.SSL_CERT;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            String encoded = URLEncoder.encode(domain, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://crt.sh/?q=" + encoded + "&output=json"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                markFailed(task, "crt.sh returned status " + response.statusCode());
                return;
            }

            JsonNode certs = objectMapper.readTree(response.body());

            List<Map<String, String>> certList = new ArrayList<>();
            String latestIssuer = null;
            String latestExpiry = null;

            if (certs.isArray()) {
                for (int i = 0; i < Math.min(certs.size(), 10); i++) {
                    JsonNode cert = certs.get(i);
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("issuer",    getField(cert, "issuer_name"));
                    entry.put("notBefore", getField(cert, "not_before"));
                    entry.put("notAfter",  getField(cert, "not_after"));
                    entry.put("commonName", getField(cert, "common_name"));
                    certList.add(entry);

                    if (i == 0) {
                        latestIssuer = getField(cert, "issuer_name");
                        latestExpiry = getField(cert, "not_after");
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalCertsFound", certs.isArray() ? certs.size() : 0);
            result.put("latestIssuer",    latestIssuer);
            result.put("latestExpiry",    latestExpiry);
            result.put("recentCerts",     certList);

            contextService.writeSslData(
                    task.getInvestigation().getId(),
                    latestIssuer,
                    latestExpiry,
                    true
            );

            markCompleted(task, toJson(result), response.body());

        } catch (Exception ex) {
            markFailed(task, "crt.sh request failed: " + ex.getMessage());
        }
    }

    private String getField(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : null;
    }
}
