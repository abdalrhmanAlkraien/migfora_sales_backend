package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:27 PM
 */
@Entity
@Table(name = "investigations")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Investigation {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String domain;

    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvestigationStatus status = InvestigationStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    private String triggeredBy;

    // Recon results stored as JSON columns
    @Column(columnDefinition = "TEXT")
    private String dnsRecords;

    @Column(columnDefinition = "TEXT")
    private String whoisData;

    @Column(columnDefinition = "TEXT")
    private String techStack;

    @Column(columnDefinition = "TEXT")
    private String openPorts;

    @Column(columnDefinition = "TEXT")
    private String subdomains;

    @Column(columnDefinition = "TEXT")
    private String sslInfo;

    @Column(columnDefinition = "TEXT")
    private String performanceMetrics;

    @Column(columnDefinition = "TEXT")
    private String rawFindings;

    private String errorMessage;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "investigation",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private InvestigationContext context;

    public enum InvestigationStatus {
        OPEN,       // session created, tasks can be added and run
        CLOSED,     // manually closed by user
        ARCHIVED    // old session
    }
}
