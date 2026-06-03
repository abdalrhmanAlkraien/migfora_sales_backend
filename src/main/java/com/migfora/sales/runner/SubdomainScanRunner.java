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

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 2:49 AM
 */
@Component
@Slf4j
public class SubdomainScanRunner extends BaseRunner {


    private final InvestigationContextService contextService;

    // CDN IP ranges
    private static final Set<String> CDN_RANGES = Set.of(
            "104.16.", "104.17.", "104.18.", "104.19.", "104.20.",
            "104.21.", "104.22.", "104.26.", "172.64.", "172.65.",
            "172.66.", "172.67.", "172.68.", "172.69.", "172.70.",
            "162.158.", "190.93.", "198.41.", "188.114.", "103.21.",
            "103.22.", "103.31.", "141.101.", "108.162."
    );

    public SubdomainScanRunner(ReconTaskRepository reconTaskRepository,
                               InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.SUBDOMAIN_SCAN;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        // Load subdomains from context (written by SubdomainFinderRunner)
        String subdomainsJson = ctx.getSubdomains();
        if (subdomainsJson == null || subdomainsJson.isBlank()) {
            markFailed(task,
                    "No subdomains found — run SUBDOMAINS task first");
            return;
        }

        try {
            // Parse subdomain list from context
            List<String> subdomains = parseSubdomainList(subdomainsJson);
            log.info("[SubdomainScan] Starting | domain={} count={}", domain, subdomains.size());

            if (subdomains.isEmpty()) {
                markFailed(task, "Subdomain list is empty");
                return;
            }

            // Limit to 20 subdomains to avoid timeout
            if (subdomains.size() > 20) {
                log.warn("[SubdomainScan] Limiting to 20 subdomains | total={}",
                        subdomains.size());
                subdomains = subdomains.subList(0, 20);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            List<Map<String, Object>> flagged = new ArrayList<>();

            for (String subdomain : subdomains) {
                log.info("[SubdomainScan] Scanning | subdomain={}", subdomain);
                Map<String, Object> info = scanSubdomain(subdomain, domain);
                results.add(info);

                // Collect flagged subdomains
                @SuppressWarnings("unchecked")
                List<String> flags = (List<String>) info.getOrDefault("flags", List.of());
                if (!flags.isEmpty() &&
                        (flags.contains("DEV_ENVIRONMENT") ||
                                flags.contains("STAGING_ENVIRONMENT") ||
                                flags.contains("ADMIN_PANEL") ||
                                flags.contains("API_ENDPOINT") ||
                                flags.contains("SERVER_ERROR_EXPOSED") ||
                                flags.contains("VERSION_EXPOSED"))) {
                    flagged.add(info);
                }
            }

            // Build final result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalScanned",  results.size());
            result.put("flaggedCount",  flagged.size());
            result.put("flagged",       flagged);
            result.put("subdomains",    results);

            // Save to context
            contextService.writeSubdomainScanData(
                    task.getInvestigation().getId(),
                    toJson(result)
            );

            log.info("[SubdomainScan] Complete | domain={} scanned={} flagged={}",
                    domain, results.size(), flagged.size());
            markCompleted(task, toJson(result), toJson(result));

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    // ── Scan single subdomain ─────────────────────────────────────────────────

    private Map<String, Object> scanSubdomain(String subdomain, String mainDomain) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("subdomain", subdomain);
        info.put("type",      inferType(subdomain));

        // ── Step 1: Resolve IP ────────────────────────────────────────────
        String resolvedIp = resolveIp(subdomain);
        info.put("ip",          resolvedIp);
        boolean isCdn = resolvedIp != null && isCdnIp(resolvedIp);
        info.put("cdnDetected", isCdn);
        info.put("sameIpAsMain", resolvedIp != null &&
                resolvedIp.equals(mainDomain));

        // ── Step 2: Normal HTTP scan ──────────────────────────────────────
        Map<String, Object> scanResult = scanHttp(subdomain, "https");
        boolean httpsWorked = Boolean.TRUE.equals(scanResult.get("reachable"));

        if (!httpsWorked) {
            Map<String, Object> httpResult = scanHttp(subdomain, "http");
            if (Boolean.TRUE.equals(httpResult.get("reachable"))) {
                scanResult = httpResult;
                scanResult.put("https", false);   // ← already correct
            } else {
                scanResult.put("https", false);
            }
        } else {
            scanResult.put("https", true);
        }

        info.putAll(scanResult);

        // ── Step 3: Direct IP scan if real IP found ───────────────────────
        if (resolvedIp != null && !isCdn) {
            log.info("[SubdomainScan] Real IP found — running direct scan | " +
                    "subdomain={} ip={}", subdomain, resolvedIp);
            Map<String, Object> directScan = directIpScan(resolvedIp, subdomain);
            info.put("directScan", directScan);

            // Override server info with real data bypassing CDN
            Map<String, String> directHeaders =
                    getHeaders(directScan);
            if (directHeaders != null) {
                if (directHeaders.get("server") != null) {
                    info.put("realServer",    directHeaders.get("server"));
                    info.put("realPoweredBy", directHeaders.get("x-powered-by"));
                }
            }
        }

        // ── Step 4: Deep analysis from headers ───────────────────────────
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>)
                scanResult.getOrDefault("headers", new LinkedHashMap<>());
        Map<String, Object> deepAnalysis = deepAnalyze(subdomain, headers);
        info.put("analysis", deepAnalysis);

        // ── Step 5: Detect flags ──────────────────────────────────────────
        Integer statusCode = (Integer) scanResult.get("statusCode");
        List<String> flags = detectFlags(subdomain, headers, statusCode, resolvedIp);
        info.put("flags", flags);

        // ── Step 6: CORS info for API subdomains ──────────────────────────
        if (subdomain.toLowerCase().contains("api")) {
            info.put("cors",           headers.getOrDefault("access-control-allow-origin", null));
            info.put("allowedMethods", headers.getOrDefault("access-control-allow-methods", null));
        }

        info.remove("headers");
        log.info("[SubdomainScan] Done | subdomain={} status={} server={} flags={}",
                subdomain, statusCode, headers.get("server"), flags);

        return info;
    }

    // ── IP resolver ───────────────────────────────────────────────────────────
    private String resolveIp(String subdomain) {
        try {
            List<String> cmd = List.of("dig", "+short", subdomain);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);

            if (!output.isBlank()) {
                // Take first valid IP line
                for (String line : output.split("\n")) {
                    line = line.trim();
                    if (line.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        return line;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[SubdomainScan] IP resolve failed | subdomain={} error={}",
                    subdomain, ex.getMessage());
        }
        return null;
    }

    // ── HTTP scanner ──────────────────────────────────────────────────────────
    private Map<String, Object> scanHttp(String subdomain, String scheme) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<String> cmd = List.of(
                    "curl", "-s", "-I",
                    "--connect-timeout", "5",
                    "--max-time", "8",
                    "-H", "User-Agent: Mozilla/5.0",
                    "--insecure",
                    scheme + "://" + subdomain
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor(8, TimeUnit.SECONDS);

            if (output.isBlank()) {
                result.put("reachable", false);
                return result;
            }

            Integer statusCode = parseStatusCode(output);
            Map<String, String> headers = parseHeaders(output);

            result.put("reachable",   true);
            result.put("statusCode",  statusCode);
            result.put("server",      headers.get("server"));
            result.put("poweredBy",   headers.get("x-powered-by"));
            result.put("headers",     headers);

        } catch (Exception ex) {
            result.put("reachable", false);
            result.put("error",     ex.getMessage());
        }
        return result;
    }

    // ── direct ip scanner ──────────────────────────────────────────────────────────

    private Map<String, Object> directIpScan(String ip, String domain) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // Try HTTP first (most origin servers only listen on 80)
            List<String> cmd = List.of(
                    "curl", "-s", "-I",
                    "--connect-timeout", "8",
                    "--max-time", "10",
                    "-H", "Host: " + domain,
                    "-H", "User-Agent: Mozilla/5.0",
                    "--insecure",
                    "http://" + ip
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor(10, TimeUnit.SECONDS);

            log.info("[SubdomainScan] Direct IP scan | ip={} subdomain={}\n{}",
                    ip, domain, output);

            if (!output.isBlank()) {
                result.put("reachable",   true);
                result.put("statusCode",  parseStatusCode(output));
                result.put("headers",     parseHeaders(output));
                result.put("scheme",      "http");
            } else {
                // Try HTTPS
                cmd = List.of(
                        "curl", "-s", "-I",
                        "--connect-timeout", "8",
                        "--max-time", "10",
                        "-H", "Host: " + domain,
                        "-H", "User-Agent: Mozilla/5.0",
                        "--insecure",
                        "https://" + ip
                );
                pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                p = pb.start();
                output = new String(p.getInputStream().readAllBytes());
                p.waitFor(10, TimeUnit.SECONDS);

                if (!output.isBlank()) {
                    result.put("reachable",  true);
                    result.put("statusCode", parseStatusCode(output));
                    result.put("headers",    parseHeaders(output));
                    result.put("scheme",     "https");
                } else {
                    result.put("reachable", false);
                }
            }
        } catch (Exception ex) {
            result.put("reachable", false);
            result.put("error",     ex.getMessage());
        }
        return result;
    }

    // ── deepAnalyze scanner ──────────────────────────────────────────────────────────

    private Map<String, Object> deepAnalyze(String subdomain,
                                            Map<String, String> headers) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        String sub = subdomain.toLowerCase();

        // ── Tech detection from headers ───────────────────────────────────
        List<String> tech = new ArrayList<>();
        String contentType = headers.getOrDefault("content-type", "");
        String cacheControl = headers.getOrDefault("cache-control", "");
        String server = headers.getOrDefault("server", "");
        String poweredBy = headers.getOrDefault("x-powered-by", "");

        // Content type reveals backend
        if (contentType.contains("application/json")) {
            analysis.put("responseType", "JSON API");
            tech.add("REST API");
        }
        if (contentType.contains("text/html"))  analysis.put("responseType", "Web App");
        if (contentType.contains("text/xml"))   analysis.put("responseType", "XML/SOAP API");

        // Cache control patterns reveal framework
        if (cacheControl.contains("no-store") && cacheControl.contains("must-revalidate")) {
            tech.add("Laravel (cache pattern)");
            analysis.put("frameworkHint", "Laravel");
        }
        if (headers.containsKey("x-ratelimit-limit")) {
            analysis.put("hasRateLimit", true);
            analysis.put("rateLimit", headers.get("x-ratelimit-limit"));
        }
        if (headers.containsKey("x-ratelimit-remaining")) {
            analysis.put("rateLimitRemaining", headers.get("x-ratelimit-remaining"));
        }

        // Security headers analysis
        analysis.put("hasXFrameOptions",    headers.containsKey("x-frame-options"));
        analysis.put("hasXssProtection",    headers.containsKey("x-xss-protection"));
        analysis.put("hasContentTypeOptions", headers.containsKey("x-content-type-options"));
        analysis.put("hasHsts",             headers.containsKey("strict-transport-security"));
        analysis.put("hasCsp",              headers.containsKey("content-security-policy"));
        analysis.put("hasReferrerPolicy",   headers.containsKey("referrer-policy"));

        // Security score
        int secScore = 0;
        if (headers.containsKey("x-frame-options"))           secScore++;
        if (headers.containsKey("x-xss-protection"))          secScore++;
        if (headers.containsKey("x-content-type-options"))    secScore++;
        if (headers.containsKey("strict-transport-security")) secScore++;
        if (headers.containsKey("content-security-policy"))   secScore++;
        if (headers.containsKey("referrer-policy"))           secScore++;
        analysis.put("securityScore",   secScore + "/6");
        analysis.put("securityRating",  secScore >= 5 ? "STRONG" :
                secScore >= 3 ? "MODERATE" : "WEAK");

        // Subdomain purpose inference
        if (sub.contains("api")) {
            analysis.put("purpose", "API Server");
            analysis.put("hasPublicApi", true);
            if (headers.getOrDefault("access-control-allow-origin", "")
                    .equals("*")) {
                analysis.put("corsPolicy", "PUBLIC — allows all origins");
            } else if (headers.containsKey("access-control-allow-origin")) {
                analysis.put("corsPolicy", "RESTRICTED — " +
                        headers.get("access-control-allow-origin"));
            }
        }
        if (sub.contains("app"))     analysis.put("purpose", "Main Application");
        if (sub.contains("dev"))     analysis.put("purpose", "Development Environment");
        if (sub.contains("staging")) analysis.put("purpose", "Staging Environment");
        if (sub.contains("admin"))   analysis.put("purpose", "Admin Panel");
        if (sub.contains("tanweel")) analysis.put("purpose", "Product: Tanweel");
        if (sub.contains("marn"))    analysis.put("purpose", "Product: Marn");
        if (sub.contains("nayifat")) analysis.put("purpose", "Product: Nayifat (fintech)");

        analysis.put("detectedTech", tech);
        return analysis;
    }

    // ── Flag detector ─────────────────────────────────────────────────────────
    private List<String> detectFlags(String subdomain,
                                     Map<String, String> headers,
                                     Integer statusCode,
                                     String ip) {
        List<String> flags = new ArrayList<>();
        String s = subdomain.toLowerCase();

        // Subdomain type flags
        if (s.contains("api"))                                  flags.add("API_ENDPOINT");
        if (s.contains("admin"))                                flags.add("ADMIN_PANEL");
        if (s.contains("dev") || s.contains("develop"))        flags.add("DEV_ENVIRONMENT");
        if (s.contains("staging") || s.contains("stage"))      flags.add("STAGING_ENVIRONMENT");
        if (s.contains("test"))                                 flags.add("TEST_ENVIRONMENT");
        if (s.contains("internal"))                             flags.add("INTERNAL_SERVICE");
        if (s.contains("vpn"))                                  flags.add("VPN_ENDPOINT");
        if (s.contains("mail") || s.contains("smtp"))          flags.add("MAIL_SERVER");
        if (s.contains("ftp"))                                  flags.add("FTP_SERVER");
        if (s.contains("db") || s.contains("database"))        flags.add("DATABASE_ENDPOINT");
        if (s.contains("kibana") || s.contains("elastic"))     flags.add("LOGGING_EXPOSED");
        if (s.contains("grafana") || s.contains("monitor"))    flags.add("MONITORING_EXPOSED");
        if (s.contains("jenkins") || s.contains("ci"))         flags.add("CI_CD_EXPOSED");
        if (s.contains("jira") || s.contains("confluence"))    flags.add("PROJECT_MANAGEMENT");
        if (s.contains("dashboard"))                            flags.add("DASHBOARD_EXPOSED");

        // Status code flags
        if (statusCode != null) {
            if (statusCode == 200)  flags.add("ACCESSIBLE");
            if (statusCode == 401)  flags.add("AUTH_REQUIRED");
            if (statusCode == 403)  flags.add("FORBIDDEN_EXISTS");
            if (statusCode == 500 ||
                    statusCode == 502 ||
                    statusCode == 503)  flags.add("SERVER_ERROR_EXPOSED");
        }

        // Header flags
        String server = headers.get("server");
        if (headers.get("x-powered-by") != null)               flags.add("TECH_EXPOSED");
        if (server != null && server.matches(".*\\d+\\.\\d+.*")) flags.add("VERSION_EXPOSED");
        if (headers.get("x-debug") != null ||
                headers.get("x-debug-token") != null)              flags.add("DEBUG_HEADERS");

        // CDN bypass opportunity
        if (ip != null && !isCdnIp(ip))                        flags.add("REAL_IP_EXPOSED");

        return flags;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String inferType(String subdomain) {
        String s = subdomain.toLowerCase();
        if (s.startsWith("api.") || s.contains("api-"))   return "API";
        if (s.startsWith("app."))                          return "APPLICATION";
        if (s.startsWith("admin."))                        return "ADMIN";
        if (s.startsWith("dev."))                          return "DEVELOPMENT";
        if (s.startsWith("staging.") || s.startsWith("stage.")) return "STAGING";
        if (s.startsWith("mail.") || s.startsWith("smtp.")) return "MAIL";
        if (s.startsWith("cdn.") || s.startsWith("static.") ||
                s.startsWith("assets."))                       return "CDN";
        if (s.startsWith("www."))                          return "WEB";
        if (s.startsWith("vpn."))                          return "VPN";
        if (s.startsWith("ftp."))                          return "FTP";
        if (s.startsWith("dashboard."))                    return "DASHBOARD";
        return "OTHER";
    }

    private boolean isCdnIp(String ip) {
        return CDN_RANGES.stream().anyMatch(ip::startsWith);
    }

    private Integer parseStatusCode(String output) {
        try {
            String firstLine = output.split("\n")[0].trim();
            if (firstLine.startsWith("HTTP/")) {
                return Integer.parseInt(firstLine.split("\\s+")[1].trim());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, String> parseHeaders(String output) {
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
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseSubdomainList(String json) {
        List<String> result = new ArrayList<>();
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            if (parsed instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) result.add(s);
                    else if (item instanceof Map<?,?> m) {
                        Object sub = m.get("subdomain");
                        if (sub instanceof String s) result.add(s);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[SubdomainScan] Failed to parse subdomains | error={}", ex.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getHeaders(Map<String, Object> scanResult) {
        Object headers = scanResult.get("headers");
        if (headers instanceof Map<?, ?>) {
            return (Map<String, String>) headers;
        }
        return null;
    }
}
