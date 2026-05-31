package com.migfora.sales.service;

import com.migfora.sales.dto.InvestigationContextDtos.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.repository.InvestigationContextRepository;
import com.migfora.sales.repository.InvestigationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                                 Integer httpStatus,
                                 boolean httpsRedirect) {

        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setServerHeader(serverHeader);
        ctx.setPoweredByHeader(poweredByHeader);
        ctx.setHttpStatusCode(httpStatus);
        ctx.setHttpsRedirect(httpsRedirect);
        contextRepository.save(ctx);

        log.info("Headers data written | investigationId={} server={}",
                investigationId, serverHeader);
    }

    // ── Write WHOIS data ──────────────────────────────────────────────────────

    @Transactional
    public void writeWhoisData(Long investigationId, String whoisData) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setWhoisData(whoisData);
        contextRepository.save(ctx);
        log.info("WHOIS data written | investigationId={}", investigationId);
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
        contextRepository.save(ctx);
        log.info("SSL data written | investigationId={}", investigationId);
    }

    // ── Write performance data ────────────────────────────────────────────────

    @Transactional
    public void writePerformanceData(Long investigationId,
                                     Double ttfb,
                                     Double dnsTime,
                                     Double connectTime,
                                     Double tlsTime,
                                     Double totalTime) {
        InvestigationContext ctx = getOrCreate(investigationId);
        ctx.setTtfb(ttfb);
        ctx.setDnsResolveTime(dnsTime);
        ctx.setConnectTime(connectTime);
        ctx.setTlsTime(tlsTime);
        ctx.setTotalTime(totalTime);
        contextRepository.save(ctx);
        log.info("Performance data written | investigationId={} ttfb={}ms",
                investigationId, ttfb);
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
                ctx.getResolvedIp(), ctx.getRealIp(),
                ctx.isCdnDetected(), ctx.getCdnProvider(),
                ctx.getDnsRecords(), ctx.getNameservers(),
                ctx.getServerHeader(), ctx.getPoweredByHeader(),
                ctx.getHttpStatusCode(), ctx.isHttpsRedirect(),
                ctx.getOpenPorts(), ctx.getExposedServices(),
                ctx.getSslIssuer(), ctx.getSslExpiry(), ctx.isSslValid(),
                ctx.getTechStack(), ctx.getSubdomains(), ctx.getWhoisData(),
                ctx.getTtfb(), ctx.getDnsResolveTime(), ctx.getConnectTime(),
                ctx.getTlsTime(), ctx.getTotalTime(),
                ctx.getIpCountry(), ctx.getIpCity(),
                ctx.getIpOrg(), ctx.getIpAsn(), ctx.getIpHosting(),
                ctx.getUpdatedAt()
        );
    }
}
