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
 * @Time: 3:21 PM
 */
@Entity
@Table(name = "contacts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String title;
    private String email;
    private String phone;
    private String linkedIn;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContactStatus status = ContactStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime nextFollowUpAt;
    private LocalDateTime lastFollowUpAt;

    public enum ContactStatus {
        NEW,
        CONTACTED,
        INTERESTED,
        MEETING_SET,
        PROPOSAL_SENT,
        NEGOTIATING,
        WON,
        LOST,
        ON_HOLD
    }
}
