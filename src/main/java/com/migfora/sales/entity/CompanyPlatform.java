package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 11:59 AM
 */
@Entity
@Table(name = "company_platforms")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyPlatform {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType type;

    @Column(nullable = false)
    private String name;

    private String url;
    private String domain;
    private String bundleId;
    private String appStoreUrl;
    private String playStoreUrl;
    private String description;
    private String technology;
    private String hostingProvider;
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlatformStatus status = PlatformStatus.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum PlatformType {
        WEBSITE,
        WEB_APP,
        MOBILE_APP,
        API,
        ADMIN_PANEL,
        E_COMMERCE,
        PORTAL,
        OTHER
    }

    public enum PlatformStatus {
        ACTIVE,
        INACTIVE,
        UNDER_DEVELOPMENT,
        DECOMMISSIONED
    }
}
