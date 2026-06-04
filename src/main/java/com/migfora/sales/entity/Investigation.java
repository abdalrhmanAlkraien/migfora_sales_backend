package com.migfora.sales.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private CompanyPlatform platform;

    public enum InvestigationStatus {
        OPEN,       // session created, tasks can be added and run
        CLOSED,     // manually closed by user
        ARCHIVED    // old session
    }
}
