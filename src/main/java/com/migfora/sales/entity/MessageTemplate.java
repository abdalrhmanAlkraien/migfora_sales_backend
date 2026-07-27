package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:46 PM
 */
@Entity
@Table(name = "message_templates")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;           // internal name e.g. "AWS Migration Intro"

    private String subject;         // for emails — optional for LinkedIn

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;         // the message body

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType type;      // industry/content type

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoorType doorType;      // who it's aimed at

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateChannel channel; // LinkedIn, Email, WhatsApp, etc.

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TemplateLanguage language = TemplateLanguage.EN;

    private String tags;            // comma-separated e.g. "aws,cloud,migration"

    @Builder.Default
    private boolean active = true;

    private LocalDateTime lastUsedAt;

    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum TemplateType {
        EDUCATION,          // educational/awareness content
        ADVERTISING,        // promotional/ads
        SOFTWARE,           // software development related
        CLOUD,              // cloud/AWS related
        DEVOPS,             // DevOps and CI/CD
        SECURITY,           // cybersecurity
        AI,                 // AI/ML related
        GENERAL             // general outreach
    }

    public enum DoorType {
        MIGFORA_SHIELD,     // security/compliance offering
        PARTNER,            // partnership proposals
        PERFORMANCE,        // performance optimization
        IMPLEMENT_APP,      // application implementation
        CLOUD_MIGRATION,    // cloud migration offering
        MANAGED_SERVICES,   // managed services
        GENERAL             // general MIGFORA intro
    }

    public enum TemplateChannel {
        LINKEDIN,
        EMAIL,
        WHATSAPP,
        SMS,
        GENERAL             // works for any channel
    }

    public enum TemplateLanguage {
        EN,
        AR
    }
}