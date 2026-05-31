package com.migfora.sales.dto;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:37 PM
 */
@NoArgsConstructor
public class InvestigationContextDtos {


    public record InvestigationContextResponse(
            Long investigationId,

            // DNS
            String resolvedIp,
            String realIp,
            boolean cdnDetected,
            String cdnProvider,
            String dnsRecords,
            String nameservers,

            // HTTP
            String serverHeader,
            String poweredByHeader,
            Integer httpStatusCode,
            boolean httpsRedirect,

            // Network
            String openPorts,
            String exposedServices,

            // SSL
            String sslIssuer,
            String sslExpiry,
            boolean sslValid,

            // Tech
            String techStack,

            // Subdomains
            String subdomains,

            // WHOIS
            String whoisData,

            // Performance
            Double ttfb,
            Double dnsResolveTime,
            Double connectTime,
            Double tlsTime,
            Double totalTime,

            // IP Info
            String ipCountry,
            String ipCity,
            String ipOrg,
            String ipAsn,
            String ipHosting,

            LocalDateTime updatedAt
    ) {}
}
