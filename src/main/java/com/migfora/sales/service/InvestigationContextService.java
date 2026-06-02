package com.migfora.sales.service;

import com.migfora.sales.dto.InvestigationContextDtos.*;
import com.migfora.sales.dto.InvestigationContextDtos.InvestigationContextResponse;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.InvestigationContextRepository;
import com.migfora.sales.repository.InvestigationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;       // ← Jackson 3.x
import tools.jackson.databind.ObjectMapper;   // ← Jackson 3.x

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:36 PM
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationContextService {

    private final InvestigationContextRepository contextRepository;
    private final InvestigationRepository investigationRepository;
    private final ObjectMapper objectMapper;

    // ── Get or create context ─────────────────────────────────────────────────

    @Transactional
    public InvestigationContext getOrCreate(Long investigationId) {
        return contextRepository.findByInvestigationId(investigationId)
                .orElseGet(() -> {
                    Investigation investigation = investigationRepository
                            .findById(investigationId)
                            .orElseThrow(() -> new AuthException(
                                    "Investigation not found."));

                    InvestigationContext context = InvestigationContext.builder()
                            .investigation(investigation)
                            .build();

                    log.info("Context created | investigationId={}", investigationId);
                    return contextRepository.save(context);
                });
    }

    // ── Read context ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InvestigationContext get(Long investigationId) {
        return contextRepository.findByInvestigationId(investigationId)
                .orElseThrow(() -> new AuthException(
                        "No context found for this investigation. " +
                                "Run DNS_LOOKUP first."));
    }

    // ── Write DNS data (called after DNS_LOOKUP completes) ────────────────────

    @Transactional
    public void writeDnsData(Long investigationId,
                             String resolvedIp,
                             String dnsRecords,
                             String nameservers,
                             boolean cdnDetected,
                             String cdnProvider) {

        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setResolvedIp(resolvedIp);
        ctx.setDnsRecords(dnsRecords);
        ctx.setNameservers(nameservers);
        ctx.setCdnDetected(cdnDetected);
        ctx.setCdnProvider(cdnProvider);
        contextRepository.save(ctx);

        // Also update the investigation IP field for dependency checks
        investigationRepository.findById(investigationId).ifPresent(inv -> {
            inv.setIpAddress(resolvedIp);
            investigationRepository.save(inv);
        });

        log.info("DNS data written | investigationId={} ip={} cdn={}",
                investigationId, resolvedIp, cdnDetected);
    }

    // ── Write HTTP headers (called after HEADERS completes) ───────────────────

    @Transactional
    public void writeHeadersData(Long investigationId,
                                 String serverHeader,
                                 String poweredByHeader,
                                 Integer httpStatusCode,
                                 boolean httpsRedirect,
                                 String allHeadersJson) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setServerHeader(serverHeader);
        ctx.setPoweredByHeader(poweredByHeader);
        ctx.setHttpStatusCode(httpStatusCode);
        ctx.setHttpsRedirect(httpsRedirect);
        ctx.setAllHeaders(allHeadersJson);
        ctx.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(ctx);
        log.info("Context updated — headers | investigationId={}", investigationId);
    }

    // ── Write WHOIS data ──────────────────────────────────────────────────────

    @Transactional
    public void writeWhoisData(Long investigationId, String whoisJson) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setWhoisData(whoisJson);
        ctx.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(ctx);
        log.info("Context updated — whois | investigationId={}", investigationId);
    }

    // ── Write tech stack ──────────────────────────────────────────────────────

    @Transactional
    public void writeTechStack(Long investigationId, String techJson) {
        InvestigationContext ctx = getOrCreate(investigationId);
        try {
            Map<String, Object> data = objectMapper.readValue(techJson, Map.class);

            // Extract each section and store separately
            Object detected  = data.get("detected");
            Object inferred  = data.get("inferredFromHeaders");
            Object sources   = data.get("sources");

            ctx.setTechDetected(detected != null  ? objectMapper.writeValueAsString(detected)  : null);
            ctx.setTechInferred(inferred != null  ? objectMapper.writeValueAsString(inferred)  : null);
            ctx.setTechSources(sources != null    ? objectMapper.writeValueAsString(sources)   : null);
            ctx.setUpdatedAt(LocalDateTime.now());
            contextRepository.save(ctx);
            log.info("Tech stack written | investigationId={}", investigationId);
        } catch (Exception ex) {
            log.warn("Failed to parse tech stack JSON | error={}", ex.getMessage());
            ctx.setTechDetected(techJson);
            ctx.setUpdatedAt(LocalDateTime.now());
            contextRepository.save(ctx);
        }
    }

    // ── Write open ports ──────────────────────────────────────────────────────

    @Transactional
    public void writeOpenPorts(Long investigationId,
                               String openPorts,
                               String exposedServices) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setOpenPorts(openPorts);
        ctx.setExposedServices(exposedServices);
        contextRepository.save(ctx);
        log.info("Open ports written | investigationId={}", investigationId);
    }

    // ── Write SSL data ────────────────────────────────────────────────────────

    @Transactional
    public void writeSslData(Long investigationId,
                             String issuer,
                             String expiry,
                             boolean valid) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setSslIssuer(issuer);
        ctx.setSslExpiry(expiry);
        ctx.setSslValid(valid);
        ctx.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(ctx);
        log.info("Context updated — ssl | investigationId={}", investigationId);
    }

    // ── Write performance data ────────────────────────────────────────────────

    @Transactional
    public void writePerformanceData(Long investigationId,
                                     Double ttfb,
                                     Double dnsTime,
                                     Double connectTime,
                                     Double tlsTime,
                                     Double totalTime,
                                     Integer httpCode,
                                     Long sizeBytes) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setTtfb(ttfb);
        ctx.setDnsResolveTime(dnsTime);
        ctx.setConnectTime(connectTime);
        ctx.setTlsTime(tlsTime);
        ctx.setTotalTime(totalTime);
        ctx.setPerformanceHttpCode(httpCode);
        ctx.setPerformanceSizeBytes(sizeBytes);
        ctx.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(ctx);
        log.info("Context updated — performance | investigationId={}", investigationId);
    }

    // ── Write IP info ─────────────────────────────────────────────────────────

    @Transactional
    public void writeIpInfo(Long investigationId,
                            String country,
                            String city,
                            String region,
                            String org,
                            String asn,
                            String timezone,
                            String hostname) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setIpCountry(country);
        ctx.setIpCity(city);
        ctx.setIpRegion(region);
        ctx.setIpOrg(org);
        ctx.setIpAsn(asn);
        ctx.setIpTimezone(timezone);
        ctx.setIpHostname(hostname);
        ctx.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(ctx);
        log.info("Context updated — ipInfo | investigationId={}", investigationId);
    }

    // ── Write subdomains ──────────────────────────────────────────────────────

    @Transactional
    public void writeSubdomains(Long investigationId, String subdomains) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setSubdomains(subdomains);
        contextRepository.save(ctx);
        log.info("Subdomains written | investigationId={}", investigationId);
    }

    @Transactional(readOnly = true)
    public InvestigationContextResponse getContext(Long investigationId) {
        InvestigationContext ctx = get(investigationId);

        return new InvestigationContextResponse(
                investigationId,
                parseDns(ctx),
                parseNsLookup(ctx),
                parseWhois(ctx.getWhoisData()),
                parseHeaders(ctx),
                parsePerformance(ctx),
                parseSsl(ctx),
                parseTechStack(ctx),           // ← pass full ctx, not ctx.getTechStack()
                parseSubdomains(ctx.getSubdomains()),
                parseIpInfo(ctx),
                parseShodan(ctx.getOpenPorts(), ctx.getExposedServices()),
                parseCensys(ctx),
                ctx.getUpdatedAt()
        );
    }

    // ── DNS ───────────────────────────────────────────────────────────────────────

    private DnsInfo parseDns(InvestigationContext ctx) {
        if (ctx.getResolvedIp() == null && ctx.getDnsRecords() == null) return null;
        try {
            List<String> aRecords = parseJsonArray(ctx.getDnsRecords());
            List<String> nsRecords = parseJsonArray(ctx.getNameservers());
            return new DnsInfo(
                    ctx.getResolvedIp(),
                    ctx.isCdnDetected(),
                    ctx.getCdnProvider(),
                    aRecords,
                    List.of(),   // MX not stored separately — extend if needed
                    List.of(),   // TXT not stored separately
                    nsRecords
            );
        } catch (Exception ex) {
            log.warn("Failed to parse DNS info | error={}", ex.getMessage());
            return null;
        }
    }

// ── NsLookup ──────────────────────────────────────────────────────────────────

    private NsLookupInfo parseNsLookup(InvestigationContext ctx) {
        if (ctx.getResolvedIp() == null) return null;
        return new NsLookupInfo(
                ctx.getResolvedIp(),
                parseJsonArray(ctx.getNameservers())
        );
    }

// ── WHOIS ─────────────────────────────────────────────────────────────────────

    private WhoisInfo parseWhois(String whoisJson) {
        if (whoisJson == null || whoisJson.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(whoisJson);

            List<String> nameservers = new ArrayList<>();
            JsonNode nsNode = node.get("nameservers");
            if (nsNode != null && nsNode.isArray()) {
                nsNode.forEach(n -> nameservers.add(n.asString()));
            }

            return new WhoisInfo(
                    getText(node, "domain"),
                    getText(node, "ip"),
                    getText(node, "registrantName"),
                    getText(node, "registrar"),
                    getText(node, "status"),
                    getText(node, "createdDate"),
                    getText(node, "updatedDate"),
                    nameservers,
                    getText(node, "dnssec"),
                    getText(node, "ipOrg"),
                    getText(node, "ipCountry"),
                    getText(node, "ipCity")
            );
        } catch (Exception ex) {
            log.warn("Failed to parse whois JSON | error={}", ex.getMessage());
            return null;
        }
    }

// ── Headers ───────────────────────────────────────────────────────────────────

    private HeadersInfo parseHeaders(InvestigationContext ctx) {
        if (ctx.getServerHeader() == null && ctx.getHttpStatusCode() == null) return null;

        Map<String, String> allHeaders = new LinkedHashMap<>();
        if (ctx.getAllHeaders() != null) {
            try {
                JsonNode node = objectMapper.readTree(ctx.getAllHeaders());
                node.properties().forEach(e ->
                        allHeaders.put(e.getKey(), e.getValue().asString()));
            } catch (Exception ex) {
                log.warn("Failed to parse allHeaders JSON | error={}", ex.getMessage());
            }
        }

        return new HeadersInfo(
                ctx.isHttpsRedirect(),
                ctx.getHttpStatusCode(),
                ctx.getServerHeader(),
                ctx.getPoweredByHeader(),
                ctx.getXFrameOptions(),
                ctx.getContentSecurityPolicy(),
                null,
                null,
                null,
                allHeaders
        );
    }

// ── Performance ───────────────────────────────────────────────────────────────

    private PerformanceInfo parsePerformance(InvestigationContext ctx) {
        if (ctx.getTtfb() == null) return null;

        double ttfb = ctx.getTtfb();
        String rating;
        if (ttfb < 200)       rating = "EXCELLENT";
        else if (ttfb < 500)  rating = "GOOD";
        else if (ttfb < 1000) rating = "AVERAGE";
        else if (ttfb < 2000) rating = "SLOW";
        else                  rating = "VERY_SLOW";

        return new PerformanceInfo(
                ctx.getTtfb(),
                ctx.getDnsResolveTime(),
                ctx.getConnectTime(),
                ctx.getTlsTime(),
                ctx.getTotalTime(),
                ctx.getPerformanceHttpCode(),
                ctx.getPerformanceSizeBytes(),
                rating
        );
    }

// ── SSL ───────────────────────────────────────────────────────────────────────

    private SslInfo parseSsl(InvestigationContext ctx) {
        if (ctx.getSslIssuer() == null) return null;

        Integer daysUntilExpiry = null;
        String expiryStatus = "UNKNOWN";

        if (ctx.getSslExpiry() != null) {
            try {
                LocalDateTime expiry = LocalDateTime.parse(ctx.getSslExpiry());
                long days = java.time.Duration.between(
                        LocalDateTime.now(), expiry).toDays();
                daysUntilExpiry = (int) days;

                if (days < 0)        expiryStatus = "EXPIRED";
                else if (days < 30)  expiryStatus = "EXPIRING_SOON";
                else                 expiryStatus = "VALID";
            } catch (Exception ex) {
                log.warn("Failed to parse SSL expiry | error={}", ex.getMessage());
            }
        }

        return new SslInfo(
                ctx.getSslIssuer(),
                ctx.getSslExpiry(),
                ctx.isSslValid(),
                daysUntilExpiry,
                expiryStatus
        );
    }

// ── Tech Stack ────────────────────────────────────────────────────────────────

    private TechStackInfo parseTechStack(InvestigationContext ctx) {
        if (ctx.getTechDetected() == null && ctx.getTechInferred() == null) return null;
        try {
            List<String> detected = new ArrayList<>();
            Map<String, String> inferred = new LinkedHashMap<>();
            Map<String, String> sources = new LinkedHashMap<>();

            if (ctx.getTechDetected() != null) {
                JsonNode node = objectMapper.readTree(ctx.getTechDetected());
                if (node.isArray()) {
                    node.forEach(n -> detected.add(n.asString()));
                }
            }

            if (ctx.getTechInferred() != null) {
                JsonNode node = objectMapper.readTree(ctx.getTechInferred());
                node.properties().forEach(e ->
                        inferred.put(e.getKey(), e.getValue().asString()));
            }

            if (ctx.getTechSources() != null) {
                JsonNode node = objectMapper.readTree(ctx.getTechSources());
                node.properties().forEach(e ->
                        sources.put(e.getKey(), e.getValue().asString()));
            }

            return new TechStackInfo(detected, inferred, sources);

        } catch (Exception ex) {
            log.warn("Failed to parse tech stack | error={}", ex.getMessage());
            return null;
        }
    }

// ── Subdomains ────────────────────────────────────────────────────────────────

    private SubdomainsInfo parseSubdomains(String subdomainsJson) {
        if (subdomainsJson == null || subdomainsJson.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(subdomainsJson);
            if (node.isArray()) {
                List<String> list = new ArrayList<>();
                node.forEach(n -> list.add(n.asString()));
                return new SubdomainsInfo(list.size(), list, null);
            }
            List<String> subs = parseJsonArray(
                    node.has("subdomains") ? node.get("subdomains").toString() : null);
            return new SubdomainsInfo(
                    node.has("total") ? node.get("total").asInt() : subs.size(),
                    subs,
                    node.has("source") ? node.get("source").asString() : null
            );
        } catch (Exception ex) {
            log.warn("Failed to parse subdomains JSON | error={}", ex.getMessage());
            return null;
        }
    }

// ── IP Info ───────────────────────────────────────────────────────────────────

    private IpInfo parseIpInfo(InvestigationContext ctx) {
        if (ctx.getIpCountry() == null && ctx.getIpOrg() == null) return null;
        return new IpInfo(
                ctx.getResolvedIp(),
                ctx.getIpHostname(),
                ctx.getIpCity(),
                ctx.getIpRegion(),
                ctx.getIpCountry(),
                ctx.getIpOrg(),
                ctx.getIpAsn(),
                ctx.getIpTimezone()
        );
    }
// ── Shodan ────────────────────────────────────────────────────────────────────

    private ShodanInfo parseShodan(String openPortsJson, String servicesJson) {
        if (openPortsJson == null) return null;
        try {
            List<Integer> ports = new ArrayList<>();
            JsonNode portsNode = objectMapper.readTree(openPortsJson);
            if (portsNode.isArray()) {
                portsNode.forEach(n -> ports.add(n.asInt()));
            }
            Object services = servicesJson != null
                    ? objectMapper.readTree(servicesJson) : null;
            return new ShodanInfo(null, null, null, null, null, ports, services, null);
        } catch (Exception ex) {
            log.warn("Failed to parse Shodan data | error={}", ex.getMessage());
            return null;
        }
    }

// ── Censys ────────────────────────────────────────────────────────────────────

    private CensysInfo parseCensys(InvestigationContext ctx) {
        // Censys data not stored separately yet — extend entity if needed
        return null;
    }

// ── Helpers ───────────────────────────────────────────────────────────────────

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(n -> result.add(n.asString()));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String getText(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asString() : null;
    }
}
