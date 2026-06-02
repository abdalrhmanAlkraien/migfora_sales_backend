package com.migfora.sales.dto;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:37 PM
 */
@NoArgsConstructor
public class InvestigationContextDtos {


    // ── DNS ───────────────────────────────────────────────────────────────────
    public record DnsInfo(
            String resolvedIp,
            boolean cdnDetected,
            String cdnProvider,
            List<String> aRecords,
            List<String> mxRecords,
            List<String> txtRecords,
            List<String> nsRecords
    ) {}

    public record NsLookupInfo(
            String resolvedIp,
            List<String> nameservers
    ) {}

    // ── WHOIS ─────────────────────────────────────────────────────────────────
    public record WhoisInfo(
            String domain,
            String ip,
            String registrantName,
            String registrar,
            String status,
            String createdDate,
            String updatedDate,
            List<String> nameservers,
            String dnssec,
            String ipOrg,
            String ipCountry,
            String ipCity
    ) {}

    // ── Headers ───────────────────────────────────────────────────────────────
    public record HeadersInfo(
            boolean httpsAvailable,
            Integer httpStatusCode,
            String server,
            String xPoweredBy,
            String xFrameOptions,
            String contentSecurityPolicy,
            String strictTransportSecurity,
            String via,
            String cfRay,
            Map<String, String> allHeaders
    ) {}

    // ── Performance ───────────────────────────────────────────────────────────
    public record PerformanceInfo(
            Double ttfbMs,
            Double dnsMs,
            Double connectMs,
            Double tlsMs,
            Double totalMs,
            Integer httpCode,      // ← add
            Long sizeBytes,        // ← add
            String rating
    ) {}

    // ── SSL ───────────────────────────────────────────────────────────────────
    public record SslInfo(
            String issuer,
            String expiry,
            boolean valid,
            Integer daysUntilExpiry,
            String expiryStatus       // VALID | EXPIRING_SOON | EXPIRED
    ) {}

    // ── Tech Stack ────────────────────────────────────────────────────────────
    public record TechStackInfo(
            List<String> builtWith,
            List<String> wappalyzer,
            String webServer,
            String language,
            String cdn
    ) {}

    // ── Subdomains ────────────────────────────────────────────────────────────
    public record SubdomainsInfo(
            Integer total,
            List<String> subdomains,
            String source
    ) {}

    // ── IP Info ───────────────────────────────────────────────────────────────
    public record IpInfo(
            String ip,
            String hostname,
            String city,
            String region,
            String country,
            String org,
            String asn,
            String timezone
    ) {}

    // ── Shodan ────────────────────────────────────────────────────────────────
    public record ShodanInfo(
            String ip,
            String organization,
            String isp,
            String country,
            String city,
            List<Integer> openPorts,
            Object services,
            Object tags
    ) {}

    // ── Censys ────────────────────────────────────────────────────────────────
    public record CensysInfo(
            String ip,
            String asn,
            String asnName,
            String country,
            String city,
            Object services,
            String lastUpdatedAt
    ) {}

    // ── Full Context Response ─────────────────────────────────────────────────
    public record InvestigationContextResponse(
            Long investigationId,

            DnsInfo dns,
            NsLookupInfo nsLookup,
            WhoisInfo whois,
            HeadersInfo headers,
            PerformanceInfo performance,
            SslInfo ssl,
            TechStackInfo techStack,
            SubdomainsInfo subdomains,
            IpInfo ipInfo,
            ShodanInfo shodan,
            CensysInfo censys,

            LocalDateTime updatedAt
    ) {}
}
