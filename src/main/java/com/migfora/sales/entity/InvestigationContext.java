package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:35 PM
 */
@Entity
@Table(name = "investigation_contexts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvestigationContext {


    @Id
    private Long id; // Same as investigation ID — one-to-one

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "investigation_id")
    private Investigation investigation;

    private String realIp;

    // ── DNS data (written by DNS_LOOKUP) ──────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String dnsHistory;           // JSON array of full IP history
    private String resolvedIp;
    private boolean cdnDetected;
    private String cdnProvider;

    @Column(columnDefinition = "TEXT")
    private String dnsRecords;        // JSON: { A:[...], MX:[...], TXT:[...] }

    @Column(columnDefinition = "TEXT")
    private String nameservers;       // JSON array

    // ── HTTP data (written by HEADERS) ────────────────────────────────────────
    private String serverHeader;      // nginx/1.18, Apache/2.4...
    private String poweredByHeader;   // PHP/8.1, ASP.NET...
    private String xFrameOptions;
    private String contentSecurityPolicy;
    private Integer httpStatusCode;
    private boolean httpsRedirect;
    @Column(columnDefinition = "TEXT")
    private String allHeaders;

    // ── Network data (written by SHODAN/CENSYS) ───────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String openPorts;         // JSON array [80, 443, 3306...]

    @Column(columnDefinition = "TEXT")
    private String exposedServices;   // JSON: { 3306: "MySQL", 6379: "Redis" }

    // ── SSL data (written by SSL_CERT) ────────────────────────────────────────
    private String sslIssuer;
    private String sslExpiry;
    private boolean sslValid;

    // ── Tech stack (written by TECH_STACK) ───────────────────────────────────

    // Tech Stack — structured
    @Column(columnDefinition = "TEXT")
    private String techDetected;          // JSON array of detected tech

    @Column(columnDefinition = "TEXT")
    private String techInferred;          // JSON object of inferred data

    @Column(columnDefinition = "TEXT")
    private String techSources;           // JSON object of analysis sources

    // ── Subdomains (written by SUBDOMAINS) ────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String subdomains;        // JSON array

    // ── Subdomains scan (written by SUBDOMAINS scan) ────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String subdomainScanData;    // JSON: detailed per-subdomain intel

    // ── WHOIS data (written by WHOIS) ─────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String whoisData;         // JSON

    // ── Performance (written by PERFORMANCE) ─────────────────────────────────
    private Double ttfb;              // time to first byte (ms)
    private Double dnsResolveTime;
    private Double connectTime;
    private Double tlsTime;
    private Double totalTime;
    private Integer performanceHttpCode;
    private Long performanceSizeBytes;

    // ── IP Info (written by IP_INFO) ──────────────────────────────────────────
    private String ipCountry;
    private String ipCity;
    private String ipOrg;
    private String ipAsn;
    private String ipHosting;
    private String ipRegion;
    private String ipTimezone;
    private String ipHostname;

    // ── DNS History (written by DNS_HISTORY) ──────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String nonCdnIps;            // JSON array of non-CDN IPs

    // ── Direct IP Scan (written by DIRECT_IP_SCAN) ────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String directScanFindings;   // JSON: full findings from direct scan

    private String realServer;           // real web server bypassing CDN
    private String realPoweredBy;        // real X-Powered-By bypassing CDN
    private String realRuntime;          // PHP-FPM, Gunicorn, etc.
    private boolean loadBalanced;
    private String orchestration;        // None / Kubernetes / ECS

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
