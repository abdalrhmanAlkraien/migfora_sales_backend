package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 12:15 AM
 */

@Entity
@Table(name = "follow_ups")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FollowUp {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FollowUpStatus status = FollowUpStatus.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime completedAt;

    private String notes;
    private String outcome;
    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum FollowUpType {
        CALL,           // Phone call
        VISIT,          // In-person visit
        MEETING,        // Online or in-person meeting
        EMAIL,          // Email outreach
        WHATSAPP,       // WhatsApp message
        LINKEDIN,       // LinkedIn message or connection request
        SMS,            // Text message
        PROPOSAL,       // Send a proposal or quote
        DEMO            // Product demo session
    }

    public enum FollowUpStatus {
        SCHEDULED,
        DONE,
        MISSED
    }
}
