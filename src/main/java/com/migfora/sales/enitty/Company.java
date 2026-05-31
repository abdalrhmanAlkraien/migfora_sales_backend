package com.migfora.sales.enitty;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:12 PM
 */
@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String domain;

    private String industry;
    private String country;
    private String city;
    private String website;
    private String size;           // STARTUP, SME, ENTERPRISE
    private String notes;

    @Column(nullable = false)
    private String createdBy;      // Cognito sub of the sales engineer

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.PROSPECT;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum CompanyStatus {
        PROSPECT, CONTACTED, QUALIFIED, PROPOSAL, CLOSED_WON, CLOSED_LOST
    }
}
