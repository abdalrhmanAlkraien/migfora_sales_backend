package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:52 PM
 */

@Entity
@Table(name = "recon_tasks")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconTask {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconTaskType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReconTaskStatus status = ReconTaskStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String rawOutput;

    private String errorMessage;
    private String triggeredBy;

    // What blocked this task
    private String blockedReason;

    // CDN detection flag
    private boolean cdnDetected;
    private String cdnProvider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investigation_id")
    private Investigation investigation;

    // Which pipeline step triggered this task
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_step_id")
    private PipelineStep pipelineStep;

    // Execution order within the investigation
    private Integer executionOrder;

    // Pipeline execution context
    private Long pipelineId;
    private String pipelineName;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;


    // ── Task Types ────────────────────────────────────────────────────────────

    public enum ReconTaskType {
        DNS_LOOKUP,     // REQUIRED FIRST — resolves IP // dig ANY, TXT, MX + nslookup
        WHOIS,          // requires: IP from DNS_LOOKUP // whois on domain/IP
        SHODAN,         // requires: IP from DNS_LOOKUP // shodan host info
        CENSYS,         // requires: IP from DNS_LOOKUP // censys.io
        IP_INFO,        // requires: IP from DNS_LOOKUP  // ipinfo.io
        TECH_STACK,     // requires: domain reachable (DNS_LOOKUP success) // BuiltWith + Wappalyzer
        SUBDOMAINS,     // requires: domain (DNS_LOOKUP success) // subfinder
        SUBDOMAIN_SCAN, // requires: domain (DNS_LOOKUP, SUBDOMAINS success, ) new
        SSL_CERT,       // requires: domain reachable (DNS_LOOKUP success) // crt.sh
        PERFORMANCE,    // requires: domain reachable (DNS_LOOKUP success) // curl timing metrics
        HEADERS,         // requires: domain reachable (DNS_LOOKUP success) // curl -I response headers
        DNS_HISTORY,     // ← new
        DIRECT_IP_SCAN   // ← new
    }

    // ── Task Statuses ─────────────────────────────────────────────────────────

    public enum ReconTaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED,    // dependency failed — auto skipped
        BLOCKED     // dependency not yet run
    }
}
