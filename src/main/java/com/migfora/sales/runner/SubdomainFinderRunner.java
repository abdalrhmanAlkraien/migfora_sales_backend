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
 * @Date: 01/06/2026
 * @Time: 2:40 AM
 */

@Component
@Slf4j
public class SubdomainFinderRunner extends BaseRunner {


    private final InvestigationContextService contextService;

    public SubdomainFinderRunner(ReconTaskRepository reconTaskRepository,
                                 InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.SUBDOMAINS;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            // Run subfinder — must be installed on server
            ProcessResult subfinder = exec(
                    List.of("subfinder", "-d", domain, "-silent"), 60
            );

            // Fallback — use crt.sh subdomain discovery if subfinder fails
            List<String> subdomains = new ArrayList<>();

            if (subfinder.success() && !subfinder.output().isBlank()) {
                subdomains = Arrays.stream(subfinder.output().split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .sorted()
                        .toList();
            } else {
                log.warn("subfinder failed or not installed | domain={} — trying crt.sh",
                        domain);
                subdomains = discoverViaCrtSh(domain);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total",      subdomains.size());
            result.put("subdomains", subdomains);
            result.put("source",     subfinder.success() ? "subfinder" : "crt.sh");

            contextService.writeSubdomains(
                    task.getInvestigation().getId(),
                    toJson(subdomains)
            );

            log.info("Subdomains found | domain={} count={}", domain, subdomains.size());
            markCompleted(task, toJson(result), subfinder.output());

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    private List<String> discoverViaCrtSh(String domain) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(
                            "https://crt.sh/?q=%25." + domain + "&output=json"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            var response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return List.of();

            var json = objectMapper.readTree(response.body());
            Set<String> subdomains = new LinkedHashSet<>();

            if (json.isArray()) {
                for (var cert : json) {
                    String name = cert.path("name_value").asText("");
                    Arrays.stream(name.split("\n"))
                            .map(String::trim)
                            .filter(s -> s.endsWith(domain) && !s.equals(domain))
                            .filter(s -> !s.startsWith("*"))
                            .forEach(subdomains::add);
                }
            }

            return new ArrayList<>(subdomains);

        } catch (Exception ex) {
            log.warn("crt.sh subdomain discovery failed | error={}", ex.getMessage());
            return List.of();
        }
    }
}
