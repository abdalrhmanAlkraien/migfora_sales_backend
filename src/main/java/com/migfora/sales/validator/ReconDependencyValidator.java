package com.migfora.sales.validator;

import com.migfora.sales.dto.ValidationResult;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskStatus;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:15 PM
 */
@Component
public class ReconDependencyValidator {


    // ── Dependency map ────────────────────────────────────────────────────────

    private static final Map<ReconTaskType, ReconTaskType> DEPENDS_ON = Map.ofEntries(
            Map.entry(ReconTaskType.WHOIS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SHODAN, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.CENSYS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.IP_INFO, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.TECH_STACK, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SUBDOMAINS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.SSL_CERT, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.PERFORMANCE, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.HEADERS, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.DNS_HISTORY, ReconTaskType.DNS_LOOKUP),
            Map.entry(ReconTaskType.DIRECT_IP_SCAN, ReconTaskType.DNS_HISTORY),
            Map.entry(ReconTaskType.SUBDOMAIN_SCAN, ReconTaskType.SUBDOMAINS)
    );

    // Tasks that need a resolved IP (CDN warning applies)
    private static final Set<ReconTaskType> NEEDS_REAL_IP = Set.of(
            ReconTaskType.WHOIS,
            ReconTaskType.SHODAN,
            ReconTaskType.CENSYS,
            ReconTaskType.IP_INFO
    );

    // Known CDN IP ranges (simplified — expand as needed)
    private static final List<String> CDN_PREFIXES = List.of(
            "104.16.", "104.17.", "104.18.", "104.19.", "104.20.", "104.21.",
            "172.64.", "172.65.", "172.66.", "172.67.", "162.158.",
            "141.101.", "108.162.", "190.93.",  // Cloudflare
            "151.101.",                          // Fastly
            "205.251.", "204.246.", "99.84.",    // CloudFront
            "23.235.", "151.101."                // Fastly
    );

    // ── Validate before running ───────────────────────────────────────────────

    public ValidationResult validate(ReconTaskType taskType,
                                     List<ReconTask> existingTasks,
                                     String resolvedIp) {

        ReconTaskType dependency = DEPENDS_ON.get(taskType);

        // DNS_LOOKUP has no dependency — always allowed
        if (dependency == null) {
            return ValidationResult.allowed();
        }

        // Find the dependency task
        Optional<ReconTask> depTask = existingTasks.stream()
                .filter(t -> t.getType() == dependency)
                .findFirst();

        // Dependency not run yet
        if (depTask.isEmpty()) {
            return ValidationResult.blocked(
                    "DNS_LOOKUP must run first before " + taskType.name()
            );
        }

        // Dependency failed
        if (depTask.get().getStatus() == ReconTaskStatus.FAILED) {
            return ValidationResult.skipped(
                    "Skipped — DNS_LOOKUP failed: " + depTask.get().getErrorMessage()
            );
        }

        // Dependency still running
        if (depTask.get().getStatus() == ReconTaskStatus.RUNNING ||
                depTask.get().getStatus() == ReconTaskStatus.PENDING) {
            return ValidationResult.blocked(
                    "DNS_LOOKUP is still running — wait for it to complete"
            );
        }

        // CDN detection for IP-based tasks
        if (NEEDS_REAL_IP.contains(taskType) && resolvedIp != null) {
            String cdnProvider = detectCdn(resolvedIp);
            if (cdnProvider != null) {
                return ValidationResult.allowedWithWarning(
                        cdnProvider,
                        "CDN detected (" + cdnProvider + ") — IP " + resolvedIp +
                                " belongs to CDN, not origin server. " +
                                "Results may reflect CDN infrastructure, not the real host."
                );
            }
        }

        return ValidationResult.allowed();
    }

    // ── CDN detection ─────────────────────────────────────────────────────────

    public String detectCdn(String ip) {
        if (ip == null) return null;
        for (String prefix : CDN_PREFIXES) {
            if (ip.startsWith(prefix)) {
                if (ip.startsWith("104.") || ip.startsWith("172.6") ||
                        ip.startsWith("162.158") || ip.startsWith("141.101") ||
                        ip.startsWith("108.162") || ip.startsWith("190.93")) {
                    return "Cloudflare";
                }
                if (ip.startsWith("151.101") || ip.startsWith("23.235")) {
                    return "Fastly";
                }
                if (ip.startsWith("205.251") || ip.startsWith("204.246") ||
                        ip.startsWith("99.84")) {
                    return "AWS CloudFront";
                }
            }
        }
        return null;
    }
}
