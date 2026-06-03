package com.migfora.sales.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class DnsHistoryRunner extends BaseRunner {

    private final InvestigationContextService contextService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Value("${recon.whoisxml.api-key:}")
    private String whoisXmlApiKey;

    public DnsHistoryRunner(ReconTaskRepository reconTaskRepository,
                            InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.DNS_HISTORY;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            List<Map<String, String>> allHistory = new ArrayList<>();
            String realIp = null;

            // ── Source 1: HackerTarget hostsearch ────────────────────────
            log.info("[DNS_HISTORY] Source 1: HackerTarget hostsearch | domain={}", domain);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.hackertarget.com/hostsearch/?q=" + domain))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                log.info("[Source1] status={} body=\n{}", response.statusCode(), response.body());

                if (response.statusCode() == 200 && !response.body().contains("error")) {
                    List<Map<String, String>> records = new ArrayList<>();
                    for (String line : response.body().split("\n")) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String ip = parts[1].trim();
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("ip",     ip);
                            entry.put("host",   parts[0].trim());
                            entry.put("source", "hackertarget-hostsearch");
                            records.add(entry);
                        }
                    }
                    allHistory.addAll(records);
                    log.info("[Source1] Parsed {} records", records.size());
                } else {
                    log.warn("[Source1] Skipped — error or bad status");
                }
            } catch (Exception ex) {
                log.warn("[Source1] Failed | error={}", ex.getMessage());
            }

            // ── Source 2: HackerTarget DNS lookup (current A records) ────
            log.info("[DNS_HISTORY] Source 2: HackerTarget dnslookup | domain={}", domain);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.hackertarget.com/dnslookup/?q=" + domain))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                log.info("[Source2] status={} body=\n{}", response.statusCode(), response.body());

                if (response.statusCode() == 200 && !response.body().contains("error")) {
                    List<Map<String, String>> records = new ArrayList<>();
                    for (String line : response.body().split("\n")) {
                        if (line.startsWith("A :")) {
                            String ip = line.replace("A :", "").trim();
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("ip",     ip);
                            entry.put("type",   "A");
                            entry.put("note",   "current record");
                            entry.put("source", "hackertarget-dnslookup");
                            records.add(entry);
                        }
                    }
                    allHistory.addAll(records);
                    log.info("[Source2] Parsed {} A records", records.size());
                } else {
                    log.warn("[Source2] Skipped — error or bad status");
                }
            } catch (Exception ex) {
                log.warn("[Source2] Failed | error={}", ex.getMessage());
            }


            // ── Source 3: SecurityTrails (free, no key for basic) ────────────
            log.info("[DNS_HISTORY] Source 3: SecurityTrails | domain={}", domain);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://securitytrails.com/domain/" + domain + "/dns"))
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                log.info("[Source3] SecurityTrails status={}", response.statusCode());

                if (response.statusCode() == 200) {
                    List<Map<String, String>> records = parseSecurityTrails(response.body());
                    allHistory.addAll(records);
                    log.info("[Source3] Parsed {} records", records.size());
                } else {
                    log.warn("[Source3] Skipped — status={}", response.statusCode());
                }
            } catch (Exception ex) {
                log.warn("[Source3] SecurityTrails failed | error={}", ex.getMessage());
            }

            // ── Source 4: ViewDNS IP history ──────────────────────────────
            log.info("[DNS_HISTORY] Source 3: ViewDNS IP history | domain={}", domain);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://viewdns.info/iphistory/?domain=" + domain))
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                log.info("[Source4] status={}", response.statusCode());
                log.debug("[Source4] body=\n{}", response.body());

                if (response.statusCode() == 200) {
                    List<Map<String, String>> records = parseViewDns(response.body());
                    allHistory.addAll(records);
                    log.info("[Source4] Parsed {} records", records.size());
                } else {
                    log.warn("[Source4] Skipped — status={}", response.statusCode());
                }
            } catch (Exception ex) {
                log.warn("[Source3] Failed | error={}", ex.getMessage());
            }

            // ── Source 4: RapidDNS subdomain search ───────────────────────
            log.info("[DNS_HISTORY] Source 5: RapidDNS | domain={}", domain);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://rapiddns.io/subdomain/" + domain + "?full=1"))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                log.info("[Source5] status={}", response.statusCode());

                if (response.statusCode() == 200) {
                    List<Map<String, String>> records = parseRapidDns(response.body());
                    allHistory.addAll(records);
                    log.info("[Source5] Parsed {} records", records.size());
                } else {
                    log.warn("[Source5] Skipped — status={}", response.statusCode());
                }
            } catch (Exception ex) {
                log.warn("[Source5] Failed | domain={} error={}", domain, ex.getMessage());
            }

            // ── Source 6: WhoisXML DNS history (needs API key) ────────────
            if (whoisXmlApiKey != null && !whoisXmlApiKey.isBlank()) {
                log.info("[DNS_HISTORY] Source 5: WhoisXML DNS history | domain={}", domain);
                try {
                    String url = "https://dns-history.whoisxmlapi.com/api/v1" +
                            "?apiKey=" + whoisXmlApiKey +
                            "&domain=" + domain;

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Accept", "application/json")
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(
                            request, HttpResponse.BodyHandlers.ofString());

                    log.info("[Source6] status={} body=\n{}", response.statusCode(),
                            response.body().substring(0, Math.min(500, response.body().length())));

                    if (response.statusCode() == 200
                            && !response.body().contains("\"code\"")) {
                        List<Map<String, String>> records = parseWhoisXml(response.body());
                        allHistory.addAll(records);
                        log.info("[Source6] Parsed {} records", records.size());
                    } else {
                        log.warn("[Source6] Skipped — error in response body");
                    }
                } catch (Exception ex) {
                    log.warn("[Source6] Failed | error={}", ex.getMessage());
                }
            } else {
                log.info("[DNS_HISTORY] Source 6: WhoisXML skipped — no API key configured");
            }

            // ── CDN detection + real IP extraction ───────────────────────
            log.info("[DNS_HISTORY] Total records before dedup: {}", allHistory.size());

            Set<String> knownCdnRanges = Set.of(
                    "104.16.", "104.17.", "104.18.", "104.19.", "104.20.",
                    "104.21.", "104.22.", "104.26.", "172.64.", "172.65.",
                    "172.66.", "172.67.", "172.68.", "172.69.", "172.70.",
                    "162.158.", "190.93.", "198.41.", "197.234.", "103.21.",
                    "103.22.", "103.31.", "141.101.", "108.162.", "188.114."
            );

            List<Map<String, String>> nonCdnIps = new ArrayList<>();
            for (Map<String, String> entry : allHistory) {
                String ip = entry.get("ip");
                if (ip != null) {
                    boolean isCdn = knownCdnRanges.stream().anyMatch(ip::startsWith);
                    entry.put("isCdn", String.valueOf(isCdn));
                    if (!isCdn) {
                        nonCdnIps.add(entry);
                        if (realIp == null) realIp = ip;
                    }
                }
            }

            // Deduplicate by IP
            Map<String, Map<String, String>> deduped = new LinkedHashMap<>();
            for (Map<String, String> entry : allHistory) {
                deduped.putIfAbsent(entry.get("ip"), entry);
            }

            log.info("[DNS_HISTORY] Summary | total={} nonCdn={} realIp={}",
                    deduped.size(), nonCdnIps.size(), realIp);

            // ── Build result ──────────────────────────────────────────────
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalRecords", deduped.size());
            result.put("realIpFound",  realIp != null);
            result.put("realIp",       realIp);
            result.put("nonCdnIps",    nonCdnIps);
            result.put("fullHistory",  new ArrayList<>(deduped.values()));

            // Save real IP to context
            if (realIp != null) {
                contextService.writeDnsHistory(
                        task.getInvestigation().getId(),
                        toJson(new ArrayList<>(deduped.values())),
                        toJson(nonCdnIps),
                        realIp
                );
                log.info("[DNS_HISTORY] Real IP saved | domain={} realIp={}", domain, realIp);
            } else {
                log.warn("[DNS_HISTORY] No real IP found | domain={}", domain);
            }

            markCompleted(task, toJson(result), toJson(result));

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    private List<Map<String, String>> parseViewDns(String html) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            Elements rows = doc.select("table tr");
            log.debug("[Source3] ViewDNS table rows found: {}", rows.size());
            for (org.jsoup.nodes.Element row : rows) {
                org.jsoup.select.Elements cols = row.select("td");
                if (cols.size() >= 4) {
                    String ip = cols.get(0).text().trim();
                    if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("ip",       ip);
                        entry.put("location", cols.get(1).text().trim());
                        entry.put("owner",    cols.get(2).text().trim());
                        entry.put("lastSeen", cols.get(3).text().trim());
                        entry.put("source",   "viewdns");
                        results.add(entry);
                        log.debug("[Source3] Found IP: {} owner: {}", ip, cols.get(2).text());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Source3] Parse failed | error={}", ex.getMessage());
        }
        return results;
    }

    private List<Map<String, String>> parseRapidDns(String html) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            Elements rows = doc.select("table tbody tr");
            log.debug("[Source4] RapidDNS table rows found: {}", rows.size());
            for (org.jsoup.nodes.Element row : rows) {
                org.jsoup.select.Elements cols = row.select("td");
                if (cols.size() >= 3) {
                    String ip = cols.get(2).text().trim();
                    if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("ip",     ip);
                        entry.put("host",   cols.get(0).text().trim());
                        entry.put("type",   cols.get(1).text().trim());
                        entry.put("source", "rapiddns");
                        results.add(entry);
                        log.debug("[Source4] Found IP: {} host: {}", ip, cols.get(0).text());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Source4] Parse failed | error={}", ex.getMessage());
        }
        return results;
    }

    private List<Map<String, String>> parseWhoisXml(String json) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode records = root.get("records");
            if (records != null && records.isArray()) {
                for (JsonNode record : records) {
                    String ip = record.path("address").asText();
                    if (!ip.isBlank()) {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("ip",       ip);
                        entry.put("lastSeen", record.path("date").asText());
                        entry.put("source",   "whoisxmlapi");
                        results.add(entry);
                        log.debug("[Source5] Found IP: {} lastSeen: {}",
                                ip, record.path("date").asText());
                    }
                }
            } else {
                log.warn("[Source5] No 'records' field in response | body={}",
                        json.substring(0, Math.min(300, json.length())));
            }
        } catch (Exception ex) {
            log.warn("[Source5] Parse failed | error={}", ex.getMessage());
        }
        return results;
    }

    private List<Map<String, String>> parseSecurityTrails(String html) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            // SecurityTrails embeds JSON data in script tags
            for (org.jsoup.nodes.Element script : doc.select("script")) {
                String content = script.html();
                if (content.contains("\"ipv4\"") || content.contains("\"ip\"")) {
                    // Extract IPs using regex from JSON in script
                    java.util.regex.Pattern ipPattern = java.util.regex.Pattern.compile(
                            "\"(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\"");
                    java.util.regex.Matcher matcher = ipPattern.matcher(content);
                    Set<String> seen = new HashSet<>();
                    while (matcher.find()) {
                        String ip = matcher.group(1);
                        if (seen.add(ip)) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("ip",     ip);
                            entry.put("source", "securitytrails");
                            results.add(entry);
                            log.debug("[Source3] Found IP: {}", ip);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Source3] SecurityTrails parse failed | error={}", ex.getMessage());
        }
        return results;
    }
}