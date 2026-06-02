package com.migfora.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void writeTechStack(Long investigationId, String techStack) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setTechStack(techStack);
        contextRepository.save(ctx);
        log.info("Tech stack written | investigationId={}", investigationId);
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
                            String org,
                            String asn,
                            String hosting) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setIpCountry(country);
        ctx.setIpCity(city);
        ctx.setIpOrg(org);
        ctx.setIpAsn(asn);
        ctx.setIpHosting(hosting);
        contextRepository.save(ctx);
        log.info("IP info written | investigationId={} country={} org={}",
                investigationId, country, org);
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
                parseTechStack(ctx.getTechStack()),
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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(whoisJson);

            List<String> nameservers = new ArrayList<>();
            JsonNode nsNode = node.get("nameservers");
            if (nsNode != null && nsNode.isArray()) {
                nsNode.forEach(n -> nameservers.add(n.asText()));
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
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode node = objectMapper.readTree(ctx.getAllHeaders());
                node.fields().forEachRemaining(e ->
                        allHeaders.put(e.getKey(), e.getValue().asText()));
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

    private TechStackInfo parseTechStack(String techJson) {
        if (techJson == null || techJson.isBlank()) return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(techJson);

            List<String> builtWith = parseJsonArray(
                    node.has("builtWith") ? node.get("builtWith").toString() : null);
            List<String> wappalyzer = parseJsonArray(
                    node.has("wappalyzer") ? node.get("wappalyzer").toString() : null);

            JsonNode inferred = node.get("inferredFromHeaders");
            String webServer = inferred != null ? getText(inferred, "webServer") : null;
            String language  = inferred != null ? getText(inferred, "language")  : null;
            String cdn       = inferred != null ? getText(inferred, "cdn")       : null;

            return new TechStackInfo(builtWith, wappalyzer, webServer, language, cdn);
        } catch (Exception ex) {
            log.warn("Failed to parse techStack JSON | error={}", ex.getMessage());
            return null;
        }
    }

// ── Subdomains ────────────────────────────────────────────────────────────────

    private SubdomainsInfo parseSubdomains(String subdomainsJson) {
        if (subdomainsJson == null || subdomainsJson.isBlank()) return null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(subdomainsJson);
            if (node.isArray()) {
                List<String> list = new ArrayList<>();
                node.forEach(n -> list.add(n.asText()));
                return new SubdomainsInfo(list.size(), list, null);
            }
            List<String> subs = parseJsonArray(
                    node.has("subdomains") ? node.get("subdomains").toString() : null);
            return new SubdomainsInfo(
                    node.has("total") ? node.get("total").asInt() : subs.size(),
                    subs,
                    node.has("source") ? node.get("source").asText() : null
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
                null,
                ctx.getIpCity(),
                null,
                ctx.getIpCountry(),
                ctx.getIpOrg(),
                ctx.getIpAsn(),
                null
        );
    }

// ── Shodan ────────────────────────────────────────────────────────────────────

    private ShodanInfo parseShodan(String openPortsJson, String servicesJson) {
        if (openPortsJson == null) return null;
        try {
            List<Integer> ports = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(n -> result.add(n.asText()));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String getText(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : null;
    }
}
