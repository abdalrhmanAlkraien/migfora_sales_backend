package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DirectIpScanRunner extends BaseRunner {

    private final InvestigationContextService contextService;

    public DirectIpScanRunner(ReconTaskRepository reconTaskRepository,
                              InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.DIRECT_IP_SCAN;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        String realIp = ctx.getRealIp();
        markRunning(task);

        if (realIp == null || realIp.isBlank()) {
            markFailed(task,
                    "No real IP found — run DNS_HISTORY first to discover origin IP");
            return;
        }

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("scannedIp", realIp);
            result.put("domain",    domain);

            log.info("[DirectIpScan] Starting | ip={} domain={}", realIp, domain);

            // ── Scan HTTP and HTTPS ───────────────────────────────────────
            Map<String, Object> httpResult  = scanWithCurl(realIp, domain, "http");
            Map<String, Object> httpsResult = scanWithCurl(realIp, domain, "https");
            result.put("http",  httpResult);
            result.put("https", httpsResult);

            // ── Port scan ─────────────────────────────────────────────────
            Map<String, Object> ports = checkPorts(realIp, domain);
            result.put("ports", ports);

            // ── Extract findings ──────────────────────────────────────────
            Map<String, Object> findings = extractFindings(
                    httpResult, httpsResult, ports, realIp);
            result.put("findings", findings);

            // ── Save to context ───────────────────────────────────────────
            contextService.writeDirectScanData(
                    task.getInvestigation().getId(),
                    realIp,
                    toJson(findings),
                    (String) findings.get("realServer"),
                    (String) findings.get("realPoweredBy"),
                    (String) findings.get("realRuntime"),
                    (boolean) findings.getOrDefault("loadBalanced", false),
                    (String) findings.get("orchestration")
            );

            log.info("[DirectIpScan] Complete | ip={} domain={} server={}",
                    realIp, domain, findings.get("realServer"));
            markCompleted(task, toJson(result), toJson(result));

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    // ── curl scan (bypasses Java Host header restriction) ────────────────────

    private Map<String, Object> scanWithCurl(String ip, String domain, String scheme) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<String> cmd = List.of(
                    "curl", "-s", "-I",
                    "--connect-timeout", "10",
                    "--max-time", "15",
                    "-H", "Host: " + domain,
                    "-H", "User-Agent: Mozilla/5.0",
                    "--insecure",
                    scheme + "://" + ip
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(15, TimeUnit.SECONDS);

            log.info("[DirectIpScan] {} raw | ip={}\n{}",
                    scheme.toUpperCase(), ip, output);

            if (output.isBlank()) {
                result.put("reachable", false);
                result.put("error",     "No response");
                return result;
            }

            // Parse status code from first line e.g. "HTTP/1.1 301 Moved Permanently"
            String firstLine = output.split("\n")[0].trim();
            Integer statusCode = null;
            if (firstLine.startsWith("HTTP/")) {
                try {
                    statusCode = Integer.parseInt(firstLine.split("\\s+")[1].trim());
                } catch (Exception ignored) {}
            }

            result.put("reachable",  true);
            result.put("statusCode", statusCode);

            // Parse headers
            Map<String, String> headers = new LinkedHashMap<>();
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.startsWith("HTTP/") || line.isBlank()) continue;
                int idx = line.indexOf(":");
                if (idx > 0) {
                    String key = line.substring(0, idx).trim().toLowerCase();
                    String val = line.substring(idx + 1).trim();
                    headers.put(key, val);
                }
            }
            result.put("headers", headers);

            log.info("[DirectIpScan] {} | ip={} status={} server={}",
                    scheme.toUpperCase(), ip, statusCode, headers.get("server"));

        } catch (Exception ex) {
            result.put("reachable", false);
            result.put("error",     ex.getMessage());
            log.warn("[DirectIpScan] {} failed | ip={} error={}",
                    scheme.toUpperCase(), ip, ex.getMessage());
        }
        return result;
    }

    // ── Port scanner ──────────────────────────────────────────────────────────

    private Map<String, Object> checkPorts(String ip, String domain) {
        List<Integer> open   = new ArrayList<>();
        List<Integer> closed = new ArrayList<>();

        int[] ports = {80, 443, 8080, 8443, 3000, 3001, 5000, 8000, 9000};

        for (int port : ports) {
            try {
                String scheme = (port == 443 || port == 8443) ? "https" : "http";
                List<String> cmd = List.of(
                        "curl", "-s", "-o", "/dev/null",
                        "--connect-timeout", "3",
                        "--max-time", "5",
                        "-w", "%{http_code}",
                        "-H", "Host: " + domain,
                        "--insecure",
                        scheme + "://" + ip + ":" + port
                );

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String code = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor(5, TimeUnit.SECONDS);

                if (!code.equals("000")) {
                    open.add(port);
                    log.info("[DirectIpScan] Port {} OPEN | ip={} code={}", port, ip, code);
                } else {
                    closed.add(port);
                }
            } catch (Exception ex) {
                closed.add(port);
            }
        }

        log.info("[DirectIpScan] Ports | ip={} open={} closed={}", ip, open, closed);
        return Map.of("open", open, "closed", closed);
    }

    // ── Extract findings ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFindings(Map<String, Object> http,
                                                Map<String, Object> https,
                                                Map<String, Object> ports,
                                                String ip) {
        Map<String, Object> findings = new LinkedHashMap<>();
        findings.put("realIp",          ip);
        findings.put("httpReachable",   http.get("reachable"));
        findings.put("httpsReachable",  https.get("reachable"));
        findings.put("httpStatusCode",  http.get("statusCode"));
        findings.put("httpsStatusCode", https.get("statusCode"));

        // Use HTTPS headers first, fallback to HTTP
        Map<String, String> headers = null;
        if (Boolean.TRUE.equals(https.get("reachable"))) {
            headers = (Map<String, String>) https.get("headers");
        } else if (Boolean.TRUE.equals(http.get("reachable"))) {
            headers = (Map<String, String>) http.get("headers");
        }

        if (headers != null) {
            String server    = headers.get("server");
            String poweredBy = headers.get("x-powered-by");
            findings.put("realServer",    server);
            findings.put("realPoweredBy", poweredBy);

            // Tech detection
            List<String> tech = new ArrayList<>();
            if (server != null) {
                String s = server.toLowerCase();
                if (s.contains("nginx"))     tech.add("Nginx");
                if (s.contains("apache"))    tech.add("Apache");
                if (s.contains("tomcat"))    tech.add("Tomcat");
                if (s.contains("litespeed")) tech.add("LiteSpeed");
                if (s.contains("iis"))       tech.add("Microsoft IIS");
                if (s.contains("openresty")) tech.add("OpenResty");
                if (s.contains("gunicorn"))  tech.add("Gunicorn");
                if (s.contains("caddy"))     tech.add("Caddy");
            }
            if (poweredBy != null) {
                String p = poweredBy.toLowerCase();
                if (p.contains("php"))      tech.add("PHP");
                if (p.contains("next"))     tech.add("Next.js");
                if (p.contains("express"))  tech.add("Express.js");
                if (p.contains("asp.net")) tech.add("ASP.NET");
                if (p.contains("django"))   tech.add("Django");
                if (p.contains("rails"))    tech.add("Ruby on Rails");
            }

            // Runtime detection
            String runtime = null;
            for (Map.Entry<String, String> h : headers.entrySet()) {
                String k = h.getKey().toLowerCase();
                String v = h.getValue().toLowerCase();
                if (k.contains("fastcgi") || v.contains("php-fpm")) {
                    tech.add("PHP-FPM");
                    runtime = "PHP-FPM";
                }
                if (v.contains("unicorn")) runtime = "Ruby/Unicorn";
                if (v.contains("puma"))    runtime = "Ruby/Puma";
            }
            findings.put("realRuntime",   runtime);
            findings.put("realTechStack", tech);

            // SSL termination analysis
            boolean httpOk  = Boolean.TRUE.equals(http.get("reachable"));
            boolean httpsOk = Boolean.TRUE.equals(https.get("reachable"));
            if (httpOk && !httpsOk) {
                findings.put("sslTermination",
                        "CDN/Proxy — origin serves HTTP only, SSL terminated upstream");
            } else if (httpsOk) {
                findings.put("sslTermination",
                        "Origin — server handles SSL directly");
            }
        } else {
            findings.put("realServer",    null);
            findings.put("realPoweredBy", null);
            findings.put("realRuntime",   null);
            findings.put("realTechStack", new ArrayList<>());
            findings.put("sslTermination", null);
        }

        // Ports
        List<Integer> openPorts   = (List<Integer>) ports.getOrDefault("open", List.of());
        List<Integer> closedPorts = (List<Integer>) ports.getOrDefault("closed", List.of());
        findings.put("openPorts",   openPorts);
        findings.put("closedPorts", closedPorts);

        // Orchestration
        boolean loadBalanced = openPorts.size() > 3;
        findings.put("loadBalanced",  loadBalanced);
        findings.put("orchestration",
                loadBalanced
                        ? "Possible load balancer — multiple ports open"
                        : "None detected — single origin server");

        // Notes
        findings.put("notes", buildNotes(findings));

        return findings;
    }

    private String buildNotes(Map<String, Object> f) {
        StringBuilder notes = new StringBuilder();
        if (f.get("realServer")      != null) notes.append("Server: ").append(f.get("realServer")).append(". ");
        if (f.get("sslTermination")  != null) notes.append(f.get("sslTermination")).append(". ");
        if (f.get("realRuntime")     != null) notes.append("Runtime: ").append(f.get("realRuntime")).append(". ");
        if (f.get("openPorts")       != null) notes.append("Open ports: ").append(f.get("openPorts")).append(". ");
        if (f.get("orchestration")   != null) notes.append(f.get("orchestration")).append(".");
        return notes.toString().trim();
    }
}