package com.migfora.sales.entity;

import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Investigation;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:31 PM
 */
@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private CompanyPlatform platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id")
    private Investigation investigation;

    // ── LLM metadata ──────────────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String content;          // full markdown content from LLM

    @Column(columnDefinition = "TEXT")
    private String summary;          // short 3-5 line summary

    private String aiProvider;       // qubrid | bedrock | migfora
    private String aiModel;          // model used
    private String language;         // en | ar
    private Integer tokenCount;      // estimated tokens used

    // ── S3 ────────────────────────────────────────────────────────────────────
    private String s3Key;            // S3 object key for PDF
    private String s3Bucket;         // S3 bucket name

    // ── Metadata ──────────────────────────────────────────────────────────────
    private String title;
    private String generatedBy;
    private String errorMessage;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime generatedAt;

    public enum ReportType {
        TECHNICAL_OVERVIEW,
        SALES_ROADMAP
    }

    public enum ReportStatus {
        PENDING,
        GENERATING,
        COMPLETED,
        FAILED
    }
}
